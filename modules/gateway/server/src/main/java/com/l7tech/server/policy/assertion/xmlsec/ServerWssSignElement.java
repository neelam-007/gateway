package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.xml.xpath.DeferredFailureDomCompiledXpathHolder;
import com.l7tech.server.message.AuthenticationContext;
import org.jaxen.JaxenException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap message sent from the ssg server.
 * <p/>
 * This assertion schedules decoration of a message with an xml d-sig.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 */
public class ServerWssSignElement extends ServerAddWssSignature<WssSignElement> {
    private static final Logger logger = Logger.getLogger(ServerWssSignElement.class.getName());
    private final DeferredFailureDomCompiledXpathHolder compiledXpath;

    public ServerWssSignElement( final WssSignElement assertion,
                                 final ApplicationContext ctx ) throws IOException {
        super(assertion, assertion, assertion, ctx, logger);
        this.compiledXpath = new DeferredFailureDomCompiledXpathHolder(assertion.getXpathExpression());
    }

    /**
     * @return the number of elements that will be affected by the decorator, <code>0</code> if
     *         none, or <code>-1</code> if the assertion should fail.
     */
    @Override
    protected int addDecorationRequirements( final PolicyEnforcementContext context,
                                             final AuthenticationContext authContext,
                                             final Document soapmsg,
                                             final DecorationRequirements wssReq)
            throws PolicyAssertionException
    {
        List selectedElements;
        try {
            selectedElements = compiledXpath.getCompiledXpath().rawSelectElements(soapmsg,
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
