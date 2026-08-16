package org.hermi.constraint.mask.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.hermi.constraint.annotations.Mask;
import org.hermi.constraint.mask.ConstraintSerializer;

/**
 * Generic masking serializer that preserves the first and last character of a value, replacing
 * interior characters with {@code *}. Single-character and two-character values pass through
 * unmasked, as does null.
 */
public class MaskSerializer extends ConstraintSerializer<Mask, Object> {

  public MaskSerializer() {
    super(Object.class);
  }

  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    if (value == null) {
      gen.writeString((String) null);
      return;
    }
    String s = value.toString();
    if (s.length() <= 1) {
      gen.writeString(s);
      return;
    }
    String masked = s.charAt(0) + "*".repeat(s.length() - 2) + s.charAt(s.length() - 1);
    gen.writeString(masked);
  }
}
