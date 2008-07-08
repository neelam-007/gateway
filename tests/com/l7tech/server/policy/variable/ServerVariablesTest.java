package com.l7tech.server.policy.variable;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import junit.framework.TestCase;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.ApplicationContext;

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
        PolicyEnforcementContext pec = getPolicyEncorcementContext();
        PublishedService ps = new PublishedService();
        String serviceName = "testServiceNameContextVariable";
        ps.setName(serviceName);
        pec.setService(ps);
        //Now the pec has a service so the variable service.name should be available
        String variableName = "service.name";
        String variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service name",serviceName,variableValue);
        
    }

    /*
    * testServiceNameContextVariable creates a PolicyEncofcementContext and gives it a
    * PublishedService. The static ServerVariabes(String, PolicyEncorcementContext) is used
    * to retrieve the value of service.oid which should equal the oid of the service created.
    * */
    public void testServiceOidContextVariable() throws Exception{
        PolicyEnforcementContext pec = getPolicyEncorcementContext();
        PublishedService ps = new PublishedService();
        Long l = new Long(123456L);
        ps.setOid(l);
        String serviceName = "testServiceOidContextVariable";
        ps.setName(serviceName);
        pec.setService(ps);
        //Now the pec has a service so the variable service.oid should be available
        String variableName = "service.oid";
        String variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service oid",l.toString(),variableValue);

    }    

    /*
    * Test the service.url context variable and associated suffixes
    * : host, protocol, path, file, query
    * */
    public void testServiceUrlContextVariables() throws Exception{
        ApplicationContext applicationContext = getApplicationContext();
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<myresponse/>"));
        PolicyEnforcementContext pec = new PolicyEnforcementContext(request, response);

        String host = "servername.l7tech.com";
        String protocol = "http";
        String port = "8080";
        String filePath = "/HelloTestService";
        String query = "?query";
        String url = protocol+"://"+host+":"+port+filePath+query;
        HttpRoutingAssertion hRA = new HttpRoutingAssertion(url);
        ServerHttpRoutingAssertion sHRA  = new ServerHttpRoutingAssertion(hRA, applicationContext);
        try{
            sHRA.checkRequest(pec);
        }catch(Exception ex){
            //This is expected, we don't expect to rouet to the url, just want
            //tryUrl to get called so that it sets the routed url property
        }
        
        String variableName = "service.url";
        String variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",url,variableValue);

        variableName = "service.url.host";
        variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",host,variableValue);

        variableName = "service.url.protocol";
        variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",protocol,variableValue);

        variableName = "service.url.port";
        variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",port,variableValue);

        //file expects the query string
        variableName = "service.url.file";
        variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",filePath+query,variableValue);

        //path doesn't expect the query string
        variableName = "service.url.path";
        variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",filePath,variableValue);

        variableName = "service.url.query";
        variableValue = ServerVariables.get(variableName, pec).toString();
        assertEquals("ServerVariable should equal service url",query,variableValue);
    }

    private PolicyEnforcementContext getPolicyEncorcementContext(){
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<myresponse/>"));
        PolicyEnforcementContext pec = new PolicyEnforcementContext(request, response);
        return pec;
    }

    private ApplicationContext getApplicationContext(){
        //this context is the same as in the gateway boot process
        //probably don't need all of these resources, just loading all for now
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/server/resources/dataAccessContext.xml",
                "com/l7tech/server/resources/ssgApplicationContext.xml",
                "com/l7tech/server/resources/adminContext.xml",
                "com/l7tech/server/resources/rbacEnforcementContext.xml",
                "org/codehaus/xfire/spring/xfire.xml",
        });
        return applicationContext;        
    }
}
