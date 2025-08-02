package org.sitmun.mbtiles.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Getter
public class TemporaryFileConfiguration {

  @Value("${mbtiles.temp.directory:${java.io.tmpdir}}")
  private String tempDirectory;

  @Value("${mbtiles.temp.cleanup.enabled:true}")
  private boolean cleanupEnabled;

  @Value("${mbtiles.temp.cleanup.cron:0 0 2 * * *}")
  private String cleanupCron;
}
