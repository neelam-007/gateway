package com.l7tech.common.locator;

import java.util.Collection;

/**
 * The <code>ObjectFactory</code> implementations create the
 * the types or subtypes specified by the <code>Class</code>
 * parameter in the {@link ObjectFactory#getInstance} method.
 * <p>
 * The implementations may be used in conjunction with the
 * <code>Locator</code> implementation
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface ObjectFactory {
    /**
     * create the object instance of the class cl with
     * optional context <code>Collection</code>.
     *
     * @param cl the class that
     * @param context optional context collection
     * @return the object instance of the class type
     */
    Object getInstance(Class cl, Collection context);
}
