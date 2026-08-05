package org.hermi.test;

import java.util.List;

public interface Boundary<T> {
  T validValue();
  List<T> invalidValues();
}
