package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import com.l7tech.util.XmlSafe;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 18/02/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public enum ISO8583EncoderType {
    ASCII("ASCII", "com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.ISO8583EncoderASCII"),
    PBCD("Packed BCD", "com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.ISO8583EncoderPackedBCD"),
    BINARY("Binary", "com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.ISO8583EncoderBinary");

    private String displayName = "";
    private String className = "";

    private ISO8583EncoderType(String _displayName, String _className) {
        displayName = _displayName;
        className = _className;
    }

    public String getClassName() {
        return className;
    }

    public static ISO8583EncoderType fromString(String value) {

        ISO8583EncoderType[] encoderTypes = ISO8583EncoderType.values();

        for (ISO8583EncoderType encoderType : encoderTypes) {
            if (encoderType.toString().equals(value))
                return encoderType;
        }

        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }

    ;
}
