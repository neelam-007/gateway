package com.l7tech.service.resolution;

import cirrus.hibernate.HibernateException;
import cirrus.hibernate.Query;
import cirrus.hibernate.Session;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.service.PublishedService;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 25, 2003
 * Time: 10:46:59 AM
 * $Id$
 *
 * The ResolutionManager (actually its corresponding table) enforces the uniqueness of resolution
 * parameters across all services.
 *
 * This is used by the ServiceManager when updating and saving services to ensure that resolution
 * parameters do not conflict.
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
        Collection existingParameters = existingResolutionParameters(service);

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

        // remove all trace of resolution parameters for this service
        try {
            for (Iterator i = existingParameters.iterator(); i.hasNext();) {
                ResolutionParameters todelete = (ResolutionParameters)i.next();
                session.delete(todelete);
            }
        } catch (SQLException e) {
            String msg = "error deleting exsiting resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
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
        } catch (SQLException e) {
            String msg = "error adding resolution parameters. throwing duplicate exception";
            logger.log(Level.WARNING, msg, e);
            throw new DuplicateObjectException(msg, e);
        } catch (HibernateException e) {
            String msg = "error adding resolution parameters. throwing duplicate exception";
            logger.log(Level.WARNING, msg, e);
            throw new DuplicateObjectException(msg, e);
        }
    }

    private Collection getDistinct(PublishedService service) {
        ArrayList listOfParameters = new ArrayList();
        SoapActionResolver soapresolver = new SoapActionResolver();
        UrnResolver urnresolver = new UrnResolver();
        Set soapactions = soapresolver.getDistinctParameters(service);
        for (Iterator i = soapactions.iterator(); i.hasNext();) {
            Set urns = urnresolver.getDistinctParameters(service);
            String soapaction = (String)i.next();
            for (Iterator j = urns.iterator(); j.hasNext();) {
                ResolutionParameters parameters = new ResolutionParameters();
                parameters.setServiceid(service.getOid());
                parameters.setSoapaction(soapaction);
                parameters.setUrn((String)j.next());
                listOfParameters.add(parameters);
            }
        }
        return listOfParameters;
    }

    private Collection existingResolutionParameters(PublishedService service) {
        String query = "from " + TABLE_NAME + " in class " + ResolutionParameters.class.getName() +
                       " where " + TABLE_NAME + "." + SVCID_COLUMN + " = ?";

        List hibResults = null;
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Query q = context.getSession().createQuery(query);
            q.setLong(0, service.getOid());
            hibResults = q.list();
        } catch (SQLException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding resolution parameters for " + service.getOid(), e);
        }  catch (HibernateException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding resolution parameters for " + service.getOid(), e);
        }

        return hibResults;
    }

    private static final String TABLE_NAME = "service_resolution";
    private static final String SVCID_COLUMN = "serviceid";
    private static final String SOAPACTION_COLUMN = "soapaction";
    private static final String URN_COLUMN = "urn";

    protected Logger logger = LogManager.getInstance().getSystemLogger();
}
