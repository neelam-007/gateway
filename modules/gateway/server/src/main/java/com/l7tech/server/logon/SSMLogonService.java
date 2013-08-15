package com.l7tech.server.logon;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.LogonInfo;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.system.SystemEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.dao.CannotAcquireLockException;

/**
 * The logon service manager that will control the login activites that takes place in the SSM component.
 * <p/>
 * User: dlee
 * Date: Jun 27, 2008
 */
public class SSMLogonService implements LogonService, PropertyChangeListener, ApplicationContextAware {
    private ApplicationContext applicationContext;

    //- PUBLIC

    public SSMLogonService(final PlatformTransactionManager transactionManager,
                           final LogonInfoManager logonManager,
                           final Config config,
                           final RoleManager roleManager,
                           final IdentityProviderFactory identityProviderFactory) {
        if (transactionManager == null)
            throw new IllegalArgumentException("PlateformTransactionManager cannot be null.");
        if (logonManager == null) throw new IllegalArgumentException("LogonInfoManager cannot be null.");
        if ( config == null) throw new IllegalArgumentException("ServerConfig cannot be null.");

        this.transactionManager = transactionManager;
        this.logonManager = logonManager;
        setConfig( config );
        this.roleManager = roleManager;
        this.identityProviderFactory = identityProviderFactory;

        this.maxLoginAttempts = this.config.getIntProperty( ServerConfigParams.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW, DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW);
        this.maxLockoutTime = this.config.getIntProperty( ServerConfigParams.PARAM_MAX_LOCKOUT_TIME, DEFAULT_MAX_LOCKOUT_TIME_IN_SECS);
        this.maxInactivityPeriod = this.config.getIntProperty( ServerConfigParams.PARAM_INACTIVITY_PERIOD, DEFAULT_MAX_INACTIVITY_PERIOD);
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public LogonInfoManager getLogonManager() {
        return logonManager;
    }

    public void setLogonManager(LogonInfoManager logonManager) {
        this.logonManager = logonManager;
    }

    public void setConfig(Config config) {
        final ValidatedConfig validatedConfig = new ValidatedConfig(config, logger);
        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW, 1);
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW, 20);
        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_MAX_LOCKOUT_TIME, 1);
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_MAX_LOCKOUT_TIME, 86400);
        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_INACTIVITY_PERIOD, 1);
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_INACTIVITY_PERIOD, 365);
        this.config = validatedConfig;

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (propertyName != null && propertyName.equals( ServerConfigParams.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW)) {
            this.maxLoginAttempts = this.config.getIntProperty( ServerConfigParams.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW, DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW);
        }

        if (propertyName != null && propertyName.equals( ServerConfigParams.PARAM_MAX_LOCKOUT_TIME)) {
            this.maxLockoutTime = this.config.getIntProperty( ServerConfigParams.PARAM_MAX_LOCKOUT_TIME, DEFAULT_MAX_LOCKOUT_TIME_IN_SECS);
        }


        if (propertyName != null && propertyName.equals( ServerConfigParams.PARAM_INACTIVITY_PERIOD)) {
            this.maxInactivityPeriod = this.config.getIntProperty( ServerConfigParams.PARAM_INACTIVITY_PERIOD, DEFAULT_MAX_INACTIVITY_PERIOD);
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.auditor = new Auditor(this, this.applicationContext, logger);
    }

    @Override
    public void hookPreLoginCheck(final User user) throws AuthenticationException {
        try {

            if (user instanceof InternalUser) {
                if (!((InternalUser) user).isEnabled()) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_DISABLED, user.getLogin());
                    String msg = "Credentials login matches an internal user " + user.getLogin() + ", but access is denied because user is disabled.";
                    logger.info(msg);
                    throw new UserDisabledException(msg);
                }
            }

            final long now = System.currentTimeMillis();

            // We should get a lock, but this is not any more wrong than it
            // was before we had support for locking.
            //
            // This means that the check is faster but theoretically exposes
            // us to the possiblity of 100s of simultaneous login attempts
            // being processed.
            LogonInfo logonInfo = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);

            if (logonInfo == null) throw new FindException("No entry for '" + user.getLogin() + "'");

            switch (logonInfo.getState()) {
                case ACTIVE:
                    break; //
                case INACTIVE: {   // exception for administrator users
                    if (!isUserFullAdministrator(user)) {
                        String msg = "Access is denied because of inactivity";
                        logger.info(msg);
                        throw new FailInactivityPeriodExceededException(msg);
                    }
                }
            }

            //verify if has reached login attempts
            if (logonInfo.getState() == LogonInfo.State.EXCEED_ATTEMPT || logonInfo.getFailCount() >= this.maxLoginAttempts) {
                //if the retry was not after the locked time, then we still lock the account.  Otherwise,
                //the user is good to go for retry and we'll reset the fail count
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(logonInfo.getLastAttempted());
                cal.add(Calendar.SECOND, this.maxLockoutTime);

                if (cal.getTimeInMillis() >= now) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_EXCEED_ATTEMPT, user.getLogin(), Integer.toString(logonInfo.getFailCount()), Integer.toString(this.maxLoginAttempts));
                    String msg = "Credentials login matches an internal user " + user.getLogin() + ", but access is denied because of number of failed attempts.";
                    logger.info(msg);
                    doUpdateLogonAttempt(user, null);
                    doUpdateLogonState(user, LogonInfo.State.EXCEED_ATTEMPT);
                    throw new FailAttemptsExceededException(msg);
                } else {
                    doUpdateLogonState(user, LogonInfo.State.ACTIVE);
                }
            }

            // verify if user has exceeded the inactivity period
            Calendar inactivityCal = Calendar.getInstance();
            inactivityCal.setTimeInMillis(logonInfo.getLastActivity());
            inactivityCal.add(Calendar.HOUR, this.maxInactivityPeriod * 24);
            if (!isUserFullAdministrator(user) && this.maxInactivityPeriod != 0 && logonInfo.getLastActivity() > 0 && inactivityCal.getTimeInMillis() <= now) {
                long daysAgo = (now - logonInfo.getLastActivity()) / 1000 / 60 / 60 / 24;
                auditor.logAndAudit(SystemMessages.AUTH_USER_EXCEED_INACTIVITY, user.getLogin(), Long.toString(daysAgo), Integer.toString(this.maxInactivityPeriod));
                String msg = "Credentials login matches an internal user " + user.getLogin() + ", but access is denied because of account inactivity.";
                logger.info(msg);
                doUpdateLogonState(user, LogonInfo.State.INACTIVE);
                throw new FailInactivityPeriodExceededException(msg);
            }
            doUpdateLastActivity(user);

        } catch (FindException fe) {
            //if we can't find this user then we'll just ignore it and we'll carry on and it'll get updated later
            //this find exception could occur because of backwards compatibility
        }

    }

    @Override
    public void updateLogonAttempt(User user, AuthenticationResult ar) {
        doUpdateLogonAttempt(user, ar);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void updateInactivityInfo() {
        String exceptionMsg = null;
        final String actionName = "Account Inactivity Monitor";
        try {
            final long now = System.currentTimeMillis();

            Calendar inactivityCal = Calendar.getInstance();
            Collection<LogonInfo> logonInfos = logonManager.findAll();

            for (LogonInfo logonInfo : logonInfos) {
                if (logonInfo.getState() != LogonInfo.State.ACTIVE)
                    continue;
                inactivityCal.setTimeInMillis(now);
                inactivityCal.setTimeInMillis(logonInfo.getLastActivity());
                inactivityCal.add(Calendar.HOUR, maxInactivityPeriod * 24);
                if (maxInactivityPeriod != 0 &&
                        logonInfo.getLastActivity() > 0 &&
                        inactivityCal.getTimeInMillis() <= now) {
                    try {
                        logonManager.update(logonInfo);
                        long daysAgo = (now - logonInfo.getLastActivity()) / 1000 / 60 / 60 / 24;
                        String msg = MessageFormat.format(SystemMessages.AUTH_USER_EXCEED_INACTIVITY.getMessage(), logonInfo.getLogin(), Long.toString(daysAgo), Integer.toString(maxInactivityPeriod));
                        applicationContext.publishEvent(
                                new SystemEvent(this,
                                        Component.GW_ACCOUNT_MANAGER,
                                        null,
                                        Level.INFO,
                                        msg) {

                                    @Override
                                    public String getAction() {
                                        return actionName;
                                    }
                                }
                        );
                        logonInfo.setState(LogonInfo.State.INACTIVE);
                    } catch (UpdateException e) {
                        exceptionMsg = "Failed to update inactivity for '" + logonInfo.getLogin() + "'. " + ExceptionUtils.getMessage(e);
                        break;
                    }
                }
            }
        } catch (FindException e) {
            exceptionMsg = "Failed to update users inactivity. " + ExceptionUtils.getMessage(e);
        }

        if (exceptionMsg != null) {
            final String msg = "Account activity monitor failed. " + exceptionMsg;
            applicationContext.publishEvent(
                    new SystemEvent(this,
                            Component.GW_ACCOUNT_MANAGER,
                            null,
                            Level.INFO,
                            msg) {

                        @Override
                        public String getAction() {
                            return actionName;
                        }
                    }
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void checkLogonInfos() {
        String exceptionMsg = null;
        try {
            final Collection<Pair<Goid, String>> allPairs = roleManager.getExplicitRoleAssignments();
            for (Pair<Goid, String> pair : allPairs) {
                Goid providerId = pair.left;
                final String login;
                if (providerId.equals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID)) {
                    final InternalUserManager userManager = getInternalUserManager();
                    final InternalUser internalUser = userManager.findByPrimaryKey(pair.right);
                    login = internalUser.getLogin();
                } else {
                    login = pair.right;
                }

                final LogonInfo logonInfo = logonManager.findByCompositeKey(providerId, login, false);
                if (logonInfo == null) {
                    final LogonInfo newInfo = new LogonInfo(providerId, login);
                    logonManager.save(newInfo);
                }
            }
        } catch (FindException e) {
            exceptionMsg = ExceptionUtils.getMessage(e);
        } catch (SaveException e) {
            exceptionMsg = ExceptionUtils.getMessage(e);
        } finally {
            if (exceptionMsg != null) {
                final String msg = "New administrator account maintenance failed. " + exceptionMsg;
                applicationContext.publishEvent(
                        new SystemEvent(this,
                                Component.GW_ACCOUNT_MANAGER,
                                null,
                                Level.INFO,
                                msg) {

                            @Override
                            public String getAction() {
                                return "New Administrator Account Monitor";
                            }
                        }
                );
            }
        }
    }

    //- PRIVATE
    private static final Logger logger = Logger.getLogger(SSMLogonService.class.getName());

    private PlatformTransactionManager transactionManager; // required for TransactionTemplate
    private LogonInfoManager logonManager;
    private Config config;
    private Auditor auditor;
    private final RoleManager roleManager;
    private InternalUserManager internalUserManager;
    private final IdentityProviderFactory identityProviderFactory;

    private int maxLoginAttempts;
    private int maxLockoutTime;
    private int maxInactivityPeriod;

    private static final int DEFAULT_MAX_LOCKOUT_TIME_IN_SECS = 1200;
    private static final int DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW = 5;
    private static final int DEFAULT_MAX_INACTIVITY_PERIOD = 35;

    private void doUpdateLogonState(final User user, final LogonInfo.State logonState) {
        doLogonInfoUpdate(user, new Functions.UnaryVoid<LogonInfo>() {
            @Override
            public void call(final LogonInfo logonInfo) {
                logonInfo.setState(logonState);
            }
        });
    }

    private void doUpdateLastActivity(final User user) {
        doLogonInfoUpdate(user, new Functions.UnaryVoid<LogonInfo>() {
            @Override
            public void call(final LogonInfo logonInfo) {
                final long now = System.currentTimeMillis();
                logonInfo.setLastActivity(now);
                logonInfo.setState(LogonInfo.State.ACTIVE);
            }
        });
    }

    private void doUpdateLogonAttempt(final User user, final AuthenticationResult ar) {
        doLogonInfoUpdate(user, new Functions.UnaryVoid<LogonInfo>() {
            @Override
            public void call(final LogonInfo logonInfo) {
                //if the authentication result is NULL then the fail logon failed.
                final long now = System.currentTimeMillis();
                if (ar != null) {
                    logonInfo.resetFailCount(now);
                } else {
                    logonInfo.failLogonAttempt(now);
                }
            }
        });
    }

    private void doLogonInfoUpdate(final User user, final Functions.UnaryVoid<LogonInfo> callback) {
        //based on the user id and the provider id, we'll update the logon information.
        //We'll need to execute the transaction at system level because we won't have
        //an admin account
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(user);
        Subject subject = new Subject(true, principals, Collections.emptySet(), Collections.emptySet());
        final String name = user.getLogin();

        Subject.doAs(subject, new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    //we'll need to create a new transaction to do the work for us because the parent transaction could
                    //rollback if there are any exceptions thrown that may cause it to rollback.  In any case, even if
                    //an exception has occurred, we still want to be able to update the logon attempt
                    TransactionTemplate txn = new TransactionTemplate(transactionManager);
                    txn.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                    txn.execute(new TransactionCallback() {
                        @Override
                        public Object doInTransaction(TransactionStatus transactionStatus) {
                            boolean newRecord = false;
                            try {
                                LogonInfo logonInfo = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);

                                //we gotta check if the log info is NULL because older accounts without the upgrade
                                //wont have this record in place.  If NULL, we'll just instantiate a new one for it
                                if (logonInfo == null) {
                                    logonInfo = new LogonInfo(user.getProviderId(), user.getLogin());
                                    newRecord = true;
                                }

                                callback.call(logonInfo);

                                //update it or save it depending on whether the record existed
                                if (newRecord) {
                                    logonManager.save(logonInfo);
                                } else {
                                    logonManager.update(logonInfo);
                                }
                            } catch (Exception e) {
                                transactionStatus.setRollbackOnly();
                                if (ExceptionUtils.causedBy(e, CannotAcquireLockException.class)) {
                                    logger.log(Level.WARNING, "Failed to update logon attempt for '" + name + "', could not acquire lock.", ExceptionUtils.getDebugException(e));
                                } else {
                                    logger.log(Level.WARNING, "Failed to update logon attempt for '" + name + "'", e);
                                }
                            }
                            return null;
                        }
                    });
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to update logon attempt for '" + name + "'", e);
                }
                return null;
            }
        });
    }

    private boolean isUserFullAdministrator(User user) throws FindException {
        for (Role role : roleManager.getAssignedRoles(user)) {
            if (role.getTag() == Role.Tag.ADMIN) {
                return true;
            }
        }

        return false;
    }

    private InternalUserManager getInternalUserManager() throws FindException {
        if (internalUserManager == null) {
            final InternalIdentityProvider identityProvider =
                    (InternalIdentityProvider) identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
            if (identityProvider == null) {
                throw new FindException("IdentityProvider could not be found");
            }
            internalUserManager = identityProvider.getUserManager();
        }
        return internalUserManager;
    }
}
