/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author alex
 */
public interface Message {

    static final String PREFIX             = "com.l7tech.message";
    static final String PREFIX_HTTP        = PREFIX + ".http";
    static final String PREFIX_HTTP_HEADER = "header";

    public static final String PARAM_HTTP_CONTENT_TYPE      = PREFIX_HTTP_HEADER + "." + XmlUtil.CONTENT_TYPE;
    public static final String PARAM_HTTP_CONTENT_LENGTH    = PREFIX_HTTP_HEADER + "." + XmlUtil.CONTENT_LENGTH;
    public static final String PARAM_HTTP_DATE              = PREFIX_HTTP_HEADER + ".Date";

    TransportMetadata getTransportMetadata();
    Iterator getParameterNames();
    void setParameter( String name, Object value );
    void setParameterIfEmpty( String name, Object value );

    /**
     * Returns the value of a parameter, or the first value in a multivalued parameter if it has multiple values.
     */
    Object getParameter( String name );

    /**
     * Returns the array of values for a parameter, or an array with one element if it has one value.
     */
    Object[] getParameterValues( String name );

    /**
     * Obtain the ordered list of ServerAssertion instances that should be applied to this message after
     * policy tree traversal has completed.
     *
     * @return an ordered Collection of ServerAssertion instances.  May be null.
     */
    Collection getDeferredAssertions();

    /**
     * Schedule a deferred ServerAssertion to apply to the message after policy tree traversal has finished.
     * Each real, policy-embedded ServerAssertion instance may schedule at most one deferred assertion per Message.
     *
     * @param owner       The real, policy-embedded ServerAssertion that wants to apply this deferred decoration
     * @param decoration  The assertion to append to the list of deferred decorations.
     */
    void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration);

    /**
     * Cancel a deferred ServerAssertion, perhaps because its owner assertion (or some ancestor) was eventually
     * falsified.
     *
     * @param owner The real, policy-embedded ServerAssertion whose deferred assertion (if any) should be canceled.
     */
    void removeDeferredAssertion(ServerAssertion owner);
}
