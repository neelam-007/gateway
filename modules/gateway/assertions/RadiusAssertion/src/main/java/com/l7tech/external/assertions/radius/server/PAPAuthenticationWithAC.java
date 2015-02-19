package com.l7tech.external.assertions.radius.server;

import com.l7tech.util.HexUtils;
import net.jradius.client.auth.PAPAuthenticator;
import net.jradius.exception.RadiusException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.AttributeList;

public class PAPAuthenticationWithAC extends PAPAuthenticator {

    /**
     * Process Challenge method to handle SecureID related challenges
     * @see net.jradius.client.auth.RadiusAuthenticator#processChallenge(net.jradius.packet.RadiusPacket, net.jradius.packet.RadiusPacket)
     */
    public void processChallenge(RadiusPacket request, RadiusPacket challenge)  throws RadiusException
    {   super.processChallenge(request, challenge);
        request.setIdentifier(-1);
        AttributeList attributeList = challenge.getAttributes();
        throw new RadiusExceptionWithAttributes("There was an Access Challenge", attributeList, HexUtils.encodeBase64(challenge.getAuthenticator()));
    }

}
