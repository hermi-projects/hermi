package org.hermi.logging.commons.util;

import org.hermi.annotations.SSN;
import org.hermi.logging.annotations.HermiLoggingRequired;

public class SSNCustomer {
  @SSN @HermiLoggingRequired private String ssn;

  public SSNCustomer(String ssn) {
    this.ssn = ssn;
  }
}
