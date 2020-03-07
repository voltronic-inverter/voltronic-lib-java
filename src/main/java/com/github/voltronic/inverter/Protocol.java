package com.github.voltronic.inverter;

import com.github.voltronic.inverter.exception.BufferOverflowException;
import com.github.voltronic.inverter.exception.DigestMismatchException;
import com.github.voltronic.inverter.exception.TimeoutException;
import com.github.voltronic.inverter.exception.TruncatedDataException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface Protocol {

  /**
   * Read from <code>device</code> using this protocol until the end is reached
   * or the timeout is exceeded
   *
   * @param device hardware interface
   * @param timeout timeout before giving up
   * @param timeoutTimeUnit TimeUnit to use for the timeout value
   * @return data read from the device
   * @exception NullPointerException if <code>device</code> or <code>timeoutTimeUnit</code> is null
   * @exception IllegalArgumentException if <code>timeout</code> < 0
   * @exception BufferOverflowException the device responded with more data than the buffer can hold
   * @exception DigestMismatchException the calculated digest for the data does not match the device response digest
   * @exception TimeoutException the <code>timeout</code> was reached before end of response was reached
   * @exception TruncatedDataException the response form the device was truncated/did not contain enough bytes
   * @exception java.io.InterruptedIOException if the thread was interrupted before all data could be read
   * @throws IOException if the underlying <code>device</code> threw an IOException
   */
  String read(Device device, long timeout, TimeUnit timeoutTimeUnit) throws IOException;

  /**
   * Write <code>input</code> to the device
   *
   * @param device hardware interface
   * @param input input to write to device
   * @throws NullPointerException if <code>device</code> or <code>input</code> is null
   * @throws IOException if the underlying <code>device</code> threw an IOException
   */
  void write(Device device, CharSequence input) throws IOException;

}
