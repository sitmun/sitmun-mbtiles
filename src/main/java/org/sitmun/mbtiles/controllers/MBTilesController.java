package org.sitmun.mbtiles.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.MBTilesJobStatusDto;
import org.sitmun.mbtiles.dto.ResourceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.sitmun.mbtiles.service.MBTilesEstimateService;
import org.sitmun.mbtiles.service.MBTilesJobService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mbtiles")
public class MBTilesController {

  private final MBTilesEstimateService mbTilesEstimateService;
  private final MBTilesJobService mbTilesJobService;

  public MBTilesController(
      MBTilesEstimateService mbTilesEstimateService, MBTilesJobService mbTilesJobService) {
    this.mbTilesEstimateService = mbTilesEstimateService;
    this.mbTilesJobService = mbTilesJobService;
  }

  @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> startMBTilesJob(@Valid @RequestBody TileRequestDto tileRequest) {
    Long jobId = mbTilesJobService.startJob(tileRequest);
    return ResponseEntity.ok(jobId.toString());
  }

  @PostMapping(value = "/estimate", produces = MediaType.APPLICATION_JSON_VALUE)
  public MBTilesEstimateDto estimateMBTilesSize(@Valid @RequestBody TileRequestDto tileRequest) {
    return mbTilesEstimateService.estimateSize(tileRequest);
  }

  @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public MBTilesJobStatusDto getMBTilesJobStatus(@NotNull @Positive @PathVariable Long jobId) {
    return mbTilesJobService.getJobStatus(jobId);
  }

  @GetMapping(value = "/{jobId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<ByteArrayResource> downloadFile(
      @NotNull @Positive @PathVariable Long jobId) {
    ResourceDto resourceDto = mbTilesJobService.getMBTilesFile(jobId);
    ByteArrayResource resource = new ByteArrayResource(resourceDto.getFileBytes());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resourceDto.getFileName())
        .contentLength(resourceDto.getFileBytes().length)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }
}
