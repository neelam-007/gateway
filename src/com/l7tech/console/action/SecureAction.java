package com.l7tech.console.action;

import java.awt.event.ActionEvent;
import java.security.AccessControlException;

/**
 * @author emil
 * @version 3-Sep-2004
 */
public abstract class SecureAction extends BaseAction {
    /**
     * Test whether the current subject is authorized to perform the action
     * @return
     */
    public boolean isAuthorized() {
        //todo: authorization check
        return true;
    }
    /**
     * Overriden {@link BaseAction#actionPerformed(java.awt.event.ActionEvent)}
     * that performs security check
     *
     * @param ev the action event
     * @throws AccessControlException
     */
    public final void actionPerformed(ActionEvent ev) throws AccessControlException {
        if (!isAuthorized()) {
            throw new AccessControlException("Not authorized, action: "+getName());
        }
        super.actionPerformed(ev);
    }
}
