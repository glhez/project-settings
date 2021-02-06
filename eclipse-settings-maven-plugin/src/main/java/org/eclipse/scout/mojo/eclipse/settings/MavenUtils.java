package org.eclipse.scout.mojo.eclipse.settings;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;

public class MavenUtils {
  private MavenUtils() {
    // ...
  }

  public static InputLocation getInputLocation(org.apache.maven.model.Plugin plugin) {
    InputLocation location = plugin.getLocation("");
    if (location != null) {
      return null;
    }
    return getInputLocation(plugin, "groupId", "artifactId", "version");
  }

  public static InputLocation getInputLocation(org.apache.maven.model.Dependency dependency) {
    InputLocation location = dependency.getLocation("");
    if (location != null) {
      return null;
    }
    return getInputLocation(dependency, "groupId", "artifactId", "version");
  }

  public static InputLocation getInputLocation(InputLocationTracker tracker, String... fields) {
    InputLocation initial = null;
    for (String field : fields) {
      initial = InputLocation.merge(initial, tracker.getLocation(field), true);
    }
    return initial;
  }

  public static String formatInputLocation(InputLocation location) {
    return formatInputLocation(" @ %s", "", location);
  }

  public static String formatInputLocation(String format, InputLocation location) {
    return formatInputLocation(format, "", location);
  }

  public static String formatInputLocation(String format, String missingFormat, InputLocation location) {
    if (location == null) {
      return missingFormat;
    }
    String s = formatInputSource(location.getSource());
    if (s == null) {
      return missingFormat;
    }
    if (location.getLineNumber() > 0) {
      s += ":" + Integer.toString(location.getLineNumber());
      if (location.getColumnNumber() > 0) {
        s += ":" + Integer.toString(location.getColumnNumber());
      }
    }
    return String.format(format, s);
  }

  private static String formatInputSource(InputSource source) {
    if (source == null) {
      return null;
    }
    if (source.getLocation() != null) {
      return source.getLocation();
    }
    if (source.getModelId() != null) {
      return "pom://" + source.getModelId();
    }
    return null;
  }
}
