package com.l7tech.server.service.resolution;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.service.PublishedService;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class ResolutionManager extends HibernateDaoSupport {

    /**
     * Records resolution parameters for the passed service.
     * <p/>
     * If those resolution parameters conflict with resolution parameters of another service, this
     * will throw a DuplicateObjectException exception.
     * <p/>
     * This sould be called by service manager when saving and updating services. If this throws, a rollback
     * should occur.
     *
     * @param service the service whose resolution parameters should be recorded
     * @throws DuplicateObjectException this is thrown when there is a conflict between the resolution parameters of
     *                                  the passed service and the ones of another service. should rollback at that point.
     * @throws UpdateException          something went wrong, should rollback at that point
     */
    public void recordResolutionParameters(PublishedService service) throws DuplicateObjectException, UpdateException {
        Collection distinctItemsToSave = getDistinct(service);
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

        Session session = getSession();

        // delete the resolution parameters that are no longer part of the new ones
        try {
            for (Iterator i = existingParameters.iterator(); i.hasNext();) {
                ResolutionParameters maybeTodelete = (ResolutionParameters)i.next();
                boolean delete = true;
                if (distinctItemsToSave != null && distinctItemsToSave.contains(maybeTodelete)) {
                    delete = false;
                }
                if (delete) {
                    session.delete(maybeTodelete);
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
                    session.save(maybeToAdd);
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
    public void deleteResolutionParameters(long serviceOid) throws DeleteException {
        String query = "from " + TABLE_NAME + " in class " + ResolutionParameters.class.getName() +
          " where " + TABLE_NAME + "." + SVCID_COLUMN + " = " + serviceOid;
        Session session = null;
        try {
            session = getSession();
            int deleted = session.delete(query);
            logger.finest("deleted " + deleted + " resolution parameters.");
        } catch (HibernateException e) {
            String msg = "error deleting resolution parameters with query " + query;
            logger.log(Level.WARNING, msg, e);
            throw new DeleteException(msg, e);
        }
    }

    private boolean isSameParameters(Collection paramcol1, Collection paramcol2) {
        if (paramcol1.size() != paramcol2.size()) {
            return false;
        }

        if (!paramcol2.containsAll(paramcol1)) {
            return false;
        }
        return true;
    }

    private Collection getDistinct(PublishedService service) {
        ArrayList listOfParameters = new ArrayList();

        SoapActionResolver soapresolver = new SoapActionResolver();
        UrnResolver urnresolver = new UrnResolver();
        HttpUriResolver uriresolver = new HttpUriResolver();

        String httpuri = (String)uriresolver.doGetTargetValues(service)[0];
        Set soapactions = soapresolver.getDistinctParameters(service);
        for (Iterator i = soapactions.iterator(); i.hasNext();) {
            Set urns = urnresolver.getDistinctParameters(service);
            String soapaction = (String)i.next();
            for (Iterator j = urns.iterator(); j.hasNext();) {
                ResolutionParameters parameters = new ResolutionParameters();
                parameters.setServiceid(service.getOid());
                parameters.setSoapaction(soapaction);
                parameters.setUrn((String)j.next());
                parameters.setUri(httpuri);
                listOfParameters.add(parameters);
            }
        }
        return listOfParameters;
    }

    private Collection existingResolutionParameters(long serviceid) {
        String query = "from " + TABLE_NAME + " in class " + ResolutionParameters.class.getName() +
          " where " + TABLE_NAME + "." + SVCID_COLUMN + " = ?";

        List hibResults = null;
        try {
            Query q = getSession().createQuery(query);
            q.setLong(0, serviceid);
            hibResults = q.list();
        } catch (HibernateException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding resolution parameters for " + serviceid, e);
        }

        return hibResults;
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
    private void checkForDuplicateResolutionParameters(Collection parameters, long serviceIdToIgnore)
      throws HibernateException, DuplicateObjectException {
        String query = "from " + TABLE_NAME + " in class " + ResolutionParameters.class.getName();

        Set duplicates = new HashSet();
        Query q = getSession().createQuery(query);
        List results = q.list();
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


    private static final String TABLE_NAME = "service_resolution";
    private static final String SVCID_COLUMN = "serviceid";
    //private static final String SOAPACTION_COLUMN = "soapaction";
    //private static final String URN_COLUMN = "urn";

    protected final Logger logger = Logger.getLogger(getClass().getName());
}
