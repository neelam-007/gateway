package com.l7tech.policy.server.filter;

import com.l7tech.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.logging.LogManager;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * User: flascell
 * Date: Aug 14, 2003
 * Time: 2:58:48 PM
 * $Id$
 *
 * Manages the filters that are applied to the policies before they are sent back to a requestor.
 */
public class FilterManager {
    public static FilterManager getInstance() {
        if (singleton == null) {
            singleton = new FilterManager();
        }
        return singleton;
    }

    /**
     * Takes an assertion tree and passes it through the registered filters.
     * @param policyRequestor
     * @param rootAssertion
     * @return
     * @throws FilteringException
     */
    public Assertion applyAllFilters(User policyRequestor, Assertion rootAssertion) throws FilteringException {
        for (int i = 0; i < filterTypes.length; i++) {
            Filter filter = null;
            try {
                filter = (Filter)filterTypes[i].newInstance();
                rootAssertion = filter.filter(policyRequestor, rootAssertion);
                if (rootAssertion == null) {
                    logger.log(Level.WARNING, "filter returned null root assertion " + filterTypes[i].getName());
                    return null;
                }
            } catch (InstantiationException e) {
                throw new FilteringException("could not instantiate filter ", e);
            } catch (IllegalAccessException e) {
                throw new FilteringException("could not instantiate filter ", e);
            } catch (FilteringException e) {
                throw new FilteringException("error while applying filter ", e);
            }
        }
        return rootAssertion;
    }

    /**
     * This takes the passed policy and sends back a filtered version
     * @param policyRequestor
     * @param policyToFilter this is not affected
     * @return the policy filtered by all registered filters
     * @throws FilteringException
     */
    public PublishedService applyAllFilters(User policyRequestor, PublishedService policyToFilter) throws FilteringException {
        // make local copy. dont touch original!
        PublishedService localCopyOfService = new PublishedService();
        try {
            localCopyOfService.copyFrom(policyToFilter);
        } catch (IOException e) {
            throw new FilteringException(e);
        }

        // start at the top
        Assertion rootassertion = null;
        // modify the assertion tree
        try {
            rootassertion = WspReader.parse(localCopyOfService.getPolicyXml());
        } catch (IOException e) {
            throw new FilteringException(e);
        }
        applyAllFilters(policyRequestor, rootassertion);
        localCopyOfService.setPolicyXml(WspWriter.getPolicyXml(rootassertion));
        return localCopyOfService;
    }

    protected FilterManager() {
        loadFilterTypes();
    }

    protected synchronized void loadFilterTypes() {
        // todo, load this from some config file
        filterTypes = new Class[2];
        filterTypes[0] = IdentityRule.class;
        filterTypes[1] = HideRoutingTarget.class;
        logger = LogManager.getInstance().getSystemLogger();
    }

    private static FilterManager singleton = null;
    private Class[] filterTypes = null;
    private Logger logger = null;
}
