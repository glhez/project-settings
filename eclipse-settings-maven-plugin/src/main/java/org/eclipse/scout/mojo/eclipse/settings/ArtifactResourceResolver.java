package org.eclipse.scout.mojo.eclipse.settings;

import static java.util.stream.Collectors.joining;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;

public class ArtifactResourceResolver implements ResourceResolver {
  private final List<ArtifactJarFile> artifacts;
  private final String prefix;

  public ArtifactResourceResolver(final List<Artifact> artifacts, final String prefix) throws IOException {
    this.artifacts = newArtifactJarFiles(artifacts);
    this.prefix = p(StringUtils.trimToNull(prefix));
  }

  private static String p(final String prefix) {
    if (prefix == null || prefix.endsWith("/")) {
      return prefix;
    }
    return prefix + "/";
  }

  @Override
  public Resource getResource(final String resource) throws IOException {
    String path;
    if (resource.startsWith("/")) {
      path = resource.substring(1);
    } else if (prefix != null) {
      path = prefix + "/" + resource;
    } else {
      path = resource;
    }

    for (final ArtifactJarFile artifact : this.artifacts) {
      final JarEntry entry = artifact.getJarEntry(path);
      if (null != entry) {
        return new ArtifactResourceImpl(artifact, entry);
      }
    }
    return null;
  }

  @Override
  public String toString() {
    final String as = artifacts.stream().map(ArtifactJarFile::getArtifact).map(Artifact::toString)
        .collect(joining(",", "[", "]"));
    return "jar:" + (prefix != null ? prefix : "") + " artifacts: " + as;
  }

  private static List<ArtifactJarFile> newArtifactJarFiles(final List<Artifact> artifacts) throws IOException {
    final List<ArtifactJarFile> artifactJarFiles = new ArrayList<>(artifacts.size());
    for (final Artifact artifact : artifacts) {
      artifactJarFiles.add(new ArtifactJarFile(artifact));
    }
    return artifactJarFiles;
  }

  private static class ArtifactResourceImpl implements Resource {
    private final ArtifactJarFile artifactJarFile;
    private final JarEntry entry;

    public ArtifactResourceImpl(final ArtifactJarFile artifactJarFile, final JarEntry entry) {
      this.artifactJarFile = artifactJarFile;
      this.entry = entry;
    }

    @Override
    public InputStream getResourceAsStream() throws IOException {
      return artifactJarFile.getInputStream(entry);
    }

    @Override
    public String toString() {
      return artifactJarFile.artifact.toString() + "@" + entry;
    }

  }

  private static class ArtifactJarFile extends JarFile {
    private final Artifact artifact;

    private ArtifactJarFile(final Artifact artifact) throws IOException {
      super(artifact.getFile());
      this.artifact = artifact;
    }

    public Artifact getArtifact() {
      return artifact;
    }

  }
}
