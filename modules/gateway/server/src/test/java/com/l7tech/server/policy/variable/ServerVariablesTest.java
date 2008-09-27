package com.l7tech.server.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.common.io.XmlUtil;
import org.springframework.context.ApplicationContext;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 */
public class ServerVariablesTest {

    /*
    * testServiceNameContextVariable creates a PolicyEncofcementContext and gives it a
    * PublishedService. The static ServerVariabes(String, PolicyEncorcementContext) is used
    * to retrieve the value of service.name which should equal the name of the service created.
    * */
    @Test
    public void testServiceNameContextVariable() throws Exception{
        PolicyEnforcementContext pec = getPolicyEncorcementContext();
        PublishedService ps = new PublishedService();
        String serviceName = "testServiceNameContextVariable";
        ps.setName(serviceName);
        pec.setService(ps);
        //Now the pec has a service so the variable service.name should be available
        String variableName = "service.name";
        String variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal service name",serviceName,variableValue);
        
    }

    /*
    * testServiceNameContextVariable creates a PolicyEncofcementContext and gives it a
    * PublishedService. The static ServerVariabes(String, PolicyEncorcementContext) is used
    * to retrieve the value of service.oid which should equal the oid of the service created.
    * */
    @Test
    public void testServiceOidContextVariable() throws Exception{
        PolicyEnforcementContext pec = getPolicyEncorcementContext();
        PublishedService ps = new PublishedService();
        Long l = 123456L;
        ps.setOid(l);
        String serviceName = "testServiceOidContextVariable";
        ps.setName(serviceName);
        pec.setService(ps);
        //Now the pec has a service so the variable service.oid should be available
        String variableName = "service.oid";
        String variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal service oid",l.toString(),variableValue);

    }    

    /*
    * Test the service.url context variable and associated suffixes
    * : host, protocol, path, file, query
    * */
    @Test
    public void testServiceUrlContextVariables() throws Exception{
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
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

        String serviceurl = BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_URL;

        // Test the deprecated context variable service.url
        String variableName = serviceurl;
        String variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url", url, variableValue);
        // Test the new context variable httpRouting.url
        variableName = HttpRoutingAssertion.VAR_HTTP_ROUTING_URL;
        variableValue = (String)pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url", url, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_HOST;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url host", host, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlHost();
        variableValue = (String)pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url host", host, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_PROTOCOL;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url protocol", protocol, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlProtocol();
        variableValue = (String)pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url protocol", protocol, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_PORT;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url port", port, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlPort();
        variableValue = pec.getVariable(variableName).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url port", port, variableValue);

        //file expects the query string
        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_FILE;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url file", filePath+query, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlFile();
        variableValue = (String)pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url file", filePath+query, variableValue);

        //path doesn't expect the query string
        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_PATH;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url path", filePath, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlPath();
        variableValue = (String)pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url path", filePath, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_QUERY;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url query", query, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlQuery();
        variableValue = (String)pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url query", query, variableValue);
    }

    private PolicyEnforcementContext getPolicyEncorcementContext(){
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<myresponse/>"));
        return new PolicyEnforcementContext(request, response);
    }

}
