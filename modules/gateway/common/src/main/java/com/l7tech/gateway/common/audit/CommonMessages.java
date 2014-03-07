/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by various components (not just the SSG).
 * The ID range 100-999 inclusive is reserved for these messages.
 */
public class CommonMessages extends Messages {
    public static final M WSDL_OPERATION_NO_STYLE         = m(100, Level.FINE, "Couldn''t get style for BindingOperation {0}; assuming \"document\"");
    public static final M WSDL_OPERATION_PART_TYPE        = m(101, Level.INFO, "Part {0} has both an element and a type");
    public static final M WSDL_OPERATION_BAD_STYLE        = m(102, Level.INFO, "Unsupported style ''{0}'' for {1}");
    public static final M WSDL_OPERATION_NO_QNAMES_FOR_OP = m(103, Level.INFO, "Unable to find payload element QNames for BindingOperation {0}");
    public static final M WSDL_OPERATION_PART_INVALID     = m(104, Level.INFO, "Part {0} not found");

    public static final M TEMPLATE_SUBSCRIPT_OUTOFRANGE = m(150, Level.WARNING, "Array subscript ({0}) in {1} out of range ({2} values); returning no values");
    public static final M TEMPLATE_SUSPICIOUS_TOSTRING  = m(151, Level.WARNING, "Variable ''{0}'' is not a String and cannot be converted to one.  (Actual type: {1})");
    public static final M TEMPLATE_UNSUPPORTED_VARIABLE = m(152, Level.FINE, "Unsupported variable: {0}");
    public static final M TEMPLATE_UNSUPPORTED_VARIABLE_WITH_EXCEPTION = m(153, Level.WARNING, "Unsupported variable: {0} ({1})");
}
