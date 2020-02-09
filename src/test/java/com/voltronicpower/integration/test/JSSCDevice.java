package com.voltronicpower.integration.test;

import com.voltronicpower.Device;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import jssc.SerialPort;
import jssc.SerialPortException;

public class JSSCDevice implements Device {

  private final SerialPort serialPort;
  private InputStream inputStream;

  public JSSCDevice(final String serialPortName) throws IOException {
    final SerialPort serialPort = new SerialPort(serialPortName);
    boolean success = false;
    try {
      serialPort.openPort();
      serialPort.setParams(2400, 8, 1, 0);
      success = true;
    } catch (SerialPortException ex) {
      throw new IOException(ex.getMessage());
    } finally {
      if (!success) {
        try {
          serialPort.closePort();
        } catch(SerialPortException e) {
          System.err.println(e.toString());
        }
      }
    }

    this.serialPort = serialPort;
    this.inputStream = new ByteArrayInputStream(new byte[0]);
  }

  public int read(byte[] b, int off, int len) throws IOException {
    final int bytesRead = this.inputStream.read(b, off, len);

    if (bytesRead > 0) {
      return bytesRead;
    } else {
      final byte[] bytes;
      try {
        bytes = this.serialPort.readBytes();
      } catch (final Exception e) {
        throw new IOException(e.getMessage());
      }

      if (bytes != null && bytes.length > 0) {
        this.inputStream = new ByteArrayInputStream(bytes);
        return this.inputStream.read(b, off, len);
      } else {
        return 0;
      }
    }
  }

  public void write(byte[] b, int off, int len) throws IOException {
    final byte[] copy = new byte[len];
    System.arraycopy(b, off, copy, 0, len);

    try {
      this.serialPort.writeBytes(copy);
    } catch(final Exception e) {
      throw new IOException(e.getMessage());
    }
  }

  public void close() throws IOException {
    try {
      this.serialPort.closePort();
    } catch (final Exception e) {
      throw new IOException(e.getMessage());
    }
  }

}
