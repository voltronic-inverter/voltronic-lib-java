package com.voltronicpower.exception;

import java.io.IOException;

public class TruncatedDataException extends IOException {

  private final int bytesExpected;
  private final int bytesReceived;

  public TruncatedDataException(int bytesExpected, int bytesReceived) {
    super(new StringBuilder(64)
        .append("Expected at least ")
        .append(bytesExpected = Math.max(0, bytesExpected))
        .append(" bytes but received ")
        .append(bytesReceived = Math.max(0, bytesReceived))
        .append(" bytes")
        .toString());

    this.bytesExpected = bytesExpected;
    this.bytesReceived = bytesReceived;
  }

  public int getBytesReceived() {
    return this.bytesReceived;
  }

  public int getBytesExpected() {
    return this.bytesExpected;
  }

}
