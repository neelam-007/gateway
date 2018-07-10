package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 4/2/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class HL7MarshalingException extends Exception {

    public HL7MarshalingException(String msg) {
        super(msg);
    }

    public HL7MarshalingException(Throwable cause) {
        super(cause);
    }
}