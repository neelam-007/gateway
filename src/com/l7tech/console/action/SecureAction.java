package com.l7tech.console.action;

import com.l7tech.console.security.LogonEvent;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;

import javax.security.auth.Subject;
import java.awt.event.ActionEvent;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 3-Sep-2004
 */
public abstract class SecureAction extends BaseAction implements LogonListener {
    static final Logger logger = Logger.getLogger(SecureAction.class.getName());

    protected SecureAction() {
    }

    /**
     * Test whether the current subject is authorized to perform the action
     * If the current subject is not set <code>false</code> is returnced
     *
     * @return true if the current subject is authorized, false otheriwse
     */
    public final boolean isAuthorized() {
        return isInRole(requiredRoles());
    }

    /**
     * Determines whether the current subject belongs to the specified Role (group).
     *
     * @param roles the string array of role names
     * @return true if the subject, belongs to one of the the specified roles,
     *         false, otherwise.
     */

    public final boolean isInRole(String[] roles) {
        final Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null || subject.getPrincipals().isEmpty()) { // if no subject or no principal
            return false;
        }
        return getSecurityProvider().isSubjectInRole(subject, roles);
    }

    /**
     * Returns true if the action is enabled.
     *
     * @return true if the action is enabled, false otherwise
     * @see javax.swing.Action#isEnabled
     */
    public boolean isEnabled() {
        final boolean enabled = super.isEnabled() && isAuthorized();
        return enabled;
    }

    /**
     * Enables or disables the action.
     *
     * @param b true to enable the action, false to
     *          disable it
     * @see javax.swing.Action#setEnabled
     */
    public void setEnabled(boolean b) {
        final boolean enabled = b && isAuthorized();
        super.setEnabled(enabled);
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
            throw new AccessControlException("Not authorized, action: " + getName());
        }
        super.actionPerformed(ev);
    }

    protected final SecurityProvider getSecurityProvider() {
        return Registry.getDefault().getSecurityProvider();
    }

    /**
     * Return the required roles for this action, one of the roles. The base
     * implementatoin requires the strongest admin role.
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME};
    }


    public void onLogon(LogonEvent e) {
        setEnabled(isAuthorized());
    }

    public void onLogoff(LogonEvent e) {
        setEnabled(false);
    }
}
