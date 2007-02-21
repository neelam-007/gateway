package com.l7tech.external.assertions.echorouting.server;

import com.l7tech.common.message.*;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.external.assertions.echorouting.EchoRoutingAssertion;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Assertion that echoes the request into the response.
 *
 * @author emil
 * @version 21-Mar-2005
 */
public class ServerEchoRoutingAssertion extends ServerRoutingAssertion<EchoRoutingAssertion> {

    //- PUBLIC

    public ServerEchoRoutingAssertion(EchoRoutingAssertion ea, ApplicationContext applicationContext) {
        super(ea, applicationContext, logger);
        auditor = new Auditor(this, applicationContext, logger);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();

        XmlKnob xnob = (XmlKnob)request.getKnob(XmlKnob.class);
        if (xnob == null) {
            String contenttype = "unknown";
            HttpRequestKnob htknob = (HttpRequestKnob) request.getKnob(HttpRequestKnob.class);
            if (htknob != null) {
                String[] ctypes = htknob.getHeaderValues("content-type");
                if (ctypes != null && ctypes.length > 0) contenttype = ctypes[0];
            }
            auditor.logAndAudit(AssertionMessages.CANNOT_ECHO_NON_XML, new String[] {contenttype});
            return AssertionStatus.FAILED;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(context,
                                          data.getCurrentSecurityHeaderHandling(),
                                          data.getXmlSecurityActorToPromote());
        }
        catch(SAXException se) {
            logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
        }

        // Initialize request
        StashManager stashManager = stashManagerFactory.createStashManager();
        MimeKnob mk = request.getMimeKnob();
        try {
            response.initialize(
                    stashManager,
                    mk.getOuterContentType(),
                    mk.getEntireMessageBodyAsInputStream());
        }
        catch(NoSuchPartException nspe) {
            throw new CausedIOException("Unable copy request to response.", nspe);
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerEchoRoutingAssertion.class.getName());
    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;
}
