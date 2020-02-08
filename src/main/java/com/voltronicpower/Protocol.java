package com.voltronicpower;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Protocol {

  String read(Device device, long timeout, TimeUnit timeoutTimeUnit) throws IOException, TimeoutException;

  void write(Device device, CharSequence input) throws IOException;

}
