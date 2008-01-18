package com.l7tech.console.action;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.security.rbac.AttemptedOperation;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.PermissionRefreshListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.LicenseListener;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.Assertion;

import java.awt.event.ActionEvent;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 3-Sep-2004
 */
public abstract class SecureAction extends BaseAction implements LogonListener, LicenseListener, PermissionRefreshListener {
    static final Logger logger = Logger.getLogger(SecureAction.class.getName());

    // Well-known feature set names
    public static final String TRUSTSTORE_FEATURESET_NAME = "service:TrustStore";
    public static final String KEYSTORE_FEATURESET_NAME = "service:KeyStore";
    public static final String CA_FEATURESET_NAME = "service:CSRHandler";
    public static final String FTP_INPUT_FEATURESET_NAME = "service:FtpMessageInput";
    public static final String HTTP_INPUT_FEATURESET_NAME = "service:HttpMessageInput";
    public static final String JMS_INPUT_FEATURESET_NAME = "service:JmsMessageInput";

    public static final String UI_PUBLISH_SERVICE_WIZARD = "ui:PublishServiceWizard";
    public static final String UI_PUBLISH_XML_WIZARD = "ui:PublishXmlWizard";
    public static final String UI_WSDL_CREATE_WIZARD = "ui:WsdlCreateWizard";
    public static final String UI_AUDIT_WINDOW = "ui:AuditWindow";
    public static final String UI_RBAC_ROLE_EDITOR = "ui:RbacRoleEditor";
    public static final String UI_DASHBOARD_WINDOW = "ui:DashboardWindow";

    /** Specify that an action requires at least one authentication assertion to be licensed. */
    public static final Collection<Class> LIC_AUTH_ASSERTIONS =
            Arrays.asList(new Class[] { SpecificUser.class, MemberOfGroup.class });

    /** Flag value meaning "This action should never be enabled." */
    protected static final AttemptedOperation NOT_ALLOWED = new AttemptedOperation(EntityType.ANY) {
        public OperationType getOperation() {
            return OperationType.NONE;
        }
    };

    private final Set<String> featureSetNames = new HashSet<String>();
    private final AttemptedOperation attemptedOperation;
    private String name = "Secure Action";

    /**
     * Create a SecureAction which may only be enabled if the user meets the admin requirement,
     * and which does not depend on any licensing feature to be enabled.
     */
    protected SecureAction(AttemptedOperation attemptedOperation) {
        this.attemptedOperation = attemptedOperation;
    }

    /**
     * Create a SecureAction with a name, which may only be enabled if hte user meets the admin requirement,
     * and which does not depend on any licensing feature to be enabled.
     *
     * @param name  the name to return from getName().
     * @param attemptedOperation  the operation that needs to be enabled to allow this action
     */
    protected SecureAction(String name, AttemptedOperation attemptedOperation) {
        this.attemptedOperation = attemptedOperation;
        this.name = name;
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if the specified assertion license is enabled (if specified).
     *
     * @param requiredAssertionLicense  if non-null, action will be disabled unless the specified assertion is licensed
     */
    protected SecureAction(AttemptedOperation attemptedOperation, Class requiredAssertionLicense) {
        this(attemptedOperation);
        if (requiredAssertionLicense != null)
            featureSetNames.add(Assertion.getFeatureSetName(requiredAssertionLicense));
        initLicenseListener();
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if at least one of the specified assertion licenses is enabled.
     *
     * @param attemptedOperation  the operation that needs to be enabled to allow this action
     * @param allowedAssertionLicenses  a collection of Assertion classes, any of which will, if licensed, enable this action
     */
    protected SecureAction(AttemptedOperation attemptedOperation, Collection<Class> allowedAssertionLicenses) {
        this.attemptedOperation = attemptedOperation;
        if (allowedAssertionLicenses != null)
            for (Class clazz : allowedAssertionLicenses)
                featureSetNames.add(Assertion.getFeatureSetName(clazz));
        initLicenseListener();
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and if at least one of the specified feature sets is enabled by the license.
     *
     * @param attemptedOperation  the operation that needs to be enabled to allow this action
     * @param allowedFeatureSetNames zero or more feature set names, any of which will, if licensed, enable this action
     */
    protected SecureAction(AttemptedOperation attemptedOperation, String[] allowedFeatureSetNames) {
        this.attemptedOperation = attemptedOperation;
        for (String featureSet : allowedFeatureSetNames) {
            featureSetNames.add(featureSet);
        }
        initLicenseListener();
    }

    /**
     * Create a SecureAction which will only be enabled if the user meets the admin requirement (if specified)
     * and the specified feature set license is enabled.
     *
     * @param attemptedOperation
     * @param requiredFeaturesetLicense  required feature set name, ie "service:TrustStore"
     */
    protected SecureAction(AttemptedOperation attemptedOperation, String requiredFeaturesetLicense) {
        this(attemptedOperation);
        if (requiredFeaturesetLicense != null)
            featureSetNames.add(requiredFeaturesetLicense);
        initLicenseListener();
    }

    /** Registers this action as a license listener, if any license requirements were set on it. */
    private void initLicenseListener() {
        if (featureSetNames.isEmpty()) return;
        Registry.getDefault().getLicenseManager().addLicenseListener(this);
    }

    /**
     * Test whether the current subject is authorized to perform the action
     * If the current subject is not set <code>false</code> is returnced
     *
     * @return true if the current subject is authorized, false otheriwse
     */
    public boolean isAuthorized() {
        return attemptedOperation == null || canAttemptOperation(attemptedOperation);
    }

    public void onPermissionRefresh() {
        setEnabled(isAuthorized());
    }

    /**
     * Test whether the current license allows performing the action.
     * @return true if at least one of the required assertion license classnames is licensed.
     */
    public final boolean isLicensed() {
        if (featureSetNames.isEmpty())
            return true;

        ConsoleLicenseManager lm = ConsoleLicenseManager.getInstance();
        for (String s : featureSetNames)
            if (lm.isFeatureEnabled(s))
                return true;
        return false;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public final boolean canAttemptOperation(AttemptedOperation ao) {
        if (ao == null) return true;
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        return getSecurityProvider().hasPermission(ao);
    }

    public String getName() {
        return name;
    }

    /**
     * Returns true if the action is enabled.
     *
     * @return true if the action is enabled, false otherwise
     * @see javax.swing.Action#isEnabled
     */
    public final boolean isEnabled() {
        return super.isEnabled() && isLicensed() && isAuthorized();
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
        final boolean enabled = b && isLicensed() && isAuthorized();
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
            logger.warning("Not authorized, action: " + getName()); // TODO pop a dialog?
            return;
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
