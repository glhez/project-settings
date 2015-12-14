package org.eclipse.scout.mojo.eclipse.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "eclipse-settings", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ProjectSettingsConfigurator extends AbstractMojo {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectSettingsConfigurator.class);

    /**
     * The Maven Project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The Plugin Descriptor
     */
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    /**
     * The maven-eclipse-plugin allows you to move settings files from one
     * location to another. You use that to put each configuration file from
     * your settings JAR in the right location:
     *
     * <pre>
     * &lt;plugin&gt;
     *     &lt;...&gt;
     *     &lt;configuration&gt;
     *         &lt;additionalConfig&gt;
     *             &lt;file&gt;
     *                 &lt;name&gt;.settings/org.eclipse.jdt.core.prefs&lt;/name&gt;
     *                 &lt;location&gt;/org.eclipse.jdt.core.prefs&lt;/location&gt;
     *             &lt;/file&gt;
     *             &lt;file&gt;
     *                 &lt;name&gt;.settings/org.eclipse.jdt.ui.prefs&lt;/name&gt;
     *                &lt;location&gt;/org.eclipse.jdt.ui.prefs&lt;/location&gt;
     *             &lt;/file&gt;
     *             &lt;!-- and more... --&gt;
     *         &lt;/additionalConfig&gt;
     *     &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     */
    @Parameter
    private EclipseSettingsFile[] additionalConfig;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (configureEclipseMeta()) {
                LOGGER.info("Project configured.");
            } else {
                LOGGER.error("Project not configured.");
            }
        } catch (IOException e) {
            LOGGER.error("Failure during settings configuration", e);
        }
    }

    private boolean configureEclipseMeta() throws IOException, MojoExecutionException {
        if (additionalConfig == null || additionalConfig.length <= 0) {
            LOGGER.warn("No settings specified.");
            return false;
        }

        List<Artifact> artifacts = collectArtifacts();
        List<JarFile> jarFiles = JarFileUtil.resolveJar(artifacts);

        writeAdditionalConfig(jarFiles);

        return true;
    }

    private List<Artifact> collectArtifacts() {
        List<Artifact> artifacts = new ArrayList<>();

        PluginManagement pluginManagement = project.getBuild().getPluginManagement();
        if (pluginManagement != null) {
            artifacts
                    .addAll(getEclipseProjectSettingsPluginDependenciesAsArtifacts(pluginManagement.getPluginsAsMap()));
        }

        artifacts.addAll(getEclipseProjectSettingsPluginDependenciesAsArtifacts(project.getBuild().getPluginsAsMap()));

        return artifacts;
    }

    private List<Artifact> getEclipseProjectSettingsPluginDependenciesAsArtifacts(Map<String, Plugin> plugins) {
        List<Artifact> artifacts = new ArrayList<>();

        Plugin eclipseSettingsPlugin = plugins.get(plugin.getGroupId() + ":" + plugin.getArtifactId());
        if (eclipseSettingsPlugin != null) {
            for (Dependency dep : eclipseSettingsPlugin.getDependencies()) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                artifacts.add(plugin.getArtifactMap().get(depKey));
            }
        }
        return artifacts;
    }

    private void writeAdditionalConfig(List<JarFile> jarFiles) throws MojoExecutionException {
        if (additionalConfig != null) {
            for (EclipseSettingsFile file : additionalConfig) {
                File projectRelativeFile = new File(project.getBasedir(), file.getName());
                if (projectRelativeFile.isDirectory()) {
                    // just ignore?
                    LOGGER.warn(MessageFormat.format("{0} is a directory, ignoring.",
                            projectRelativeFile.getAbsolutePath()));
                }

                projectRelativeFile.getParentFile().mkdirs();
                try (InputStream inStream = openStream(file.getLocation(), jarFiles)) {
                    if (inStream != null) {
                        try (OutputStream outStream = new FileOutputStream(projectRelativeFile)) {
                            IOUtil.copy(inStream, outStream);
                        }
                    } else {
                        LOGGER.warn(
                                MessageFormat.format("No file found at {0}", projectRelativeFile.getAbsolutePath()));
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(MessageFormat.format("Unable to write to file: {0}",
                            projectRelativeFile.getAbsolutePath()));
                }
            }
        }
    }

    private InputStream openStream(String filePath, List<JarFile> jarFiles) throws IOException {
        if (filePath.startsWith("/"))
            filePath = filePath.substring(1);

        for (JarFile jarFile : jarFiles) {
            ZipEntry entry = jarFile.getEntry(filePath);
            if (entry != null) {
                return jarFile.getInputStream(entry);
            }
        }
        LOGGER.warn("Entry " + filePath + " not found in " + jarFiles);
        return null;
    }
}
