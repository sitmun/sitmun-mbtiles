package org.sitmun.mbtiles.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sitmun.mbtiles.config.TemporaryFileConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

class TemporaryFileServiceTest {

  private TemporaryFileService service;
  private TemporaryFileConfiguration config;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    config = new TemporaryFileConfiguration();
    ReflectionTestUtils.setField(config, "tempDirectory", tempDir.toString());
    ReflectionTestUtils.setField(config, "cleanupEnabled", false);
    ReflectionTestUtils.setField(config, "cleanupCron", "0 0 2 * * *");

    service = new TemporaryFileService(config);
  }

  @Test
  void shouldCreateTempDirectoryWithCustomPrefix() throws IOException {
    Path tempDir = service.createTempDirectory("custom");

    assertNotNull(tempDir);
    assertTrue(Files.exists(tempDir));
    assertTrue(Files.isDirectory(tempDir));
    assertTrue(tempDir.getFileName().toString().startsWith("custom"));
  }

  @Test
  void shouldCreateUniqueTempFile() throws IOException {
    Path tempFile = service.createUniqueTempFile("mbtiles");

    assertNotNull(tempFile);
    assertTrue(Files.exists(tempFile));
    assertTrue(tempFile.getFileName().toString().endsWith(".mbtiles"));
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistentFile() {
    Path nonExistentFile = Path.of("non-existent-file.tmp");

    boolean deleted = service.deleteTempFile(nonExistentFile);

    assertFalse(deleted);
  }

  @Test
  void shouldRespectCleanupEnabledFlag() throws IOException {
    Path tempFile = service.createUniqueTempFile("test");
    Files.setLastModifiedTime(tempFile, FileTime.from(Instant.now().minus(Duration.ofHours(25))));
    assertTrue(Files.exists(tempFile));

    ReflectionTestUtils.setField(config, "cleanupEnabled", false);
    service.cleanupOldTempFiles();

    assertTrue(Files.exists(tempFile));

    service.deleteTempFile(tempFile);
  }
}
