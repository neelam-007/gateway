package com.l7tech.service;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.ServiceResolutionException;

import javax.wsdl.WSDLException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Class ServiceManagerStub.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
public class ServiceManagerStub implements ServiceManager {
    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     * @return
     * @throws FindException
     */
    public PublishedService findByPrimaryKey(long oid) throws FindException {
        return null;
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
            return Wsdl.newInstance(null, new InputStreamReader(new URL(url).openStream())).toString();
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
        return 0;
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

    }

    /**
     * deletes the service
     * @param service
     * @throws DeleteException
     */
    public void delete(PublishedService service) throws DeleteException {

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
        return null;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return null;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code>
     * objects for all instances of the entity class corresponding
     * to this Manager.
     *
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection findAll() throws FindException {
        return null;
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
        return null;
    }
}
