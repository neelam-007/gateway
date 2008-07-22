package com.l7tech.identity;

import com.l7tech.objectmodel.DeleteException;

/**
 * Thrown when an attempt is made to delete an administrator account.
 * <br/><br/>
 * User: flascell<br/>
 * Date: Jul 1, 2003
 */
public final class CannotDeleteAdminAccountException extends DeleteException {
    public CannotDeleteAdminAccountException() {
        super("Deletion of administration account is not allowed.");
    }
}
