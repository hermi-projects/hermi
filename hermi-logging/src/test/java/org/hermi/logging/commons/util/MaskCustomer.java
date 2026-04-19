package org.hermi.logging.commons.util;

import org.hermi.annotations.Mask;
import org.hermi.logging.annotations.HermiLoggingRequired;

public class MaskCustomer {
  @Mask @HermiLoggingRequired private String ssn;

  public MaskCustomer(String ssn) {
    this.ssn = ssn;
  }
}
