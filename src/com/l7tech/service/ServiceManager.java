package com.l7tech.service;

import com.l7tech.objectmodel.EntityManager;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 * Service API. Get instance of this through the Locator class.
 */
public interface ServiceManager extends EntityManager {

    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     *
     * @param url
     * @return a string containing the xml document
     */
    public String resolveWsdlTarget(String url);

    // NOTE:
    // add methods as they become necessary
}
