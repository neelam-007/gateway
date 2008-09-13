package com.l7tech.gateway.common.spring.remoting;

import java.lang.annotation.Annotation;

/**
 * The remoting provider interface implemented for the server side of remoting.
 *
 * @author steve
 */
public interface RemotingProvider<T extends Annotation> {

    /**
     * Check that the activity is permitted for the facility.
     *
     * <p>A runtime exception should be thrown on denial.</p>
     *
     * @param facility The facility being accessed
     * @param annotation The annotation for the access (may be null)
     * @param activty A description of the activty
     */
    void checkPermitted( T annotation, String facility, String activty );
}
