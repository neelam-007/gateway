/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.util;

/**
 * Interim-only, cheap Canadian knock-off of currently-unavailable Croatian interface Refreshable.
 * TODO: Replace with true version when it becomes available.
 */
public interface Refreshable {
    boolean canRefresh();
    void refresh();
}
