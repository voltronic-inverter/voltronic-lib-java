package com.github.voltronic.inverter;

import java.security.MessageDigest;

public interface MessageDigestSupplier {

  /**
   * Provide a new instance of a MessageDigest
   * @return new MessageDigest
   */
  MessageDigest get();

}