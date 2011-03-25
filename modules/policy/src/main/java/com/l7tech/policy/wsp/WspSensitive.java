package com.l7tech.policy.wsp;

import java.lang.annotation.*;

/**
 * Annotation used to mark a field as containing sensitive information (such as a password).
 * Eventually such fields may be encrypted at the WSP level.
 * For now, this annotation will cause a validator warning if the field serializes as a string value
 * that is not either empty or similar to "${secpass.*.ciphertext}".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Documented
public @interface WspSensitive {
}
