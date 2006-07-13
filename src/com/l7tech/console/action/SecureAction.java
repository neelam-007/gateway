package com.l7tech.console.action;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.security.auth.Subject;
import java.awt.event.ActionEvent;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 3-Sep-2004
 */
public abstract class SecureAction extends BaseAction implements LogonListener {
    static final Logger logger = Logger.getLogger(SecureAction.class.getName());

    /** Specify that an action requires at least one authentication assertion to be licensed. */
    public static final Collection<Class<? extends Assertion>> LIC_AUTH_ASSERTIONS =
            Arrays.<Class<? extends Assertion>>asList(SpecificUser.class, MemberOfGroup.class);

    private final Set<String> assertionLicenseClassnames = new HashSet<String>();
    private final boolean requiresAdmin;

    /**
     * Create a SecureAction which may only be enabled if the user meets the admin requirement,
     * and which does not depend on any licensing feature to be enabled.
     *
     * @param requireAdmin  if true, action will be disabled unless current user is in Admin role
     */
    protected SecureAction(boolean requireAdmin) {
        this.requiresAdmin = requireAdmin;
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if the specified assertion license is enabled (if specified).
     *
     * @param requireAdmin  if true, action will be disabled unless current user is in Admin role
     * @param requiredAssertionLicense  if non-null, action will be disabled unless the specified assertion is licensed
     */
    protected SecureAction(boolean requireAdmin, Class<? extends Assertion> requiredAssertionLicense) {
        this(requireAdmin);
        if (requiredAssertionLicense != null)
            assertionLicenseClassnames.add(requiredAssertionLicense.getName());
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if at least one of the specified assertion licenses is enabled.
     *
     * @param requireAdmin
     * @param allowedAssertionLicenses
     */
    protected SecureAction(boolean requireAdmin, Collection<Class<? extends Assertion>> allowedAssertionLicenses) {
        this.requiresAdmin = requireAdmin;
        if (allowedAssertionLicenses != null)
            for (Class<? extends Assertion> clazz : allowedAssertionLicenses)
                assertionLicenseClassnames.add(clazz.getName());
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
     * Test whether the current license allows performing the action.
     * @return true if at least one of the required assertion license classnames is licensed.
     */
    public final boolean isLicensed() {
        if (assertionLicenseClassnames.isEmpty())
            return true;

        ConsoleLicenseManager lm = Registry.getDefault().getLicenseManager();
        for (String s : assertionLicenseClassnames)
            if (lm.isAssertionEnabled(s))
                return true;
        return false;
    }

    /**
     * Determines whether the current subject belongs to the specified Role (group).
     *
     * @param roles the string array of role names
     * @return true if the subject, belongs to one of the the specified roles,
     *         false, otherwise.
     */
    public final boolean isInRole(String[] roles) {
        if (!requiresAdmin)
            return true;

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
    public final boolean isEnabled() {
        return super.isEnabled() && isAuthorized() && isLicensed();
    }

    /**
     * Enables or disables the action.
     *
     * @param b true to enable the action, false to
     *          disable it
     * @see javax.swing.Action#setEnabled
     */
    public final void setEnabled(boolean b) {
        final boolean enabled = b && isAuthorized() && isLicensed();
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
