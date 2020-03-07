package com.github.voltronic.inverter.exception;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TimeoutException extends IOException {

  private final long nanosecondsTimeout;

  public TimeoutException(long timeout, final TimeUnit timeoutTimeUnit) {
    super(new StringBuilder(48)
        .append(timeout = Math.max(0, timeout))
        .append(' ')
        .append(timeoutTimeUnit.name())
        .append(" timeout reached")
        .toString());

    this.nanosecondsTimeout = TimeUnit.NANOSECONDS.convert(timeout, timeoutTimeUnit);
  }

  public long getNanosecondsTimeout() {
    return this.nanosecondsTimeout;
  }

}
