package org.eclipse.scout.mojo.eclipse.settings;

import java.io.IOException;

public class EclipseSettingMissingIOException extends IOException {
  private static final long serialVersionUID = 3_2_0L;

  public EclipseSettingMissingIOException() {
    super();
  }

  public EclipseSettingMissingIOException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public EclipseSettingMissingIOException(final String message) {
    super(message);
  }

  public EclipseSettingMissingIOException(final Throwable cause) {
    super(cause);
  }

}
