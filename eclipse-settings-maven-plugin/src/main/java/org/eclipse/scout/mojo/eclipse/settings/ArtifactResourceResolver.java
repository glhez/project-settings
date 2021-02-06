package org.eclipse.scout.mojo.eclipse.settings;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;

public class ArtifactResourceResolver implements ResourceResolver {
  private static final String SEPARATOR = "/";

  private final List<ArtifactJarFile> artifacts;
  private final String prefix;
  private boolean closed;

  public ArtifactResourceResolver(final Collection<Artifact> artifacts, final String prefix) throws IOException {
    this.artifacts = newArtifactJarFiles(artifacts);
    this.prefix = p(StringUtils.trimToNull(prefix));
  }

  private static String p(final String prefix) {
    if (prefix == null || prefix.endsWith(SEPARATOR)) {
      return prefix;
    }
    return prefix + SEPARATOR;
  }

  @Override
  public Resource getResource(final String resource) {
    if (closed) {
      throw new IllegalStateException("ArtifactResourceResolver is closed");
    }
    String path;
    if (resource.startsWith(SEPARATOR)) {
      path = resource.substring(1);
    } else if (prefix != null) {
      path = prefix + SEPARATOR + resource;
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

  @Override
  public void close() throws IOException {
    IOException ioException = null;
    for (ArtifactJarFile jar : this.artifacts) {
      try {
        jar.close();
      } catch (IOException e) {
        if (ioException == null) {
          ioException = e;
        } else {
          ioException.addSuppressed(e);
        }
      }
    }
    this.closed = true;
    this.artifacts.clear();
    if (ioException != null) {
      throw ioException;
    }
  }

  private static List<ArtifactJarFile> newArtifactJarFiles(final Collection<Artifact> artifacts) throws IOException {
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

  private static class ArtifactJarFile implements AutoCloseable {
    private final JarFile jarFile;
    private final Artifact artifact;

    private ArtifactJarFile(final Artifact artifact) throws IOException {
      this.artifact = artifact;
      this.jarFile = new JarFile(artifact.getFile());
    }

    public InputStream getInputStream(JarEntry entry) throws IOException {
      return jarFile.getInputStream(entry);
    }

    public JarEntry getJarEntry(String path) {
      return jarFile.getJarEntry(path);
    }

    public Artifact getArtifact() {
      return artifact;
    }

    @Override
    public void close() throws IOException {
      jarFile.close();
    }

  }
}
