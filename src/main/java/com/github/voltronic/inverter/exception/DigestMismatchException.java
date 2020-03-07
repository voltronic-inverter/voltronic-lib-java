package com.github.voltronic.inverter.exception;

import java.io.IOException;

public class DigestMismatchException extends IOException {

  private static final char[] HEX_TABLE = "0123456789ABCDEF".toCharArray();

  private final byte[] receivedDigest;
  private final byte[] calculatedDigest;

  public DigestMismatchException(final byte[] receivedDigest, final byte[] calculatedDigest) {
    super(new StringBuilder(128)
        .append("Digest received ")
        .append(hex(receivedDigest))
        .append(" != ")
        .append(hex(calculatedDigest))
        .append(" calculated")
        .toString());

    this.receivedDigest = copy(receivedDigest);
    this.calculatedDigest = copy(calculatedDigest);
  }

  public byte[] getReceivedDigest() {
    return copy(this.receivedDigest);
  }

  public byte[] getCalculatedDigest() {
    return copy(this.calculatedDigest);
  }

  private static byte[] copy(final byte[] bytes) {
    if (bytes != null && bytes.length > 0) {
      final byte[] copy = new byte[bytes.length];
      System.arraycopy(bytes, 0, copy, 0, copy.length);
      return copy;
    } else {
      return new byte[0];
    }
  }

  private static String hex(final byte[] bytes) {
    if (bytes != null && bytes.length > 0) {
      final StringBuilder builder = new StringBuilder(1 + (bytes.length * 5));
      builder.append('[');
      for (byte b : bytes) {
        int i = b & 0xFF;
        builder.append('0').append('x').append(HEX_TABLE[i >>> 4]).append(HEX_TABLE[i & 0x0F]).append(',');
      }
      builder.setCharAt(builder.length() - 1, ']');
      return builder.toString();
    } else {
      return "[]";
    }
  }

}
