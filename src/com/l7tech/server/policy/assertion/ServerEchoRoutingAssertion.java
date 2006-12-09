package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.*;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EchoRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.StashManagerFactory;

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
public class ServerEchoRoutingAssertion extends ServerRoutingAssertion {

    //- PUBLIC

    public ServerEchoRoutingAssertion(EchoRoutingAssertion ea, ApplicationContext applicationContext) {
        super(ea, applicationContext);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();

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
    private final StashManagerFactory stashManagerFactory;
}
