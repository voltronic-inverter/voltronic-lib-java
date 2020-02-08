package com.voltronicpower.digest;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.voltronicpower.ByteUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class V1VoltronicMessageDigestTest {

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
  @DisplayName("Reserved bytes check matches known reserved bytes")
  public void reservedBytesDetectedCorrectly() {
    final V1VoltronicMessageDigest md = new V1VoltronicMessageDigest();

    for (int count = 0; count <= 0xFF; ++count) {
      final byte b = (byte) count;
      if (RESERVED_BYTES.contains(b)) {
        assertTrue(md.isReservedByte(b));
      } else {
        assertFalse(md.isReservedByte(b));
      }
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

}
