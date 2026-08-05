package org.hermi.test.boundaries;

import org.hermi.test.Boundary;

import java.util.List;

public class StringBoundary implements Boundary<String> {
  @Override
  public String validValue() {
    return "string";
  }

  @Override
  public List<String> invalidValues() {
    return List.of(null, "", "   ", "A".repeat(255));
  }
}
