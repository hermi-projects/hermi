package org.hermi.logging.commons.util;

import org.hermi.logging.annotations.HermiLoggingRequired;

public class Customer {
  @HermiLoggingRequired private String ssn;
  private String firstName;

  public Customer(String ssn) {
    this.ssn = ssn;
  }

  public Customer(String ssn, String firstName) {
    this(ssn);
    this.firstName = firstName;
  }
}
