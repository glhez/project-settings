package org.eclipse.scout.mojo.eclipse.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "eclipse-settings", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.NONE)
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
   * Fail the build if the file could not be copied (for example: missing, target could not be
   * overriden, ...).
   * <p>
   * This may be overriding per file.
   */
  @Parameter(defaultValue = "true", readonly = false, required = false)
  private boolean failOnError;

  /**
   * List of packaging to filter from these settings.
   * <p>
   * List must be space separated.
   * <p>
   * Default is {@code !pom}, meaning "pom" packaging is excluded.
   */
  @Parameter(defaultValue = "!pom", readonly = false, required = false)
  private String packagings;

  /**
   * Where are file to be copied found?
   * <p>
   * The plugin provide two way of locating file:
   * <ul>
   * <li>From dependencies, using {@code jar:}.</li>
   * <li>From file system, using {@code file:}.</li>
   * </ol>
   * After the prefix, a path may be provided: this will be prepend to each location before
   * searching them.
   */
  @Parameter(defaultValue = "jar:/", readonly = false, required = false)
  private String source;

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
   *                 &lt;location&gt;/org.eclipse.jdt.ui.prefs&lt;/location&gt;
   *                 &lt;packagings&gt;jar&lt;/packagings&gt;
   *                 &lt;failOnError&gt;false&lt;failOnError&gt;
   *             &lt;/file&gt;
   *             &lt;!-- and more... --&gt;
   *         &lt;/additionalConfig&gt;
   *     &lt;/configuration&gt;
   * &lt;/plugin&gt;
   * </pre>
   */
  @Parameter
  private EclipseSettingsFile[] additionalConfig;

  /**
   * Set this to <code>true</code> to bypass project settings configuration.
   */
  @Parameter
  private boolean skip;

  @Component
  private BuildContext buildContext;

  public void setSkip(final boolean skip) {
    this.skip = skip;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      if (skip) {
        LOGGER.info("Skipping project settings configuration.");
      } else if (configureEclipseMeta()) {
        LOGGER.info("Project configured.");
      } else {
        LOGGER.error("Project not configured.");
      }
    } catch (final IOException e) {
      LOGGER.error("Failure during settings configuration", e);
    }
  }

  private boolean configureEclipseMeta() throws IOException, MojoExecutionException {
    if (additionalConfig == null || additionalConfig.length <= 0) {
      LOGGER.warn("No settings specified.");
      return false;
    }

    source = StringUtils.trimToNull(source);
    if (source == null) {
      throw new MojoExecutionException("<source> is missing.");
    }

    ResourceResolver resolver;
    String prefix;
    if (source.equals("jar") || source.startsWith("jar:")) {
      final List<Artifact> artifacts = collectArtifacts();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Resolved {} artifacts", artifacts.size());
        for (final Artifact artifact : artifacts) {
          LOGGER.debug("  {}", artifact);
        }
      }
      if (artifacts.isEmpty()) {
        LOGGER.warn("Could not find dependencies attached to this plugin.");
      }
      prefix = source.equals("jar") ? "" : source.substring("jar:".length());
      resolver = new ArtifactResourceResolver(artifacts, prefix);
    } else if (source.equals("file") || source.startsWith("file:")) {
      prefix = source.equals("file") ? "" : source.substring("file:".length());
      resolver = new FileSystemResourceResolver(
          prefix.isEmpty() ? project.getBasedir() : new File(prefix).getAbsoluteFile());
    } else {
      throw new MojoExecutionException("<source> does not start with 'jar:' or 'file:' ");
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Resolving file using {}", resolver);
    }
    writeAdditionalConfig(additionalConfig, resolver);
    return true;
  }

  private List<Artifact> collectArtifacts() {
    LOGGER.debug("Collecting artifacts (dependencies) to use");
    final List<Plugin> plugins = collectPlugins();

    final Map<String, Artifact> artifactMap = plugin.getArtifactMap();
    final Set<Artifact> artifacts = new LinkedHashSet<>();
    for (final Plugin plugin : plugins) {
      if (pluginFilter(plugin)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Checking {} dependencies...", plugin);
          printInputLocation(plugin);
        }
        for (final Dependency dependency : plugin.getDependencies()) {
          final String versionlessKey = ArtifactUtils.versionlessKey(dependency.getGroupId(),
              dependency.getArtifactId());
          final Artifact artifact = artifactMap.get(versionlessKey);
          if (null != artifact) {
            if (artifacts.add(artifact)) {
              LOGGER.debug("++ adding {}: {} to plugin artifacts...", versionlessKey, artifact);
            } else {
              LOGGER.debug("++ ignoring {}: {} (already added)", versionlessKey, artifact);
            }
          }
        }
      }
    }

    return new ArrayList<>(artifacts);
  }

  private List<Plugin> collectPlugins() {
    final List<Plugin> plugins = new ArrayList<>();
    final PluginManagement pluginManagement = project.getBuild().getPluginManagement();
    if (null != pluginManagement) {
      plugins.addAll(pluginManagement.getPlugins());
    }
    plugins.addAll(project.getBuildPlugins());
    return plugins;
  }

  private void printInputLocation(final Plugin plugin) {
    if (LOGGER.isDebugEnabled()) {
      final InputLocation loc = computeInputLocation(plugin);
      if (null != loc) {
        LOGGER.debug("Located at: {}:{}:{}", loc.getSource(), loc.getLineNumber(), loc.getColumnNumber());
      }
    }
  }

  private InputLocation computeInputLocation(final Plugin plugin) {
    final InputLocation gil = plugin.getLocation("groupId");
    final InputLocation ail = plugin.getLocation("artifactId");
    final InputLocation vil = plugin.getLocation("version");

    InputLocation r = InputLocation.merge(null, gil, true);
    r = InputLocation.merge(r, ail, true);
    r = InputLocation.merge(r, vil, true);
    return r;
  }

  private boolean pluginFilter(final Plugin plugin) {
    return this.plugin.getPlugin().getKey().equals(plugin.getKey());
  }

  private void writeAdditionalConfig(final EclipseSettingsFile[] additionalConfig, final ResourceResolver resolver)
      throws MojoExecutionException {
    if (additionalConfig == null || additionalConfig.length == 0) {
      return;
    }
    LOGGER.info("Copying {} resources using {} resolver.", additionalConfig.length, resolver);

    final List<File> updatedFiles = new ArrayList<>();
    final List<IOException> failures = new ArrayList<>();
    int missingFileError = 0;

    final PackagingFilter packagingFilter = PackagingFilter.newPackagingFilter(packagings);
    final String projectPackaging = project.getPackaging();
    final File basedir = project.getBasedir();

    try {

      for (final EclipseSettingsFile config : additionalConfig) {
        final String location = getOrFail(config.getLocation(), "location");
        final String name = getOrFail(config.getName(), "name");
        final PackagingFilter localPackagingFilter = packagingFilter.join(config.getPackagings());

        if (!localPackagingFilter.test(projectPackaging)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ignoring {} -> {} because packaging '{}' is filtered by {}", location, name, projectPackaging,
                localPackagingFilter);
          }
          continue;
        }
        final boolean failOnError = getBooleanOrInherit(config.getFailOnError(), this.failOnError);
        final File target = new File(basedir, name);

        try {
          final Resource source = resolver.getResource(location);
          if (null == source) {
            if (failOnError) {
              LOGGER.error("Source file {} does not exists.", location);
              ++missingFileError;
            } else {
              LOGGER.warn("Source file {} does not exists.", location);
            }
            continue;
          }

          /*
           * if we try to write to a directory, this will fail. We don't try to behave like mv which
           * relocate the
           * source into the folder (eg: mv a b will give b/a).
           */
          if (target.isDirectory()) {
            LOGGER.warn("{} is a directory, ignoring.", target.getAbsolutePath());
            continue;
          }

          target.getParentFile().mkdirs();

          LOGGER.info("Copying {} to {}", source, target);
          try (InputStream inStream = source.getResourceAsStream();
              OutputStream outStream = new FileOutputStream(target)) {
            IOUtil.copy(inStream, outStream);
          }
          updatedFiles.add(target);
        } catch (final IOException e) {
          if (failOnError) {
            LOGGER.error("Could not copy {} to {}", location, target.getAbsolutePath(), e);
            failures.add(e);
          } else {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.warn("Could not copy {} to {}", location, target.getAbsolutePath(), e);
            } else {
              LOGGER.warn("Could not copy {} to {} ({})", location, target.getAbsolutePath(), e.getMessage());
            }
          }
        }
      }

      if (!failures.isEmpty() || missingFileError > 0) {
        final MojoExecutionException ex = new MojoExecutionException(
            "Unable to copy " + (failures.size() + missingFileError) + " files");
        for (final IOException e : failures) {
          ex.addSuppressed(e);
        }
        throw ex;
      }

    } finally {
      for (final File file : updatedFiles) {
        buildContext.refresh(file);
      }
    }
  }

  private String getOrFail(final String value, final String tagName) throws MojoExecutionException {
    if (StringUtils.isBlank(value)) {
      throw new MojoExecutionException("No <" + tagName + " > for some file, please fix it");
    }
    return value;
  }

  private boolean getBooleanOrInherit(final Boolean local, final boolean parent) {
    if (local == null) {
      return parent;
    }
    return local.booleanValue();
  }
}
