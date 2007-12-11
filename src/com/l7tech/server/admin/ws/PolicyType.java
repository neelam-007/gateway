package com.l7tech.server.admin.ws;

import org.codehaus.xfire.aegis.MessageReader;
import org.codehaus.xfire.aegis.MessageWriter;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.fault.XFireFault;

import com.l7tech.common.policy.Policy;

/**
 * Aegis type mapping for Policy -> policy XML
 *
 * @author Steve Jones
 */
public class PolicyType extends org.codehaus.xfire.aegis.type.Type {

    /**
     * Not implemented
     */
    public Object readObject(final MessageReader messageReader,
                             final MessageContext messageContext) throws XFireFault {
        throw new XFireFault(new UnsupportedOperationException("Read not supported"));
    }

    /**
     * Write the policy as XML
     */
    public void writeObject(final Object object,
                            final MessageWriter messageWriter,
                            final MessageContext messageContext) throws XFireFault {
        Policy policy = (Policy) object;

        messageWriter.writeValue(policy.getXml());
    }
}
