package org.sitmun.mbtiles.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class MBTilesUnexpectedInternalException extends RuntimeException {
  public MBTilesUnexpectedInternalException(Exception cause) {
    super(cause);
  }
}
