/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.*;

/**
 * Attached to the field or getter of an attribute on an {@link com.l7tech.objectmodel.Entity} to
 * indicate that the attribute is readable by default.
 * @author alex
 */
@Documented
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface DefaultReadable {
}
