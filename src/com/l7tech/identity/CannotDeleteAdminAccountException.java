package com.l7tech.identity;

import com.l7tech.objectmodel.DeleteException;

/**
 * User: flascell
 * Date: Jul 1, 2003
 * Time: 10:27:45 AM
 *
 * Thrown when an attempt is made to delete an administrator account.
 */
public final class CannotDeleteAdminAccountException extends DeleteException {
    public CannotDeleteAdminAccountException() {
        super("Deletion of administration account is not allowed.");
    }
}
