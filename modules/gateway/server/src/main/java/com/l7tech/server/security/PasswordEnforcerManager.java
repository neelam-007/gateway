package com.l7tech.server.security;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.PasswordChangeRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This manager provides implementation that will enforce conditions that has to do with password.  For example,
 * one of the conditions we need to enforce is that the password is compilable to Security Technical Implementation Guidelines (STIG).
 *
 * User: dlee
 * Date: Jun 18, 2008
 */
public class PasswordEnforcerManager  implements PropertyChangeListener, ApplicationContextAware {

    private static final Logger logger = Logger.getLogger(PasswordEnforcerManager.class.getName());

    private IdentityProviderPasswordPolicyManager passwordPolicyManager;
    private ServerConfig serverConfig;
    private ApplicationContext applicationContext;
    private RoleManager roleManager;

    private Audit auditor;
   
    public PasswordEnforcerManager(ServerConfig serverConfig,
                                   IdentityProviderPasswordPolicyManager passwordPolicyManager,
                                   RoleManager roleManager) {
        this.passwordPolicyManager = passwordPolicyManager;
        this.serverConfig = serverConfig;
        this.roleManager = roleManager;
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        if(this.applicationContext!=null) throw new IllegalStateException("applicationContext already initialized!");
        
        this.applicationContext = applicationContext;
        auditor = new Auditor( this, this.applicationContext, logger );
        auditPasswordPolicyMinimums(!serverConfig.getBooleanProperty(ServerConfig.PARAM_PCIDSS_ENABLED,false), getPasswordPolicy(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID));
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        String newValue = (String) evt.getNewValue();

        if ( propertyName != null && propertyName.equals(ServerConfig.PARAM_PCIDSS_ENABLED) ){
            boolean newVal = Boolean.valueOf(newValue);
            auditPasswordPolicyMinimums(!newVal, getPasswordPolicy(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID));
        }
    }

    public void auditPasswordPolicy(IdentityProviderPasswordPolicy passwordPolicy){
        auditPasswordPolicyMinimums(!serverConfig.getBooleanProperty(ServerConfig.PARAM_PCIDSS_ENABLED,false), passwordPolicy);
    }

