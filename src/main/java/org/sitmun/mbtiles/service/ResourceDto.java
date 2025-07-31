package org.sitmun.mbtiles.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ResourceDto {
  private String fileName;
  private byte[] fileBytes;
}
