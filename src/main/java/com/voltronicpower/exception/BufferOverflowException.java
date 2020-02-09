package com.voltronicpower.exception;

import java.io.IOException;

public class BufferOverflowException extends IOException {

  private final int byteLimit;

  public BufferOverflowException(int byteLimit) {
    super(new StringBuilder(48)
        .append("Limit of ")
        .append(byteLimit = Math.max(0, byteLimit))
        .append(" bytes reached")
        .toString());

    this.byteLimit = byteLimit;
  }

  public int getByteLimit() {
    return this.byteLimit;
  }

}
