package org.hermi.annotations.serializers;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class MaskSerializer extends StdSerializer<Object> {

  public MaskSerializer() {
    super(Object.class);
  }

  @Override
  public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt)
      throws JacksonException {
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
