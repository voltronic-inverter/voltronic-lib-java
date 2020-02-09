package com.voltronicpower;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ByteUtil {

  private ByteUtil() {
  }

  public static byte[] bytes(final byte b) {
    return new byte[] { b };
  }

  public static byte[] bytes(final byte[]... arrays) {
    int length = 0;
    for (int index = 0; index < arrays.length; ++index) {
      length += arrays[index].length;
    }
    final byte[] b = new byte[length];
    int offset = 0;
    for (int index = 0; index < arrays.length; ++index) {
      final byte[] array = arrays[index];
      System.arraycopy(array, 0, b, offset, array.length);
      offset += array.length;
    }
    return b;
  }

  public static byte[] bytes(final int... array) {
    final byte[] b = new byte[array.length];
    for (int index = 0; index < b.length; ++index) {
      b[index] = (byte) array[index];
    }
    return b;
  }

  public static byte[] bytes(final String string) {
    final byte[] bytes = new byte[string.length()];
    for(int index = 0; index < string.length(); ++index) {
      final int codepoint = string.codePointAt(index);
      if (codepoint > 255) {
        throw new IllegalArgumentException("Only ASCII supported");
      }
      bytes[index] = (byte) codepoint;
    }
    return bytes;
  }

  public static List<Byte> list(final int... array) {
    final List<Byte> list = new ArrayList<Byte>(array.length);
    for (int b : array) {
      list.add((byte) b);
    }
    return Collections.unmodifiableList(list);
  }

}
