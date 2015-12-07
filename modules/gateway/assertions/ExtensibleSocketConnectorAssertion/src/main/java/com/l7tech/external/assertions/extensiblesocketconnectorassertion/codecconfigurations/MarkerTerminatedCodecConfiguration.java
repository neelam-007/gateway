package com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/12
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class MarkerTerminatedCodecConfiguration implements CodecConfiguration, Serializable {
    private byte[] termnatorBytes = new byte[]{(byte) 0x00};

    @XmlSafe
    public byte[] getTermnatorBytes() {
        return termnatorBytes;
    }

    @XmlSafe
    public void setTermnatorBytes(byte[] termnatorBytes) {
        this.termnatorBytes = termnatorBytes;
    }

    @Override
    public boolean requiresListenerRestart(CodecConfiguration newConfig) {
        if (!(newConfig instanceof MarkerTerminatedCodecConfiguration)) {
            return !Arrays.equals(termnatorBytes, ((MarkerTerminatedCodecConfiguration) newConfig).getTermnatorBytes());
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
