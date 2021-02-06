package org.eclipse.scout.mojo.eclipse.settings;

import static java.util.stream.Collectors.toCollection;
import static org.eclipse.scout.mojo.eclipse.settings.MavenUtils.formatInputLocation;
import static org.eclipse.scout.mojo.eclipse.settings.MavenUtils.getInputLocation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class ArtifactCollector {
  private final MavenProject project;
  private final Log log;
  private final PluginDescriptor pluginDescriptor;
  private final Plugin plugin;
  private final ConcurrentMap<Artifact, InputLocation> locations;
  private final Map<String, Artifact> artifactMap;
  private final boolean trackLocation;

  public ArtifactCollector(MavenProject project, Log log, PluginDescriptor pluginDescriptor) {
    this.project = project;
    this.log = log;
    this.pluginDescriptor = pluginDescriptor;
    this.plugin = this.pluginDescriptor.getPlugin();
    this.artifactMap = pluginDescriptor.getArtifactMap();
    this.trackLocation = log.isDebugEnabled();
    this.locations = new ConcurrentHashMap<>();
  }

  public Collection<Artifact> collect() {
    Collection<Artifact> artifacts = this.collectPlugins()
                                         .filter(this::pluginFilter)
                                         .flatMap(plugin -> plugin.getDependencies().stream())
                                         .map(this::toArtifact)
                                         .filter(Objects::nonNull)
                                         .collect(toCollection(LinkedHashSet::new));

    if (log.isDebugEnabled()) {
      log.debug("Resolved " + artifacts.size() + " artifacts");
      for (final Artifact artifact : artifacts) {
        log.debug("  " + artifact + formatInputLocation(locations.get(artifact)) + ".");
      }
    }
    if (artifacts.isEmpty() && log.isWarnEnabled()) {
      log.warn(
               "Could not find dependencies attached to plugin " + pluginDescriptor
                   + formatInputLocation(getInputLocation(pluginDescriptor.getPlugin())) + ".");
    }
    return artifacts;

  }

  private boolean pluginFilter(final Plugin plugin) {
    return this.plugin.getKey().equals(plugin.getKey());
  }

  private Artifact toArtifact(Dependency dependency) {
    String key = ArtifactUtils.versionlessKey(dependency.getGroupId(), dependency.getArtifactId());
    Artifact artifact = artifactMap.get(key);
    if (artifact == null) {
      if (log.isWarnEnabled()) {
        log.warn("Missing dependency " + key + formatInputLocation(getInputLocation(dependency)) + ".");
      }
      return null;
    }
    if (trackLocation) {
      locations.putIfAbsent(artifact, getInputLocation(dependency));
    }
    return artifact;
  }

  private Stream<Plugin> collectPlugins() {
    Stream<Plugin> a = toPluginStream(project.getBuild().getPluginManagement(), PluginManagement::getPlugins);
    Stream<Plugin> b = toPluginStream(project, MavenProject::getBuildPlugins);
    if (a == null) {
      return b == null ? Stream.empty() : b;
    }
    if (b == null) {
      return a;
    }
    return Stream.concat(a, b);
  }

  private <T> Stream<Plugin> toPluginStream(T element, Function<T, List<Plugin>> pluginExtractor) {
    if (element == null) {
      return null;
    }
    return pluginExtractor.apply(element).stream();
  }

}
