package com.l7tech.server.policy.variable;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.test.BugNumber;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.token.http.HttpBasicToken;
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
    private static final String REQUEST_BODY = "<myrequest/>";
    private static final String RESPONSE_BODY = "<myresponse/>";

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
    public void testRequest() throws Exception {
        doTestMessage("request", REQUEST_BODY);
    }

    @Test
    public void testResponse() throws Exception {
        doTestMessage("response", RESPONSE_BODY);
    }

    private void doTestMessage(String prefix, String messageBody) throws Exception {
        expandAndCheck(context(), "${" + prefix + ".size}", String.valueOf(messageBody.getBytes().length));
        expandAndCheck(context(), "${" + prefix + ".mainpart}", messageBody);
        expandAndCheck(context(), "${" + prefix + ".wss.certificates.count}", "0");
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
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
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
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
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
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test1, ou=foobar, o=bling", "test1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test2", "test2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test3", "test3"), new OpaqueSecurityToken()));
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
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "||${request.authenticatedDns|||}||", "||cn=test\\+1, ou=foobar, o=bling||cn=test\\+2||cn=test\\+3||");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDns|, }", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDn.0}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    public void testResponseNoUser() throws Exception {
        expandAndCheck(context(), "${response.authenticatedUser}", "");
        expandAndCheck(context(), "${response.authenticatedUsers}", "");
        expandAndCheck(context(), "${response.authenticatedDn}", "");
        expandAndCheck(context(), "${response.authenticatedDns}", "");
    }

    @Test
    public void testResponseNoDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new InternalUser("blah"));
        expandAndCheck(context, "${RESPONSE.authenticatedUser}", "blah");
        expandAndCheck(context, "${RESPONSE.authenticatedUsers}", "blah");
        expandAndCheck(context, "${RESPONSE.authenticatedDn}", "");
        expandAndCheck(context, "${RESPONSE.authenticatedDns}", "");
    }

    @Test
    public void testResponseNullDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(-3823, null, null));
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
    }

    @Test
    public void testResponseNonNumericSuffix() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(-3823, "cn=blah", "blah"));
        expandAndCheck(context, "${response.authenticatedUser.bogus}", "");
        expandAndCheck(context, "${response.authenticatedUsers.bogus}", "");
        expandAndCheck(context, "${response.authenticatedDn.bogus}", "");
        expandAndCheck(context, "${response.authenticatedDns.bogus}", "");
    }

    @Test
    public void testResponseOutOfBoundsSuffix() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${response.authenticatedUser}", "test+3");
        expandAndCheck(context, "${response.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${response.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${response.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${response.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${response.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${response.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testResponseEmptyDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(-3823, "", null));
        expandAndCheck(context, "${response.authenticatedUser}", "");
        expandAndCheck(context, "${response.authenticatedUsers}", "");
        expandAndCheck(context, "${response.authenticatedDn}", "");
        expandAndCheck(context, "${response.authenticatedDns}", "");
    }

    @Test
    public void testResponseEscapedDnUserName() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${response.authenticatedUser}", "test+1");
        expandAndCheck(context, "${response.authenticatedUsers}", "test+1");
    }

    @Test
    public void testResponseMultipleUserNames() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${response.authenticatedUser}", "test+3");
        expandAndCheck(context, "${response.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${response.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${response.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${response.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${response.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${response.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testResponseMultipleUserDns() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test1, ou=foobar, o=bling", "test1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test2", "test2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test3", "test3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${response.authenticatedDn}", "cn=test3");
        expandAndCheck(context, "${response.authenticatedDn.0}", "cn=test1, ou=foobar, o=bling");
        expandAndCheck(context, "${response.authenticatedDn.1}", "cn=test2");
        expandAndCheck(context, "${response.authenticatedDn.2}", "cn=test3");
        expandAndCheck(context, "||${response.authenticatedDns|||}||", "||cn=test1, ou=foobar, o=bling||cn=test2||cn=test3||");
        expandAndCheck(context, "${response.authenticatedDns}", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
        expandAndCheck(context, "${response.authenticatedDns|, }", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
    }

    @Test
    public void testResponseEscapedDnUserDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${response.authenticatedDn}", "cn=test\\+1, ou=foobar, o=bling");
        expandAndCheck(context, "${response.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    public void testResponseMultipleUserDnsEscaped() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3823, "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3824, "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(-3825, "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "||${response.authenticatedDns|||}||", "||cn=test\\+1, ou=foobar, o=bling||cn=test\\+2||cn=test\\+3||");
        expandAndCheck(context, "${response.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${response.authenticatedDns|, }", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${response.authenticatedDn}", "cn=test\\+3");
        expandAndCheck(context, "${response.authenticatedDn.0}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    public void testUsernamePassword() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext( context.getRequest() ).addCredentials( LoginCredentials.makeLoginCredentials( new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class ));
        context.getAuthenticationContext( context.getResponse() ).addCredentials( LoginCredentials.makeLoginCredentials( new UsernameTokenImpl("Bob", "secret".toCharArray()), WssBasic.class ));
        expandAndCheck(context, "${request.username}", "Alice");
        expandAndCheck(context, "${request.password}", "password");
        expandAndCheck(context, "${response.username}", "Bob");
        expandAndCheck(context, "${response.password}", "secret");
    }

    /*
    * Test the service.url context variable and associated suffixes
    * : host, protocol, path, file, query
    * */
    @Test
    public void testServiceUrlContextVariables() throws Exception{
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(REQUEST_BODY));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(RESPONSE_BODY));
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
        request.initialize(XmlUtil.stringAsDocument(REQUEST_BODY));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(RESPONSE_BODY));
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
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(user, new OpaqueSecurityToken()));
        return context;
    }

    private PolicyEnforcementContext responseContext(User user) {
        final PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(user, new OpaqueSecurityToken()));
        return context;
    }
}
