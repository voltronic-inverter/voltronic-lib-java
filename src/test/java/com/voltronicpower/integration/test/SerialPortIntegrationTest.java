package com.voltronicpower.integration.test;

import com.voltronicpower.Device;
import com.voltronicpower.Protocol;
import com.voltronicpower.protocol.P30Protocol;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class SerialPortIntegrationTest {

  private static final String SERIAL_PORT_LOCATION = "/dev/tty.usbserial";

  private Device device;
  private Protocol protocol;

  @Test
  @DisplayName("Test QPIGS command")
  public void testQPIGS() throws IOException {
    System.out.println(sendCommand("QPIGS"));
  }

  @Test
  @DisplayName("Test QPI command")
  public void testQPI() throws IOException {
    System.out.println(sendCommand("QPI"));
  }

  @BeforeAll
  public void createProtocol() {
    this.protocol = new P30Protocol();
  }

  @BeforeAll
  public void setupDevice() throws IOException {
    device = new JSSCDevice(SERIAL_PORT_LOCATION);
  }

  @AfterAll
  public void closeDevice() throws IOException {
    device.close();
  }

  public String sendCommand(final String command) throws IOException {
    this.protocol.write(device, command);
    return this.protocol.read(device, 2, TimeUnit.SECONDS);
  }

}
