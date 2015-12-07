package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 19/02/13
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583MessageTypeIndicator {

    private int version = 0;
    private int messageClass = 0;
    private int function = 0;
    private int origin = 0;

    public ISO8583MessageTypeIndicator() {
    }

    public ISO8583MessageTypeIndicator(int _version, int _messageClass, int _function, int _origin) {
        version = _version;
        messageClass = _messageClass;
        function = _function;
        origin = _origin;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getMessageClass() {
        return messageClass;
    }

    public void setMessageClass(int messageClass) {
        this.messageClass = messageClass;
    }

    public int getFunction() {
        return function;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public int getOrigin() {
        return origin;
    }

    public void setOrigin(int origin) {
        this.origin = origin;
    }
}
