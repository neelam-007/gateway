package com.l7tech.service;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.wsdl.WSDLException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class ServiceManagerStub.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceManagerStub extends ApplicationObjectSupport implements ServiceManager, InitializingBean {
    private Map<Long, PublishedService> services;
    private static Logger logger = Logger.getLogger(ServiceManagerStub.class.getName());
    private ServiceCache serviceCache;

    public ServiceManagerStub() {
        services = StubDataStore.defaultStore().getPublishedServices();
    }

    /**
     * Retreive the actual PublishedService object from it's oid.
     * 
     * @param oid
     * @throws FindException
     */
    public PublishedService findByPrimaryKey(long oid) throws FindException {
        return services.get(oid);
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
    public String resolveWsdlTarget(String url) {
        try {
            Wsdl wsdl =
              Wsdl.newInstance(null, new InputStreamReader(new URL(url).openStream()));
            StringWriter sw = new StringWriter();
            wsdl.toWriter(sw);
            return sw.toString();
        } catch (WSDLException e) {
            throw new RuntimeException("resolveWsdlTarget()", e);
        } catch (java.io.IOException e) {
            throw new RuntimeException("resolveWsdlTarget()", e);
        }
    }

    /**
     * saves a published service along with it's policy assertions
     * 
     * @param service
     * @throws SaveException
     */
    public long save(PublishedService service) throws SaveException {
        long oid = StubDataStore.defaultStore().nextObjectId();
        service.setOid(oid);
        if (services.get(oid) != null) {
            throw new SaveException("Record exists, service oid= " + service.getOid());
        }
        services.put(oid, service);
        try {
            serviceCache.cache(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return oid;
    }

    public void initiateServiceCache() {
    }

    public void addManageServiceRole(PublishedService service) throws SaveException {
        // No-op for stub mode
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
        Long key = service.getOid();
        if (services.get(key) == null) {
            throw new UpdateException("Record missing, service oid= " + service.getOid());
        }
        PublishedService oldService = services.remove(key);
        services.put(key, service);
        try {
            serviceCache.removeFromCache(oldService);
            serviceCache.cache(service);
        } catch (Exception e) {
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
        if (services.remove(service.getOid()) == null) {
            throw new DeleteException("Could not find service oid= " + service.getOid());
        }
        try {
            serviceCache.removeFromCache(service);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * called at run time to discover which service is being invoked based
     * on the request headers and/or document.
     * 
     * @param request
     * @return a PublishedService instance.
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
    public Collection<EntityHeader> findAllHeaders() throws FindException {
        Collection<EntityHeader> list = new ArrayList<EntityHeader>();
        for (Long key : services.keySet()) {
            list.add(fromService(services.get(key)));
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
    public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        Collection<EntityHeader> list = new ArrayList<EntityHeader>();
        int index = 0;
        int count = 0;
        for (Iterator i =
          services.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long)i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromService(services.get(key)));
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
    public Collection<PublishedService> findAll() throws FindException {
        Collection<PublishedService> list = new ArrayList<PublishedService>();
        for (Long key : services.keySet()) {
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
    public Collection<PublishedService> findAll(int offset, int windowSize) throws FindException {
        Collection<PublishedService> list = new ArrayList<PublishedService>();
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
        return services.get(oid).getVersion();
    }

    public Map<Long, Integer> findVersionMap() throws FindException {
        Map<Long, Integer> versions = new HashMap<Long, Integer>();
        for (Object o : services.keySet()) {
            Long oid = (Long) o;
            Integer version = getVersion(oid.longValue());
            versions.put(oid, version);
        }
        return versions;
    }

    public PublishedService getCachedEntity( long o, int maxAge ) throws FindException, CacheVeto {
        return findByPrimaryKey(o);
    }

    public PublishedService findByUniqueName(String name) throws FindException {
        for (Map.Entry<Long, PublishedService> entry : services.entrySet()) {
            PublishedService ps = entry.getValue();
            if (ps != null && ps.getName().equals(name)) return ps;
        }
        return null;
    }

    public void delete(long oid) throws DeleteException, FindException {
        services.remove(oid);
    }

    public PublishedService findEntity(long l) throws FindException {
        return findByPrimaryKey(l);
    }

    public Class getImpClass() {
        return PublishedService.class;
    }

    public Class getInterfaceClass() {
        return PublishedService.class;
    }

    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    public String getTableName() {
        return "published_service";
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
                Collection<PublishedService> services = findAll();
                for (PublishedService service : services) {
                    try {
                        serviceCache.cache(service);
                    } catch (ServerPolicyException e) {
                        logger.log(Level.WARNING, "Service disabled: " + ExceptionUtils.getMessage(e));
                    }
                }
            }
            // make sure the integrity check is running
            serviceCache.initiateIntegrityCheckProcess();
        } catch (InterruptedException e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }

}
