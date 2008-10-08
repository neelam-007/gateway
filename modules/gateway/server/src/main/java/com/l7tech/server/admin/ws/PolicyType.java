package com.l7tech.server.admin.ws;

import javax.xml.namespace.QName;

import com.l7tech.policy.Policy;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;

/**
 * Aegis type mapping for Policy -> policy XML
 *
 * @author Steve Jones
 */
public class PolicyType extends Type {

    /**
     * Not implemented
     */
    public Object readObject(final MessageReader messageReader,
                             final Context messageContext) throws DatabindingException {
        throw new DatabindingException("Read not supported", new UnsupportedOperationException("Read not supported"));
    }

    /**
     * Write the policy as XML
     */
    public void writeObject(final Object object,
                            final MessageWriter messageWriter,
                            final Context messageContext) throws DatabindingException {
        Policy policy = (Policy) object;

        messageWriter.writeValue(policy.getXml());
    }

    /**
     * Policy is an XML string 
     */
    public QName getSchemaType() {
        return new QName("http://www.w3.org/2001/XMLSchema", "string");
    }
}
