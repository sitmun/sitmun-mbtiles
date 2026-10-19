package org.sitmun.mbtiles.dto;

import lombok.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@With
@JsonDeserialize(builder = ResourceDto.ResourceDtoBuilder.class)
public class ResourceDto {
  String fileName;
  byte[] fileBytes;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ResourceDtoBuilder {}
}
