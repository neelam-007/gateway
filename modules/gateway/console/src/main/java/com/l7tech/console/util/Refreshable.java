/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.util;

/**
 * Objects such as trees and editors may optionally implement this interface to
 * provide the capability to refresh itself.
 *
 * @author emil
 * @version 15-Apr-2004
 */
public interface Refreshable {
    /**
     * Refresh the object
     */
    void refresh();

    /**
     * Determine whether th object can be refreshed at this time
     *
     * @return true if can refresh, false otherwise
     */
    boolean canRefresh();
}