    private void auditPasswordPolicyMinimums(boolean isSTIG, IdentityProviderPasswordPolicy passwordPolicy)
    {
        // STIG minimum values
        boolean STIG_FORCE_CHANGE = true;
        int STIG_MIN_LENGTH = 8;
        int STIG_MAX_LENGTH = 32;
        int STIG_FREQUENCY = 10;
        int STIG_EXPIRY = 90;
        boolean STIG_ALLOW_CHANGE = true;
        int STIG_UPPER = 1;
        int STIG_LOWER = 1;
        int STIG_NUM = 1;
        int STIG_SYMBOL = 1;
        int STIG_DIFF = 4;
        boolean STIG_REPEAT = true;

        // PCI-DSS minimum values
        boolean PCI_FORCE_CHANGE = true;
        int PCI_MIN_LENGTH = 7;
        int PCI_FREQUENCY = 4;
        int PCI_EXPIRY = 90;
        int PCI_UPPER = 1;
        int PCI_LOWER = 1;
        int PCI_NUM = 1;

        boolean aboveMinimum;
        aboveMinimum = (passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE));
        aboveMinimum = aboveMinimum && passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH) >= (isSTIG?STIG_MIN_LENGTH : PCI_MIN_LENGTH);
        aboveMinimum = aboveMinimum && (!isSTIG || passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH) >= STIG_MAX_LENGTH);
        aboveMinimum = aboveMinimum && (passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY) >= (isSTIG?STIG_FREQUENCY : PCI_FREQUENCY));
        aboveMinimum = aboveMinimum && (passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY) >= (isSTIG?STIG_EXPIRY : PCI_EXPIRY));
        aboveMinimum = aboveMinimum && (!isSTIG || passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES) == STIG_ALLOW_CHANGE);
        aboveMinimum = aboveMinimum && (passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.UPPER_MIN) >= (isSTIG ? STIG_UPPER : PCI_UPPER));
        aboveMinimum = aboveMinimum && (passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.LOWER_MIN) >= (isSTIG?STIG_LOWER : PCI_LOWER));
        aboveMinimum = aboveMinimum && (passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.NUMBER_MIN)) >= (isSTIG?STIG_NUM : PCI_NUM);
        aboveMinimum = aboveMinimum && (!isSTIG || passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN) >= STIG_SYMBOL);
        aboveMinimum = aboveMinimum && (!isSTIG || passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN)>= STIG_DIFF);
        aboveMinimum = aboveMinimum && (!isSTIG || passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.NO_REPEAT_CHARS) == STIG_REPEAT);

        if (!aboveMinimum){
            // log audit message
            auditor.logAndAudit(SystemMessages.PASSWORD_BELOW_MINIMUM, new String[] {isSTIG? "STIG":"PCI-DSS", "Internal Identity Provider"});
        }
    }

    /**
     * Verifies is the password is compliant.  Will throw an exception when a particular constraint is not
     * satisfied.
     * 
     * @param user  The user
     * @param newPassword   The new password to be checked against
     * @param hashedNewPassword The hashed version of the new password
     * @param currentUnHashedPassword   The unhased of the current password, used for comparison of the new and old password
     * @return  TRUE if the password is compiliant.
     * @throws InvalidPasswordException Thrown when a constraint is not satsified.
     */
    public boolean isPasswordPolicyCompliant(final User user, final String newPassword,
                                    final String hashedNewPassword,
                                    final String currentUnHashedPassword) throws InvalidPasswordException {

        if (user instanceof InternalUser) {
            final IdentityProviderPasswordPolicy policy = getPasswordPolicy(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
            if(policy == null)
                return false;

            try{
                // check if paasswords are different
                if(hashedNewPassword.equals(((InternalUser) user).getHashedPassword()))
                    throw new InvalidPasswordException("New password must be different from old password",policy.getDescription());
                
                validatePasswordChangesAllowable(user, policy);
                validatePasswordString(newPassword, policy);
                validatePasswordIsDifferent(newPassword, currentUnHashedPassword, policy);
                validateAgainstPrevPasswords(user, hashedNewPassword, policy);
            }catch(InvalidPasswordException e){
                auditor.logAndAudit(SystemMessages.PASSWORD_CHANGE_FAILED ,new String[]{user.getName()}, ExceptionUtils.getDebugException(e));
                throw e;
            }
        }
        return true;
    }

    public boolean isPasswordPolicyCompliant(final String newPassword, long identityProviderOid) throws InvalidPasswordException{
        final IdentityProviderPasswordPolicy policy = getPasswordPolicy(identityProviderOid);
        if(policy == null)
            return false;

        try{
            validatePasswordString(newPassword, policy);
        }catch(InvalidPasswordException e){
            auditor.logAndAudit(SystemMessages.PASSWORD_CHANGE_FAILED ,new String[]{"password reset"}, ExceptionUtils.getDebugException(e));            
            throw e;
        }
        return true;
    }
                                                                                                    
    private IdentityProviderPasswordPolicy getPasswordPolicy(long identityProviderOid){
        if(identityProviderOid != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID)
        {
            logger.warning("password policy not found");
            return null;

        }
        try {
            IdentityProviderPasswordPolicy policy = passwordPolicyManager.findByInternalIdentityProviderOid(identityProviderOid);
            return policy;
        } catch (FindException e) {
            logger.warning("password policy not found");
        }
        return null;
    }

    /**
     * Validates that the password meets the required characters in the password policy
     *
     *  - meets the min. length size
     *  - meets the min upper case letter
     *  - meets the min lower case letter
     *  - meets the min numerical value
     *  - meets the min special character
     *  - meets the min non-numeric character
     *  - checks for consecutively repeating characters
     * @param newPassword   the new password
     * @throws InvalidPasswordException
     */
    private void validatePasswordString(final String newPassword, final IdentityProviderPasswordPolicy policy) throws InvalidPasswordException {

        if ( newPassword.length() < policy.getIntegerProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH))
            throw new InvalidPasswordException(MessageFormat.format("Password must be at least {0} characters in length",
                    policy.getIntegerProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH)),policy.getDescription());

        int maxLength =  policy.getIntegerProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH);
        if( ( maxLength>0 && newPassword.length() > maxLength))
            throw new InvalidPasswordException(MessageFormat.format("Password must be less then or equal to {0} characters in length", maxLength),policy.getDescription());

        //you could use one regular expression to do the work
        final char[] pass = newPassword.toCharArray();
        int upperCount = 0;
        int lowerCount = 0;
        int digitCount = 0;
        int specialCharacterCount = 0;
        boolean hasRepeatChar = false;
        char prevChar = '0';
        for (int i=0; i < newPassword.length(); i++) {
            if ( Character.isUpperCase(pass[i]) ) upperCount++;
            if ( Character.isLowerCase(pass[i]) ) lowerCount++;
            if ( Character.isDigit(pass[i]) ) digitCount++;
            if ( !Character.isLetterOrDigit(pass[i]) && !Character.isWhitespace(pass[i])) specialCharacterCount++;

            //check for consecutive repeating characters
            if ( i != 0 && pass[i] == prevChar ) hasRepeatChar = true;
            prevChar = pass[i];
        }

        if (policy.getIntegerProperty(IdentityProviderPasswordPolicy.UPPER_MIN) > upperCount )
            throw new InvalidPasswordException(
                    MessageFormat.format("Password must contain at least {0} upper case characters",
                                         policy.getIntegerProperty(IdentityProviderPasswordPolicy.UPPER_MIN)),policy.getDescription());

        if (policy.getIntegerProperty(IdentityProviderPasswordPolicy.LOWER_MIN) > lowerCount )
            throw new InvalidPasswordException(
                    MessageFormat.format("Password must contain at least {0} lower case characters",
                                         policy.getIntegerProperty(IdentityProviderPasswordPolicy.LOWER_MIN)),policy.getDescription());

        if (policy.getIntegerProperty(IdentityProviderPasswordPolicy.NUMBER_MIN) > digitCount )
            throw new InvalidPasswordException(
                    MessageFormat.format("Password must contain at least {0} numbers",
                                         policy.getIntegerProperty(IdentityProviderPasswordPolicy.NUMBER_MIN)),policy.getDescription());

        if (policy.getIntegerProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN) > specialCharacterCount )
            throw new InvalidPasswordException(
                    MessageFormat.format("Password must contain at least {0} special characters",
                                         policy.getIntegerProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN)),policy.getDescription());

        if (policy.getIntegerProperty(IdentityProviderPasswordPolicy.NON_NUMERIC_MIN) > (upperCount+lowerCount+specialCharacterCount) )
            throw new InvalidPasswordException(
                    MessageFormat.format("Password must contain at least {0} non-numeric characters",
                                         policy.getIntegerProperty(IdentityProviderPasswordPolicy.NON_NUMERIC_MIN)),policy.getDescription());

        if ( policy.getBooleanProperty(IdentityProviderPasswordPolicy.NO_REPEAT_CHARS) && hasRepeatChar )
            throw new InvalidPasswordException("Password contains consecutive repeating characters",policy.getDescription());
    }

    /**
     * Validates that the new password is at least x chracters are different from the current password.
     *
     * @throws InvalidPasswordException
     */
    private void validatePasswordIsDifferent(final String newPassword, final String currentUnHashedPassword, final IdentityProviderPasswordPolicy policy) throws InvalidPasswordException {
        HashSet<String> oldChars = new HashSet<String>();
        for (int i=0; i<currentUnHashedPassword.length(); i++)
            oldChars.add(currentUnHashedPassword.substring(i,i+1));

        int minCharDiff = policy.getIntegerProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN);
        if(minCharDiff<0) return;
        int differCount = 0;
        for (int i=0; i<newPassword.length(); i++)
            if (! oldChars.contains(newPassword.substring(i,i+1)) && (++differCount >= minCharDiff))
                return;

        throw new InvalidPasswordException(
                    MessageFormat.format("New password must differ from previous password by at least {0} characters.",minCharDiff),policy.getDescription());
    }

    /**
     * This method will validate that the new password meets all of the following:
     *  - new password not resued within x-number of password changes
     *
     * @param user  The user
     * @param hashedNewPassword The new hashed password
     * @throws InvalidPasswordException
     */
    private void validateAgainstPrevPasswords(final User user, final String hashedNewPassword, final IdentityProviderPasswordPolicy policy) throws InvalidPasswordException {
        //compare the new hashedPassword to the previous passwords
        List<PasswordChangeRecord> changes = ((InternalUser) user).getPasswordChangesHistory();
        PasswordChangeRecord change;
        int repeatFrequency = policy.getIntegerProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY);
        if(repeatFrequency < 0) return;

        ListIterator iter = changes.listIterator(Math.max(0, changes.size() - repeatFrequency + 1));
        while (iter.hasNext()) {
            change = (PasswordChangeRecord) iter.next();
            if (change != null && hashedNewPassword.equals(change.getPrevHashedPassword()))
                throw new InvalidPasswordException(MessageFormat.format("New password cannot be reused within {0} password changes",repeatFrequency),policy.getDescription());
        }
    }

    /**
     * Validate to see if the password can be changed based on the allowable password changes timeframe.
     *
     * @param user  The user
     * @throws InvalidPasswordException
     */
    public void validatePasswordChangesAllowable(final User user, final IdentityProviderPasswordPolicy policy) throws InvalidPasswordException {
        boolean allowableChanges = policy.getBooleanProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES);
        if(!allowableChanges)
            return;

        long now = System.currentTimeMillis();
        Calendar xDaysAgo = Calendar.getInstance();
        xDaysAgo.setTimeInMillis(now);
        xDaysAgo.add(Calendar.DAY_OF_YEAR, -1);

        //compare if allowable to change password
        Calendar lastChange = Calendar.getInstance();
        if ( user instanceof InternalUser ){
            InternalUser internalUser = (InternalUser) user;

            if (hasAdminRole(internalUser) || internalUser.isChangePassword()) return;

            List<PasswordChangeRecord> changeHistory = internalUser.getPasswordChangesHistory();
            if (changeHistory != null) {
                for (PasswordChangeRecord changeRecord : changeHistory) {
                    long lastChangedMillis = changeRecord.getLastChanged();
                    lastChange.setTimeInMillis(lastChangedMillis);

                    if (lastChange.after(xDaysAgo)) {
                        long nextChangeMinutes = 24 * 60 - (now - lastChangedMillis) / (1000 * 60);
                        throw new InvalidPasswordException(MessageFormat.format("Password cannot be changed more than once every 24 hours. Please retry in {0} minutes",
                                (nextChangeMinutes >= 60 ? nextChangeMinutes / 60 + " hours and " : "") + nextChangeMinutes % 60 ),policy.getDescription()) ;
                    }
                }
            }
        }
    }

    public void setUserPasswordPolicyAttributes(final InternalUser user, boolean isNewOrReset){
        final IdentityProviderPasswordPolicy policy = getPasswordPolicy(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        user.setPasswordExpiry(getExpiryPasswordDate(policy,System.currentTimeMillis()));
        if(isNewOrReset){
            // force password change
            boolean force = policy.getBooleanProperty(IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE);
            user.setChangePassword(force);
        }
    }
          /**
     * Determines if the user has administrative roles.
     *
     * @return  TRUE if has admin role, otherwise FALSE
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
        } catch(FindException e) {
            return false;
        }
    }

    /**
     * Get the STIG compilance expiry password time.
     *
     * @param time  The time that the password is being changed at so it can be used
     *              to create the expiry date
     * @return  The long format of the expiry date, -1 if password expiry is not set
     */
    private static long getExpiryPasswordDate(final IdentityProviderPasswordPolicy policy,final long time) {
        try {
            int expiry  = (Integer)policy.getPropertyValue(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY);
            return expiry < 0 ? -1 : calcExpiryDate(time, expiry);
        } catch (NullPointerException e){
            // no property
            return -1;
        }
    }

    /**
     * Verifies if the user's password has expired based on the number of days that the password
     * suppose to expire.
     *
     * @param user  The internal user instance
     * @return  TRUE if the password has expired, otherwise FALSE.
     */
    public boolean isPasswordExpired( final User user ){
        if (user instanceof InternalUser){
            final InternalUser internalUser = (InternalUser) user;

            //if change password is set, then it means we are forced to change the password (ie password was expired)
            if (internalUser.isChangePassword()) return true;
            if (internalUser.getPasswordExpiry() < 0 ) return false;

            Calendar expireDate = Calendar.getInstance();
            expireDate.setTimeInMillis(internalUser.getPasswordExpiry());

            Calendar currentDate = Calendar.getInstance();
            currentDate.setTimeInMillis(System.currentTimeMillis());

            if ( currentDate.after(expireDate) ) return true;

        }
        return false;
    }

    /**
     * Utility to calculate the next expiry date based on the number of days to expire.
     *
     * @param time  The time that will be used to calculate the expiry date
     * @param numOfDays The number of days to expiry date
     * @return  The long format of the expiry date                                              
     */
    private static long calcExpiryDate(final long time, int numOfDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.add(Calendar.DAY_OF_YEAR, numOfDays);

        return cal.getTimeInMillis();
    }
}
