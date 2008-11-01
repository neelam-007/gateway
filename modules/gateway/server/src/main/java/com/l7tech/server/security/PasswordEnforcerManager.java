package com.l7tech.server.security;

import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.PasswordChangeRecord;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.gateway.common.security.rbac.Role;

import java.util.*;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * This manager provides implementation that will enforce conditions that has to do with password.  For example,
 * one of the conditions we need to enforce is that the password is compilable to Security Technical Implementation Guidelines (STIG).
 *
 * User: dlee
 * Date: Jun 18, 2008
 */
public class PasswordEnforcerManager implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(PasswordEnforcerManager.class.getName());

    private static final int MIN_CHARACTER_LENGTH = 8;
    private static final int MAX_CHARACTER_LENGTH = 32;
    private static final int NUM_OF_OLD_PASSWORD_NOT_REUSABLE = 10;
    private static final int DEFAULT_PASSWORD_EXPIRE_IN_DAYS = 90;
    private static final int ALLOWABLE_PASSWORD_CHANGES_IN_DAYS = 1;
    private static final int MIN_CHAR_DIFFER = 4;

    private static int PASSWORD_EXPIRY;
    private ServerConfig serverConfig;
    private RoleManager roleManager;

    public PasswordEnforcerManager(ServerConfig serverConfig, RoleManager roleManager) {
        if (serverConfig == null ) throw new IllegalArgumentException("ServerConfig cannot be null.");

        this.serverConfig = serverConfig;
        this.roleManager = roleManager;
        PASSWORD_EXPIRY = serverConfig.getIntProperty(ServerConfig.PARAM_PASSWORD_EXPIRY, DEFAULT_PASSWORD_EXPIRE_IN_DAYS);
        if ( PASSWORD_EXPIRY <= 0 || PASSWORD_EXPIRY > 90 ) {
             PASSWORD_EXPIRY = DEFAULT_PASSWORD_EXPIRE_IN_DAYS;
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        String newValue = (String) evt.getNewValue();

        try {
            if ( propertyName != null && propertyName.equals(ServerConfig.PARAM_PASSWORD_EXPIRY) ){
                int newVal = Integer.valueOf(newValue);
                if (newVal <= 0 || newVal > 90) throw new NumberFormatException();
                PASSWORD_EXPIRY = newVal;
            }
        } catch (NumberFormatException nfe) {
            PASSWORD_EXPIRY = DEFAULT_PASSWORD_EXPIRE_IN_DAYS;
            logger.warning("Parameter " + propertyName + " value '" + newValue + "' not a valid numeric value.  Reuse back to default '" + PASSWORD_EXPIRY + "'");
        }
    }

    public boolean isSTIGEnforced() {
        return serverConfig.getBooleanProperty("security.stig.enabled", true);
    }

    /**
     * Verifies is the password is STIG compilant.  Will throw an exception when a particular STIG constraint is not
     * satisfied.
     * 
     * @param user  The user
     * @param newPassword   The new password to be checked against
     * @param hashedNewPassword The hashed version of the new password
     * @param currentUnHashedPassword   The unhased of the current password, used for comparison of the new and old password
     * @return  TRUE if the password is stig compiliant.
     * @throws InvalidPasswordException Thrown when STIG is satsified.
     */
    public boolean isSTIGCompilance(final User user, final String newPassword,
                                    final String hashedNewPassword,
                                    final String currentUnHashedPassword) throws InvalidPasswordException {
        boolean check = true;
        if (serverConfig != null) {
            check = serverConfig.getBooleanProperty("security.stig.enabled", true);
        }

        if (check) {
            validatePasswordChangesAllowable(user);
            validatePasswordString(newPassword);
            validatePasswordIsDifferent(newPassword, currentUnHashedPassword);
            validateAgainstPrevPasswords(user, hashedNewPassword);
        }

        return true;
    }

    /**
     * Validates that the password meets all of the following:
     *  - meets the min. length size
     *  - contains at least one upper case letter
     *  - contains at least one lower case letter
     *  - contains at least one numerical value
     *  - contains at least one special character
     *  - does not contain consecutively repeating characters
     *
     * @param newPassword   the new password
     * @throws InvalidPasswordException
     */
    private void validatePasswordString(final String newPassword) throws InvalidPasswordException {

        if ( newPassword.length() < MIN_CHARACTER_LENGTH || newPassword.length() > MAX_CHARACTER_LENGTH)
            throw new InvalidPasswordException("Password must be between 8 and 32 characters in length.");

        //you could use one regular expression to do the work
        final char[] pass = newPassword.toCharArray();
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        boolean hasRepeatChar = false;
        char prevChar = '0';
        for (int i=0; i < newPassword.length(); i++) {
            if ( Character.isUpperCase(pass[i]) ) hasUpper = true;
            if ( Character.isLowerCase(pass[i]) ) hasLower = true;
            if ( Character.isDigit(pass[i]) ) hasDigit = true;
            if ( !Character.isLetterOrDigit(pass[i]) && !Character.isWhitespace(pass[i])) hasSpecialChar = true;

            //check for consecutive repeating characters
            if ( i != 0 && pass[i] == prevChar ) hasRepeatChar = true;
            prevChar = pass[i];
        }

        if ( !hasUpper || !hasLower || !hasDigit || !hasSpecialChar || hasRepeatChar )
            throw new InvalidPasswordException("Unable to change your password. The new password must be at least 8 characters and contain " +
                    "a mixture of upper and lower case letters, numbers, and special characters, with no consecutive repeating characters.");
    }

    /**
     * Validates that the new password is at least four chracters are different from the current password.
     *
     * @throws InvalidPasswordException
     */
    private void validatePasswordIsDifferent(final String newPassword, final String currentUnHashedPassword) throws InvalidPasswordException {
        HashSet<String> oldChars = new HashSet<String>();
        for (int i=0; i<currentUnHashedPassword.length(); i++)
            oldChars.add(currentUnHashedPassword.substring(i,i+1));

        int differCount = 0;
        for (int i=0; i<newPassword.length(); i++)
            if (! oldChars.contains(newPassword.substring(i,i+1)) && (++differCount >= MIN_CHAR_DIFFER))
                return;

        throw new InvalidPasswordException("New password must differ from previous password by at least " + MIN_CHAR_DIFFER + " characters.");
    }

    /**
     * This method will validate that the new password meets all of the following:
     *  - new password not resued within x-number of password changes
     *
     * @param user  The user
     * @param hashedNewPassword The new hashed password
     * @throws InvalidPasswordException
     */
    private void validateAgainstPrevPasswords(final User user, final String hashedNewPassword) throws InvalidPasswordException {
        //compare the new hashedPassword to the previous passwords
        if ( user instanceof InternalUser) {
            List<PasswordChangeRecord> changes = ((InternalUser) user).getPasswordChangesHistory();
            PasswordChangeRecord change;
            ListIterator iter = changes.listIterator(Math.max(0, changes.size() - NUM_OF_OLD_PASSWORD_NOT_REUSABLE + 1));
            while (iter.hasNext()) {
                change = (PasswordChangeRecord) iter.next();
                if (change != null && hashedNewPassword.equals(change.getPrevHashedPassword()))
                    throw new InvalidPasswordException("New password cannot be reused within " +  NUM_OF_OLD_PASSWORD_NOT_REUSABLE + " password changes.");
            }
        }
    }

    /**
     * Validate to see if the password can be changed based on the allowable password changes timeframe.
     *
     * @param user  The user
     * @throws InvalidPasswordException
     */
    public void validatePasswordChangesAllowable(final User user) throws InvalidPasswordException {
        long now = System.currentTimeMillis();
        Calendar xDaysAgo = Calendar.getInstance();
        xDaysAgo.setTimeInMillis(now);
        xDaysAgo.add(Calendar.DAY_OF_YEAR,  ALLOWABLE_PASSWORD_CHANGES_IN_DAYS * -1);

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
                        long nextChangeMinutes = ALLOWABLE_PASSWORD_CHANGES_IN_DAYS * 24 * 60 - (now - lastChangedMillis) / (1000 * 60);
                        throw new InvalidPasswordException("Password cannot be changed more than once every " + ALLOWABLE_PASSWORD_CHANGES_IN_DAYS * 24 +
                                " hours. Please retry in " + (nextChangeMinutes >= 60 ? nextChangeMinutes / 60 + " hours and " : "") + nextChangeMinutes % 60 + " minutes") ;
                    }
                }
            }
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
     * @return  The long format of the expiry date
     */
    public long getSTIGExpiryPasswordDate(final long time) {
        return calcExpiryDate(time, PASSWORD_EXPIRY);
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
    public long calcExpiryDate(final long time, int numOfDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.add(Calendar.DAY_OF_YEAR, numOfDays);

        return cal.getTimeInMillis();
    }



}
