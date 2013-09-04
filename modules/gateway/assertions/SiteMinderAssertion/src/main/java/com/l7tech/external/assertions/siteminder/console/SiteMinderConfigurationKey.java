package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.Goid;

public class SiteMinderConfigurationKey {

    private String agentId;
    private Goid goid;

    public SiteMinderConfigurationKey(SiteMinderConfiguration config) {
        agentId = config.getName();
        goid = config.getGoid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiteMinderConfigurationKey)) return false;

        SiteMinderConfigurationKey that = (SiteMinderConfigurationKey) o;

        return !(agentId != null ? !agentId.equals(that.agentId) : that.agentId != null);
    }

    @Override
    public int hashCode() {
        return agentId != null ? agentId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return agentId;
    }

    public Goid getGoid() {
        return goid;
    }

    public String getAgentId() {
        return agentId;
    }
}
