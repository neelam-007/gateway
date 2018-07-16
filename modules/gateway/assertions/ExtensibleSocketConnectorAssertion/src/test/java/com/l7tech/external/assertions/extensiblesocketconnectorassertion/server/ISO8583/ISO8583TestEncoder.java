package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 26/02/13
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ISO8583TestEncoder {

    public void testEncodingMTI() throws Exception;

    public void testEncodingStaticFieldNonPadded() throws Exception;

    public void testEncodingStaticFieldPadded() throws Exception;

    public void testEncodingVariableField() throws Exception;

    public void testEncodingErrors() throws Exception;
}
