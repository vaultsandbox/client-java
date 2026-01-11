package com.vaultsandbox.client.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class WaitOptionsTest {

  @Test
  void testBuilderWithAllOptions() {
    EmailFilter filter = EmailFilter.builder().subject("Test").build();
    Duration timeout = Duration.ofSeconds(30);
    Duration pollInterval = Duration.ofMillis(500);

    WaitOptions options =
        WaitOptions.builder().filter(filter).timeout(timeout).pollInterval(pollInterval).build();

    assertSame(filter, options.getFilter());
    assertEquals(timeout, options.getTimeout());
    assertEquals(pollInterval, options.getPollInterval());
  }

  @Test
  void testBuilderWithDefaultFilter() {
    WaitOptions options = WaitOptions.builder().timeout(Duration.ofSeconds(60)).build();

    assertNotNull(options.getFilter());
    assertEquals(Duration.ofSeconds(60), options.getTimeout());
    assertNull(options.getPollInterval());
  }

  @Test
  void testBuilderWithNullFilter() {
    WaitOptions options = WaitOptions.builder().filter(null).build();

    // Should default to EmailFilter.any() when filter is null
    assertNotNull(options.getFilter());
  }

  @Test
  void testBuilderWithOnlyPollInterval() {
    Duration pollInterval = Duration.ofMillis(250);

    WaitOptions options = WaitOptions.builder().pollInterval(pollInterval).build();

    assertNotNull(options.getFilter());
    assertNull(options.getTimeout());
    assertEquals(pollInterval, options.getPollInterval());
  }

  @Test
  void testBuilderWithNoOptions() {
    WaitOptions options = WaitOptions.builder().build();

    assertNotNull(options.getFilter());
    assertNull(options.getTimeout());
    assertNull(options.getPollInterval());
  }

  @Test
  void testBuilderChaining() {
    WaitOptions.Builder builder = WaitOptions.builder();

    // Each builder method should return the same builder instance
    assertSame(builder, builder.filter(EmailFilter.any()));
    assertSame(builder, builder.timeout(Duration.ofSeconds(10)));
    assertSame(builder, builder.pollInterval(Duration.ofMillis(100)));
  }

  @Test
  void testStaticBuilderMethod() {
    WaitOptions.Builder builder = WaitOptions.builder();
    assertNotNull(builder);
  }
}
