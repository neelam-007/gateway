package com.l7tech.service;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.StubDataStore;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;

import javax.wsdl.WSDLException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class ServiceManagerStub.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceManagerStub implements ServiceManager {
    private Map services;
    private static Logger logger = Logger.getLogger(ServiceManagerStub.class.getName());

    public ServiceManagerStub() throws ObjectModelException {
        services = StubDataStore.defaultStore().getPublishedServices();
        // build the cache if necessary
        try {
            if (ServiceCache.getInstance().size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.finest("building service cache");
                Collection services = findAll();
                PublishedService service;
                for (Iterator i = services.iterator(); i.hasNext();) {
                    service = (PublishedService)i.next();
                    ServiceCache.getInstance().cache(service);
                }
            }
        } catch (InterruptedException e) {
            throw new ObjectModelException("exception building cache", e);
        } catch (FindException e) {
            throw new ObjectModelException("exception building cache", e);
        }
    }

    /**
     * Retreive the actual PublishedService object from it's oid.
     * 
     * @param oid 
     * @return 
     * @throws FindException 
     */
    public PublishedService findByPrimaryKey(long oid) throws FindException {
        return (PublishedService)services.get(new Long(oid));
    }

    public Map serviceMap() {
        return Collections.unmodifiableMap(services);
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
     * 
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

    public int getCurrentPolicyVersion(long policyId) throws FindException {
        throw new FindException("not implemented");
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
     * 
     * @param service 
     * @throws DeleteException 
     */
    public void delete(PublishedService service) throws DeleteException {
        if (services.remove(new Long(service.getOid())) == null) {
            throw new DeleteException("Could not find service oid= " + service.getOid());
        }
    }

    public ServerAssertion getServerPolicy(long serviceOid) throws FindException {
        try {
            return ServiceCache.getInstance().getServerPolicy(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing policy from cache", e);
        }
    }

    public PublishedService resolve(Request req) throws ServiceResolutionException {
        return ServiceCache.getInstance().resolve(req);
    }

    public ServiceStatistics getServiceStatistics(long serviceOid) throws FindException {
         try {
            return ServiceCache.getInstance().getServiceStatistics(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    public Collection getAllServiceStatistics() throws FindException {
         try {
            return ServiceCache.getInstance().getAllServiceStatistics();
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    /**
     * called at run time to discover which service is being invoked based
     * on the request headers and/or document.
     * 
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
            Long key = (Long)i.next();
            list.add(fromService((PublishedService)services.get(key)));
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
            Long key = (Long)i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromService((PublishedService)services.get(key)));
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
            Long key = (Long)i.next();
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
            Long key = (Long)i.next();

            if (index >= offset && count <= windowSize) {
                list.add(services.get(key));
                count++;
            }
        }
        return list;
    }

    public Integer getVersion( long oid ) throws FindException {
        return new Integer(((PublishedService)services.get(new Long(oid))).getVersion());
    }

    public Map findVersionMap() throws FindException {
        Map versions = new HashMap();
        for ( Iterator i = services.keySet().iterator(); i.hasNext(); ) {
            Long oid = (Long) i.next();
            Integer version = getVersion(oid.longValue());
            versions.put(oid,version);
        }
        return versions;
    }

    public Entity getCachedEntity( long o, int maxAge ) throws FindException, CacheVeto {
        return findByPrimaryKey(o);
    }

    private EntityHeader fromService(PublishedService s) {
        return
          new EntityHeader(s.getOid(), EntityType.SERVICE, s.getName(), null);
    }
}
