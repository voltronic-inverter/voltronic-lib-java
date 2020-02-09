package com.voltronicpower;

import java.io.Closeable;
import java.io.IOException;

public interface Device extends Closeable {

  int read(byte[] b, int off, int len) throws IOException;

  void write(byte[] b, int off, int len) throws IOException;

  void close() throws IOException;

}
