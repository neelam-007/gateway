package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.identity.StubDataStore;

import javax.wsdl.WSDLException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;

/**
 * Class ServiceManagerStub.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
public class ServiceManagerStub implements ServiceManager {
    private Map services;

    public ServiceManagerStub() {
        services = StubDataStore.defaultStore().getPublishedServices();
    }
    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     * @return
     * @throws FindException
     */
    public PublishedService findByPrimaryKey(long oid) throws FindException {
        return
          (PublishedService)services.get(new Long(oid));
    }

    public void addServiceListener( ServiceListener listener ) {
        // Not applicable
    }

    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     *
     * @param url
     * @return a string containing the xml document
     */
    public String resolveWsdlTarget(String url) throws RemoteException {
        try {
            Wsdl wsdl =
              Wsdl.newInstance(null, new InputStreamReader(new URL(url).openStream()));
            StringWriter sw = new StringWriter();
            wsdl.toWriter(sw);
            return sw.toString();
        } catch (WSDLException e) {
            throw new RemoteException("resolveWsdlTarget()", e);
        } catch (java.io.IOException e) {
            throw new RemoteException("resolveWsdlTarget()", e);
        }
    }

    /**
     * saves a published service along with it's policy assertions
     * @param service
     * @return
     * @throws SaveException
     */
    public long save(PublishedService service) throws SaveException {
        long oid = StubDataStore.defaultStore().nextObjectId();
        service.setOid(oid);
        Long key = new Long(oid);
        if (services.get(key) != null) {
            throw new SaveException("Record exists, service oid= " + service.getOid());
        }
        services.put(key, service);
        return oid;
    }

    /**
     * updates a policy service. call this instead of save if the service
     * has an history. on the console side implementation, you can call save
     * either way and the oid will dictate whether the object should be saved
     * or updated.
     *
     * @param service
     * @throws UpdateException
     */
    public void update(PublishedService service) throws UpdateException {
            Long key = new Long(service.getOid());
        if (services.get(key) == null) {
            throw new UpdateException("Record missing, service oid= " + service.getOid());
        }
        services.remove(key);
        services.put(key, service);

    }

    /**
     * deletes the service
     * @param service
     * @throws DeleteException
     */
    public void delete(PublishedService service) throws DeleteException {
          if (services.remove(new Long(service.getOid())) == null) {
            throw new DeleteException("Could not find service oid= " + service.getOid());
        }

    }

    /**
     * called at run time to discover which service is being invoked based
     * on the request headers and/or document.
     * @param request
     * @return
     */
    public PublishedService resolveService(Request request) throws ServiceResolutionException {
        return null;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for all instances of the entity class corresponding to
     * this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
          Collection list = new ArrayList();
        for (Iterator i =
                services.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromService((PublishedService) services.get(key)));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
         Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
                services.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromService((PublishedService) services.get(key)));
                count++;
            }
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code>
     * objects for all instances of the entity class corresponding
     * to this Manager.
     *
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection findAll() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
                services.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(services.get(key));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code>
     * objects for instances of this entity class from a list
     * sorted by <code>oid</code>, selecting only a specific
     * subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
                services.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(services.get(key));
                count++;
            }
        }
        return list;
    }

    public Collection search(String searchString) throws FindException {
        throw new FindException("not implemented");
    }


    private EntityHeader fromService(PublishedService s) {
        return
                new EntityHeader(s.getOid(), EntityType.SERVICE, s.getName(), null);
    }
}
