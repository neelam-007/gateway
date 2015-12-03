package com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 4/2/13
 * Time: 10:48 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class HL7CodecConfiguration implements CodecConfiguration, Serializable {
    private String charset = "ISO-8859-1";
    private byte startByte = 0x0b;
    private byte endByte1 = 0x1c;
    private byte endByte2 = 0x0d;

    public String getCharset() {
        return charset;
    }

    @XmlSafe
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public byte getStartByte() {
        return startByte;
    }

    @XmlSafe
    public void setStartByte(byte startByte) {
        this.startByte = startByte;
    }

    public byte getEndByte1() {
        return endByte1;
    }

    @XmlSafe
    public void setEndByte1(byte endByte1) {
        this.endByte1 = endByte1;
    }

    public byte getEndByte2() {
        return endByte2;
    }

    @XmlSafe
    public void setEndByte2(byte endByte2) {
        this.endByte2 = endByte2;
    }

    @Override
    public boolean requiresListenerRestart(CodecConfiguration newConfig) {
        if (!(newConfig instanceof MLLPCodecConfiguration)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isInbound() {
        // Inbound/outbound field is not used.
        //
        throw new UnsupportedOperationException("The method HL7CodecConfiguration.isInbound() is not supported.");
    }

    @Override
    public void setInbound(boolean value) {
        // Do nothing. Inbound/outbound field is not used.
        //
    }
}