/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security;

import java.util.EventListener;

/**
 * @author alex
*/
public interface PermissionRefreshListener extends EventListener {
    void onPermissionRefresh();
}
