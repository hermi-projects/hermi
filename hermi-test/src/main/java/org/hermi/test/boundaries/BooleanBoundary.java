package org.hermi.test.boundaries;

import org.hermi.test.Boundary;

import java.util.List;

public class BooleanBoundary implements Boundary<Boolean> {
  @Override
  public Boolean validValue() {
    return true;
  }

  @Override
  public List<Boolean> invalidValues() {
    return List.of(false, null);
  }
}
