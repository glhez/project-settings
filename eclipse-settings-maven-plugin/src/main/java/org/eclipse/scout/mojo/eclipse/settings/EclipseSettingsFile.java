package org.eclipse.scout.mojo.eclipse.settings;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;

public class EclipseSettingsFile implements InputLocationTracker {
  private java.util.Map<Object, InputLocation> locations;
  private String name;
  private String location;
  private Boolean failOnError;
  private String packagings;

  /**
   * Fail if this resource could not be copied.
   */
  public Boolean getFailOnError() {
    return failOnError;
  }

  public boolean isFailOnError() {
    return failOnError == null || failOnError.booleanValue();
  }

  public void setFailOnError(final Boolean failOnError) {
    this.failOnError = failOnError;
  }

  /**
   * Get list of packagings this apply too.
   * <p>
   * The list is space separated, and an exclusion can be done using {@code !}.
   */
  public String getPackagings() {
    return packagings;
  }

  public void setPackagings(final String packagings) {
    this.packagings = packagings;
  }

  /**
   * Name of the resource to copy.
   * <P>
   * The name is relative to the {@code project.basedir} and correspond to a file to be written.
   */
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Get the location in the JAR/Files.
   */
  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  public InputLocation getInputLocation() {
    InputLocation nameLocation = getLocation("name");
    if (nameLocation != null) {
      return nameLocation;
    }
    InputLocation locationLocation = getLocation("location");
    if (locationLocation != null) {
      return locationLocation;
    }
    return getLocation("");
  }

  @Override
  public InputLocation getLocation(Object field) {
    return locations != null ? locations.get(field) : null;
  }

  @Override
  public void setLocation(Object field, InputLocation location) {
    if (location != null) {
      if (this.locations == null) {
        this.locations = new java.util.LinkedHashMap<>();
      }
      this.locations.put(field, location);
    }
  }
}
