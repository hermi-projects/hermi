package org.hermi.commons.mask;

import static org.assertj.core.api.Assertions.assertThat;

import org.hermi.annotations.SSN;
import org.junit.jupiter.api.Test;

public class MaskMapperUnitTest {

  @Test
  void shouldSerializePlainObject() {
    String json = MaskMapper.mask(new UnmaskedUser("Alice", "123-45-6789"));
    assertThat(json).contains("Alice").contains("123-45-6789");
  }

  @Test
  void shouldMaskSSNField() {
    String json = MaskMapper.mask(new MaskedUser("Bob", "123-45-6789"));
    assertThat(json).contains("\"ssn\":\"***-**-6789\"");
  }

  @Test
  void shouldNotMaskNonAnnotatedFields() {
    String json = MaskMapper.mask(new MaskedUser("Carol", "987-65-4321"));
    assertThat(json).contains("\"name\":\"Carol\"");
  }

  @SuppressWarnings("unused")
  private static class UnmaskedUser {
    private String name;
    private String ssn;

    UnmaskedUser(String name, String ssn) {
      this.name = name;
      this.ssn = ssn;
    }

    public String getName() {
      return name;
    }

    public String getSsn() {
      return ssn;
    }
  }

  @SuppressWarnings("unused")
  private static class MaskedUser {
    private String name;
    @SSN private String ssn;

    MaskedUser(String name, String ssn) {
      this.name = name;
      this.ssn = ssn;
    }

    public String getName() {
      return name;
    }

    public String getSsn() {
      return ssn;
    }
  }
}
