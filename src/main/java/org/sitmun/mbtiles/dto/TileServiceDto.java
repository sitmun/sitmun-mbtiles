package org.sitmun.mbtiles.dto;

import java.util.List;
import lombok.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@With
@JsonDeserialize(builder = TileServiceDto.TileServiceDtoBuilder.class)
public class TileServiceDto {

  String url;
  List<String> layers;
  String type;
  BoundingBoxDto bbox;
  int minZoom;
  int maxZoom;
  String matrixSet;

  @JsonPOJOBuilder(withPrefix = "")
  public static class TileServiceDtoBuilder {}
}
