package org.eclipse.scout.mojo.eclipse.settings;

import static org.eclipse.scout.mojo.eclipse.settings.MavenUtils.formatInputLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.InputLocation;
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
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "eclipse-settings", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class ProjectSettingsConfigurator extends AbstractMojo {
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
        getLog().info("Skipping project settings configuration.");
      } else if (configureEclipseMeta()) {
        getLog().info("Project configured.");
      } else {
        getLog().error("Project not configured.");
      }
    } catch (final IOException e) {
      getLog().error("Failure during settings configuration", e);
    }
  }

  private boolean configureEclipseMeta() throws IOException, MojoExecutionException {
    if (additionalConfig == null || additionalConfig.length <= 0) {
      getLog().warn("No settings specified.");
      return false;
    }

    validateMojo();
    try (ResourceResolver resolver = getResourceResolver()) {
      EclipseSettingsFile[] filteredAdditionalConfig = filterAdditionalConfig();
      writeAdditionalConfig(resolver, filteredAdditionalConfig);
      return true;
    }
  }

  private ResourceResolver getResourceResolver() throws IOException, MojoExecutionException {
    ResourceResolver resolver;
    String prefix;
    if (source.equals("jar") || source.startsWith("jar:")) {
      prefix = source.equals("jar") ? "" : source.substring("jar:".length());
      resolver = new ArtifactResourceResolver(new ArtifactCollector(project, getLog(), plugin).collect(), prefix);
    } else if (source.equals("file") || source.startsWith("file:")) {
      prefix = source.equals("file") ? "" : source.substring("file:".length());
      resolver = new FileSystemResourceResolver(
          prefix.isEmpty() ? project.getBasedir() : new File(prefix).getAbsoluteFile());
    } else {
      throw new MojoExecutionException("<source> does not start with 'jar:' or 'file:' ");
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug("Resolving file using " + resolver);
    }
    return resolver;
  }

  private void validateMojo() throws MojoExecutionException {
    this.source = StringUtils.trimToNull(source);
    if (source == null) {
      throw new MojoExecutionException("<source> is missing.");
    }

    boolean fail = false;
    int index = 0;
    for (EclipseSettingsFile config : additionalConfig) {
      config.setLocation(StringUtils.trimToNull(config.getLocation()));
      config.setName(StringUtils.trimToNull(config.getName()));
      if (config.getFailOnError() == null) {
        config.setFailOnError(failOnError);
      }

      if (config.getLocation() == null) {
        getLog().error(
                       "Invalid <location> in AdditionalConfig[" + index + "]"
                           + formatInputLocation(config.getInputLocation()) + ".");
        fail = true;
      }
      if (config.getName() == null) {
        getLog().error(
                       "Invalid <name> in AdditionalConfig[" + index + "]"
                           + formatInputLocation(config.getInputLocation()) + ".");
        fail = true;
      }
      ++index;
    }

    if (fail) {
      throw new MojoExecutionException("One or more errors were reported");
    }
  }

  private EclipseSettingsFile[] filterAdditionalConfig() {
    final PackagingFilter packagingFilter = PackagingFilter.newPackagingFilter(packagings);
    final String projectPackaging = project.getPackaging();

    List<EclipseSettingsFile> files = new ArrayList<>(additionalConfig.length);
    for (final EclipseSettingsFile config : additionalConfig) {
      final PackagingFilter localPackagingFilter = packagingFilter.join(config.getPackagings());
      if (!localPackagingFilter.test(projectPackaging)) {
        if (getLog().isDebugEnabled()) {
          getLog().debug(
                         "Ignoring " + config.getLocation() + " -> " + config.getName() + " because packaging '"
                             + projectPackaging + "' is filtered by " + localPackagingFilter);
        }
        continue;
      }
      files.add(config);
    }

    if (files.size() != additionalConfig.length) {
      getLog().info("Filtered " + (additionalConfig.length - files.size()) + " files due to packaging.");
      return files.toArray(new EclipseSettingsFile[0]);
    }
    return additionalConfig;
  }

  private void writeAdditionalConfig(final ResourceResolver resolver, final EclipseSettingsFile[] additionalConfig)
      throws MojoExecutionException {
    getLog().info("Will copy " + additionalConfig.length + " using " + resolver);

    final List<IOException> failures = new ArrayList<>();
    final File basedir = project.getBasedir();

    List<File> updatedFiles = new ArrayList<>();
    for (final EclipseSettingsFile config : additionalConfig) {
      final String location = config.getLocation();
      final File target = new File(basedir, config.getName());
      try {
        writeAdditionalConfig(resolver, config.getLocation(), target);
        updatedFiles.add(target);
      } catch (final IOException e) {
        addMessage(config, copyFailure(config, location, target, e), e);
        if (config.isFailOnError()) {
          failures.add(e);
        }
      }
    }

    if (!failures.isEmpty()) {
      final MojoExecutionException ex = new MojoExecutionException("Unable to copy " + failures.size() + " files");
      for (final IOException e : failures) {
        ex.addSuppressed(e);
      }
      throw ex;
    }
    for (File updatedFile : updatedFiles) {
      buildContext.refresh(updatedFile);
    }
  }

  private void addMessage(EclipseSettingsFile config, String message, IOException e) {
    int severity = config.isFailOnError() ? BuildContext.SEVERITY_ERROR : BuildContext.SEVERITY_WARNING;
    buildContext.addMessage(project.getFile(), 0, 0, message, severity, e);
  }

  private String copyFailure(EclipseSettingsFile config, String location, File target, IOException e) {
    InputLocation inputLocation = config.getInputLocation();
    return "Could not copy <" + location + "> to <" + target.getAbsolutePath() + ">"
        + formatInputLocation(inputLocation) + ": " + e.getMessage();
  }

  private void writeAdditionalConfig(ResourceResolver resolver, String location, File target) throws IOException {
    final Resource source = resolver.getResource(location);
    if (source == null) {
      throw new FileNotFoundException("Missing resource: " + location + " using " + resolver);
    }

    /*
     * if we try to write to a directory, this will fail. We don't try to behave like mv which
     * relocate the source into the folder (eg: mv a b will give b/a).
     */
    if (target.isDirectory()) {
      getLog().warn(target.getAbsolutePath() + " is a directory, ignoring.");
      return;
    }
    target.getParentFile().mkdirs();

    getLog().info("Copying " + source + " to " + target);
    try (InputStream inStream = source.getResourceAsStream();
        OutputStream outStream = new FileOutputStream(target)) {
      IOUtil.copy(inStream, outStream);
    }

  }

}
