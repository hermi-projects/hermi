package org.hermi.commons.mask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public final class MaskMapper {

  private MaskMapper() {}

  private static final Logger LOG = LoggerFactory.getLogger(MaskMapper.class);
  private static final JsonMapper MAPPER = JsonMapper.builder().addModule(new MaskModule()).build();

  public static String mask(Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (JacksonException e) {
      LOG.error("Mask serialization failed, falling back to toString()", e);
      return obj.toString();
    }
  }
}
