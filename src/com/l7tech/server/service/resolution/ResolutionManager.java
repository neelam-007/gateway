package com.l7tech.server.service.resolution;

import com.l7tech.objectmodel.*;
import com.l7tech.service.PublishedService;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * The ResolutionManager (actually its corresponding table) enforces the uniqueness of resolution
 * parameters across all services.
 *
 * This is used by the ServiceManager when updating and saving services to ensure that resolution
 * parameters do not conflict.
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Nov 25, 2003<br/>
 * $Id$
 *
 */
public class ResolutionManager {

    /**
     * Records resolution parameters for the passed service.
     *
     * If those resolution parameters conflict with resolution parameters of another service, this
     * will throw a DuplicateObjectException exception.
     *
     * This sould be called by service manager when saving and updating services. If this throws, a rollback
     * should occur.
     *
     * @param service the service whose resolution parameters should be recorded
     * @throws DuplicateObjectException this is thrown when there is a conflict between the resolution parameters of
     * the passed service and the ones of another service. should rollback at that point.
     * @throws UpdateException something went wrong, should rollback at that point
     */
    public void recordResolutionParameters(PublishedService service) throws DuplicateObjectException, UpdateException {
        Collection distinctItemsToSave = getDistinct(service);
        Collection existingParameters = existingResolutionParameters(service.getOid());

        if (isSameParameters(distinctItemsToSave, existingParameters)) {
            logger.finest("resolution parameters unchanged");
            return;
        }

        // get the hibernate session
        HibernatePersistenceContext pc = null;
        Session session = null;
        try {
            pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            session = pc.getSession();
        } catch (SQLException e) {
            String msg = "cannot get hibernate session";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        } catch (HibernateException e) {
            String msg = "cannot get hibernate session";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        try {
            for (Iterator i = existingParameters.iterator(); i.hasNext();) {
                ResolutionParameters todelete = (ResolutionParameters)i.next();
                session.delete(todelete);
            }
        } catch (HibernateException e) {
            String msg = "error deleting exsiting resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // insert these new ones
        try {
            for (Iterator i = distinctItemsToSave.iterator(); i.hasNext();) {
                ResolutionParameters toadd = (ResolutionParameters)i.next();
                session.save(toadd);
            }
            logger.fine("saved " + distinctItemsToSave.size() + " parameters for service " + service.getOid());
        } catch (HibernateException e) {
            String msg = "error adding resolution parameters. throwing duplicate exception";
            logger.log(Level.WARNING, msg, e);
            throw new DuplicateObjectException(msg, e);
        }
    }

    /**
     * deletes all resolution parameters previously recorded for a particular service
     * @param serviceOid id of the service for which recorded resolution parameters will be recorded
     */
    public void deleteResolutionParameters(long serviceOid) throws DeleteException {
        String query = "from " + TABLE_NAME + " in class " + ResolutionParameters.class.getName() +
                       " where " + TABLE_NAME + "." + SVCID_COLUMN + " = " + serviceOid;
        HibernatePersistenceContext context = null;
        Session session = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            session = context.getSession();
            int deleted = session.delete(query);
            logger.finest("deleted " + deleted + " resolution parameters.");
        } catch (SQLException e) {
            String msg = "error deleting resolution parameters with query " + query;
            logger.log(Level.WARNING, msg, e);
            throw new DeleteException(msg, e);
        } catch (HibernateException e) {
            String msg = "error deleting resolution parameters with query " + query;
            logger.log(Level.WARNING, msg, e);
            throw new DeleteException(msg, e);
        }
    }

    private boolean isSameParameters(Collection paramcol1, Collection paramcol2) {
        if (paramcol1.size() != paramcol2.size()) return false;
        if (!paramcol2.containsAll(paramcol1)) return false;
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
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Query q = context.getSession().createQuery(query);
            q.setLong(0, serviceid);
            hibResults = q.list();
        } catch (SQLException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding resolution parameters for " + serviceid, e);
        }  catch (HibernateException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding resolution parameters for " + serviceid, e);
        }

        return hibResults;
    }

    private static final String TABLE_NAME = "service_resolution";
    private static final String SVCID_COLUMN = "serviceid";
    //private static final String SOAPACTION_COLUMN = "soapaction";
    //private static final String URN_COLUMN = "urn";

    protected final Logger logger = Logger.getLogger(getClass().getName());
}
