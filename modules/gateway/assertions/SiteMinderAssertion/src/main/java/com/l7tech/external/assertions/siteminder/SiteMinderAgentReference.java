package com.l7tech.external.assertions.siteminder;

import com.l7tech.objectmodel.Goid;

/**
 * Interface to indicate that the assertion references a SiteMinder Agent.
 * User: pakhy01
 * Date: 2017-02-15
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SiteMinderAgentReference {

    /**
     * Gets the agent GOID.
     * @return the agent GOID
     */
    Goid getAgentGoid();

    /***
     * Sets the agent GOID.
     * @param agentID the agent GOID
     */
    void setAgentGoid(Goid agentID);

    /**
     * Gets the agent name.
     * @return the agent name
     */
    String getAgentId();

    /**
     * Sets the agent name.
     * @param agentId the agent name
     */
    void setAgentId(String agentId);
}
