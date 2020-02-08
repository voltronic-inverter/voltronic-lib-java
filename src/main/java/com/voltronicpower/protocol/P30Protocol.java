package com.voltronicpower.protocol;

import com.voltronicpower.Device;
import com.voltronicpower.Protocol;
import com.voltronicpower.digest.V1VoltronicMessageDigest;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.BufferOverflowException;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class P30Protocol implements Protocol {

  static final byte END_OF_INPUT = (byte) '\r';

  private int maximumBufferSize;
  private boolean verifyDigest;

  public P30Protocol() {
    this.setMaximumBufferSize(1024 * 16);
    this.verifyDigest = true;
  }

  public String read(
      final Device device,
      final long timeout,
      final TimeUnit timeoutTimeUnit) throws IOException, TimeoutException {

    final int length;
    final byte[] bytes;
    if (device == null) {
      throw new NullPointerException("device is null");
    } else if (timeout < 0) {
      throw new IllegalArgumentException("timeout < 0");
    } else if (timeoutTimeUnit == null) {
      throw new NullPointerException("timeoutTimeUnit is null");
    } else {
      final Buffer buffer = readLoop(device, timeout, timeoutTimeUnit);
      length = buffer.length;
      bytes = buffer.bytes;
    }

    final MessageDigest digest = this.getMessageDigest();
    final int digestLength = digest.getDigestLength();
    final int dataLength = length - (digestLength + 1);

    if (digestLength > length) {
      throw new IOException("Truncated response from device");
    } else if (this.verifyDigest) {
      digest.update(bytes, 0, dataLength + 1);
      final byte[] calculatedDigest = digest.digest();
      final byte[] receivedDigest = new byte[digestLength];

      for (int bytesIdx = dataLength, digestIdx = 0; digestIdx < digestLength; ++digestIdx) {
        receivedDigest[digestIdx] = bytes[++bytesIdx];
      }

      if (!MessageDigest.isEqual(calculatedDigest, receivedDigest)) {
        throw new DigestMismatchException(receivedDigest, calculatedDigest);
      }
    }

    return ascii(bytes, 0, length - digestLength);
  }

  public void write(
      final Device device,
      final CharSequence input) throws IOException {

    if (device == null) {
      throw new NullPointerException("device is null");
    } else if (input == null) {
      throw new NullPointerException("input is null");
    } else {
      final byte[] bytes = ascii(input);
      final MessageDigest digest = getMessageDigest();
      if (bytes.length > 0) {
        device.write(bytes, 0, bytes.length);
        digest.update(bytes, 0, bytes.length);
      }

      final byte[] digestBytes = digest.digest();
      device.write(digestBytes, 0, digestBytes.length);

      device.write(new byte[]{END_OF_INPUT}, 0, 1);
    }
  }

  public final void setMaximumBufferSize(final int maximumBufferSize) {
    if (maximumBufferSize <= 0) {
      throw new IllegalArgumentException("maximumBufferSize <= 0");
    } else if (maximumBufferSize >= (Integer.MAX_VALUE / 2)) {
      throw new IllegalArgumentException("maximumBufferSize is too large");
    } else {
      this.maximumBufferSize = maximumBufferSize;
    }
  }

  public final void setVerifyDigest(final boolean verifyDigest) {
    this.verifyDigest = verifyDigest;
  }

  MessageDigest getMessageDigest() {
    return new V1VoltronicMessageDigest();
  }

  int getDefaultBufferSize() {
    return 128;
  }

  byte[] resize(final byte[] bytes, final int newSize) {
    final byte[] copy = new byte[newSize];
    System.arraycopy(bytes, 0, copy, 0, bytes.length);
    return copy;
  }

  private Buffer readLoop(
      final Device device,
      final long timeout,
      final TimeUnit timeoutTimeunit) throws IOException, TimeoutException {

    byte[] bytes = new byte[Math.min(this.getDefaultBufferSize(), this.maximumBufferSize)];
    int bytesIndex = 0;
    final long endTime = System.nanoTime() + timeoutTimeunit.toNanos(timeout) + 1;
    while (true) {
      int bytesRead = device.read(bytes, bytesIndex, bytes.length - bytesIndex);

      if (bytesRead < 0) {
        throw new EOFException("End of stream reached while reading input");
      } else if (bytesRead > 0) {
        do {
          if (bytes[bytesIndex++] == END_OF_INPUT) {
            return new Buffer(bytes, bytesIndex - 1);
          }
        } while (--bytesRead != 0);
      }

      if (System.nanoTime() > endTime) {
        throw new TimeoutException("Timeout reached while reading input");
      } else if (bytesIndex + 1 >= bytes.length) {
        int newSize = Math.min(bytes.length * 2, this.maximumBufferSize);
        if (newSize <= bytes.length) {
          throw new BufferOverflowException();
        } else {
          bytes = resize(bytes, newSize);
        }
      } else {
        try {
          Thread.sleep(50);
        } catch (final InterruptedException e) {
          final InterruptedIOException e2 = new InterruptedIOException(e.getMessage());
          e2.bytesTransferred = bytesIndex;
          Thread.currentThread().interrupt();
          throw e2;
        }
      }
    }
  }

  private static String ascii(final byte[] bytes, int off, int len) {
    if (len > 0) {
      int charIdx = -1;
      --off;
      final char[] chars = new char[len];
      do {
        chars[++off] = (char) (bytes[++charIdx] & 0xFF);
      } while (--len != 0);

      return new String(chars);
    } else {
      return "";
    }
  }

  private static byte[] ascii(final CharSequence ch) {
    final char[] chars = ch.toString().toCharArray();
    final byte[] bytes = new byte[chars.length];
    for (int index = 0; index < bytes.length; ++index) {
      bytes[index] = (byte) chars[index];
    }
    return bytes;
  }

  private static final class Buffer {

    private final byte[] bytes;
    private final int length;

    Buffer(final byte[] bytes, final int length) {
      this.bytes = bytes;
      this.length = length;
    }
  }

}
