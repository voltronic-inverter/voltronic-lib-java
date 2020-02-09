## VoltronicLibJava
Library providing [Voltronic](http://voltronicpower.com) device specific code.

## License
This entire library is licenced under GNU GENERAL PUBLIC LICENSE v3

That means if you want to use this library you can, provided your source code is also also open sourced.

**I will not take lightly any use of this implementation in ANY closed source software**

##Hardware interface
This is a library providing an implementation for the underlying Voltronic protocol.  It does not implement any hardware interfacing.  Interfacing with Voltronic hardware requires a SerialPort, USB, etc. library, many exist already.  Instead this library implements the protocol on top of an existing hardware interfacing library.

Device wrapping [RXTX](http://rxtx.qbang.org/wiki/index.php/Two_way_communcation_with_the_serial_port)
```java
import com.voltronicpower.Device;

public Device wrap(final gnu.io.SerialPort serialPort configuredSerialPort) {
  final InputStream in = serialPort.getInputStream();
  final OutputStream out = serialPort.getOutputStream();

  return new Device() {
    public int read(byte[] b, int off, int len) throws IOException {
      return int.read(b, off, len);
    }

    public void write(byte[] b, int off, int len) throws IOException {
      return out.write(b, off, len);
    }

    public void close() throws IOException {
      configuredSerialPort.close();
    }
  };
}
```

## Usage

Writing a command to the device
```java
import com.voltronicpower.Device;
import com.voltronicpower.Protocol;
import com.voltronicpower.protocol.P30Protocol;
import com.voltronicpower.exception.BufferOverflowException;
import com.voltronicpower.exception.DigestMismatchException;
import com.voltronicpower.exception.TimeoutException;
import com.voltronicpower.exception.TruncatedDataException;
import java.util.concurrent.TimeUnit;

public class ExampleClass {
  Protocol protocol = new P30Protocol();

  public String sendCommand(Device device, String command, long timeoutSeconds) throws IOException {
    // Write command to the device
    try {
      protocol.write(device, command);
    } catch(IOException e) {
      System.err.println("The hardware library threw an IOException while writing the command; " + e.getMessage());
      throw e;
    }

    // Read response from the device
    try {
      return protocol.read(device, timeoutMilliseconds, TimeUnit.SECONDS);
    } catch(BufferOverflowException e) {
      System.err.println("Device keeps responding with data, limit is " + e.getByteLimit());
      throw e;
    } catch(DigestMismatchException e) {
      System.err.println("Device response did not match the calculated CRC; " + e.getMessage());
      System.err.println("Received CRC bytes = " + Arrays.toString(e.getReceivedDigest()));
      System.err.println("Calculated CRC bytes = " + Arrays.toString(e.getCalculatedDigest()));
      throw e;
    } catch(TimeoutException e) {
      System.err.println("Device took longer than 2 seconds to respond (" + e.getNanosecondsTimeout() + " nanoseconds)");
      throw e;
    } catch(TruncatedDataException e) {
      System.err.println("Device responded with " + e.getBytesReceived() " bytes but at least " + e.getBytesExpected()" bytes were expected; " + e.getMessage());
      throw e;
    } catch(InterruptedIOException e) {
      System.err.println("Could not complete reading data from the device because the thread was interrupted by the JVM. " + e.bytesTransferred + " bytes were read before being interrupted");
      throw e;
    } catch(IOException e) {
      System.err.println("The hardware library threw an IOException while reading the response; " + e.getMessage());
      throw e;
    }
  }

  public static void main() throws Exception {
    try(Device device = createMyDeviceMethod()) {
      System.out.println(sendCommand(device, "QPIGS", 2));
    }
  }
}
```

## Communication protocol
The communication protocol consists of the following format:

**Overall the protocol has the following format:**

`{bytes}{CRC16}{end of input character}`
- **bytes** the actual bytes being sent to the device, generally speaking this is the *"command"*
- **CRC16** common CRC protocol with many implementations online
- **end of input character** character signaling the end of input

### Reserved characters
These characters are reserved
- `\r` (*0x0d*) End of input character
- `(` (*0x28*) Seems to indicate start of input
- `\n` (*0x0a*) No material importance but still reserved

### Bytes
The bytes being sent to the device appear to be simply ASCII in the form of a command

Multiple documents exist listing possible commands
 - [Axpert](https://s3-eu-west-1.amazonaws.com/osor62gd45llv5fcg47yijafsz6dcrjn/HS_MS_MSX_RS232_Protocol_20140822_after_current_upgrade.pdf)
 - [Infini Solar](https://s3-eu-west-1.amazonaws.com/osor62gd45llv5fcg47yijafsz6dcrjn/Infini_RS232_Protocol.pdf)

### CRC
The CRC used is a modified variant of [CRC16 XMODEM](https://pycrc.org/models.html#xmodem)

**Background**

Multiple methods exist to [generate CRC](https://en.wikipedia.org/wiki/Computation_of_cyclic_redundancy_checks).

CRC16 as the name implies contains 16 bits or 2 bytes of data.
It is commonly written as hexadecimal for readability reason, ie> `0x17AD`

Two hexadecimal character represent a single byte so given the example above.
`0x` part simply indicates hexadecimal
`0x17` is the first byte
`0xAD` is the second byte

**Exception**

The **Reserved characters** are not allowed in the CRC.
It appears the device simply expects them to be incremented by 1

So `0x28` becomes `0x29`, `0x0d` becomes `0x0e`, etc.

### End of input character
The `\r` character signals to the device end of input

Regardless of what the device received up to that point `\r` signals to the device end of current input

Once this character is received all input up to that point is taken as the *command* to the device

## Input methods
Devices from Voltronic are shipped with 4 possible hardware interfaces: RS232, USB, Bluetooth & RS485

All input methods share the same bandwidth & latency.
Although it would appear at surface that USB should be faster, no measureable difference exists to device response time and symbol rate.

USB is also an asynchronous protocol and as such could be influenced by other factors slowing it down further

### Simultaneous communication across multiple interfaces
During testing it was found that simultaneous communication across USB & RS232 for example would result in device lockup.
The device keeps operating, but the device will no longer respond to input or produce output.

As such it is adviced to pick an interface and use it exclusively

### RS232
Nothing special to mention here, synchronous protocol with the following configuration:
- **Baud** *2400*
- **Data bits** *8*
- **Stop bits** *1*
- **Parity** *None*

### USB
The device makes use of an [HID interface](https://en.wikipedia.org/wiki/USB_human_interface_device_class).
In Linux the device is presented as a [*HIDRaw* device](https://www.kernel.org/doc/Documentation/hid/hidraw.txt)

It is **not** a USB->Serial

So in Linux for example:

**Ruby:**
```ruby
fd = File.open('/dev/hidraw0', IO::RDWR|IO::NONBLOCK) # May need root, or make the file 666 using udev rules
fd.binmode
fd.sync = true
fd.write("QPI\xBE\xAC\r") # Will write QPI => Returns 6
fd.gets("\r") #=> "(PI30\x9A\v\r"
```

**Python:**
```python
import os, sys
fd = open("/dev/hidraw0", os.O_RDWR|os.O_NONBLOCK)
os.write(fd, "QPI\xBE\xAC\r")
os.read(fd, 512)
```

**Avoiding the need for root**

Make use of [**udev**](https://wiki.debian.org/udev) to specify more broad access:

```bash
# may require root
touch /etc/udev﻿/rules.d/15-voltronic.rules
echo 'ATTRS{idVendor}=="0665", ATTRS{idProduct}=="5161", SUBSYSTEMS=="usb", ACTION=="add", MODE="0666", SYMLINK+="hidVoltronic"' > /etc/udev﻿/rules.d/15-voltronic.rules
```

When the device is connected it will present in `/dev/hidVoltronic`.

Note that if multiple devices are to be connected to the same machine, an additional **udev** parameter should be specified such as the device serial number to with different symlink names

### Bluetooth
Newer generation [Axpert devices](http://voltronicpower.com/en-US/Product/Detail/Axpert-King-3KVA-5KVA) feature Bluetooth

No testing has been completed on these devices but Bluetooth simply operates exactly like RS232 and therefore there is no reason to believe it would be otherwise

### RS485
Newer generation [Axpert devices](http://voltronicpower.com/en-US/Product/Detail/Axpert-King-3KVA-5KVA) feature RS485 support

No testing has been completed on these devices but there is no reason to believe the underlying protocol has changed at all
