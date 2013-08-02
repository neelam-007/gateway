package com.l7tech.server.policy.filter;

import com.l7tech.util.ConstructorInvocation;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.identity.IdentityProviderFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Manages the filters that are applied to the policies before they are sent back to a requestor.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 */
public class FilterManager {
    private final Logger logger = Logger.getLogger(FilterManager.class.getName());
    private final IdentityProviderFactory identityProviderFactory;
    private Class[] filterTypes = null;

    public FilterManager(IdentityProviderFactory identityProviderFactory, Class[] filters) {
        if (identityProviderFactory == null) {
            throw new IllegalArgumentException("Identity Provider Factory cannot be null");
        }
        this.identityProviderFactory = identityProviderFactory;

        if (filters == null) {
            throw new IllegalArgumentException("Filters cannot be null");
        }
        filterTypes = filters;
    }

    public IdentityProviderFactory getIdentityProviderFactory() {
        return identityProviderFactory;
    }

    /**
     * Takes an assertion tree and passes it through the registered filters.
     *
     * @param policyRequestor or null is the requestor is anonymous
     * @param rootAssertion   is modified by the filter(s)
     * @return the filtered policy, null if a filter decided that this user has absolutely no business with this policy
     *         null can also mean that the policy was stripped of all its content. it should not be regrded as an error
     *         if the policyRequestor arg was null
     * @throws FilteringException
     */
    public Assertion applyAllFilters(User policyRequestor, Assertion rootAssertion) throws FilteringException {
        for (Class filterType : filterTypes) {
            Filter filter;
            try {
                // see whether therte is a constructr accepting a filter manager
                Constructor ctor = ConstructorInvocation.findMatchingConstructor(filterType, new Class[]{FilterManager.class});
                if (ctor != null) {
                    filter = (Filter) ctor.newInstance(this);
                } else {
                    filter = (Filter) filterType.newInstance();
                }

                rootAssertion = filter.filter(policyRequestor, rootAssertion);
                if (rootAssertion == null) {
                    logger.warning("filter returned null root assertion " + filterType.getName());
                    return null;
                }
            } catch (InstantiationException e) {
                throw new FilteringException("could not instantiate filter ", e);
            } catch (IllegalAccessException e) {
                throw new FilteringException("could not instantiate filter ", e);
            } catch (FilteringException e) {
                throw new FilteringException("error while applying filter ", e);
            } catch (InvocationTargetException e) {
                throw new FilteringException("could not instantiate filter ", e);
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
     *
     * @param policyRequestor or null is the requestor is anonymous
     * @param policyToFilter  this is not affected
     * @return the policy filtered by all registered filters
     * @throws FilteringException
     */
    public PublishedService applyAllFilters(User policyRequestor, PublishedService policyToFilter)
      throws FilteringException {
        // make local copy. dont touch original!
        PublishedService localCopyOfService = new PublishedService( policyToFilter );

        // start at the top
        Assertion rootassertion;
        // modify the assertion tree
        try {
            rootassertion = WspReader.getDefault().parsePermissively(localCopyOfService.getPolicy().getXml(), WspReader.OMIT_DISABLED);
        } catch (IOException e) {
            throw new FilteringException(e);
        }
        if (applyAllFilters(policyRequestor, rootassertion) == null) {
            return null;
        }

        localCopyOfService.setGoid(policyToFilter.getGoid());
        localCopyOfService.getPolicy().setXml(WspWriter.getPolicyXml(rootassertion));
        return localCopyOfService;
    }

    protected synchronized void loadFilterTypes() {
        // todo, load this from some config file
        filterTypes = new Class[]{
            IdentityRule.class,
            //HideRoutingTarget.class, // no longer needed
            HideUnsupportedClientAssertions.class,
        };
    }

}
