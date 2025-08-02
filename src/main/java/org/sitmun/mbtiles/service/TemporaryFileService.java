package org.sitmun.mbtiles.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.config.TemporaryFileConfiguration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TemporaryFileService {

  private final TemporaryFileConfiguration config;
  private static final String TEMPORARY_FOLDER = "mbtiles";

  public TemporaryFileService(TemporaryFileConfiguration config) {
    this.config = config;
  }

  public Path createTempDirectory(String folderName) throws IOException {
    Path tempDir = Paths.get(config.getTempDirectory()).resolve(folderName);

    if (!Files.exists(tempDir)) {
      Files.createDirectories(tempDir);
      log.debug("Created temporary directory: {}", tempDir);
    } else {
      log.debug("Temporary directory already exists: {}", tempDir);
    }
    return tempDir;
  }

  /**
   * Creates a temporary file with a unique name based on UUID.
   *
   * @param extension file extension (without dot)
   * @return Path to the created temporary file
   * @throws IOException if file creation fails
   */
  public Path createUniqueTempFile(String extension) throws IOException {
    String filename = UUID.randomUUID() + "." + extension;

    Path tempFile = createTempDirectory(TEMPORARY_FOLDER).resolve(filename);
    Files.createFile(tempFile);
    log.debug("Created unique temporary file: {}", tempFile);

    return tempFile;
  }

  /**
   * Safely deletes a temporary file.
   *
   * @param path path to the file to delete
   * @return true if file was deleted, false otherwise
   */
  public boolean deleteTempFile(Path path) {
    try {
      if (path != null && Files.exists(path)) {
        boolean deleted = Files.deleteIfExists(path);
        if (deleted) {
          log.debug("Deleted temporary file: {}", path);
        }
        return deleted;
      }
    } catch (IOException e) {
      log.warn("Failed to delete temporary file: {}", path, e);
    }
    return false;
  }

  /**
   * Cleans up old temporary files in the configured directory. Scheduled to run based on cron
   * expression from configuration.
   */
  @Scheduled(cron = "${mbtiles.temp.cleanup.cron:0 0 2 * * *}")
  public void cleanupOldTempFiles() {
    if (!config.isCleanupEnabled()) {
      return;
    }

    try {
      Path tempDir = Paths.get(config.getTempDirectory()).resolve(TEMPORARY_FOLDER);
      if (!Files.exists(tempDir)) {
        return;
      }

      long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours
      final int[] deletedCount = {0};

      try (Stream<Path> tempFiles = Files.list(tempDir)) {
        tempFiles
            .filter(
                path -> {
                  try {
                    return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                  } catch (IOException e) {
                    log.warn("Failed to get last modified time for file: {}", path, e);
                    return false;
                  }
                })
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                    deletedCount[0]++;
                    log.debug("Deleted old temporary file: {}", path);
                  } catch (IOException e) {
                    log.warn("Failed to delete old temporary file: {}", path, e);
                  }
                });
      }
      if (deletedCount[0] > 0) {
        log.debug("Cleaned up {} old temporary files", deletedCount[0]);
      }
    } catch (Exception e) {
      log.warn("Error during temporary file cleanup", e);
    }
  }
}
