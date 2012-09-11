package com.l7tech.external.assertions.demostrategy;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.failover.*;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DemoStrategy<ST> extends AbstractFailoverStrategy<ST> implements ConfigurableFailoverStrategy {


    private Map<String, String> properties = new HashMap<String,String>();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public DemoStrategy(ST[] servers) {
        super(servers);
    }

    @Override
    public ST selectService() {
        return servers[0];
    }

    @Override
    public void reportFailure(ST service) {
    }

    @Override
    public void reportSuccess(ST service) {
    }

    @Override
    public String getName() {
        return "DemoStrategy";
    }

    @Override
    public String getDescription() {
        return "Demo Strategy";
    }

    @Override
    public void setProperties(Map properties) {
        this.properties = properties;
    }

    @Override
    public String getEditorClass() {
        return "com.l7tech.external.assertions.demostrategy.console.ContentAwareStrategyEditor";
    }

    @Override
    public void reportContent(Object content, Feedback feedback) {
        PolicyEnforcementContext pec = (PolicyEnforcementContext) content;
        System.out.println("HTTP Status:" + pec.getResponse().getHttpResponseKnob().getStatus());
        Set<HttpCookie> cookies  = pec.getCookies();
        if (cookies != null) {
            for (Iterator<HttpCookie> iterator = cookies.iterator(); iterator.hasNext(); ) {
                HttpCookie next =  iterator.next();
                System.out.println(next);
            }
        }
    }
}
