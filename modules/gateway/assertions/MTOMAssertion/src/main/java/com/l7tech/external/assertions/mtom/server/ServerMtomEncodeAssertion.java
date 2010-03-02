package com.l7tech.external.assertions.mtom.server;

import com.l7tech.server.audit.Auditor;
import com.l7tech.external.assertions.mtom.MtomEncodeAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.message.Message;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.ExceptionUtils;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultNodeSet;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.xpath.XpathVariableFinder;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.LinkedList;
import java.io.IOException;

/**
 * Server side implementation of the MtomEncodeAssertion.
 *
 * @see com.l7tech.external.assertions.mtom.MtomEncodeAssertion
 */
public class ServerMtomEncodeAssertion extends AbstractMessageTargetableServerAssertion<MtomEncodeAssertion> {

    //- PUBLIC

    public ServerMtomEncodeAssertion( final MtomEncodeAssertion assertion,
                                      final ApplicationContext context ) throws PolicyAssertionException {
        super( assertion, assertion );

        this.auditor = new Auditor(this, context, logger);
        this.stashManagerFactory = (StashManagerFactory) context.getBean( "stashManagerFactory", StashManagerFactory.class );
        this.compiledXpaths = compileXpaths( assertion.getXpathExpressions() );
    }

    //- PROTECTED

    protected ServerMtomEncodeAssertion( final MtomEncodeAssertion assertion,
                                         final Auditor auditor,
                                         final StashManagerFactory stashManagerFactory ){
        super( assertion, assertion );

        this.auditor = auditor;
        this.stashManagerFactory = stashManagerFactory;
        this.compiledXpaths = compileXpaths( assertion.getXpathExpressions() );
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.UNDEFINED;

        if ( message.getMimeKnob().getOuterContentType().isXml() ) {
            Collection<Element> elements = new LinkedList<Element>();
            if ( compiledXpaths != null ) {
                try {
                    final ElementCursor cursor = new DomElementCursor( message.getXmlKnob().getDocumentReadOnly() );
                    final XpathVariableFinder xpathVariableFinder = new PolicyEnforcementContextXpathVariableFinder(context);
                    
                    for ( CompiledXpath compiledXpath : compiledXpaths ) {
                        XpathResult result = cursor.getXpathResult( compiledXpath, xpathVariableFinder, true );
                        if ( result.matches() ) {
                            XpathResultNodeSet nodeSet = result.getNodeSet();
                            if ( nodeSet != null ) {
                                XpathResultIterator iterator = nodeSet.getIterator();
                                while ( iterator.hasNext() ) {
                                    ElementCursor elementCursor = iterator.nextElementAsCursor();
                                    if ( elementCursor != null ) {
                                        elements.add( elementCursor.asDomElement() );
                                    }
                                }
                            }
                        } else if ( assertion.isFailIfNotFound() ) {
                            status = AssertionStatus.FALSIFIED;
                            auditor.logAndAudit( MTOM_ENCODE_XPATH_FAILED );
                            break;
                        }
                    }
                } catch (SAXException e) {
                    status = getBadMessageStatus();
                    auditor.logAndAudit(
                            MTOM_ENCODE_ERROR,
                            new String[]{"Error parsing message when matching elements to encode '"+ ExceptionUtils.getMessage(e)+"'"},
                            ExceptionUtils.getDebugException(e) );
                } catch (XPathExpressionException e) {
                    status = AssertionStatus.FAILED;
                    auditor.logAndAudit(
                            MTOM_ENCODE_ERROR,
                            new String[]{"Error matching elements to encode '"+ ExceptionUtils.getMessage(e)+"'"},
                            ExceptionUtils.getDebugException(e) );
                }
            } else {
                status = AssertionStatus.FAILED;
                auditor.logAndAudit( MTOM_ENCODE_ERROR, "Invalid XPath expression" );
            }

            if ( status == AssertionStatus.UNDEFINED ) {
                if ( !assertion.isAlwaysEncode() && elements.isEmpty() ) {
                    // even if not encoding, we may  still need to copy the source message to ouput message
                    auditor.logAndAudit( MTOM_ENCODE_NONE );
                }

                Message outputMessage = null;
                if ( assertion.getOutputTarget() == null ) {
                    outputMessage = message;
                } else {
                    try {
                        outputMessage = context.getOrCreateTargetMessage( assertion.getOutputTarget(), false );
                    } catch (NoSuchVariableException nsve) {
                        status = AssertionStatus.FAILED;
                        auditor.logAndAudit( MTOM_ENCODE_ERROR, ExceptionUtils.getMessage(nsve));
                    } catch (VariableNotSettableException vnse) {
                        auditor.logAndAudit( MTOM_ENCODE_ERROR, ExceptionUtils.getMessage(vnse));
                        status = AssertionStatus.FAILED;
                    }
                }

                if ( outputMessage != null ) {
                    try {
                        XOPUtils.extract(
                                message,
                                outputMessage,
                                elements,
                                assertion.getOptimizationThreshold(),
                                assertion.isAlwaysEncode(),
                                stashManagerFactory );

                        status = AssertionStatus.NONE;
                    } catch ( Exception e) {
                        auditor.logAndAudit(
                                MTOM_ENCODE_ERROR,
                                new String[]{"Error encoding XOP '"+ ExceptionUtils.getMessage(e)+"'"},
                                e instanceof XOPUtils.XOPException ? e.getCause() : e );

                        status = AssertionStatus.FALSIFIED;
                    }
                }
            }
        } else {
            auditor.logAndAudit( MTOM_ENCODE_ERROR, "Message not xml" );
            status = AssertionStatus.NOT_APPLICABLE;
        }

        return status;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerMtomEncodeAssertion.class.getName());
    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;
    private final CompiledXpath[] compiledXpaths;

    private CompiledXpath[] compileXpaths( final XpathExpression[] xpathExpressions ) {
        CompiledXpath[] xpaths;

        if ( xpathExpressions != null ) {
            xpaths = new CompiledXpath[ xpathExpressions.length ];
            XpathExpression xpathExpression  = null;
            try {
                for ( int i=0; i<xpathExpressions.length; i++ ) {
                    xpathExpression = xpathExpressions[i];
                    xpaths[i] = xpathExpression.compile();
                }
            } catch ( InvalidXpathException e ) {
                xpaths = null;
                auditor.logAndAudit( MTOM_ENCODE_INVALID_XPATH, xpathExpression.getExpression() );
            }
        } else {
            xpaths = new CompiledXpath[0];
        }

        return xpaths;
    }

}
