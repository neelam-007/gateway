package com.l7tech.xml;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;

/**
 * Utility methods and singletons related to use of the Saxon API.
 */
public class SaxonUtils {
    private static final Processor processor = new Processor(false);

    /**
     * Get a singleton Processor instance.
     *
     * @return the singleton processor.  Never null.
     */
    public static Processor getProcessor() {
        return processor;
    }

    /**
     * Get a singleton Saxon Configuration instance.
     *
     * @return the singleton configuration.  Never null.
     */
    public static Configuration getConfiguration() {
        return processor.getUnderlyingConfiguration();
    }
}
