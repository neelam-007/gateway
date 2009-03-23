package com.l7tech.server.mapping;

import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Query;

import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 13, 2008
 */
@Transactional(propagation= Propagation.REQUIRED)
public class MessageContextMappingKeyManagerImpl
    extends HibernateEntityManager<MessageContextMappingKeys, EntityHeader>
    implements MessageContextMappingKeyManager {

    private final Logger logger = Logger.getLogger(MessageContextMappingKeyManagerImpl.class.getName());
    private final String HQL_GET_MAPPING_KEYS_BY_OID = "FROM " + getTableName() +
        " IN CLASS " + getImpClass().getName() + " WHERE objectid = ?";
    private final String HQL_GET_MAPPING_KEYS_BY_DIGEST = "FROM " + getTableName() +
        " IN CLASS " + getImpClass().getName() + " WHERE digested = ?";

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

    public MessageContextMappingKeys getMessageContextMappingKeys(final MessageContextMappingKeys lookupKeys) throws FindException {
        if ( lookupKeys.getDigested()==null ) lookupKeys.setDigested( lookupKeys.generateDigest() );
        final String digested = lookupKeys.getDigested();
        try {
            return (MessageContextMappingKeys)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @SuppressWarnings({"unchecked"})
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_GET_MAPPING_KEYS_BY_DIGEST);
                    q.setString(0, digested);
                    Collection<MessageContextMappingKeys> matches = (Collection<MessageContextMappingKeys>)q.list();
                    if ( matches.size()==0 ) {
                        return null;
                    } else if ( matches.size()==1 ) {
                        MessageContextMappingKeys keys = matches.iterator().next();
                        if ( keys.matches( lookupKeys ) ) {
                            return keys;
                        }
                    } else {
                        for ( MessageContextMappingKeys keys : matches ) {
                            if ( keys.matches( lookupKeys ) ) {
                                return keys;
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            String errorMsg = "Cannot retrieve the keys of a message context mapping with digest '" + digested + "'.";
            logger.log(Level.WARNING, errorMsg, e);
            throw new FindException(errorMsg);
        }
    }

    public Class<MessageContextMappingKeys> getImpClass() {
        return MessageContextMappingKeys.class;
    }

    public Class<MessageContextMappingKeys> getInterfaceClass() {
        return MessageContextMappingKeys.class;
    }

    public String getTableName() {
        return "message_context_mapping_keys";
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
