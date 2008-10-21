package com.l7tech.objectmodel;

/**
 * Marker IdentityHeader that signals that the collection it is part of exceeded
 * the maximum configured limit when it was constructed.
 *
 * Replaces the previous artificial EntityType.MAXED_OUT_SEARCH_RESULT.
 * Should be further replaced with a proper signaling mechanism and consistent UI behaviour.
 *
 * @author jbufu
 */
public class LimitExceededMarkerIdentityHeader extends IdentityHeader {

    public LimitExceededMarkerIdentityHeader() {
        super(-1, new EntityHeader());
    }

    public String getName() {
        return "Exceeded maximum search result size";
    }
}
