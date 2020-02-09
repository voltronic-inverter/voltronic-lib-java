package com.voltronicpower.digest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.voltronicpower.ByteUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class V1VoltronicMessageDigestTest {

  private static final ByteOrder V1_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
  private static final Collection<Byte> RESERVED_BYTES = Collections.unmodifiableSet(new HashSet<Byte>(list(
      0x0A, 0x0D, 0x28)));

  private MessageDigest md;

  @BeforeEach
  public void setup() {
    md = new V1VoltronicMessageDigest();
  }

  @Test
  @DisplayName("Assert byte size of MessageDigest is 2 bytes")
  public void byteSizeCorrect() {
    assertEquals(2, md.getDigestLength());
  }

  @Test
  @DisplayName("Reserved bytes check matches known reserved bytes during spot check")
  public void reservedBytesDetectedCorrectly() {
    assertFalse(new V1VoltronicMessageDigest().isReservedByte((byte) 0x22));
    assertTrue(new V1VoltronicMessageDigest().isReservedByte((byte) 0x0A));
    assertTrue(new V1VoltronicMessageDigest().isReservedByte((byte) 0x0D));
    assertTrue(new V1VoltronicMessageDigest().isReservedByte((byte) 0x28));
  }

  @Test
  @DisplayName("Reserved bytes check matches known reserved bytes during exhaustive test")
  public void reservedBytesExhaustivelyDetectedCorrectly() {
    for (int count = 0; count <= 0xFF; ++count) {
      final byte b = (byte) count;
      if (RESERVED_BYTES.contains(b)) {
        assertTrue(new V1VoltronicMessageDigest().isReservedByte(b));
      } else {
        assertFalse(new V1VoltronicMessageDigest().isReservedByte(b));
      }
    }
  }

  @Test
  @DisplayName("Writes reserved bytes correctly during spot check")
  public void writeReservedBytesCorrectlySpotTest() {
    assertArrayEquals(bytes(0x34, 0x16), writeCrcBytes(0x3416));

    assertArrayEquals(bytes(0x0B, 0xBF), writeCrcBytes(0x0ABF));
    assertArrayEquals(bytes(0x18, 0x0B), writeCrcBytes(0x180A));

    assertArrayEquals(bytes(0x0E, 0x54), writeCrcBytes(0x0D54));
    assertArrayEquals(bytes(0x22, 0x0E), writeCrcBytes(0x220D));

    assertArrayEquals(bytes(0x29, 0x18), writeCrcBytes(0x2818));
    assertArrayEquals(bytes(0x34, 0x29), writeCrcBytes(0x3428));
  }

  @Test
  @DisplayName("Writes reserved bytes correctly during exhaustive test")
  public void writeReservedBytesCorrectlyExhaustiveTest() {
    final byte[] buffer = new byte[2];
    final byte[] bytes = new byte[2];

    final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
    byteBuffer.order(V1_BYTE_ORDER).position(0);

    for (int crc = 0; crc <= Character.MAX_VALUE; ++crc) {
      Arrays.fill(buffer, (byte) 0);
      Arrays.fill(bytes, (byte) 0);

      byteBuffer.order(V1_BYTE_ORDER).putChar((char) (crc & 0xFFFF)).order(V1_BYTE_ORDER).position(0);
      new V1VoltronicMessageDigest().writeCrcBytes(crc, bytes, 0);

      for (int bufferIndex = 0; bufferIndex < buffer.length; ++bufferIndex) {
        if (RESERVED_BYTES.contains(buffer[bufferIndex])) {
          buffer[bufferIndex]++;
        }
      }

      assertArrayEquals(buffer, bytes);
    }
  }

  @Test
  @DisplayName("Empty digest output is 0")
  public void emptyDigestIs0() {
    assertArrayEquals(bytes(0, 0), md.digest());
  }

  @Test
  @DisplayName("Reset clears digest")
  public void resetClearsDigest() {
    md.update(bytes("abcde"));
    md.reset();
    assertArrayEquals(bytes(0, 0), md.digest());
  }

  @Test
  @DisplayName("Test known inputs for correctness")
  public void testKnownInputs() {
    md.reset();
    md.update(bytes("QPIGS"));
    assertArrayEquals(bytes(0xB7, 0xA9), md.digest());

    md.reset();
    md.update(bytes(
        "(240.1 49.9 240.1 49.9 0240 0185 004 435 54.00 000 100 0055 0000 000.0 00.00 00000 00010101 00 00 00000 110"));
    assertArrayEquals(bytes(0xDB, 0x0E), md.digest());

    md.reset();
    md.update(bytes("QPI"));
    assertArrayEquals(bytes(0xBE, 0xAC), md.digest());
  }

  private byte[] writeCrcBytes(final int crc) {
    final byte[] bytes = new byte[2];
    Arrays.fill(bytes, (byte) 0);
    new V1VoltronicMessageDigest().writeCrcBytes(crc, bytes, 0);
    return bytes;
  }

}
