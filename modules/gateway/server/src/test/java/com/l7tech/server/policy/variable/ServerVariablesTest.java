package com.l7tech.server.policy.variable;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.test.BugNumber;
import org.junit.*;
import static org.junit.Assert.*;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class ServerVariablesTest {
    private static final Logger logger = Logger.getLogger(ServerVariablesTest.class.getName());
    private static final LogOnlyAuditor auditor = new LogOnlyAuditor(logger);

    /*
    * testServiceNameContextVariable creates a PolicyEncofcementContext and gives it a
    * PublishedService. The static ServerVariabes(String, PolicyEncorcementContext) is used
    * to retrieve the value of service.name which should equal the name of the service created.
    * */
    @Test
    public void testServiceNameContextVariable() throws Exception{
        PolicyEnforcementContext pec = context();
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
        PolicyEnforcementContext pec = context();
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

    @Test
    public void testNoUser() throws Exception {
        expandAndCheck(context(), "${request.authenticatedUser}", "");
        expandAndCheck(context(), "${request.authenticatedUsers}", "");
        expandAndCheck(context(), "${request.authenticatedDn}", "");
        expandAndCheck(context(), "${request.authenticatedDns}", "");
    }

    @Test
    public void testNoDn() throws Exception {
        PolicyEnforcementContext context = context(new InternalUser("blah"));
        expandAndCheck(context, "${request.authenticatedUser}", "blah");
        expandAndCheck(context, "${request.authenticatedUsers}", "blah");
        expandAndCheck(context, "${request.authenticatedDn}", "");
        expandAndCheck(context, "${request.authenticatedDns}", "");
    }

    @Test
    public void testNullDn() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(-3823, null, null));
        expandAndCheck(context, "${request.authenticatedUser}", "");
        expandAndCheck(context, "${request.authenticatedUsers}", "");
        expandAndCheck(context, "${request.authenticatedDn}", "");
        expandAndCheck(context, "${request.authenticatedDns}", "");
    }

    @Test
    public void testNonNumericSuffix() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(-3823, "cn=blah", "blah"));
        expandAndCheck(context, "${request.authenticatedUser.bogus}", "");
        expandAndCheck(context, "${request.authenticatedUsers.bogus}", "");
        expandAndCheck(context, "${request.authenticatedDn.bogus}", "");
        expandAndCheck(context, "${request.authenticatedDns.bogus}", "");
    }

    @Test
    public void testOutOfBoundsSuffix() throws Exception {
        PolicyEnforcementContext context = context();
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3")));
        expandAndCheck(context, "${request.authenticatedUser}", "test+3");
        expandAndCheck(context, "${request.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${request.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${request.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${request.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${request.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${request.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testEmptyDn() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(-3823, "", null));
        expandAndCheck(context, "${request.authenticatedUser}", "");
        expandAndCheck(context, "${request.authenticatedUsers}", "");
        expandAndCheck(context, "${request.authenticatedDn}", "");
        expandAndCheck(context, "${request.authenticatedDns}", "");
    }

    @Test
    public void testEscapedDnUserName() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${request.authenticatedUser}", "test+1");
        expandAndCheck(context, "${request.authenticatedUsers}", "test+1");
    }

    @Test
    public void testMultipleUserNames() throws Exception {
        PolicyEnforcementContext context = context();
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3")));
        expandAndCheck(context, "${request.authenticatedUser}", "test+3");
        expandAndCheck(context, "${request.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${request.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${request.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${request.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${request.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${request.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testMultipleUserDns() throws Exception {
        PolicyEnforcementContext context = context();
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test1, ou=foobar, o=bling", "test1")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test2", "test2")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test3", "test3")));
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test3");
        expandAndCheck(context, "${request.authenticatedDn.0}", "cn=test1, ou=foobar, o=bling");
        expandAndCheck(context, "${request.authenticatedDn.1}", "cn=test2");
        expandAndCheck(context, "${request.authenticatedDn.2}", "cn=test3");
        expandAndCheck(context, "||${request.authenticatedDns|||}||", "||cn=test1, ou=foobar, o=bling||cn=test2||cn=test3||");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
        expandAndCheck(context, "${request.authenticatedDns|, }", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
    }

    @Test
    @BugNumber(6813)
    public void testEscapedDnUserDn() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test\\+1, ou=foobar, o=bling");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    @BugNumber(6813)
    public void testMultipleUserDnsEscaped() throws Exception {
        PolicyEnforcementContext context = context();
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2")));
        context.addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3")));
        expandAndCheck(context, "||${request.authenticatedDns|||}||", "||cn=test\\+1, ou=foobar, o=bling||cn=test\\+2||cn=test\\+3||");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDns|, }", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDn.0}", "cn=test\\+1, ou=foobar, o=bling");
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

    private PolicyEnforcementContext context(){
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<myresponse/>"));
        return new PolicyEnforcementContext(request, response);
    }

    private void expandAndCheck(PolicyEnforcementContext context, String expression, String expectedValue) throws IOException, PolicyAssertionException {
        String[] usedVars = Syntax.getReferencedNames(expression);
        Map<String,Object> vars = context.getVariableMap(usedVars, auditor);
        String expanded = ExpandVariables.process(expression, vars, auditor);
        assertEquals(expectedValue, expanded);
    }

    private PolicyEnforcementContext context(User user) {
        final PolicyEnforcementContext context = context();
        context.addAuthenticationResult(new AuthenticationResult(user));
        return context;
    }
}
