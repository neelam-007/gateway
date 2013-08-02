package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * Implementation of Manager for ServiceDocuments
 *
 * @author Steve Jones
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class ServiceDocumentManagerImpl extends HibernateGoidEntityManager<ServiceDocument, EntityHeader> implements ServiceDocumentManager {

    //- PUBLIC

    public Collection<ServiceDocument> findByServiceId(final Goid serviceId) throws FindException {
        try {
            return (Collection<ServiceDocument>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(ServiceDocument.class);

                    if (Goid.isDefault(serviceId)) {
                        crit.add(Restrictions.isNull(PROP_SERVICE_GOID));
                    } else {
                        crit.add(Restrictions.eq(PROP_SERVICE_GOID, serviceId));
                    }

                    List results = crit.list();
                    ArrayList<ServiceDocument> out = new ArrayList<ServiceDocument>();
                    for (Object result : results) {
                        ServiceDocument serviceDocument = (ServiceDocument) result;
                        out.add(serviceDocument);
                    }
                    return out;
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Error finding ServiceDocuments by service identifier '"+serviceId+"'.", e);
        }
    }

    public Collection<ServiceDocument> findByServiceIdAndType(final Goid serviceId, final String type) throws FindException {
        try {
            return (Collection<ServiceDocument>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(ServiceDocument.class);

                    if (Goid.isDefault(serviceId)) {
                        crit.add(Restrictions.isNull(PROP_SERVICE_GOID));
                    } else {
                        crit.add(Restrictions.eq(PROP_SERVICE_GOID, serviceId));
                    }

                    if (type == null) {
                        crit.add(Restrictions.isNull(PROP_TYPE));
                    } else {
                        crit.add(Restrictions.eq(PROP_TYPE, type));
                    }

                    List results = crit.list();
                    ArrayList<ServiceDocument> out = new ArrayList<ServiceDocument>();
                    for (Object result : results) {
                        ServiceDocument serviceDocument = (ServiceDocument) result;
                        out.add(serviceDocument);
                    }
                    return out;
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Error finding ServiceDocuments by service identifier '"+serviceId+"'.", e);
        }
    }

    public Goid save(final ServiceDocument entity) throws SaveException {
        if (!isValid(entity)) {
            throw new SaveException("Invalid service document.");
        }
        return super.save(entity);
    }

    public void update(final ServiceDocument entity) throws UpdateException {
        if (!isValid(entity)) {
            throw new UpdateException("Invalid service document.");            
        }
        super.update(entity);
    }

    @Transactional(propagation=SUPPORTS)
    public Class getImpClass() {
        return ServiceDocument.class;
    }

    @Transactional(propagation=SUPPORTS)
    public Class getInterfaceClass() {
        return ServiceDocument.class;
    }

    @Transactional(propagation=SUPPORTS)
    public String getTableName() {
        return "service_documents";
    }

    //- PROTECTED

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    private static final String PROP_SERVICE_GOID = "serviceId";
    private static final String PROP_TYPE = "type";

    /**
     * Check if the given service document is valid.
     */
    private boolean isValid(final ServiceDocument serviceDocument) {
        boolean valid = true;
        if (serviceDocument == null) {
            valid = false; 
        } else {
            if (Goid.isDefault(serviceDocument.getServiceId())) {
                valid = false;
            }
            if (serviceDocument.getUri() == null) {
                valid = false;
            }
            if (serviceDocument.getContents() == null) {
                valid = false;
            }
            if (serviceDocument.getType() == null) {
                valid = false;
            }
            if (serviceDocument.getContentType() == null) {
                valid = false;
            }
        }
        return valid;
    }
}
