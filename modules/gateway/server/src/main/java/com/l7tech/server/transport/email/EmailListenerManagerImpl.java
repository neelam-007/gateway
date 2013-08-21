package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailListenerState;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages EmailListener objects. Can persist them and look them up.
 */
public class EmailListenerManagerImpl
        extends HibernateEntityManager<EmailListener, EntityHeader>
        implements EmailListenerManager, InitializingBean
{
    protected static final Logger logger = Logger.getLogger(EmailListenerManagerImpl.class.getName());

    private static final String COLUMN_NODEID = "ownerNodeId";
    private static final String COLUMN_LAST_POLL_TIME = "lastPollTime";
    private static final String COLUMN_EMAIL_LISTENER_ID = "email_listener_goid";

    private static final String HQL_UPDATE_TIME_BY_ID = "UPDATE VERSIONED " + EmailListenerState.class.getName() +
                    " set " + COLUMN_LAST_POLL_TIME + " = :"+COLUMN_LAST_POLL_TIME+" where " + COLUMN_EMAIL_LISTENER_ID + " = :"+ COLUMN_EMAIL_LISTENER_ID;

    public Class<? extends Entity> getImpClass() {
        return EmailListener.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return EmailListener.class;
    }

    public String getTableName() {
        return "email_listener";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EMAIL_LISTENER;
    }

    public void updateState(final EmailListenerState state) throws UpdateException {
        Goid emailListenerGoid = state.getEmailListener().getGoid();
        try {
            EmailListener emailListener = findByPrimaryKey(emailListenerGoid);
            if (emailListener != null) {
                EmailListenerState updateState = emailListener.getEmailListenerState();
                updateState.copyTo(state);
                getHibernateTemplate().update(updateState);
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Unable to update email listener state for listener '" + emailListenerGoid + "'");
            throw new UpdateException("Unable to update email listener state for listener '" + emailListenerGoid + "'");
        }
    }

    public List<EmailListener> getEmailListenersForNode(final String clusterNodeId) throws FindException {
        final List<EmailListener> emailListeners;
        try {
            //noinspection unchecked
            emailListeners = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(EmailListener.class, "el");
                    crit.createCriteria("el.emailListenerState", "state");
                    crit.add(Restrictions.eq("state." + COLUMN_NODEID, clusterNodeId));                   // This node is the one responsible for notifying this subscription
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Unable to find subscriptions", e);
        }

        if (emailListeners == null || emailListeners.isEmpty()) return Collections.emptyList();

        return emailListeners;
    }

    @SuppressWarnings({"unchecked"})
    public List<EmailListener> stealSubscriptionsFromDeadNodes(final String clusterNodeId) {
        final List<EmailListener> staleEmailListeners;
        try {
            staleEmailListeners = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(EmailListener.class, "el");
                    crit.createCriteria("el.emailListenerState", "state");
                    crit.add(Restrictions.ne("state." + COLUMN_NODEID, clusterNodeId));       // It's someone else's subscription

                    long now = System.currentTimeMillis();
                    //noinspection unchecked
                    List<EmailListener> results = crit.list();
                    if (results != null && results.size() > 0) {
                        List<EmailListener> newResults = new ArrayList<EmailListener>();
                        for (EmailListener emailListener : results) {
                            if (emailListener.getEmailListenerState() != null && now - emailListener.getPollInterval() * 5 * 1000 > emailListener.getEmailListenerState().getLastPollTime()) {
                                newResults.add(emailListener);
                            }
                        }
                        return newResults;
                    } else {
                        return results;
                    }
                }
            });
        } catch (DataAccessException e) {
            logger.log(Level.WARNING, "Unable to find subscriptions", e);
            return Collections.emptyList();
        }

        List<EmailListener> stolenEmailListeners = new ArrayList<EmailListener>(staleEmailListeners.size());
        for (EmailListener emailListener : staleEmailListeners) {
            logger.log(Level.INFO, MessageFormat.format("Assuming control of stale email listener {0} (belonged to node {1} that is presumed dead)", emailListener.getName(), emailListener.getEmailListenerState().getOwnerNodeId()));
            emailListener.getEmailListenerState().setOwnerNodeId(clusterNodeId);
            try {
                updateState(emailListener.getEmailListenerState());
                emailListener.lock();
                stolenEmailListeners.add(emailListener);
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "Unable to reset owner ID for " + emailListener.getName());
            }
        }

        return stolenEmailListeners;
    }

    public void updateLastPolled( final Goid emailListenerOid ) throws UpdateException {
        final long now = System.currentTimeMillis();
        getHibernateTemplate().execute( new HibernateCallback(){
            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                session.createQuery( HQL_UPDATE_TIME_BY_ID )
                        .setLong(COLUMN_LAST_POLL_TIME, now )
                        .setBinary(COLUMN_EMAIL_LISTENER_ID, emailListenerOid.getBytes() )
                        .executeUpdate();
                return null;
            }
        } );
    }

    @Override
    public void update(EmailListener entity) throws UpdateException {
        //need to determine we need to reset the email listener state because the same email listerner ID
        //was changed to poll a different email server, account, or folder depending on POP/IMAP
        try {
            EmailListener oldEmailListener = findByPrimaryKey(entity.getGoid());
            if (oldEmailListener == null) throw new UpdateException("Cannot find updating email listener from database.");

            //decide if we need to update the email listener state
            boolean isNewListener = false;
            if (!entity.getHost().equals(oldEmailListener.getHost())) isNewListener = true;
            if (!entity.getUsername().equals(oldEmailListener.getUsername())) isNewListener = true;
            if (!entity.getServerType().equals(oldEmailListener.getServerType())
                    || (entity.getServerType().equals(EmailServerType.IMAP) && !entity.getFolder().equals(oldEmailListener.getFolder())))
                isNewListener = true;

            logger.log(Level.FINE, "Updates to the email listener require to reset state : " + isNewListener);
            final EmailListenerState state = oldEmailListener.getEmailListenerState();    //state is a transient object
            if ( state == null ) {
                entity.createEmailListenerState( "-", 0, 0 ); // this will be stolen by a node later
            } else {
                entity.setEmailListenerState(state);
            }
            if (isNewListener) {
                entity.getEmailListenerState().setLastMessageId(0L);
                logger.log(Level.FINE, "EmailListener " + entity.getGoid() + " state got updated");
            }
            super.update(entity);
        } catch (FindException fe) {
            throw new UpdateException("Failed to find the updating email listener.", fe);
        }
    }
}
