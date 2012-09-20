package com.l7tech.message;

import com.l7tech.common.http.GenericHttpRequestParams;

/**
 * Implementation of the HttpOutboundRequestKnob for attachment to Messages.
 *
 * <p>This facet supports aggregation of HTTP headers for use when routing.</p>
 */
public class HttpOutboundRequestFacet implements HttpOutboundRequestKnob {

    //- PUBLIC

    @Override
    public void setDateHeader( final String name, final long date ) {
        headerSupport.setDateHeader( name, date );
    }

    @Override
    public void addDateHeader( final String name, final long date ) {
        headerSupport.addDateHeader( name, date );
    }

    @Override
    public void setHeader( final String name, final String value ) {
        headerSupport.setHeader( name, value );
    }

    @Override
    public void addHeader( final String name, final String value ) {
        headerSupport.addHeader( name, value );
    }

    @Override
    public String[] getHeaderValues( final String name ) {
        return headerSupport.getHeaderValues( name );
    }

    @Override
    public String[] getHeaderNames( ) {
        return headerSupport.getHeaderNames();
    }

    @Override
    public boolean containsHeader( final String name ) {
        return headerSupport.containsHeader( name );
    }

    @Override
    public void removeHeader( final String name ) {
        headerSupport.removeHeader( name );
    }

    @Override
    public void clearHeaders() {
        headerSupport.clearHeaders();
    }

    @Override
    public void writeHeaders( final GenericHttpRequestParams target ) {
        headerSupport.writeHeaders( target );
    }

    /**
     * Ensure that the specified message has an HttpOutboundRequestKnob.
     *
     * @param message the Message that should have an HttpOutboundRequestKnob.  Required.
     * @return an existing or new HttpOutboundRequestKnob.  Never null.
     */
    public static HttpOutboundRequestKnob getOrCreateHttpOutboundRequestKnob( final Message message ) {
        HttpOutboundRequestKnob httpOutboundRequestKnob = message.getKnob(HttpOutboundRequestKnob.class);

        if ( httpOutboundRequestKnob == null ) {
            httpOutboundRequestKnob = new HttpOutboundRequestFacet();
            message.attachKnob(httpOutboundRequestKnob, HttpOutboundRequestKnob.class, OutboundHeadersKnob.class);
        }

        return httpOutboundRequestKnob;
    }

    //- PRIVATE

    private final OutboundHeaderSupport headerSupport = new OutboundHeaderSupport();
}
