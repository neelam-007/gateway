package com.l7tech.external.assertions.radius;

import net.jradius.packet.RadiusPacket;

public class RadiusReply {

    private int returnCode;
    private RadiusPacket packet;

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public RadiusPacket getPacket() {
        return packet;
    }

    public void setPacket(RadiusPacket packet) {
        this.packet = packet;
    }
}
