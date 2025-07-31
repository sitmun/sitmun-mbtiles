package org.sitmun.mbtiles.web;

import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.MBTilesJobStatusDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.sitmun.mbtiles.service.MBTilesService;
import org.sitmun.mbtiles.service.ResourceDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mbtiles")
public class MBTilesController {

  private final MBTilesService mbTilesService;

  public MBTilesController(MBTilesService mbTilesService) {
    this.mbTilesService = mbTilesService;
  }

  @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> startMBTilesJob(@RequestBody TileRequestDto tileRequest) {
    Long jobId = mbTilesService.startJob(tileRequest);
    return ResponseEntity.ok(jobId.toString());
  }

  @PostMapping(value = "/estimate", produces = MediaType.APPLICATION_JSON_VALUE)
  public MBTilesEstimateDto estimateMBTilesSize(@RequestBody TileRequestDto tileRequest) {
    return mbTilesService.estimateSize(tileRequest);
  }

  @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public MBTilesJobStatusDto getMBTilesJobStatus(@PathVariable Long jobId) {
    return mbTilesService.getJobStatus(jobId);
  }

  @GetMapping(value = "/{jobId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long jobId) {
    ResourceDto resourceDto = mbTilesService.getMBTilesFile(jobId);
    ByteArrayResource resource = new ByteArrayResource(resourceDto.getFileBytes());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resourceDto.getFileName())
        .contentLength(resourceDto.getFileBytes().length)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }
}
