package com.l7tech.server.logon;

import com.l7tech.identity.User;
import com.l7tech.identity.FailAttemptsExceededException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

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
 *
 * User: dlee
 * Date: Jun 27, 2008
 */
public class SSMLogonService implements LogonService, PropertyChangeListener, ApplicationContextAware {

    //- PUBLIC

    public SSMLogonService(PlatformTransactionManager transactionManager, LogonInfoManager logonManager, ServerConfig serverConfig) {
        if ( transactionManager == null ) throw new IllegalArgumentException("PlateformTransactionManager cannot be null.");
        if ( logonManager == null ) throw new IllegalArgumentException("LogonInfoManager cannot be null.");
        if ( serverConfig == null ) throw new IllegalArgumentException("ServerConfig cannot be null.");

        this.transactionManager = transactionManager;
        this.logonManager = logonManager;
        this.serverConfig = serverConfig;

        this.maxLoginAttempts = serverConfig.getIntProperty(ServerConfig.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW, DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW);
        if (this.maxLoginAttempts <= 0) {
            this.maxLoginAttempts = DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW;
        }
        this.maxLockoutTime = serverConfig.getIntProperty(ServerConfig.PARAM_MAX_LOCKOUT_TIME, DEFAULT_MAX_LOCKOUT_TIME_IN_SECS);
        if (this.maxLockoutTime <= 0) {
            this.maxLockoutTime = DEFAULT_MAX_LOCKOUT_TIME_IN_SECS;
        }
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

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        String newValue = (String) evt.getNewValue();

        if ( propertyName != null && propertyName.equals(ServerConfig.PARAM_MAX_LOGIN_ATTEMPTS_ALLOW) ){
            try {
                int newVal = Integer.valueOf(newValue);
                if (newVal <= 0) throw new NumberFormatException();
                maxLoginAttempts = newVal;
            } catch (NumberFormatException nfe) {
                maxLoginAttempts = DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW;
                logger.warning("Parameter " + propertyName + " value '" + newValue + "' not a positive integer value. Reuse default value '" + maxLoginAttempts + "'");
            }
        }

        if (propertyName != null && propertyName.equals(ServerConfig.PARAM_MAX_LOCKOUT_TIME)) {
            try {
                int newVal = Integer.valueOf(newValue);
                if (newVal <= 0) throw new NumberFormatException();
                maxLockoutTime = newVal;
            } catch (NumberFormatException nfe) {
                maxLockoutTime = DEFAULT_MAX_LOCKOUT_TIME_IN_SECS;
                logger.warning("Parameter " + propertyName + " value '" + newValue + "' not a positive integer value. Reuse default value '" + maxLockoutTime + "'");
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    @Override
    public void hookPreLoginCheck(final User user) throws FailAttemptsExceededException {
        try {
            final long now = System.currentTimeMillis();

            // We should get a lock, but this is not any more wrong than it
            // was before we had support for locking.
            //
            // This means that the check is faster but theoretically exposes
            // us to the possiblity of 100s of simultaneous login attempts
            // being processed.
            LogonInfo logonInfo = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);

            if (logonInfo == null) throw new FindException("No entry for '" + user.getLogin() + "'");

            //verify if has reached login attempts
            if (logonInfo.getFailCount() >= this.maxLoginAttempts) {
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
                    throw new FailAttemptsExceededException(msg);
                } else {
                    doResetLogonAttempt(user);
                    logger.info("Reset the fail logon count for '" + user.getLogin() + "'");
                }
            }
        } catch (FindException fe) {
            //if we can't find this user then we'll just ignore it and we'll carry on and it'll get updated later
            //this find exception could occur because of backwards compatibility
        }
    }

    @Override
    public void updateLogonAttempt(User user, AuthenticationResult ar) {
        doUpdateLogonAttempt(user, ar);
    }

    @Override
    public void resetLogonFailCount(User user) throws FindException, UpdateException {

        if (user == null) return;

        LogonInfo logonInfo = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), true);
        if (logonInfo != null) {
            logonInfo.setFailCount(0);

            //update
            logonManager.update(logonInfo);
        }

    }

    //- PRIVATE
    private static final Logger logger = Logger.getLogger(SSMLogonService.class.getName());

    private PlatformTransactionManager transactionManager; // required for TransactionTemplate
    private LogonInfoManager logonManager;
    private ServerConfig serverConfig;
    private Auditor auditor;

    private int maxLoginAttempts;
    private int maxLockoutTime;

    private static final int DEFAULT_MAX_LOCKOUT_TIME_IN_SECS = 1200;
    private static final int DEFAULT_MAX_LOGIN_ATTEMPTS_ALLOW = 5;


    private void doResetLogonAttempt(final User user) {
        doLogonInfoUpdate( user, new Functions.UnaryVoid<LogonInfo>(){
            @Override
            public void call( final LogonInfo logonInfo ) {
                final long now = System.currentTimeMillis();
                logonInfo.resetFailCount(now);
            }
        } );
    }

    private void doUpdateLogonAttempt(final User user, final AuthenticationResult ar) {
        doLogonInfoUpdate( user, new Functions.UnaryVoid<LogonInfo>(){
            @Override
            public void call( final LogonInfo logonInfo ) {
                //if the authentication result is NULL then the fail logon failed.
                final long now = System.currentTimeMillis();
                if (ar != null) {
                    logonInfo.successfulLogonAttempt(now);
                } else {
                    logonInfo.failLogonAttempt(now);
                }
            }
        } );
    }

    private void doLogonInfoUpdate( final User user, final Functions.UnaryVoid<LogonInfo> callback ) {
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

                                callback.call( logonInfo );

                                //update it or save it depending on whether the record existed
                                if (newRecord) {
                                    logonManager.save(logonInfo);
                                } else {
                                    logonManager.update(logonInfo);
                                }
                            } catch (Exception e) {
                                transactionStatus.setRollbackOnly();
                                if ( ExceptionUtils.causedBy( e, CannotAcquireLockException.class ) ) {
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
}
