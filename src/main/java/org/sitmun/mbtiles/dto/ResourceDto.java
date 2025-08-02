package org.sitmun.mbtiles.dto;

import lombok.*;

@Value
@Builder
@With
public class ResourceDto {
  String fileName;
  byte[] fileBytes;
}
