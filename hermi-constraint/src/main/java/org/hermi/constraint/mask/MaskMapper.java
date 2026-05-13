package org.hermi.constraint.mask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for converting an object to a masked {@link JsonNode}. All properties annotated with
 * masking constraints (e.g. {@link org.hermi.constraint.annotations.Mask}, {@link
 * org.hermi.constraint.annotations.SSN}) are automatically masked during the conversion.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * JsonNode masked = MaskMapper.mask(myObject);
 * }</pre>
 */
public final class MaskMapper {

  private MaskMapper() {}

  private static final Logger LOG = LoggerFactory.getLogger(MaskMapper.class);
  private static final JsonMapper MAPPER = JsonMapper.builder().addModule(new MaskModule()).build();

  public static JsonNode mask(Object obj) {
    try {
      return MAPPER.valueToTree(obj);
    } catch (Exception e) {
      LOG.error("Mask serialization failed, falling back to toString()", e);
      return MAPPER.valueToTree(obj.toString());
    }
  }
}
