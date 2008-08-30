package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Query;

import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 13, 2008
 */
@Transactional(propagation= Propagation.REQUIRED)
public class MessageContextMappingKeyManagerImpl
    extends HibernateEntityManager<MessageContextMappingKeys, EntityHeader>
    implements MessageContextMappingKeyManager, ApplicationContextAware {

    private final Logger logger = Logger.getLogger(MessageContextMappingKeyManagerImpl.class.getName());
    private final String HQL_GET_MAPPING_KEYS_BY_OID = "FROM " + getTableName() +
        " IN CLASS " + getImpClass().getName() + " WHERE objectid = ?";
    private final String HQL_GET_MAPPING_KEYS_BY_GUID = "FROM " + getTableName() +
        " IN CLASS " + getImpClass().getName() + " WHERE guid = ?";

    private ApplicationContext applicationContext;

    public MessageContextMappingKeys getMessageContextMappingKeys(final long oid) throws FindException {
        try {
            return (MessageContextMappingKeys)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_GET_MAPPING_KEYS_BY_OID);
                    q.setLong(0, oid);
                    return q.uniqueResult();
                }
            });
        } catch (Exception e) {
            String errorMsg = "Cannot retrieve the keys of a message context mapping with objectid = " + oid + ".";
            logger.log(Level.WARNING, errorMsg, e);
            throw new FindException(errorMsg);
        }
    }

    public MessageContextMappingKeys getMessageContextMappingKeys(final String guid) throws FindException {
        try {
            return (MessageContextMappingKeys)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_GET_MAPPING_KEYS_BY_GUID);
                    q.setString(0, guid);
                    return q.uniqueResult();
                }
            });
        } catch (Exception e) {
            String errorMsg = "Cannot retrieve the keys of a message context mapping with guid = " + guid + ".";
            logger.log(Level.WARNING, errorMsg, e);
            throw new FindException(errorMsg);
        }
    }

    public Class getImpClass() {
        return MessageContextMappingKeys.class;
    }

    public Class getInterfaceClass() {
        return MessageContextMappingKeys.class;
    }

    public String getTableName() {
        return "message_context_mapping_keys";
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
