package com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/11/12
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class LengthPrefixedCodecConfiguration implements CodecConfiguration, Serializable {
    private byte lengthBytes = 4;

    @XmlSafe
    public byte getLengthBytes() {
        return lengthBytes;
    }

    @XmlSafe
    public void setLengthBytes(byte lengthBytes) {
        this.lengthBytes = lengthBytes;
    }

    public boolean requiresListenerRestart(CodecConfiguration newConfig) {
        if (!(newConfig instanceof LengthPrefixedCodecConfiguration)) {
            return lengthBytes != ((LengthPrefixedCodecConfiguration) newConfig).getLengthBytes();
        } else {
            return true;
        }
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
