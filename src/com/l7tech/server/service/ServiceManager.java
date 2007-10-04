package com.l7tech.server.service;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.service.PublishedService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service API. Get instance of this through the Locator class.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 6, 2003
 */
public interface ServiceManager extends EntityManager<PublishedService, EntityHeader> {
    /**
     * Get what the server sees at that url.
     * Meant to be used by admin console entity.
     *
     * @param url the url target to inspect.
     * @return the payload returned at that url (hopefully)
     */
    String resolveWsdlTarget(String url);

    @Transactional(propagation= Propagation.SUPPORTS)
    void initiateServiceCache();

    /**
     * Creates a new role for the specified PublishedService.
     *
     * @param service      the PublishedService that is in need of a Role.  Must not be null.
     * @throws com.l7tech.objectmodel.SaveException  if the new Role could not be saved
     */
    public void addManageServiceRole(PublishedService service) throws SaveException;
}