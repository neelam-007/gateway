/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

/**
 * @author alex
 */
public interface HasHeaders {
    /**
     * @return an array of the values for the specified header in the request or response, or an empty array if there are none. Never null.
     */
    String[] getHeaderValues(String name);
}
