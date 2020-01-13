package org.eclipse.scout.mojo.eclipse.settings;

public class EclipseSettingsFile {
  private String name;
  private String location;
  private Boolean failIfMissing;
  private String packagings;

  public Boolean getFailIfMissing() {
    return failIfMissing;
  }

  public void setFailIfMissing(final Boolean failIfMissing) {
    this.failIfMissing = failIfMissing;
  }

  public String getPackagings() {
    return packagings;
  }

  public void setPackagings(final String packagings) {
    this.packagings = packagings;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }
}
