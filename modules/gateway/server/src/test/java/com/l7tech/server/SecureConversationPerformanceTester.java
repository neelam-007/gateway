package com.l7tech.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.xml.WssDecoratorTest;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.util.MockConfig;
import com.l7tech.util.SoapConstants;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Properties;

/**
 *
 */
public class SecureConversationPerformanceTester {
    @Test
    public void testSignatureValidationPerformance() throws Exception {
        final SecureConversationContextManager manager = new SecureConversationContextManager( new MockConfig( new Properties() ) );
        WssDecoratorTest.TestDocument td = new WssDecoratorTest().getSigningOnlyWithSecureConversationTestDocument();
        final SecureConversationSession session = manager.createContextForUser(new UserBean(3, "foo"),
                LoginCredentials.makeLoginCredentials(new HttpBasicToken("foo", "blah".toCharArray()),
                        HttpBasic.class), SoapConstants.WSSC_NAMESPACE2);
        td.req.setSecureConversationSession(new DecorationRequirements.SimpleSecureConversationSession(session.getIdentifier(),
                session.getSharedSecret(),
                session.getSecConvNamespaceUsed()));
        td.req.getElementsToSign().clear();
        td.req.getElementsToEncrypt().clear();

        Message message = new Message(td.c.message,0);
        new WssDecoratorImpl().decorateMessage(message, td.req);
        final String reqXml = XmlUtil.nodeToString(message.getXmlKnob().getDocumentReadOnly());

        System.out.println("Request (reformamted): " + XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(reqXml)));

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Message req = new Message(XmlUtil.stringAsDocument(reqXml),0);
                try {
                    final WssProcessorImpl wssProcessor = new WssProcessorImpl(req);
                    wssProcessor.setSecurityContextFinder(new SecurityContextFinder() {
                        @Override
                        public SecurityContext getSecurityContext(String securityContextIdentifier) {
                            return manager.getSecurityContext(securityContextIdentifier);
                        }
                    });
                    ProcessorResult got = wssProcessor.processMessage();
                    assertTrue(got.getElementsThatWereSigned().length > 0);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        new BenchmarkRunner(r, 1000000, 10, "WS-SecureConversation: Process signed request").run();
    }
}
