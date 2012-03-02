package com.l7tech.message;

/**
 * Interface extended by message knobs belonging to transports that provide incoming request URI path information.
 */
public interface UriKnob extends MessageKnob {
    /**
     * URI part of the URL for this request (e.g. /ssg/soap). Never null or empty.
     *
     * <p>This is used for service resolution.</p>
     *
     * @return the uri.  Never null.
     */
    String getRequestUri();
}
