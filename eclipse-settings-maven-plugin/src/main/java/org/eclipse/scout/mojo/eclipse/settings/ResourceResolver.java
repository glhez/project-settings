package org.eclipse.scout.mojo.eclipse.settings;

import java.io.IOException;

/**
 * Resolve resource.
 *
 * @author gael.lhez
 */
public interface ResourceResolver extends AutoCloseable {
  /**
   * Get some resource.
   *
   * @param path
   *          path to the resource
   * @return return a {@link Resource} or {@code null} if not found.
   */
  Resource getResource(String path);

  @Override
  void close() throws IOException;
}
