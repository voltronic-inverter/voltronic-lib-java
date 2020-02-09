package com.voltronicpower.protocol;

import com.voltronicpower.IOTestDevice;
import com.voltronicpower.MessageDigestSupplier;
import com.voltronicpower.Protocol;
import com.voltronicpower.TestMessageDigest;
import com.voltronicpower.exception.BufferOverflowException;
import com.voltronicpower.exception.DigestMismatchException;
import com.voltronicpower.exception.TimeoutException;
import com.voltronicpower.exception.TruncatedDataException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static com.voltronicpower.ByteUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class P30ProtocolTest {

  private static final char END_OF_INPUT = '\r';

  private IOTestDevice device;
  private TestMessageDigest md;
  private P30Protocol configurableProtocol;
  private Protocol protocol;

  @BeforeEach
  public void setup() {
    this.device = new IOTestDevice();

    this.md = new TestMessageDigest();
    this.md.setDigest(new byte[]{0x01, 0x02, 0x03, 0x04});

    this.protocol = this.configurableProtocol = new P30Protocol(new MessageDigestSupplier() {
      public MessageDigest get() {
        return md;
      }
    }, Charset.forName("US-ASCII"));

    this.configurableProtocol.setMaximumBufferSize(32);
    this.configurableProtocol.setVerifyDigest(true);
  }

  @Test
  @DisplayName("Default constructor works")
  public void testDefaultConstructor() throws Exception {
    new P30Protocol().write(device, "Hello");
  }

  @Test
  @DisplayName("maximumBufferSize setter enforces limits")
  public void testMaximumBufferSizeConfigurationEnforcesLimits() {
    assertThrows(IllegalArgumentException.class, new Executable() {
      public void execute() throws Throwable {
        configurableProtocol.setMaximumBufferSize(0);
      }
    });

    assertThrows(IllegalArgumentException.class, new Executable() {
      public void execute() throws Throwable {
        configurableProtocol.setMaximumBufferSize(-1);
      }
    });

    assertThrows(IllegalArgumentException.class, new Executable() {
      public void execute() throws Throwable {
        final int i = Character.MAX_VALUE;
        configurableProtocol.setMaximumBufferSize(i + 1);
      }
    });

    configurableProtocol.setMaximumBufferSize(1);
    configurableProtocol.setMaximumBufferSize(Character.MAX_VALUE);
  }

  @Test
  @DisplayName("Truncated responses throws IOException")
  public void testTruncatedResponseThrowsException() throws Exception {
    final byte[] bytes = new byte[this.md.getDigestLength()];
    bytes[bytes.length - 1] = END_OF_INPUT;
    device.setInput(bytes);

    final TruncatedDataException e = assertThrows(TruncatedDataException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 255, TimeUnit.DAYS);
      }
    });

    assertEquals(5, e.getBytesExpected());
    assertEquals(4, e.getBytesReceived());
    assertEquals("Expected at least 5 bytes but received 4 bytes", e.getMessage());
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
    assertEquals("Digest received [0x09,0x08,0x07,0x06] != [0x01,0x02,0x03,0x04] calculated", e.getMessage());
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
    final TimeoutException e = assertThrows(TimeoutException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 1, TimeUnit.SECONDS);
      }
    });
    final long endTime = System.nanoTime();

    assertTrue(endTime - startTime > 1000);
    assertEquals(TimeUnit.SECONDS.toNanos(1), e.getNanosecondsTimeout());
    assertEquals("1 SECONDS timeout reached", e.getMessage());
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

  @Test
  @DisplayName("Throws BufferOverflowException if maximumBufferSize is reached")
  public void testEnforcedMaximumBufferSize() {
    final String inputString = "The quick brown fox jumps over the lazy dog";
    final byte[] inputBytes = bytes(bytes(inputString), md.digest(), bytes(END_OF_INPUT));
    device.setInput(inputBytes);
    configurableProtocol.setMaximumBufferSize(inputBytes.length - 1);

    final BufferOverflowException e = assertThrows(BufferOverflowException.class, new Executable() {
      public void execute() throws Throwable {
        protocol.read(device, 255, TimeUnit.DAYS);
      }
    });

    assertEquals(e.getByteLimit(), inputBytes.length - 1);
    assertEquals("Limit of " + (inputBytes.length - 1) + " bytes reached", e.getMessage());
  }

}
