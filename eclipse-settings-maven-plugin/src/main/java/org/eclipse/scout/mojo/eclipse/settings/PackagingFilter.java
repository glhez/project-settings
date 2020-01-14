package org.eclipse.scout.mojo.eclipse.settings;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Filter packaging based on a predicate.
 *
 * @author gael.lhez
 *
 */
public class PackagingFilter implements Predicate<String> {
  private final Set<String> excluded;
  private final Set<String> included;

  private PackagingFilter(final Set<String> excluded, final Set<String> included) {
    this.excluded = c(excluded);
    this.included = c(included);
  }

  private static Set<String> c(final Set<String> s) {
    return s == null || s.isEmpty() ? emptySet() : Collections.unmodifiableSet(s);
  }

  public Set<String> getExcluded() {
    return excluded;
  }

  public Set<String> getIncluded() {
    return included;
  }

  public static PackagingFilter newPackagingFilter(final String filter) {
    final String[] packagings = StringUtils.split(filter);
    if (null == packagings) {
      return new PackagingFilter(null, null);
    }
    final Set<String> excluded = new HashSet<>();
    final Set<String> included = new HashSet<>();
    for (final String packaging : packagings) {
      if (packaging.startsWith("!")) {
        excluded.add(packaging.substring(1));
      } else {
        included.add(packaging);
      }
    }
    return new PackagingFilter(excluded, included);
  }

  public boolean isEmpty() {
    return excluded.isEmpty() && included.isEmpty();
  }

  public PackagingFilter join(final String localPackagingFilter) {
    final PackagingFilter other = newPackagingFilter(localPackagingFilter);
    if (other.isEmpty()) {
      return this;
    }
    return other;
  }

  @Override
  public boolean test(final String packaging) {
    if (excluded.contains(packaging)) {
      return false;
    }
    if (included.isEmpty()) {
      return true;
    }
    return included.contains(packaging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(excluded, included);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PackagingFilter other = (PackagingFilter) obj;
    return Objects.equals(excluded, other.excluded)
        && Objects.equals(included, other.included);
  }

  @Override
  public String toString() {
    if (excluded.isEmpty() && included.isEmpty()) {
      return "";
    }
    return Stream.concat(excluded.stream().map(s -> "!" + s), included.stream()).sorted().collect(joining(" "));
  }

}
