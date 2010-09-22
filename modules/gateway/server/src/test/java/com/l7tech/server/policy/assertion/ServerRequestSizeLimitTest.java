package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.service.ServicesHelper;
import com.l7tech.util.IOUtils;
import junit.framework.TestCase;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import javax.wsdl.Input;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author jbufu
 */
public class ServerRequestSizeLimitTest extends TestCase {

    @Test
    public void testCompatibilityBug5044Format() throws Exception {
        final String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:RequestSizeLimit>\n" +
            "                <L7p:Limit longValue=\"4896768\"/>\n" +
            "            </L7p:RequestSizeLimit>\n" +
            "    </wsp:Policy>\n";

        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(RequestSizeLimit.class);

        final RequestSizeLimit assertion = (RequestSizeLimit) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected request size limit 4782 kbytes, got '" + assertion.getLimit(), assertion.getLimit().equals("4782"));
    }


    @Test
    public void testIsMessageTargetable() throws Exception {

        final String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:RequestSizeLimit>\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
            "            </L7p:RequestSizeLimit>\n" +
            "    </wsp:Policy>\n";

        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(RequestSizeLimit.class);

        final RequestSizeLimit assertion = (RequestSizeLimit) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected response message target, got '" + assertion.getTarget(), assertion.getTarget().equals(TargetMessageType.RESPONSE));

    }

    @Test
    public void testRequestSizeLimit() throws Exception {

        RequestSizeLimit ass = new RequestSizeLimit();
        ass.setLimit("1");
        ass.setTarget(TargetMessageType.REQUEST);
        
        ServerRequestSizeLimit serverAss = new ServerRequestSizeLimit(ass, null);
        AssertionStatus result;

        // small message
        Message smallRequest = new Message(XmlUtil.stringAsDocument("<foo/>"));
        result = serverAss.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(smallRequest,null));
        assertEquals(AssertionStatus.NONE, result);

        //large message > 1KB
        Message bigRequest = new Message(XmlUtil.parse(getClass().getResourceAsStream("largeMessage.xml")));
        result = serverAss.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(bigRequest,null));
        assertEquals(AssertionStatus.FALSIFIED, result);

    }

    @Test
    public void testVariableSizeLimit() throws Exception {

        RequestSizeLimit ass = new RequestSizeLimit();
        ass.setLimit("1");
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("var");

        ServerRequestSizeLimit serverAss = new ServerRequestSizeLimit(ass, null);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null,null);

        AssertionStatus result;

        // small message
        Message smallRequest = new Message(XmlUtil.stringAsDocument("<foo/>"));
        context.setVariable("var",smallRequest);
        result = serverAss.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        //large message > 1KB
        Message bigRequest = new Message(XmlUtil.parse(getClass().getResourceAsStream("largeMessage.xml")));
        context.setVariable("var",bigRequest);
        result = serverAss.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);

    }

}