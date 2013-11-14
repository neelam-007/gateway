package com.l7tech.external.assertions.radius.server;

import com.l7tech.external.assertions.radius.RadiusAssertion;
import com.l7tech.external.assertions.radius.RadiusReply;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.HexUtils;
import net.jradius.exception.UnknownAttributeException;
import net.jradius.packet.RadiusPacket;

public class RadiusSelector implements ExpandVariables.Selector<RadiusSelector> {

    private RadiusReply value;

    @Override
    public Selection select(String contextName, RadiusSelector context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {

        RadiusReply reply = context.getValue();

        try {
            if (RadiusAssertion.REASON_CODE.equals(name)) {
                return new Selection(Integer.toString(reply.getReturnCode()));
            } else {
                RadiusPacket packet = reply.getPacket();
                if (packet != null) {
                    Object value = packet.getAttributeValue(name);
                    if (value != null) {
                        if (value instanceof byte[]) {
                            return new Selection(HexUtils.encodeBase64((byte[]) value));
                        } else {
                            return new Selection(value.toString());
                        }
                    }
                }
            }
        } catch (UnknownAttributeException e) {
        }
        String msg = handler.handleBadVariable("Unable to process variable name: " + name);
        if (strict) throw new IllegalArgumentException(msg);
        return null;
    }

    @Override
    public Class<RadiusSelector> getContextObjectClass() {
        return RadiusSelector.class;
    }

    public void setValue(RadiusReply value) {
        this.value = value;
    }

    public RadiusReply getValue() {
        return value;
    }
}
