package com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/12/11
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class MLLPCodecConfiguration implements CodecConfiguration, Serializable {
    private byte startByte = 0x0b;
    private byte endByte1 = 0x1c;
    private byte endByte2 = 0x0d;

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
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setInbound(boolean value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
