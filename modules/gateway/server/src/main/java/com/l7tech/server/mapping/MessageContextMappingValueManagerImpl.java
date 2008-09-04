package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.BeansException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Query;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.SQLException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 13, 2008
 */
@Transactional(propagation= Propagation.REQUIRED)
public class MessageContextMappingValueManagerImpl
    extends HibernateEntityManager<MessageContextMappingValues, EntityHeader>
    implements MessageContextMappingValueManager, ApplicationContextAware {

    private final Logger logger = Logger.getLogger(MessageContextMappingKeyManagerImpl.class.getName());
    private final String HQL_GET_MAPPING_VALUES = "FROM " + getTableName() +
        " IN CLASS " + getImpClass().getName() + " WHERE objectid = ?";
    private final String HQL_GET_MAPPING_VALUES_BY_GUID = "FROM " + getTableName() +
        " IN CLASS " + getImpClass().getName() + " WHERE guid = ?";

    private ApplicationContext applicationContext;

    public MessageContextMappingValues getMessageContextMappingValues(final long oid) throws FindException {
        try {
            return (MessageContextMappingValues)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_GET_MAPPING_VALUES);
                    q.setLong(0, oid);
                    return q.uniqueResult();
                }
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error when retrieving the values of this message context mapping.", e);
            throw new FindException("Cannot retrieve the values of this message context mapping.");
        }
    }

    public MessageContextMappingValues getMessageContextMappingValues(final String guid) throws FindException {
        try {
            return (MessageContextMappingValues)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_GET_MAPPING_VALUES_BY_GUID);
                    q.setString(0, guid);
                    return q.uniqueResult();
                }
            });
        } catch (Exception e) {
            String errorMsg = "Cannot retrieve the values of a message context mapping with guid = " + guid + ".";
            logger.log(Level.WARNING, errorMsg, e);
            throw new FindException(errorMsg);
        }
    }

    public Class getImpClass() {
        return MessageContextMappingValues.class;
    }

    public Class getInterfaceClass() {
        return MessageContextMappingValues.class;
    }

    public String getTableName() {
        return "message_context_mapping_values";
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
