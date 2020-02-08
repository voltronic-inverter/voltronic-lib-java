package com.voltronicpower;

import java.io.IOException;

public interface Device {

  int read(byte[] b, int off, int len) throws IOException;

  void write(byte[] b, int off, int len) throws IOException;

  void close() throws IOException;

}
