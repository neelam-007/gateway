package com.l7tech.adminws.logging;

import org.apache.axis.client.Call;
import javax.xml.namespace.QName;
import java.rmi.RemoteException;
import java.io.IOException;
import com.l7tech.adminws.AdminWSClientStub;

/**
 * Layer 7 Technologies, inc.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 12:59:51 PM
 *
 */
public class Client extends AdminWSClientStub {

    public Client() {
        super();
    }

    public String[] getSystemLog(int offset, int size) throws RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(LOGGING_URN, "getSystemLog"));
        call.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "size"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
		call.setReturnClass(String[].class);
        return (String[])call.invoke(new Object[]{new Integer(offset), new Integer(size)});
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected void registerTypeMappings(Call call) {
        QName qn = new QName(LOGGING_URN, "ArrayOfStrings");
        call.registerTypeMapping(String[].class, qn, new org.apache.axis.encoding.ser.ArraySerializerFactory(), new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
    }

    protected String getFullServiceTarget() throws IOException {
        return getServiceBaseURL() + Service.SERVICE_DEPENDENT_URL_PORTION;
    }


    private static final String LOGGING_URN = "http://www.layer7-tech.com/logging";
}
