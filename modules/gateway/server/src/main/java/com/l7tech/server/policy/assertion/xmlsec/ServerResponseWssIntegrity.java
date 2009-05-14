package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import org.jaxen.JaxenException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this schedules decoration of a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 */
public class ServerResponseWssIntegrity extends ServerResponseWssSignature {
    private static final Logger logger = Logger.getLogger(ServerResponseWssIntegrity.class.getName());
    private final ResponseWssIntegrity assertion;

    public ServerResponseWssIntegrity(ResponseWssIntegrity assertion, ApplicationContext ctx) throws IOException {
        super(assertion, ctx, logger);
        this.assertion = assertion;
    }

    /**
     * @return the number of elements that will be affected by the decorator, <code>0</code> if
     *         none, or <code>-1</code> if the assertion should fail.
     */
    protected int addDecorationRequirements(PolicyEnforcementContext context, Document soapmsg, DecorationRequirements wssReq)
            throws PolicyAssertionException
    {
        List selectedElements;
        final XpathExpression xpath = assertion.getXpathExpression();
        try {
            selectedElements = XpathUtil.compileAndSelectElements(soapmsg, xpath.getExpression(), xpath.getNamespaces(),
                    new PolicyEnforcementContextXpathVariableFinder(context));
        } catch (JaxenException e) {
            // this is thrown when there is an error in the expression
            // this is therefore a bad policy
            throw new PolicyAssertionException(assertion, e);
        }

        if (selectedElements == null || selectedElements.size() < 1) {
            return 0;
        }

        //noinspection unchecked
        wssReq.getElementsToSign().addAll(selectedElements);
        wssReq.setSignTimestamp();

        return selectedElements.size();
    }
}
