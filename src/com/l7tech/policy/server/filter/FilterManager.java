package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Manages the filters that are applied to the policies before they are sent back to a requestor.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 * $Id$
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
     * @param policyRequestor or null is the requestor is anonymous
     * @param rootAssertion is modified by the filter(s)
     * @return the filtered policy, null if a filter decided that this user has absolutely no business with this policy
     *         null can also mean that the policy was stripped of all its content. it should not be regrded as an error
     *         if the policyRequestor arg was null
     * @throws FilteringException
     */
    public Assertion applyAllFilters(User policyRequestor, Assertion rootAssertion) throws FilteringException {
        for (int i = 0; i < filterTypes.length; i++) {
            Filter filter = null;
            try {
                filter = (Filter)filterTypes[i].newInstance();
                rootAssertion = filter.filter(policyRequestor, rootAssertion);
                if (rootAssertion == null) {
                    logger.warning("filter returned null root assertion " + filterTypes[i].getName());
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
        // check for empty assertion to avoid sending back an empty ALL
        if (rootAssertion != null && rootAssertion instanceof AllAssertion) {
            AllAssertion root = (AllAssertion)rootAssertion;
            if (root.children() == null || !root.children().hasNext()) return null;
        }
        return rootAssertion;
    }

    /**
     * This takes the passed policy and sends back a filtered version
     * @param policyRequestor or null is the requestor is anonymous
     * @param policyToFilter this is not affected
     * @return the policy filtered by all registered filters
     * @throws FilteringException
     */
    public PublishedService applyAllFilters(User policyRequestor, PublishedService policyToFilter)
            throws FilteringException {
        // make local copy. dont touch original!
        PublishedService localCopyOfService = new PublishedService();
        try {
            localCopyOfService.copyFrom(policyToFilter);
            // copy from does not touch the version so this local object has to be
            // version set manually
            localCopyOfService.setVersion(policyToFilter.getVersion());
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
        if (applyAllFilters(policyRequestor, rootassertion) == null) {
            return null;
        }

        localCopyOfService.setOid(policyToFilter.getOid());
        localCopyOfService.setPolicyXml(WspWriter.getPolicyXml(rootassertion));
        return localCopyOfService;
    }

    protected FilterManager() {
        loadFilterTypes();
    }

    protected synchronized void loadFilterTypes() {
        // todo, load this from some config file
        filterTypes = new Class[] {
            IdentityRule.class,
            //HideRoutingTarget.class, // no longer needed
            HideUnsupportedClientAssertions.class,
        };
    }

    private static FilterManager singleton = null;
    private Class[] filterTypes = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
