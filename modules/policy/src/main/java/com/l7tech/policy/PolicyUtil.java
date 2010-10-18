package com.l7tech.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility routines for working with policy assertion trees.
 */
public class PolicyUtil {

    //- PUBLIC

    /**
     * Check if an ordinal location within a policy is after a routing assertion.
     * <p/>
     * This can be used to check whether an assertion would be post-routing if inserted into a given location,
     * even if there is currently no assertion at that location.
     *
     * @param assertionParent the composite assertion that is the parent of the location you are inspecting.
     * @param policyFinder the policy finder to use to process includes (required)
     * @param indexWithinParent the index within this parent assertion that you are inspecting
     * @return true if there is a RoutingAssertion before the specified index within the specified parent,
     *         or before the specified parent in the policy.
     */
    public static boolean isLocationPostRouting( Assertion assertionParent,
                                                 final GuidBasedEntityManager<Policy> policyFinder, 
                                                 final int indexWithinParent) {
        if (assertionParent instanceof AllAssertion) {
            AllAssertion parent = (AllAssertion)assertionParent;
            Iterator i = parent.children();
            int pos = 0;
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                if (pos < indexWithinParent) {
                    if ( isRouting( child, policyFinder ) ) {
                        return true;
                    }
                }
                pos++;
            }
        }
        Assertion previous = assertionParent;
        assertionParent = assertionParent.getParent();
        while (assertionParent != null) {
            if (assertionParent instanceof AllAssertion) {
                AllAssertion parent = (AllAssertion)assertionParent;
                Iterator i = parent.children();
                while (i.hasNext()) {
                    Assertion child = (Assertion)i.next();
                    if ( isRouting( child, policyFinder ) ) {
                        return true;
                    }
                    if (child == previous) break;
                }
            }
            previous = assertionParent;
            assertionParent = assertionParent.getParent();
        }
        return false;
    }

    /**
     * Check if an assertion within a policy is after a routing assertion.
     *
     * @param assertion the assertion to check.  If null, or if it has a null parent, this method always returns false.
     * @param policyFinder the policy finder to use to process includes (required)
     * @return true if there is a routing assertion before this assertion in its owning policy.
     */
    public static boolean isAssertionPostRouting( final Assertion assertion,
                                                  final GuidBasedEntityManager<Policy> policyFinder ) {
        if (assertion == null)
            return false;
        final CompositeAssertion parent = assertion.getParent();
        return parent != null &&
                isLocationPostRouting(parent, policyFinder, assertion.getOrdinal() - parent.getOrdinal());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyUtil.class.getName() );

    private static boolean isRouting( final Assertion child, final GuidBasedEntityManager<Policy> policyFinder ) {
        boolean routing = false;

        if ( child.isEnabled() ) {
            if ( child instanceof RoutingAssertion ) {
                routing = true;
            } else if ( child instanceof Include ) {
                Include include = (Include) child;
                try {
                    Policy policy = policyFinder.findByGuid( include.getPolicyGuid() );
                    if ( policy != null ) {
                        routing = Assertion.contains( policy.getAssertion(), RoutingAssertion.class );
                    } else {
                        logger.log( Level.WARNING, "Policy not found for GUID '"+include.getPolicyGuid()+"' when checking if post routing." );   
                    }
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error processing policy include for GUID '"+include.getPolicyGuid()+"'.", e );
                } catch (IOException e) {
                    logger.log( Level.WARNING, "Error processing policy include for GUID '"+include.getPolicyGuid()+"'.", e );
                }
            }
        }

        return routing;
    }

    /**
     * Invoke the specified visitor on the specified assertion and alll of its descendant assertions, in pre-order, including
     * include targets (if Include, and a CurrentAssertionTranslator is available).
     * <p/>
     * If a CurrentAssertionTranslator is available, Include assertions may trigger an extra visit to the Include
     * assertion itself prior to its target subtree being visited.
     *
     * @param assertion the root of the subtree of assertions to visit.  Required.
     * @param visitor the visitor to invoke for each assertion.  Required.
     */
    public static void visitDescendantsAndSelf(final Assertion assertion, Functions.UnaryVoid<Assertion> visitor) {
        visitDescendantsAndSelf(assertion, visitor, CurrentAssertionTranslator.get());
    }

    /**
     * Invoke the specified visitor on the specified assertion and all of its descendant assertions, in pre-order, including
     * include targets (if Include, and a non-null assertionTranslator is provided).
     * <p/>
     * If an assertionTranslator is supplied, Include assertions may trigger an extra visit to the Include assertion
     * itself before the root of its target subtree is (also) visited (followed by the target's descendants, if any).
     *
     * @param assertion the root of the subtree of assertions to visit.  Required.
     * @param visitor the visitor to invoke for each assertion.  Required.
     * @param assertionTranslator the assertion translator to use to process Include assertions within this assertion.  May be null, in which case
     *                            only the Include assertions will be visited and not their targets.
     */
    public static void visitDescendantsAndSelf(final Assertion assertion, Functions.UnaryVoid<Assertion> visitor, AssertionTranslator assertionTranslator) {
        Iterator<Assertion> it = assertion.preorderIterator();
        while (it.hasNext()) {
            Assertion kid = it.next();
            if (kid == null || !kid.isEnabled())
                continue;

            visitor.call(kid);

            if (assertionTranslator != null && kid instanceof Include) {
                try {
                    Assertion translated = assertionTranslator.translate(kid);
                    if (translated != kid && translated.isEnabled())
                        visitDescendantsAndSelf(translated, visitor, assertionTranslator);
                } catch (PolicyAssertionException e) {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Error translating assertion: " + ExceptionUtils.getMessage(e), e);
                } finally {
                    assertionTranslator.translationFinished(assertion);
                }
            }
        }
    }
}
