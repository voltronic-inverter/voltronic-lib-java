package com.github.voltronic.inverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class IOTestDevice implements Device {

  private ByteArrayInputStream input;
  private ByteArrayOutputStream output;
  private int readDelay;
  private int charsPerRead;

  public IOTestDevice() {
    this.reset();
  }

  public void reset() {
    output = new ByteArrayOutputStream();
    setInput(new byte[0]);
    setReadDelayMilliseconds(-1);
  }

  public void setReadDelayMilliseconds(final int milliseconds) {
    if (milliseconds == -1 || milliseconds > 0) {
      this.readDelay = milliseconds;
    } else {
      throw new IllegalArgumentException("milliseconds <= 0");
    }
  }

  public void setCharsPerRead(final int charsPerRead) {
    if (charsPerRead == -1 || charsPerRead > 0) {
      this.charsPerRead = charsPerRead;
    } else {
      throw new IllegalArgumentException("charsPerRead <= 0");
    }
  }

  public void setInput(final byte[] bytes) {
    this.setInput(bytes, 0, bytes.length);
  }

  public void setInput(final byte[] bytes, int off, int len) {
    final byte[] copy = new byte[len];
    System.arraycopy(bytes, off, copy, 0, len);
    input = new ByteArrayInputStream(copy);
  }

  public byte[] getOutput() {
    return this.output.toByteArray();
  }

  public int read(byte[] b, int off, int len) {
    if (readDelay > 0) {
      try {
        Thread.sleep(readDelay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    if (charsPerRead > 0) {
      len = Math.min(charsPerRead, len);
    }

    return input.read(b, off, len);
  }

  public void write(byte[] b, int off, int len) {
    this.output.write(b, off, len);
  }

  public void close() throws IOException {
  }

}
