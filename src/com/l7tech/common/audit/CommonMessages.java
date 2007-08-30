/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by various components (not just the SSG).
 * The ID range 100-999 inclusive is reserved for these messages.
 */
public class CommonMessages extends Messages {
    public static final M WSDL_OPERATION_NO_STYLE         = m(100, Level.INFO, "Couldn''t get style for BindingOperation {0}; assuming \"document\"");
    public static final M WSDL_OPERATION_PART_TYPE        = m(101, Level.INFO, "Part {0} has both an element and a type");
    public static final M WSDL_OPERATION_BAD_STYLE        = m(102, Level.INFO, "Unsupported style ''{0}'' for {1}");
    public static final M WSDL_OPERATION_NO_QNAMES_FOR_OP = m(103, Level.INFO, "Unable to find payload element QNames for BindingOperation {0}");
}
