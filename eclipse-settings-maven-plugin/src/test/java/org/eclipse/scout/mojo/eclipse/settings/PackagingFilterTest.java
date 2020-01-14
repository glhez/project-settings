package org.eclipse.scout.mojo.eclipse.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PackagingFilterTest {

  @Test
  public void testNewPackagingFilter() {
    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter(null);

      assertThat(pf).isNotNull();
      assertThat(pf.isEmpty()).isTrue();
      assertThat(pf.getExcluded()).isEmpty();
      assertThat(pf.getIncluded()).isEmpty();
      assertThat(pf).hasToString("");
    }
    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("");

      assertThat(pf).isNotNull();
      assertThat(pf.isEmpty()).isTrue();
      assertThat(pf.getExcluded()).isEmpty();
      assertThat(pf.getIncluded()).isEmpty();
      assertThat(pf).hasToString("");
    }
    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("a");

      assertThat(pf).isNotNull();
      assertThat(pf.isEmpty()).isFalse();
      assertThat(pf.getExcluded()).isEmpty();
      assertThat(pf.getIncluded()).contains("a");
      assertThat(pf).hasToString("a");
    }
    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("!a");

      assertThat(pf).isNotNull();
      assertThat(pf.isEmpty()).isFalse();
      assertThat(pf.getExcluded()).contains("a");
      assertThat(pf.getIncluded()).isEmpty();
      assertThat(pf).hasToString("!a");
    }
    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("a   !b   c  !d");

      assertThat(pf).isNotNull();
      assertThat(pf.isEmpty()).isFalse();
      assertThat(pf.getExcluded()).contains("b", "d");
      assertThat(pf.getIncluded()).contains("a", "c");
      assertThat(pf).hasToString("!b !d a c");
    }
  }

  @Test
  public void testJoin() {
    {
      final PackagingFilter pf1 = PackagingFilter.newPackagingFilter("a !b c !d");
      final PackagingFilter pf2 = PackagingFilter.newPackagingFilter("e !f");

      assertThat(pf1.join(null)).isSameAs(pf1);
      assertThat(pf1.join("")).isSameAs(pf1);
      assertThat(pf1.join("   ")).isSameAs(pf1);
      assertThat(pf1.join(pf2.toString())).isEqualTo(pf2);
    }
  }

  @Test
  public void testPredicate() {
    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("a !b c !d");
      assertThat(pf.test("a")).isTrue();
      assertThat(pf.test("b")).isFalse();
      assertThat(pf.test("c")).isTrue();
      assertThat(pf.test("d")).isFalse();
      assertThat(pf.test("e")).isFalse();
    }

    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("");
      assertThat(pf.test("a")).isTrue();
      assertThat(pf.test("b")).isTrue();
      assertThat(pf.test("c")).isTrue();
      assertThat(pf.test("d")).isTrue();
      assertThat(pf.test("e")).isTrue();
    }

    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("!a");
      assertThat(pf.test("a")).isFalse();
      assertThat(pf.test("b")).isTrue();
      assertThat(pf.test("c")).isTrue();
      assertThat(pf.test("d")).isTrue();
      assertThat(pf.test("e")).isTrue();
    }

    {
      final PackagingFilter pf = PackagingFilter.newPackagingFilter("a");
      assertThat(pf.test("a")).isTrue();
      assertThat(pf.test("b")).isFalse();
      assertThat(pf.test("c")).isFalse();
      assertThat(pf.test("d")).isFalse();
      assertThat(pf.test("e")).isFalse();
    }
  }

  @Test
  public void testEqualsHashCode() {
    final PackagingFilter pf1 = PackagingFilter.newPackagingFilter("a");
    final PackagingFilter pf2 = PackagingFilter.newPackagingFilter("a");
    final PackagingFilter pf3 = PackagingFilter.newPackagingFilter("!b");
    final PackagingFilter pf4 = PackagingFilter.newPackagingFilter("c !b");
    final PackagingFilter pf5 = PackagingFilter.newPackagingFilter("a2");

    assertThat(pf1).hasSameHashCodeAs(pf2).isEqualTo(pf2);
    assertThat(pf1).hasSameHashCodeAs(pf1).isEqualTo(pf1);
    assertThat(pf1).isNotEqualTo(null);
    assertThat(pf1).isNotEqualTo(new Object());
    assertThat(pf1).isNotEqualTo(pf3);
    assertThat(pf1).isNotEqualTo(pf4);
    assertThat(pf1).isNotEqualTo(pf5);

  }

}
