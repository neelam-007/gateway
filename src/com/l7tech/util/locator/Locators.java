package com.l7tech.util.locator;

import com.l7tech.util.Locator;

/**
 * A convinience class with couple of static factory methods. This class cannot
 * be instantiated.
 *
  * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Locators {

    /** this class cannot be instantiated */
    private Locators() {
    }

    /**
     * Returns the property based locator
     */
     public static Locator propertiesLocator(String resource, ClassLoader cl) {
        return new PropertiesLocator(resource, cl);
     }
}
