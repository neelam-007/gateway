package com.l7tech.server.service.resolution;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Decorator;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Query;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

/**
 * The ResolutionManager (actually its corresponding table) enforces the uniqueness of resolution
 * parameters across all services.
 * <p/>
 * This is used by the ServiceManager when updating and saving services to ensure that resolution
 * parameters do not conflict.
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 25, 2003<br/>
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ReolutionManagerImpl extends HibernateDaoSupport implements ResolutionManager, ApplicationContextAware {
    private static final String HQL_FIND_BY_SERVICE_OID =
            "FROM sr IN CLASS " + ResolutionParameters.class.getName() +
                    " WHERE sr.serviceid = ?";

    private static final String HQL_FIND_ALL = "FROM sr IN CLASS " + ResolutionParameters.class.getName();

    private SoapActionResolver soapresolver;
    private UrnResolver urnresolver;
    private UriResolver uriresolver;

    public ReolutionManagerImpl(Collection<Decorator<PublishedService>> decorators) {
        this.decorators = decorators;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        soapresolver = new SoapActionResolver(applicationContext);
        urnresolver = new UrnResolver(applicationContext);
        uriresolver = new UriResolver(applicationContext);
    }

    /**
     * Records resolution parameters for the passed service.
     * <p/>
     * If those resolution parameters conflict with resolution parameters of another service, this
     * will throw a DuplicateObjectException exception.
     * <p/>
     * This sould be called by service manager when saving and updating services. If this throws, a rollback
     * should occur.
     *
     * @param publishedService the service whose resolution parameters should be recorded
     * @throws DuplicateObjectException this is thrown when there is a conflict between the resolution parameters of
     *                                  the passed service and the ones of another service. should rollback at that point.
     * @throws UpdateException          something went wrong, should rollback at that point
     */
    public void recordResolutionParameters(PublishedService publishedService) throws DuplicateObjectException, UpdateException {
        PublishedService service = decorate(publishedService);
        Collection distinctItemsToSave;
        try {
            distinctItemsToSave = getDistinct(service);
        } catch (ServiceResolutionException sre) {
            throw new UpdateException("Cannot get service resolution data for service.", sre);
        }
        Collection existingParameters = existingResolutionParameters(service.getOid());

        if (isSameParameters(distinctItemsToSave, existingParameters)) {
            logger.finest("resolution parameters unchanged");
            return;
        } else {
            logger.finest("different resolution parameters will be recorded");
        }

        try {
            checkForDuplicateResolutionParameters(distinctItemsToSave, service.getOid());
        } catch (HibernateException e) {
            String msg = "error checking for duplicate resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // delete the resolution parameters that are no longer part of the new ones
        try {
            for (Iterator i = existingParameters.iterator(); i.hasNext();) {
                ResolutionParameters maybeTodelete = (ResolutionParameters)i.next();
                boolean delete = true;
                if (distinctItemsToSave != null && distinctItemsToSave.contains(maybeTodelete)) {
                    delete = false;
                }
                if (delete) {
                    getHibernateTemplate().delete(maybeTodelete);
                }
            }
        } catch (HibernateException e) {
            String msg = "error deleting exsiting resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // insert the ones that did not exist before
        try {
            for (Iterator i = distinctItemsToSave.iterator(); i.hasNext();) {
                ResolutionParameters maybeToAdd = (ResolutionParameters)i.next();
                boolean add = true;
                if (existingParameters != null && existingParameters.contains(maybeToAdd)) {
                    add = false;
                }
                if (add) {
                    getHibernateTemplate().save(maybeToAdd);
                }
            }
            logger.fine("saved " + distinctItemsToSave.size() + " parameters for service " + service.getOid());
        } catch (HibernateException e) {
            throw new UpdateException("error adding resolution parameters.", e);
        }
    }

    /**
     * deletes all resolution parameters previously recorded for a particular service
     *
     * @param serviceOid id of the service for which recorded resolution parameters will be recorded
     */
    public void deleteResolutionParameters(final long serviceOid) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_SERVICE_OID);
                    q.setLong(0, serviceOid);
                    int deleted = 0;
                    for (Iterator i = q.iterate(); i.hasNext();) {
                        session.delete(i.next());
                        deleted++;
                    }
                    logger.finest("deleted " + deleted + " resolution parameters.");
                    return null;
                }
            });
        } catch (Exception e) {
            String msg = "error deleting resolution parameters with query " + HQL_FIND_BY_SERVICE_OID;
            logger.log(Level.WARNING, msg, e);
            throw new DeleteException(msg, e);
        }
    }

    private boolean isSameParameters(Collection paramcol1, Collection paramcol2) {
        boolean sameParams = false;
        if (paramcol1.size() == paramcol2.size()) {
            sameParams = paramcol2.containsAll(paramcol1);
        }

        return sameParams;
    }

    private Collection getDistinct(PublishedService service) throws ServiceResolutionException {
        ArrayList<ResolutionParameters> listOfParameters = new ArrayList<ResolutionParameters>();

        String httpuri = uriresolver.doGetTargetValue(service);
        Set<String> soapactions = soapresolver.getDistinctParameters(service);
        for (String soapaction : soapactions) {
            Set<String> urns = urnresolver.getDistinctParameters(service);
            for (String urn : urns) {
                ResolutionParameters parameters = new ResolutionParameters();
                parameters.setServiceid(service.getOid());
                parameters.setSoapaction(soapaction);
                parameters.setUrn(urn);
                parameters.setUri(httpuri);
                if (!listOfParameters.contains(parameters)) {
                    listOfParameters.add(parameters);
                }
            }
        }
        return listOfParameters;
    }

    private Collection existingResolutionParameters(final long serviceid) {
        try {
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_SERVICE_OID);
                    q.setLong(0, serviceid);
                    return q.list();
                }
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "hibernate error finding resolution parameters for " + serviceid, e);
            return Collections.emptyList();
        }
    }

    public void checkDuplicateResolution(PublishedService service) throws DuplicateObjectException, ServiceResolutionException {
        checkForDuplicateResolutionParameters(getDistinct(service), -1);
    }

    /**
     * This is a temporary bandage to detect duplicate resolution params as the DuplicateObjectException
     * in this class are not working/never thrown for the duplicate param scenario, so the caller does not
     * receive the reason, it simply receives the TransactionException with root cause in SQLException
     * caused by DB contraint.
     * <p/>
     * This fix still has a problem ith multiple concurrent request, and may result in not detecting
     * the resolution parameters added in between the read in this method, and the actual commit.
     * Basically this transaction does not prevent concurrent transactions (asssumes typican read
     * committed isolation.
     * The only way how to detect duplicates accurately is to interpret the SQLException vendor erroCode
     * and sqlState caused by DB constraint violation. Spring framework offers SQLException independent
     * message interpretation; google for SQLErrorCodeSQLExceptionTranslator to learn more.
     * <p/>
     * quite correct
     *
     * @param parameters the resolution parameters to check
     * @throws DuplicateObjectException on duplicate detect
     * @throws HibernateException       on hibernate error
     */
    private void checkForDuplicateResolutionParameters(Collection parameters, long serviceIdToIgnore) throws DuplicateObjectException {

        Set duplicates = new HashSet();
        List results = getHibernateTemplate().find(HQL_FIND_ALL);
        for (Iterator ir = results.iterator(); ir.hasNext();) {
            ResolutionParameters rp = (ResolutionParameters)ir.next();
            for (Iterator ip = parameters.iterator(); ip.hasNext();) {
                ResolutionParameters r = (ResolutionParameters)ip.next();
                if (r.resolutionEquals(rp) && rp.getServiceid() != serviceIdToIgnore) {
                    duplicates.add(r);
                }
            }
        }
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer("Duplicate resolution parameters :\n");
            sb.append(duplicates);
            final String msg = sb.toString();
            logger.fine(msg);
            throw new DuplicateObjectException(msg);
        }
    }

    /**
     * Run decorators
     */
    private PublishedService decorate(PublishedService publishedService) {
        PublishedService decorated = publishedService;
        for(Decorator<PublishedService> decorator : decorators) {
            decorated = decorator.decorate(decorated);
        }
        return decorated;
    }    

    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final Collection<Decorator<PublishedService>> decorators;
}
