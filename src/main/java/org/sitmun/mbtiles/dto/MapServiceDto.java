package org.sitmun.mbtiles.dto;

import java.util.List;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapServiceDto {

  private String url;

  private List<String> layers;

  private String type;
}
