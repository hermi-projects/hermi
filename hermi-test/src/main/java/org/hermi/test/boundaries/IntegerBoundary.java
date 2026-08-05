package org.hermi.test.boundaries;

import org.hermi.test.Boundary;

import java.util.List;

public class IntegerBoundary implements Boundary<Integer> {
  @Override
  public Integer validValue() {
    return 25;
  }

  @Override
  public List<Integer> invalidValues() {
    return List.of(Integer.MIN_VALUE, -1, 0, 18, 120, Integer.MAX_VALUE);
  }
}
