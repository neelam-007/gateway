package com.l7tech.service;

import com.l7tech.common.message.Message;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

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
public class ServiceManagerStub extends ApplicationObjectSupport implements ServiceManager, InitializingBean {
    private Map services;
    private static Logger logger = Logger.getLogger(ServiceManagerStub.class.getName());
    private ServiceCache serviceCache;

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
        try {
            serviceCache.cache(service);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return oid;
    }

    public int getCurrentPolicyVersion(long policyId) throws FindException {
        throw new FindException("not implemented");
    }

    public Map getServiceVersions() throws FindException {
        HashMap versions = new HashMap();
        for (Iterator iterator = services.values().iterator(); iterator.hasNext();) {
            PublishedService publishedService = (PublishedService)iterator.next();
            versions.put(new Long(publishedService.getOid()), new Integer(1));
        }
        return versions;
    }

    public void setVisitorClassnames(String visitorClasses) {
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
        PublishedService oldService = (PublishedService)services.remove(key);
        services.put(key, service);
        try {
            serviceCache.removeFromCache(oldService);
            serviceCache.cache(service);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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
        try {
            serviceCache.removeFromCache(service);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ServerAssertion getServerPolicy(long serviceOid) throws FindException {
        try {
            return serviceCache.getServerPolicy(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing policy from cache", e);
        }
    }

    public PublishedService resolve(Message req) throws ServiceResolutionException {
        return serviceCache.resolve(req);
    }

    public ServiceStatistics getServiceStatistics(long serviceOid) throws FindException {
         try {
            return serviceCache.getServiceStatistics(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    public Collection getAllServiceStatistics() throws FindException {
         try {
            return serviceCache.getAllServiceStatistics();
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
    public PublishedService resolveService(Message request) throws ServiceResolutionException {
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
        return new EntityHeader(Long.toString(s.getOid()), EntityType.SERVICE, s.getName(), null);
    }

    public void setServiceCache(ServiceCache serviceCache) {
        this.serviceCache = serviceCache;
    }

    public void afterPropertiesSet() throws Exception {
        initializeServiceCache();
    }

    private void initializeServiceCache() throws ObjectModelException {
// build the cache if necessary
        try {
            if (serviceCache.size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.finest("building service cache");
                Collection services = findAll();
                PublishedService service;
                for (Iterator i = services.iterator(); i.hasNext();) {
                    service = (PublishedService)i.next();
                    serviceCache.cache(service);
                }
            }
            // make sure the integrity check is running
            serviceCache.initiateIntegrityCheckProcess();
        } catch (InterruptedException e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }

}
