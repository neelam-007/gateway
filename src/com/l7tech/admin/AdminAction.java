/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;



/**
 * A administrative action to be performed with admin privileges enabled,
 * that throws one or  more checked exceptions.  The action is performed by
 * invoking <code>AdminContext.invoke</code>.
 *
 * @see AdminContext
 * @see AdminContext#invoke(AdminAction[])
 */

public interface AdminAction {
    /**
     * Performs the admin action.  This method will be called by
     * <code>AdminContext.invoke</code> with admim privileges set.
     *
     * @return a class-dependent value that may represent the results of the
     *	       action or null.
     * @throws Exception an exceptional condition has occurred.
     */
    Object run() throws Exception;
}