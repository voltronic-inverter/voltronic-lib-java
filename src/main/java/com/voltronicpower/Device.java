package com.voltronicpower;

import java.io.Closeable;
import java.io.IOException;

public interface Device extends Closeable {

  /**
   * Reads up to <code>len</code> bytes of data from the device into
   * an array of bytes.  An attempt is made to read as many as
   * <code>len</code> bytes, but a smaller number may be read.
   * The number of bytes actually read is returned as an integer.
   *
   * <p> This method does not block if input data is unavailable
   *
   * <p> If <code>len</code> is zero, then no bytes are read and
   * <code>0</code> is returned; otherwise, there is an attempt to read at
   * least one byte. If no byte is available, the value <code>-1</code> is returned;
   * otherwise, at least one byte is read and stored into <code>b</code>.
   *
   * <p> The first byte read is stored into element <code>b[off]</code>, the
   * next one into <code>b[off+1]</code>, and so on. The number of bytes read
   * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
   * bytes actually read; these bytes will be stored in elements
   * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
   * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
   * <code>b[off+len-1]</code> unaffected.
   *
   * <p> In every case, elements <code>b[0]</code> through
   * <code>b[off]</code> and elements <code>b[off+len]</code> through
   * <code>b[b.length-1]</code> are unaffected.
   *
   * @param      b     the buffer into which the data is read.
   * @param      off   the start offset in array <code>b</code>
   *                   at which the data is written.
   * @param      len   the maximum number of bytes to read.
   * @return     the total number of bytes read into the buffer, or
   *             <code>-1</code> if no data is available
   * @exception  IOException If the input stream has been closed, or if
   * some other I/O error occurs.
   * @exception  NullPointerException If <code>b</code> is <code>null</code>.
   * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
   * <code>len</code> is negative, or <code>len</code> is greater than
   * <code>b.length - off</code>
   */
  int read(byte[] b, int off, int len) throws IOException;

  /**
   * Writes <code>len</code> bytes from the specified byte array
   * starting at offset <code>off</code> to this device.
   * The contract for <code>write(b, off, len)</code> is that
   * some of the bytes in the array <code>b</code> are written to the
   * device in order; element <code>b[off]</code> is the first
   * byte written and <code>b[off+len-1]</code> is the last byte written
   * by this operation.
   * <p>
   * If <code>b</code> is <code>null</code>, a
   * <code>NullPointerException</code> is thrown.
   * <p>
   * If <code>off</code> is negative, or <code>len</code> is negative, or
   * <code>off+len</code> is greater than the length of the array
   * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
   *
   * @param      b     the data.
   * @param      off   the start offset in the data.
   * @param      len   the number of bytes to write.
   * @exception  IOException  if an I/O error occurs. In particular,
   *             an <code>IOException</code> is thrown if the output
   *             stream is closed.
   */
  void write(byte[] b, int off, int len) throws IOException;

  /**
   * Closes this device and releases any system resources
   * associated with this device.
   *
   * @exception  IOException  if an I/O error occurs.
   */
  void close() throws IOException;

}
