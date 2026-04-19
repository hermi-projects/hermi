package org.hermi.annotations.markers;

import org.hermi.annotations.SSN;
import org.hermi.annotations.logging.ConstraintMarker;

public class SSNMarker implements ConstraintMarker<SSN, String> {
  @Override
  public String mark(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    // Extract digits only
    String digits = value.replaceAll("\\D", "");

    // SSN must have at least 4 digits to mask
    if (digits.length() < 4) {
      return value; // fallback
    }

    String last4 = digits.substring(digits.length() - 4);

    // Always return the canonical masked format
    return "***-**-" + last4;
  }
}
