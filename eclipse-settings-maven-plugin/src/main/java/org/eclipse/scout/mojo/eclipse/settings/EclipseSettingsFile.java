package org.eclipse.scout.mojo.eclipse.settings;

public class EclipseSettingsFile {
  private String name;
  private String location;
  private Boolean failOnError;
  private String packagings;

  public Boolean getFailOnError() {
    return failOnError;
  }

  public void setFailOnError(final Boolean failOnError) {
    this.failOnError = failOnError;
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
