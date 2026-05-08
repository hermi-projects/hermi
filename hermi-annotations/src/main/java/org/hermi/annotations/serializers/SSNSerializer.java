package org.hermi.annotations.serializers;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class SSNSerializer extends StdSerializer<String> {
  public SSNSerializer() {
    super(String.class);
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializationContext ctxt)
      throws JacksonException {
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
