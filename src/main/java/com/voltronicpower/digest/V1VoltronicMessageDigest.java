package com.voltronicpower.digest;

import java.security.DigestException;
import java.security.MessageDigest;

public class V1VoltronicMessageDigest extends MessageDigest {

  private static final char[] HALF_BYTE_CRC_TABLE = new char[]{
      (char) 0x0000, (char) 0x1021, (char) 0x2042, (char) 0x3063,
      (char) 0x4084, (char) 0x50A5, (char) 0x60C6, (char) 0x70E7,
      (char) 0x8108, (char) 0x9129, (char) 0xA14A, (char) 0xB16B,
      (char) 0xC18C, (char) 0xD1AD, (char) 0xE1CE, (char) 0xF1EF
  };

  private char crc;

  public V1VoltronicMessageDigest() {
    super("Voltronic Modified CRC-16 XMODEM");
    this.engineReset();
  }

  protected void engineUpdate(final byte b) {
    this.crc = (char) (HALF_BYTE_CRC_TABLE[(this.crc >> 12) ^ (b >> 4)] ^ (this.crc << 4));
    this.crc = (char) (HALF_BYTE_CRC_TABLE[(this.crc >> 12) ^ (b & 0x0F)] ^ (this.crc << 4));
  }

  protected void engineUpdate(final byte[] b, int off, int len) {
    if (len > 0) {
      --off;
      do {
        this.engineUpdate(b[++off]);
      } while (--len != 0);
    }
  }

  protected byte[] engineDigest() {
    final byte[] bytes = new byte[this.engineGetDigestLength()];

    try {
      this.engineDigest(bytes, 0, bytes.length);
    } catch (final DigestException e) {
      throw new RuntimeException(e);
    }

    return bytes;
  }

  protected int engineDigest(
      final byte[] buf,
      final int off,
      final int len) throws DigestException {

    final int digestLength = this.engineGetDigestLength();
    if (len < digestLength) {
      throw new DigestException("partial digests not returned");
    } else if (buf.length - off < digestLength) {
      throw new DigestException("insufficient space in the output buffer to store the digest");
    } else {
      char crc = this.crc;

      if (this.isReservedByte((byte) crc)) {
        crc += 1;
      }

      if (this.isReservedByte((byte) (crc >> 8))) {
        crc += 256;
      }

      buf[off] = (byte) ((crc >> 8) & 0xFF);
      buf[off + 1] = (byte) (crc & 0xFF);

      return digestLength;
    }
  }


  protected void engineReset() {
    this.crc = 0;
  }

  protected int engineGetDigestLength() {
    return 2;
  }

  boolean isReservedByte(final byte b) {
    return b == 0x0A || b == 0x0D || b == 0x28;
  }

}
