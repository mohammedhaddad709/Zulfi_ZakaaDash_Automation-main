package com.taf.core.media;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Default browser video evidence for a test class. An explicit {@code -Dreport.video=OFF|ON_FAILURE|ALWAYS}
 * always wins over this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VideoEvidence {

    Mode value() default Mode.ON_FAILURE;

    enum Mode { OFF, ON_FAILURE, ALWAYS }
}
