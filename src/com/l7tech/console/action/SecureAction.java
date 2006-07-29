package com.l7tech.console.action;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.security.rbac.AttemptedOperation;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.LicenseListener;
import com.l7tech.console.util.Registry;
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
public abstract class SecureAction extends BaseAction implements LogonListener, LicenseListener {
    static final Logger logger = Logger.getLogger(SecureAction.class.getName());

    /** Specify that an action requires at least one authentication assertion to be licensed. */
    public static final Collection<Class<? extends Assertion>> LIC_AUTH_ASSERTIONS =
            Arrays.<Class<? extends Assertion>>asList(SpecificUser.class, MemberOfGroup.class);

    private final Set<String> assertionLicenseClassnames = new HashSet<String>();
    private final AttemptedOperation attemptedOperation;

    /**
     * Create a SecureAction which may only be enabled if the user meets the admin requirement,
     * and which does not depend on any licensing feature to be enabled.
     */
    protected SecureAction(AttemptedOperation attemptedOperation) {
        this.attemptedOperation = attemptedOperation;
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if the specified assertion license is enabled (if specified).
     *
     * @param requiredAssertionLicense  if non-null, action will be disabled unless the specified assertion is licensed
     */
    protected SecureAction(AttemptedOperation attemptedOperation, Class<? extends Assertion> requiredAssertionLicense) {
        this(attemptedOperation);
        if (requiredAssertionLicense != null)
            assertionLicenseClassnames.add(requiredAssertionLicense.getName());
        initLicenseListener();
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if at least one of the specified assertion licenses is enabled.
     *
     * @param allowedAssertionLicenses
     */
    protected SecureAction(AttemptedOperation attemptedOperation, Collection<Class<? extends Assertion>> allowedAssertionLicenses) {
        this.attemptedOperation = attemptedOperation;
        if (allowedAssertionLicenses != null)
            for (Class<? extends Assertion> clazz : allowedAssertionLicenses)
                assertionLicenseClassnames.add(clazz.getName());
        initLicenseListener();
    }

    /** Registers this action as a license listener, if any license requirements were set on it. */
    private void initLicenseListener() {
        if (assertionLicenseClassnames.isEmpty()) return;
        Registry.getDefault().getLicenseManager().addLicenseListener(this);
    }

    /**
     * Test whether the current subject is authorized to perform the action
     * If the current subject is not set <code>false</code> is returnced
     *
     * @return true if the current subject is authorized, false otheriwse
     */
    public final boolean isAuthorized() {
        if (attemptedOperation == null) return true;
        return canAttemptOperation(attemptedOperation);
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

    public final boolean canAttemptOperation(AttemptedOperation ao) {
        return getSecurityProvider().hasPermission(Subject.getSubject(AccessController.getContext()), ao);
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
        boolean wasEnabled = isEnabled();
        final boolean enabled = b && isAuthorized() && isLicensed();
        super.setEnabled(enabled);
        boolean isEnabled = isEnabled();
        if (wasEnabled != isEnabled)
            firePropertyChange("enabled", wasEnabled, isEnabled);
    }

    public void licenseChanged(ConsoleLicenseManager licenseManager) {
        setEnabled(true); // it'll immediately get forced back to false if the license disallows it
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
//        TODO should we do hard license check here?
//        or should we allow programmatic invocation of otherwise-unlicensed actions?
//        if (!isLicensed()) {
//            throw new AccessControlException("Not licensed, action: " + getName());
//        }
        super.actionPerformed(ev);
    }

    protected final SecurityProvider getSecurityProvider() {
        return Registry.getDefault().getSecurityProvider();
    }

    public void onLogon(LogonEvent e) {
        setEnabled(isAuthorized());
    }

    public void onLogoff(LogonEvent e) {
        setEnabled(false);
    }
}
