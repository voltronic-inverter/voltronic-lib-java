package com.voltronicpower;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;

public class TestMessageDigest extends MessageDigest {

  private final ByteArrayOutputStream received = new ByteArrayOutputStream();
  private byte[] digest;

  public TestMessageDigest() {
    super(TestMessageDigest.class.getSimpleName());
    this.engineReset();
  }

  public final void setDigest(final byte[] b) {
    this.setDigest(b, 0, b.length);
  }

  public final void setDigest(final byte[] b, final int off, final int len) {
    if (b == null) {
      throw new NullPointerException();
    } else if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException();
    } else {
      final byte[] copy = new byte[len];
      System.arraycopy(b, off, copy, 0, len);
      this.digest = copy;
    }
  }

  public final byte[] getUpdateBytes() {
    return received.toByteArray();
  }

  protected final void engineUpdate(byte input) {
    this.received.write(input & 0xFF);
  }

  protected final void engineUpdate(byte[] input, int offset, int len) {
    if (input == null) {
      throw new NullPointerException();
    } else if (offset < 0 || len < 0 || len > input.length - offset) {
      throw new IndexOutOfBoundsException();
    } else if (len > 0) {
      this.received.write(input, offset, len);
    }
  }

  protected final byte[] engineDigest() {
    return this.digest;
  }

  protected final void engineReset() {
    this.received.reset();
    this.setDigest(new byte[0]);
  }

  protected final int engineGetDigestLength() {
    return this.digest.length;
  }

}
