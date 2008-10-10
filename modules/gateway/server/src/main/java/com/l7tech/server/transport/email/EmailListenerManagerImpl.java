package com.l7tech.server.transport.email;

import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * Manages EmailListener objects. Can persist them and look them up.
 */
public class EmailListenerManagerImpl
        extends HibernateEntityManager<EmailListener, EntityHeader>
        implements EmailListenerManager, InitializingBean
{
    protected static final Logger logger = Logger.getLogger(EmailListenerManagerImpl.class.getName());

    private final ServerConfig serverConfig;
    @SuppressWarnings( { "FieldCanBeLocal" } )
    private final ApplicationListener applicationListener; // need reference to prevent listener gc
    private final Map<Long, EmailListener> knownEmailListeners = new LinkedHashMap<Long, EmailListener>();

    private static final String COLUMN_NODEID = "ownerNodeId";
    private static final String COLUMN_OID = "oid";
    private static final String COLUMN_LAST_POLL_TIME = "lastPollTime";

    private static final String HQL_UPDATE_TIME_BY_ID = "UPDATE " + EmailListener.class.getName() +
                    " set " + COLUMN_LAST_POLL_TIME + " = :"+COLUMN_LAST_POLL_TIME+" where " + COLUMN_OID + " = :"+COLUMN_OID;

    public EmailListenerManagerImpl(ServerConfig serverConfig, ApplicationEventProxy eventProxy) {
        this.serverConfig = serverConfig;
        this.applicationListener = new ApplicationListener() {
            public void onApplicationEvent( ApplicationEvent event ) {
                handleEvent(event);
            }
        };

        eventProxy.addApplicationListener( applicationListener );
    }

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

    @Override
    protected void initDao() throws Exception {
        super.initDao();

        // Initialize known connections
        for (EmailListener emailListener : findAll()) {
            if (emailListener.isActive()) knownEmailListeners.put(emailListener.getOid(), emailListener);
        }
    }

    private void handleEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent))
            return;
        EntityInvalidationEvent evt = (EntityInvalidationEvent)event;
        if (!EmailListener.class.isAssignableFrom(evt.getEntityClass()))
            return;
        long[] ids = evt.getEntityIds();
        char[] ops = evt.getEntityOperations();
        for (int i = 0; i < ops.length; i++) {
            char op = ops[i];
            long id = ids[i];

            switch (op) {
                case EntityInvalidationEvent.DELETE:
                    knownEmailListeners.remove(id);
                    break;
                default:
                    onEmailListenerChanged(id);
            }
        }
    }

    private void onEmailListenerChanged(long id) {
        try {
            EmailListener emailListener = findByPrimaryKey(id);
            if (emailListener != null && emailListener.isActive())
                knownEmailListeners.put(id, emailListener);
            else
                knownEmailListeners.remove(id);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated connector with oid " + id + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    public List<EmailListener> getEmailListenersForNode(final String clusterNodeId) throws FindException {
        final List<EmailListener> emailListeners;
        try {
            emailListeners = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(EmailListener.class);
                    crit.add(Restrictions.eq(COLUMN_NODEID, clusterNodeId));                   // This node is the one responsible for notifying this subscription
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
                    Criteria crit = session.createCriteria(EmailListener.class);
                    crit.add(Restrictions.ne(COLUMN_NODEID, clusterNodeId));       // It's someone else's subscription
                    crit.add(Restrictions.sqlRestriction(Long.toString(System.currentTimeMillis()) + " - {alias}.poll_interval * 5 * 1000 > {alias}.last_poll_time"));
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            logger.log(Level.WARNING, "Unable to find subscriptions", e);
            return Collections.emptyList();
        }

        List<EmailListener> stolenEmailListeners = new ArrayList<EmailListener>(staleEmailListeners.size());
        for (EmailListener emailListener : staleEmailListeners) {
            logger.log(Level.INFO, MessageFormat.format("Assuming control of stale email listener {0} (belonged to node {1} that is presumed dead)", emailListener.getName(), emailListener.getOwnerNodeId()));
            emailListener.setOwnerNodeId(clusterNodeId);
            try {
                update(emailListener);
                stolenEmailListeners.add(emailListener);
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "Unable to reset owner ID for " + emailListener.getName());
            }
        }

        return stolenEmailListeners;
    }

    public void updateLastPolled( final long emailListenerOid ) throws UpdateException {
        final long now = System.currentTimeMillis();
        getHibernateTemplate().execute( new HibernateCallback(){
            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                session.createQuery( HQL_UPDATE_TIME_BY_ID )
                        .setLong(COLUMN_LAST_POLL_TIME, now )
                        .setLong(COLUMN_OID, emailListenerOid )
                        .executeUpdate();
                return null;
            }
        } );
    }
}
