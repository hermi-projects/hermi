package org.hermi.annotations.markers;

import org.hermi.annotations.Mask;
import org.hermi.annotations.logging.ConstraintMarker;

public class MaskMarker implements ConstraintMarker<Mask, Object> {
  @Override
  public String mark(Object value) {
    if (value == null) {
      return null;
    }

    String txt = value.toString();
    int len = txt.length();

    if (len == 0) {
      return "";
    }

    // Case 1: length > 4 → keep last 4
    if (len > 4) {
      String last4 = txt.substring(len - 4);
      return "*".repeat(len - 4) + last4;
    }

    // Case 2: length <= 4 → keep only last 2
    String last1 = txt.substring(len - 2);
    return "*".repeat(len - 2) + last1;
  }
}
