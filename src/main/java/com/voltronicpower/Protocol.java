package com.voltronicpower;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface Protocol {

  String read(Device device, long timeout, TimeUnit timeoutTimeUnit) throws IOException;

  void write(Device device, CharSequence input) throws IOException;

}
