package com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.ISO8583EncoderType;
import com.l7tech.util.XmlSafe;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 17/12/12
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class ISO8583CodecConfiguration implements CodecConfiguration, Serializable {

    private ISO8583EncoderType mtiEncoding = ISO8583EncoderType.ASCII;
    private String fieldPropertiesFileLocation = "DEFAULT";
    private boolean secondaryBitmapMandatory = false;
    private boolean tertiaryBitmapMandatory = false;
    private boolean inbound = false;
    private String messageDelimiterString = "03";

    @Override
    public boolean requiresListenerRestart(CodecConfiguration newConfig) {
        if (!(newConfig instanceof CodecConfiguration)) {
            return true;
        }

        return false;
    }

    public ISO8583EncoderType getMtiEncoding() {
        return mtiEncoding;
    }

    @XmlSafe
    public void setMtiEncoding(ISO8583EncoderType mtiEncoding) {
        this.mtiEncoding = mtiEncoding;
    }

    public String getFieldPropertiesFileLocation() {
        return fieldPropertiesFileLocation;
    }

    @XmlSafe
    public void setFieldPropertiesFileLocation(String fieldPropertiesFileLocation) {
        this.fieldPropertiesFileLocation = fieldPropertiesFileLocation;
    }

    public boolean isSecondaryBitmapMandatory() {
        return secondaryBitmapMandatory;
    }

    @XmlSafe
    public void setSecondaryBitmapMandatory(boolean secondaryBitmapMandatory) {
        this.secondaryBitmapMandatory = secondaryBitmapMandatory;
    }

    public boolean isTertiaryBitmapMandatory() {
        return tertiaryBitmapMandatory;
    }

    @XmlSafe
    public void setTertiaryBitmapMandatory(boolean tertiaryBitmapMandatory) {
        this.tertiaryBitmapMandatory = tertiaryBitmapMandatory;
    }

    public String getMessageDelimiterString() {
        return messageDelimiterString;
    }

    @XmlSafe
    public void setMessageDelimiterString(String messageDelimiterString) {
        this.messageDelimiterString = messageDelimiterString;
    }

    public byte[] getMessageDelimiterArray() {

        int messageDelimiterStringLength = messageDelimiterString.length();
        byte[] result = new byte[messageDelimiterStringLength / 2];
        int intValue = 0;
        int bytePosition = 0;

        for (int i = 0; i < messageDelimiterStringLength; i += 2) {
            intValue = Integer.valueOf(messageDelimiterString.substring(i, i + 2), 16);
            result[bytePosition] = (byte) intValue;
            bytePosition++;
        }

        return result;
    }


    @Override
    public boolean isInbound() {
        return inbound;
    }

    @XmlSafe
    @Override
    public void setInbound(boolean value) {
        inbound = value;
    }
}
