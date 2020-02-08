package com.voltronicpower.protocol;

import com.voltronicpower.IOTestDevice;
import com.voltronicpower.Protocol;
import com.voltronicpower.TestMessageDigest;
import java.io.EOFException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static com.voltronicpower.ByteUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class P30ProtocolTest {

  private static final char END_OF_INPUT = P30Protocol.END_OF_INPUT;

  private IOTestDevice device;
  private TestMessageDigest md;
  private P30Protocol configurableProtocol;
  private Protocol protocol;
  private int defaultBufferSize;
  private int resizedCount;

  @BeforeEach
  public void setup() {
    final class TestP30Protocol extends P30Protocol {

      final MessageDigest getMessageDigest() {
        return md;
      }

      final int getDefaultBufferSize() {
        return defaultBufferSize;
      }

      final byte[] resize(final byte[] bytes, final int newSize) {
        resizedCount++;
        return super.resize(bytes, newSize);
      }
    }

    this.defaultBufferSize = 16;
    this.resizedCount = 0;

    this.protocol = this.configurableProtocol = new TestP30Protocol();
    this.configurableProtocol.setMaximumBufferSize(this.defaultBufferSize * 2);
    this.configurableProtocol.setVerifyDigest(true);

    this.device = new IOTestDevice();

    this.md = new TestMessageDigest();
    this.md.setDigest(new byte[]{0x01, 0x02, 0x03, 0x04});
  }

  @Test
  @DisplayName("Truncated responses throws IOException")
  public void testTruncatedResponseThrowsException() throws Exception {
    final byte[] bytes = new byte[this.md.getDigestLength()];
    bytes[bytes.length - 1] = END_OF_INPUT;
    device.setInput(bytes);

    final IOException e = assertThrows(IOException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 255, TimeUnit.DAYS);
      }
    });

    assertEquals("Truncated response from device", e.getMessage());
  }

  @Test
  @DisplayName("End of stream throws exception")
  public void testEOFThrowsException() throws Exception {
    final byte[] bytes = new byte[3];
    bytes[0] = 1;
    bytes[1] = 2;
    bytes[2] = 3;
    device.setInput(bytes);

    final EOFException e = assertThrows(EOFException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 255, TimeUnit.DAYS);
      }
    });

    assertEquals("End of stream reached while reading input", e.getMessage());
  }

  @Test
  @DisplayName("DigestMismatchException throw when the calculated digest does not match the received digest")
  public void testDigestMismatchThrowsException() throws Exception {
    final String data = "Hi";
    final byte[] receivedDigest = bytes(0x09, 0x08, 0x07, 0x06);
    device.setInput(bytes(data + new String(receivedDigest, 0) + END_OF_INPUT));

    final DigestMismatchException e = assertThrows(DigestMismatchException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 255, TimeUnit.DAYS);
      }
    });

    assertArrayEquals(receivedDigest, e.getReceivedDigest());
    assertArrayEquals(bytes(0x01, 0x02, 0x03, 0x04), e.getCalculatedDigest());
    assertArrayEquals(bytes(data), md.getUpdateBytes());
  }

  @Test
  @DisplayName("Received digest is ignored if configured to be ignored")
  public void testDigestMismatchIgnoredWhenSoConfigured() throws Exception {
    final String expected = "Hi";
    final byte[] receivedDigest = bytes(0x09, 0x08, 0x07, 0x06);
    device.setInput(bytes(expected + new String(receivedDigest, 0) + END_OF_INPUT));

    configurableProtocol.setVerifyDigest(false);
    final String received = protocol.read(device, 255, TimeUnit.DAYS);

    assertEquals(expected, received);
  }

  @Test
  @DisplayName("Timeout exception is thrown if read time is exceeded")
  public void testTimeoutException() {
    device.setCharsPerRead(4);
    device.setReadDelayMilliseconds((int) TimeUnit.SECONDS.toMillis(2));

    device.setInput(bytes(bytes("Hello world!!"), md.digest(), bytes(END_OF_INPUT)));

    final long startTime = System.nanoTime();
    assertThrows(TimeoutException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 1, TimeUnit.SECONDS);
      }
    });
    final long endTime = System.nanoTime();

    assertTrue(endTime - startTime > 1000);
  }

  @Test
  @DisplayName("Validate resize of internal buffer functions correctly")
  public void testBufferResize() throws Exception {
    final byte[] input = bytes(
        bytes("The quick brown fox jumps over the lazy dog"),
        md.digest(),
        bytes(END_OF_INPUT));

    defaultBufferSize = 4;
    configurableProtocol.setMaximumBufferSize(input.length + 1);
    device.setInput(input);

    final String readString = protocol.read(device, 255, TimeUnit.DAYS);

    final byte[] readBytes = bytes(readString);
    final byte[] expectedBytes = copy(input, 0, readBytes.length);

    assertEquals(4, resizedCount);
    assertEquals(input.length - md.getDigestLength() - 1, readBytes.length);
    assertArrayEquals(expectedBytes, readBytes);
  }

  @Test
  @DisplayName("Writes empty string")
  public void writesEmpty() throws IOException {
    protocol.write(device, "");

    assertArrayEquals(new byte[0], md.getUpdateBytes());
    assertArrayEquals(bytes(md.digest(), bytes(END_OF_INPUT)), device.getOutput());
  }

  @Test
  @DisplayName("Writes strings that aren't empty")
  public void writesNotEmpty() throws IOException {
    protocol.write(device, "Hello world");

    assertArrayEquals(bytes("Hello world"), md.getUpdateBytes());
    assertArrayEquals(bytes(bytes("Hello world"), md.digest(), bytes(END_OF_INPUT)), device.getOutput());
  }

}
