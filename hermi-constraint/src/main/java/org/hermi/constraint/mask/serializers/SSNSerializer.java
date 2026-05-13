package org.hermi.constraint.mask.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.hermi.constraint.mask.ConstraintSerializer;

/**
 * SSN masking serializer that preserves only the last four digits. All digits beyond the last four
 * are replaced with {@code *}. Non-digit characters pass through unchanged.
 */
public class SSNSerializer extends ConstraintSerializer<String> {
  public SSNSerializer() {
    super(String.class);
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    if (value == null) {
      gen.writeString((String) null);
      return;
    }
    StringBuilder sb = new StringBuilder(value);
    int digits = 0;
    for (int i = sb.length() - 1; i >= 0; i--) {
      char c = sb.charAt(i);
      if (Character.isDigit(c)) {
        digits++;
        if (digits > 4) {
          sb.setCharAt(i, '*');
        }
      }
    }
    gen.writeString(sb.toString());
  }
}
