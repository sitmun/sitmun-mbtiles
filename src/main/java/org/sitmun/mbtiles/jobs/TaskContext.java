package org.sitmun.mbtiles.jobs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.mbtiles.dto.TileServiceDto;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaskContext {
  private TileServiceDto service;
  private String outputPath;
}
