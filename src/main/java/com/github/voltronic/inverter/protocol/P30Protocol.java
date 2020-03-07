package com.github.voltronic.inverter.protocol;

import com.github.voltronic.inverter.Device;
import com.github.voltronic.inverter.MessageDigestSupplier;
import com.github.voltronic.inverter.Protocol;
import com.github.voltronic.inverter.digest.V1VoltronicMessageDigest;
import com.github.voltronic.inverter.exception.BufferOverflowException;
import com.github.voltronic.inverter.exception.DigestMismatchException;
import com.github.voltronic.inverter.exception.TimeoutException;
import com.github.voltronic.inverter.exception.TruncatedDataException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

public class P30Protocol implements Protocol {

  private static final MessageDigestSupplier DEFAULT_MESSAGE_DIGEST_SUPPLIER;
  private static final Charset DEFAULT_CHARSET;

  private static final int READ_SIZE = 8;
  private static final int INITIAL_BUFFER_SIZE = 128;
  private static final long READ_INTERVAL_MILLISECONDS = 50;
  private static final int DEFAULT_MAXIMUM_BUFFER_SIZE = 1024 * 8;
  private static final byte END_OF_INPUT_BYTE = (byte) '\r';

  private final MessageDigestSupplier messageDigestSupplier;
  private final Charset protocolCharset;
  private int maximumBufferSize;
  private boolean verifyDigest;

  public P30Protocol() {
    this(DEFAULT_MESSAGE_DIGEST_SUPPLIER, DEFAULT_CHARSET, false);
  }

  protected P30Protocol(
      final MessageDigestSupplier messageDigestSupplier,
      final Charset protocolCharset) {

    this(messageDigestSupplier, protocolCharset, true);
  }

  private P30Protocol(
      final MessageDigestSupplier messageDigestSupplier,
      final Charset protocolCharset,
      final boolean overrideConstructor) {

    if (messageDigestSupplier == null) {
      throw new NullPointerException("messageDigestSupplier is null");
    } else if (protocolCharset == null) {
      if (overrideConstructor) {
        throw new NullPointerException("protocolCharset is null");
      } else {
        throw new UnsupportedOperationException("US-ASCII Charset not supported on current platform");
      }
    } else if (overrideConstructor) {
      final MessageDigest md = messageDigestSupplier.get();
      int digestLength;
      if (md == null) {
        throw new NullPointerException("messageDigestSupplier.get() is null");
      } else if ((digestLength = md.getDigestLength()) <= 0) {
        throw new IllegalArgumentException("messageDigestSupplier.get().getDigestLength() <= 0");
      } else if (md.digest().length != digestLength) {
        throw new IllegalArgumentException(
            "messageDigestSupplier.get().digest() != messageDigestSupplier.get().getDigestLength()");
      }
    }

    this.messageDigestSupplier = messageDigestSupplier;
    this.protocolCharset = protocolCharset;
    this.setMaximumBufferSize(DEFAULT_MAXIMUM_BUFFER_SIZE);
    this.verifyDigest = true;
  }

  public String read(
      final Device device,
      final long timeout,
      final TimeUnit timeoutTimeUnit) throws IOException {

    if (device == null) {
      throw new NullPointerException("device is null");
    } else if (timeout < 0) {
      throw new IllegalArgumentException("timeout < 0");
    } else if (timeoutTimeUnit == null) {
      throw new NullPointerException("timeoutTimeUnit is null");
    }

    final byte[] bytes = this.readLoop(device, timeout, timeoutTimeUnit);
    final MessageDigest digest = this.messageDigestSupplier.get();

    final int bytesLength = bytes.length;
    final int digestLength = digest.getDigestLength();
    final int dataLength = bytesLength - digestLength - 1;

    if (dataLength < 0) {
      throw new TruncatedDataException(digestLength + 1, bytesLength);
    } else if (this.verifyDigest) {
      digest.update(bytes, 0, dataLength);
      final byte[] calculatedDigest = digest.digest();
      final byte[] receivedDigest = new byte[digestLength];

      for (int bytesIdx = dataLength - 1, digestIdx = 0; digestIdx < digestLength; ++digestIdx) {
        receivedDigest[digestIdx] = bytes[++bytesIdx];
      }

      if (!MessageDigest.isEqual(calculatedDigest, receivedDigest)) {
        throw new DigestMismatchException(receivedDigest, calculatedDigest);
      }
    }

    return this.protocolCharset.decode(ByteBuffer.wrap(bytes, 0, dataLength)).toString();
  }

