package org.hermi.commons.mask;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class MaskMapper {

  private MaskMapper() {}

  private static class Holder {
    static final MaskMapper INSTANCE = new MaskMapper();
  }

  public static MaskMapper instance() {
    return Holder.INSTANCE;
  }

  private final JsonMapper mapper = JsonMapper.builder().addModule(new MaskModule()).build();

  public JsonMapper getMapper() {
    return mapper;
  }

  public String mask(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JacksonException e) {
      throw new RuntimeException("Mask serialization failed", e);
    }
  }
}
