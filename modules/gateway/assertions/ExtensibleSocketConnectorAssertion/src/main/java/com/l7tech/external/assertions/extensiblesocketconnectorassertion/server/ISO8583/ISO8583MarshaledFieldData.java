package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 26/02/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583MarshaledFieldData {

    private byte[] data = null;

    private boolean errored = false;
    private String errorMessage = "";

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isErrored() {
        return errored;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