  public void write(
      final Device device,
      final CharSequence input) throws IOException {

    final byte[] bytes;
    if (device == null) {
      throw new NullPointerException("device is null");
    } else if (input == null) {
      throw new NullPointerException("input is null");
    } else {
      final byte[] inputBytes = this.protocolCharset.encode(input.toString()).array();
      final int inputBytesLength = inputBytes.length;
      final MessageDigest digest = this.messageDigestSupplier.get();
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream(
          inputBytesLength + digest.getDigestLength() + 1);

      if (inputBytesLength > 0) {
        digest.update(inputBytes, 0, inputBytesLength);
        buffer.write(inputBytes, 0, inputBytesLength);
      }

      buffer.write(digest.digest());
      buffer.write(END_OF_INPUT_BYTE);
      bytes = buffer.toByteArray();
    }

    device.write(bytes, 0, bytes.length);
  }

  public void setMaximumBufferSize(final int maximumBufferSize) {
    if (maximumBufferSize <= 0) {
      throw new IllegalArgumentException("maximumBufferSize <= 0");
    } else if (maximumBufferSize > Character.MAX_VALUE) {
      throw new IllegalArgumentException("maximumBufferSize > " + Character.MAX_VALUE);
    } else {
      this.maximumBufferSize = maximumBufferSize;
    }
  }

  public void setVerifyDigest(final boolean verifyDigest) {
    this.verifyDigest = verifyDigest;
  }

  private byte[] readLoop(
      final Device device,
      final long timeout,
      final TimeUnit timeoutTimeUnit) throws IOException {

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(
        Math.min(INITIAL_BUFFER_SIZE, this.maximumBufferSize));

    final byte[] bytes = new byte[READ_SIZE];
    final int bytesLength = bytes.length;
    final long endNanoTime = System.nanoTime() + Math.max(0, timeoutTimeUnit.toNanos(timeout));

    while (true) {
      final int bytesRead = device.read(bytes, 0, bytesLength);

      if (buffer.size() + bytesRead > this.maximumBufferSize) {
        throw new BufferOverflowException(this.maximumBufferSize);
      }

      for (int index = 0; index < bytesRead; ++index) {
        final byte b = bytes[index];
        buffer.write(b);
        if (b == END_OF_INPUT_BYTE) {
          return buffer.toByteArray();
        }
      }

      if (System.nanoTime() > endNanoTime) {
        throw new TimeoutException(timeout, timeoutTimeUnit);
      }

      try {
        Thread.sleep(READ_INTERVAL_MILLISECONDS);
      } catch (final InterruptedException e) {
        final InterruptedIOException e2 = new InterruptedIOException(e.getMessage());
        e2.bytesTransferred = buffer.size();
        Thread.currentThread().interrupt();
        throw e2;
      }
    }
  }

  static {
    final class V1VoltronicMessageDigestSupplier implements MessageDigestSupplier {

      public final MessageDigest get() {
        return new V1VoltronicMessageDigest();
      }

    }

    Charset charset;
    try {
      charset = Charset.forName("US-ASCII");
    } catch (final Exception e) {
      try {
        charset = Charset.forName("ASCII");
      } catch (final Exception e2) {
        charset = null;
      }
    }

    DEFAULT_MESSAGE_DIGEST_SUPPLIER = new V1VoltronicMessageDigestSupplier();
    DEFAULT_CHARSET = charset;
  }

}
