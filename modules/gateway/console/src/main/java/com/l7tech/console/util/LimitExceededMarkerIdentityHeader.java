package com.l7tech.console.util;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

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
        super(new Goid(0,-1), new EntityHeader(-1, EntityType.USER, new String(new char[]{Character.MIN_VALUE}), ""));
    }

    public String getName() {
        return "Exceeded maximum search result size";
    }
}
