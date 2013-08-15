package com.l7tech.server.logon;

import com.l7tech.identity.LogonInfo;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.ObjectDeletedException;
import org.hibernate.LockMode;
import org.hibernate.criterion.Restrictions;

/**
 * Implementation for the LogonInfoManager
 * 
 * User: dlee
 * Date: Jun 27, 2008
 */
public class LogonInfoManagerImpl extends HibernateGoidEntityManager<LogonInfo, EntityHeader> implements LogonInfoManager {

    private static final Logger logger = Logger.getLogger(LogonInfoManagerImpl.class.getName());

    @Override
    public Class<LogonInfo> getImpClass() {
        return LogonInfo.class;
    }

    @Override
    public Class<LogonInfo> getInterfaceClass() {
        return LogonInfo.class;
    }

    @Override
    public String getTableName() {
        return "logon_info";
    }

    @Override
    public LogonInfo findByCompositeKey(final Goid providerId, final String login, final boolean lock) throws FindException {
        try {
            return (LogonInfo) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(LogonInfo.class);
                    criteria.add(Restrictions.eq("providerId", providerId));
                    criteria.add(Restrictions.eq("login", login));
                    if ( lock ) {
                        criteria.setLockMode(LockMode.UPGRADE);
                    }
                    return criteria.uniqueResult();
                }
            });
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, org.hibernate.ObjectNotFoundException.class) ||
                ExceptionUtils.causedBy(e, ObjectDeletedException.class)) {
                return null;
            }
            throw new FindException("Data access error ", e);
        }
    }


    @Override
    public void delete(Goid providerId, String login) throws DeleteException {
        try {
            //check if record exists to delete
            LogonInfo original = findByCompositeKey(providerId, login, true);

            //for backward compatibility, if there are no record exist then we dont need to
            //delete it because the record is not there
            if (original != null) {
                //delete the record
                super.delete(original);
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Failed to retrieve object to be deleted.", fe);
            throw new DeleteException(fe.getMessage(), fe);
        }

    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final LogonInfo entity ) {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("providerId", entity.getProviderId());
        map.put("login",entity.getLogin());
        return Arrays.asList(map);
    }
}
