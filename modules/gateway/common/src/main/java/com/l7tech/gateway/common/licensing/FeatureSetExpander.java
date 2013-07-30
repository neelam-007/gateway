package com.l7tech.gateway.common.licensing;

import java.util.Set;

/**
 * Interface for expansion of the input feature names into the complete set of feature names implied by the input set.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public interface FeatureSetExpander {

    /**
     * Expand the input feature names into the complete set of feature names implied by the input set.
     * <p/>
     * For example, if a license included the features "set:Profile:Firewall", "set:ssb", and "assertion:Wssp",
     * getAllEnabledFeatures() would be called with a Set containing just these three names, and would be expected
     * to return an expanded Set containing all the leaf Feature names implied by these.  In the preceding example,
     * this would return a large set of many dozens of feature sets, including subsets and leaf features,
     * specifically including (for example) "set:core", "assertion:HttpRouting", "service:MessageProcessor",
     * and "assertion:Wssp".
     *
     * @param inputSet  a Set of Strings consisting of all feature set names that were explicitly named in
     *                  the license XML.  Never null, and never contains null or empty strings.
     *                  <p/>
     *                  This may be empty if the license contained no featureset elements.  <b>Any license
     *                  generated prior to summer of 2006 will be like this.</b>  The FeatureSetExpander
     *                  is responsible for ensuring backwards compatibility by deciding what features to
     *                  enable in this case (a license that is signed and valid but that names no features).
     * @return the complete set of all features that should be enabled given this input set.  Typically this
     *         would always include the input set (and any intermediate sets) as a subset,
     *         but this isn't strictly necessary.  Must never return null.  Returning the empty set
     *         will cause all features to be disabled, roughly equivalent to rejecting the entire license.
     */
    Set<String> getAllEnabledFeatures(Set<String> inputSet);
}
