package org.hermi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.annotations.logging.Constraint;
import org.hermi.annotations.markers.MaskMarker;

@Constraint(markedBy = MaskMarker.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {}
