package com.l7tech.server.security;

import com.l7tech.gateway.common.Component;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import static com.l7tech.identity.IdentityProviderPasswordPolicy.*;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.PasswordChangeRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.SystemEvent;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import com.l7tech.util.HexUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.Ordered;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This manager provides implementation that will enforce conditions that has to do with password.  For example,
 * one of the conditions we need to enforce is that the password is compilable to Security Technical Implementation Guidelines (STIG).
 * <p/>
 * User: dlee
 * Date: Jun 18, 2008
 */
public class PasswordEnforcerManager implements PropertyChangeListener, ApplicationContextAware, PostStartupApplicationListener, Ordered {

    private static final Logger logger = Logger.getLogger(PasswordEnforcerManager.class.getName());

    private final IdentityProviderPasswordPolicyManager passwordPolicyManager;
    private final Config config;
    private ApplicationContext applicationContext;
    private final RoleManager roleManager;
    private final PasswordHasher passwordHasher;
    private IdentityProviderPasswordPolicy internalIdpPasswordPolicy;
    private final IdentityProviderPasswordPolicy stigPasswordPolicy;
    private final IdentityProviderPasswordPolicy pcidssPasswordPolicy;

    public PasswordEnforcerManager(final Config config,
                                   final IdentityProviderPasswordPolicyManager passwordPolicyManager,
                                   final RoleManager roleManager,
                                   final PasswordHasher passwordHasher,
                                   final IdentityProviderPasswordPolicy stigPasswordPolicy,
                                   final IdentityProviderPasswordPolicy pcidssPasswordPolicy) {
        this.passwordPolicyManager = passwordPolicyManager;
        this.config = config;
        this.roleManager = roleManager;
        this.passwordHasher = passwordHasher;
        this.stigPasswordPolicy = stigPasswordPolicy;
        this.pcidssPasswordPolicy = pcidssPasswordPolicy;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        if (this.applicationContext != null) throw new IllegalStateException("applicationContext already initialized!");

        this.applicationContext = applicationContext;
        internalIdpPasswordPolicy = getInternalIdpPasswordPolicy();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ReadyForMessages) {
            auditPasswordPolicyMinimums(!config.getBooleanProperty( ServerConfigParams.PARAM_PCIDSS_ENABLED, false), internalIdpPasswordPolicy);
        } else if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) applicationEvent;
            if (IdentityProviderPasswordPolicy.class.equals(eie.getEntityClass())) {
                internalIdpPasswordPolicy = getInternalIdpPasswordPolicy();
                auditPasswordPolicyMinimums(!config.getBooleanProperty( ServerConfigParams.PARAM_PCIDSS_ENABLED, false), internalIdpPasswordPolicy);
            }
        }
    }

    @Override
    public int getOrder() {
        return 50000;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        String newValue = (String) evt.getNewValue();

        if (propertyName != null && ServerConfigParams.PARAM_PCIDSS_ENABLED.equals(propertyName)) {
            boolean newVal = Boolean.valueOf(newValue);
            auditPasswordPolicyMinimums(!newVal, internalIdpPasswordPolicy);
        }
    }

    /**
     * Get the current password policy. Don't cache as it may change.
     * @return password policy. Not null if policy has been set.
     */
    public IdentityProviderPasswordPolicy getPasswordPolicy(){
        return internalIdpPasswordPolicy;
    }

    private void auditPasswordPolicyMinimums(final boolean isSTIG,
                                             final IdentityProviderPasswordPolicy passwordPolicy) {
        final IdentityProviderPasswordPolicy policy = isSTIG ?
                stigPasswordPolicy :
                pcidssPasswordPolicy;

        if (!passwordPolicy.hasStrengthOf(policy)) {
            // log audit message
            final String msg = MessageFormat.format("Password requirements are below {0} minimum for {1}", isSTIG ? "STIG" : "PCI-DSS", "Internal Identity Provider");
            applicationContext.publishEvent(
                    new SystemEvent(this,
                            Component.GW_PASSWD_POLICY_MGR,
                            null,
                            Level.WARNING,
                            msg) {

                        @Override
                        public String getAction() {
                            return "Password Policy Validation";
                        }
                    }
            );
        }
    }

    /**
     * Verifies is the password is compliant.  Will throw an exception when a particular constraint is not
     * satisfied.
     *
     * @param user                    The user
     * @param newPassword             The new password to be checked against
     * @param currentUnHashedPassword The unhased of the current password, used for comparison of the new and old password
     * @throws InvalidPasswordException Thrown when a constraint is not satsified.
     */         //todo [Donal] rename to throwIfNotPolicyCompliant
    public void isPasswordPolicyCompliant(final User user,
                                          final String newPassword,
                                          final String currentUnHashedPassword) throws InvalidPasswordException {

        if (user instanceof InternalUser) {
            // check if paasswords are different

            validatePasswordChangesAllowable(user);
            validatePasswordStringForInternalPolicy( newPassword );
            validatePasswordIsDifferent(newPassword, currentUnHashedPassword);

            validateAgainstPrevPasswords(user, newPassword);
        }
    }

    public void isPasswordPolicyCompliant(final String newPassword) throws InvalidPasswordException {
        validatePasswordStringForInternalPolicy( newPassword );
    }

    private void validatePasswordStringForInternalPolicy( final String newPassword ) throws InvalidPasswordException {
        validatePasswordString( newPassword, internalIdpPasswordPolicy );
    }

    /**
     * Validates that the password meets the required characters in the password policy
     * <p/>
     * - meets the min. length size
     * - meets the min upper case letter
     * - meets the min lower case letter
     * - meets the min numerical value
     * - meets the min special character
     * - meets the min non-numeric character
     * - checks for consecutively repeating characters
     *
     * @param newPassword the new password
     * @param policy the policy to validate
     * @throws InvalidPasswordException If the password does not meet the policy
     */
    static void validatePasswordString( final String newPassword,
                                        final IdentityProviderPasswordPolicy policy ) throws InvalidPasswordException {
        //you could use one regular expression to do the work
        final char[] pass = newPassword.toCharArray();
        int upperCount = 0;
        int lowerCount = 0;
        int digitCount = 0;
        int otherCount = 0;
        int specialCharacterCount = 0;
        boolean hasRepeatChar = false;
        char prevChar = '0';
        for (int i = 0; i < newPassword.length(); i++) {
            if (Character.isUpperCase(pass[i])) upperCount++;
            else if (Character.isLowerCase(pass[i])) lowerCount++;
            else if (Character.isDigit(pass[i])) digitCount++;
            else if (!Character.isLetterOrDigit(pass[i]) && !Character.isWhitespace(pass[i])) specialCharacterCount++;
            else otherCount++;

            //check for consecutive repeating characters
            if (i != 0 && pass[i] == prevChar) hasRepeatChar = true;
            prevChar = pass[i];
        }

        final List<String> errors = new ArrayList<String>();

        final int minLength = policy.getIntegerProperty(MIN_PASSWORD_LENGTH);
        if (newPassword.length() <minLength) {
            error( errors, "Password must be at least {0} characters in length", minLength);
        }

        final int maxLength = policy.getIntegerProperty(MAX_PASSWORD_LENGTH);
        if ((maxLength > 0 && newPassword.length() > maxLength)) {
            error( errors, "Password must be less than or equal to {0} characters in length", maxLength );
        }

        if (policy.getIntegerProperty(UPPER_MIN) > upperCount) {
            error( errors, "Password must contain at least {0} upper case characters", policy.getIntegerProperty(UPPER_MIN) );
        }

        if (policy.getIntegerProperty(LOWER_MIN) > lowerCount) {
            error( errors, "Password must contain at least {0} lower case characters", policy.getIntegerProperty(LOWER_MIN) );
        }

        if (policy.getIntegerProperty(NUMBER_MIN) > digitCount) {
            error( errors, "Password must contain at least {0} numbers", policy.getIntegerProperty(NUMBER_MIN) );
        }

        if (policy.getIntegerProperty(SYMBOL_MIN) > specialCharacterCount) {
            error( errors, "Password must contain at least {0} special characters", policy.getIntegerProperty(SYMBOL_MIN) );
        }

        if (policy.getIntegerProperty(NON_NUMERIC_MIN) > (upperCount + lowerCount + specialCharacterCount + otherCount)) {
            error( errors, "Password must contain at least {0} non-numeric characters", policy.getIntegerProperty(NON_NUMERIC_MIN) );
        }

        if (policy.getBooleanProperty(NO_REPEAT_CHARS) && hasRepeatChar) {
            error( errors, "Password contains consecutive repeating characters" );
        }

        if ( !errors.isEmpty() ) {
            throw new InvalidPasswordException( errors.get(0), policy.getDescription(), errors );
        }
    }

    private static void error( final Collection<String> errors, final String format, final Object... args ) {
        errors.add( MessageFormat.format( format, args ) );
    }

    /**
     * Validates that the new password is at least x characters are different from the current password.
     *
     * @throws InvalidPasswordException
     */
    private void validatePasswordIsDifferent(final String newPassword, final String currentUnHashedPassword) throws InvalidPasswordException {
        if (newPassword.equals(currentUnHashedPassword)) {
            throw new InvalidPasswordException("New password is the same as the users current password.");
        }

        HashSet<String> oldChars = new HashSet<String>();
        for (int i = 0; i < currentUnHashedPassword.length(); i++)
            oldChars.add(currentUnHashedPassword.substring(i, i + 1));

        int minCharDiff = internalIdpPasswordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN);
        if (minCharDiff < 0) return;
        int differCount = 0;
        for (int i = 0; i < newPassword.length(); i++)
            if (!oldChars.contains(newPassword.substring(i, i + 1)) && (++differCount >= minCharDiff))
                return;

        throw new InvalidPasswordException(
                MessageFormat.format("New password must differ from previous password by at least {0} characters", minCharDiff), internalIdpPasswordPolicy.getDescription());
    }

    /**
     * This method will validate that the new password meets all of the following:
     * - new password not resued within x-number of password changes
     *
     * @param user        The user
     * @param newPassword
     * @throws InvalidPasswordException
     */
    private void validateAgainstPrevPasswords(final User user,
                                              final String newPassword) throws InvalidPasswordException {
        //compare the new hashedPassword to the previous passwords
        List<PasswordChangeRecord> changes = ((InternalUser) user).getPasswordChangesHistory();
        PasswordChangeRecord change;
        int repeatFrequency = internalIdpPasswordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY);
        if (repeatFrequency < 0) return;

        ListIterator iter = changes.listIterator(Math.max(0, changes.size() - repeatFrequency + 1));
        final byte[] newPasswordBytes = newPassword.getBytes(Charsets.UTF8);
        while (iter.hasNext()) {
            change = (PasswordChangeRecord) iter.next();

            if (change != null) {//should never be null

                final String prevHash = change.getPrevHashedPassword();

                boolean isReusedTooSoon = false;
                try {
                    if (passwordHasher.isVerifierRecognized(prevHash)) {
                        passwordHasher.verifyPassword(newPasswordBytes, prevHash);
                        //if it's verifies then this password has been used recently, reject
                        isReusedTooSoon = true;
                    }

                } catch (IncorrectPasswordException e) {
                    //good expected, fall through
                }

                if (!isReusedTooSoon) {
                    //for backwards compatibility, check against old hashing scheme
                    final String oldHash = HexUtils.encodePasswd(user.getLogin(), newPassword, HexUtils.REALM);
                    if (prevHash.equals(oldHash)) {
                        isReusedTooSoon = true;
                    }
                }

                if (isReusedTooSoon) {
                    throw new InvalidPasswordException(
                            MessageFormat.format("New password cannot be reused within {0} password changes", repeatFrequency),
                            internalIdpPasswordPolicy.getDescription());
                }
            }
        }
    }

    /**
     * Validate to see if the password can be changed based on the allowable password changes timeframe.
     *
     * @param user The user
     * @throws InvalidPasswordException
     */
    public void validatePasswordChangesAllowable(final User user) throws InvalidPasswordException {
        boolean allowableChanges = internalIdpPasswordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES);
        if (!allowableChanges)
            return;

        long now = System.currentTimeMillis();
        Calendar xDaysAgo = Calendar.getInstance();
        xDaysAgo.setTimeInMillis(now);
        xDaysAgo.add(Calendar.DAY_OF_YEAR, -1);

        //compare if allowable to change password
        Calendar lastChange = Calendar.getInstance();
        if (user instanceof InternalUser) {
            InternalUser internalUser = (InternalUser) user;

            if (hasAdminRole(internalUser) || internalUser.isChangePassword()) return;

            List<PasswordChangeRecord> changeHistory = internalUser.getPasswordChangesHistory();
            if (changeHistory != null && !changeHistory.isEmpty()) {
                PasswordChangeRecord lastChangeRecord = changeHistory.get(changeHistory.size() - 1);
                long lastChangedMillis = lastChangeRecord.getLastChanged();
                lastChange.setTimeInMillis(lastChangedMillis);

                if (lastChange.after(xDaysAgo)) {
                    long nextChangeMinutes = 24 * 60 - (now - lastChangedMillis) / (1000 * 60);
                    throw new InvalidPasswordException(MessageFormat.format("Password cannot be changed more than once every 24 hours. Please retry in {0} minutes",
                            (nextChangeMinutes >= 60 ? nextChangeMinutes / 60 + " hours and " : "") + nextChangeMinutes % 60), internalIdpPasswordPolicy.getDescription());
                }
            }
        }
    }

    public void setUserPasswordPolicyAttributes(final InternalUser user, boolean isNewOrReset) {
        user.setPasswordExpiry(getExpiryPasswordDate(System.currentTimeMillis()));
        if (isNewOrReset) {
            // force password change
            boolean force = internalIdpPasswordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE);
            user.setChangePassword(force);
        }
    }

    /**
     * Determines if the user has administrative roles.
     *
     * @return TRUE if has admin role, otherwise FALSE
     */
    public boolean hasAdminRole(InternalUser internalUser) {
        try {
            Collection<Role> roles = roleManager.getAssignedRoles(internalUser);

            if (roles == null) return false;
            for (Role role : roles) {
                if (role.getTag() == Role.Tag.ADMIN) {
                    return true;
                }
            }
            return false;
        } catch (FindException e) {
            return false;
        }
    }

    /**
     * Get the STIG compilance expiry password time.
     *
     * @param time The time that the password is being changed at so it can be used
     *             to create the expiry date
     * @return The long format of the expiry date, -1 if password expiry is not set
     */
    private long getExpiryPasswordDate(final long time) {
        try {
            int expiry = (Integer) internalIdpPasswordPolicy.getPropertyValue(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY);
            return expiry < 0 ? -1 : calcExpiryDate(time, expiry);
        } catch (NullPointerException e) {
            // no property
            return -1;
        }
    }

    /**
     * Verifies if the user's password has expired based on the number of days that the password
     * suppose to expire.
     *
     * @param user The internal user instance
     * @return TRUE if the password has expired, otherwise FALSE.
     */
    public boolean isPasswordExpired(final User user) {//todo [Donal] rename or split - is also checking changePassword property
        if (user instanceof InternalUser) {
            final InternalUser internalUser = (InternalUser) user;

            //if change password is set, then it means we are forced to change the password (ie password was expired)
            if (internalUser.isChangePassword()) return true;
            if (internalUser.getPasswordExpiry() < 0) return false;

            Calendar expireDate = Calendar.getInstance();
            expireDate.setTimeInMillis(internalUser.getPasswordExpiry());

            Calendar currentDate = Calendar.getInstance();
            currentDate.setTimeInMillis(System.currentTimeMillis());

            if (currentDate.after(expireDate)) return true;

        }
        return false;
    }

    /**
     * Utility to calculate the next expiry date based on the number of days to expire.
     *
     * @param time      The time that will be used to calculate the expiry date
     * @param numOfDays The number of days to expiry date
     * @return The long format of the expiry date
     */
    private static long calcExpiryDate(final long time, int numOfDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.add(Calendar.DAY_OF_YEAR, numOfDays);

        return cal.getTimeInMillis();
    }

    private IdentityProviderPasswordPolicy getInternalIdpPasswordPolicy() {
        IdentityProviderPasswordPolicy passwordPolicy = null;
        final String msg = "Could not find password policy for internal identity provider.";
        try {
            passwordPolicy = passwordPolicyManager.findByInternalIdentityProviderOid(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        } catch (FindException e) {
            //this should not happen and is a serious configuration issue.
            logger.log(Level.WARNING, msg);
        }

        if (passwordPolicy == null) {
            throw new IllegalStateException(msg);
        }

        return passwordPolicy;
    }
}
