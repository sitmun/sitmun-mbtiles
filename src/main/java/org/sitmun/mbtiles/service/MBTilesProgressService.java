package org.sitmun.mbtiles.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sitmun.mbtiles.dto.MBTilesProgressDto;
import org.springframework.stereotype.Service;

@Service
public class MBTilesProgressService {

  private final Map<Long, MBTilesProgressDto> jobProgress = new ConcurrentHashMap<>();

  public void updateJobProgress(long jobId, long totalTiles, long processedTiles) {
    synchronized (jobProgress) {
      MBTilesProgressDto progress =
          jobProgress.getOrDefault(
              jobId, MBTilesProgressDto.builder().totalTiles(totalTiles).processedTiles(0).build());
      jobProgress.put(jobId, progress.withProcessedTiles(processedTiles));
    }
  }

  public MBTilesProgressDto getJobProgress(long jobId) {
    return jobProgress.get(jobId);
  }

  public void clearJobProgress(long jobId) {
    jobProgress.remove(jobId);
  }
}
