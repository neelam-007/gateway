package com.l7tech.server.policy.variable;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.service.PublishedService;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 8, 2008
 * Time: 8:58:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerVariablesTest extends TestCase {

    /*
    * testServiceNameContextVariable creates a PolicyEncofcementContext and gives it a
    * PublishedService. The static ServerVariabes(String, PolicyEncorcementContext) is used
    * to retrieve the value of service.name which should equal the name of the service created.
    * */
    public void testServiceNameContextVariable() throws Exception{
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<myresponse/>"));
        PolicyEnforcementContext pec = new PolicyEnforcementContext(request, response);
        PublishedService ps = new PublishedService();
        String serviceName = "testServiceNameContextVariable";
        ps.setName(serviceName);
        pec.setService(ps);
        //Now the pec has a service so the variable service.name should be available
        String variableName = "service.name";
        String variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service name",serviceName,variableValue);
        
    }
}
