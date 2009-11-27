package com.l7tech.external.assertions.mtom.server;

import com.l7tech.external.assertions.mtom.MtomValidateAssertion;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultNodeSet;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.ElementCursor;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class ServerMtomValidateAssertion extends AbstractMessageTargetableServerAssertion<MtomValidateAssertion> {

    //- PUBLIC

    public ServerMtomValidateAssertion( final MtomValidateAssertion assertion,
                                        final ApplicationContext context) throws PolicyAssertionException {
        super( assertion, assertion );

        this.auditor = new Auditor(this, context, logger);
        this.compiledXpaths = compileXpaths( getXpathExpressions(assertion) );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        AssertionStatus status;

        ContentTypeHeader header = message.getMimeKnob().getOuterContentType();

        if ( XOPUtils.isXop( header ) ) {
            try {
                XOPUtils.validate( message );
                status = AssertionStatus.NONE;
            } catch (XOPUtils.XOPException e) {
                status = AssertionStatus.FALSIFIED;
                auditor.logAndAudit( MTOM_VALIDATE_ERROR, ExceptionUtils.getMessage(e) );
            } catch (SAXException e) {
                status = getBadMessageStatus();
                auditor.logAndAudit(
                        MTOM_VALIDATE_ERROR,
                        new String[]{"Error parsing message for validation '"+ ExceptionUtils.getMessage(e)+"'"},
                        ExceptionUtils.getDebugException(e) );
            }

            if ( status == AssertionStatus.NONE ) {
                if ( compiledXpaths == null ) {
                    status = AssertionStatus.FAILED;
                    auditor.logAndAudit( MTOM_VALIDATE_ERROR, "Invalid XPath expression" );
                } else {
                    try {
                        final ElementCursor cursor = message.getXmlKnob().getElementCursor();
                        status = processRules( message, cursor );
                    } catch (SAXException e) {
                        status = getBadMessageStatus();
                        auditor.logAndAudit(
                                MTOM_VALIDATE_ERROR,
                                new String[]{"Error parsing message when matching elements to validate '"+ ExceptionUtils.getMessage(e)+"'"},
                                ExceptionUtils.getDebugException(e) );
                    }
                }
            }
        } else if ( assertion.isRequireEncoded() ) {
            status = getBadMessageStatus();
            auditor.logAndAudit( MTOM_VALIDATE_ERROR, "Message not encoded" );
        } else {
            status = AssertionStatus.NONE;
        }

        return status;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerMtomValidateAssertion.class.getName());
    private final Auditor auditor;
    private final CompiledXpath[] compiledXpaths;

    private XpathExpression[] getXpathExpressions( final MtomValidateAssertion assertion ) {
        XpathExpression[] xpaths;

        final MtomValidateAssertion.ValidationRule[] rules = assertion.getValidationRules();
        if ( rules == null ) {
            xpaths = new XpathExpression[0];
        } else {
            xpaths = new XpathExpression[rules.length];
            for ( int i=0; i<rules.length; i++ ) {
                xpaths[i] = rules[i].getXpathExpression();                
            }
        }

        return xpaths;
    }

    private CompiledXpath[] compileXpaths( final XpathExpression[] xpathExpressions ) {
        CompiledXpath[] xpaths = null;

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
        }

        return xpaths;
    }

    private AssertionStatus processRules( final Message message,
                                          final ElementCursor cursor ) throws IOException{
        AssertionStatus status = AssertionStatus.NONE;

        final MtomValidateAssertion.ValidationRule[] rules = assertion.getValidationRules();
        outer:
        for ( int i=0 ; i<rules.length; i++ ) {
            final MtomValidateAssertion.ValidationRule rule = rules[i];
            final CompiledXpath compiledXpath = compiledXpaths[i];

            try {
                cursor.moveToRoot();
                final XpathResult result = cursor.getXpathResult( compiledXpath );
                if ( result.matches() ) {
                    final XpathResultNodeSet nodeSet = result.getNodeSet();
                    if ( nodeSet != null ) {
                        final XpathResultIterator iterator = nodeSet.getIterator();
                        int elementCount = 0;

                        while ( iterator.hasNext() ) {
                            ElementCursor elementCursor = iterator.nextElementAsCursor();
                            if ( elementCursor != null ) {
                                elementCount++;
                                long size = XOPUtils.getSize( message, elementCursor );
                                if ( size > rule.getSize() ) {
                                    status = getBadMessageStatus();
                                    auditor.logAndAudit( MTOM_VALIDATE_ERROR, "Size exceeded" );
                                    break outer;
                                }
                            }
                        }

                        if ( elementCount > rule.getCount() ) {
                            status = getBadMessageStatus();
                            auditor.logAndAudit( MTOM_VALIDATE_ERROR, "Count exceeded" );
                            break;
                        }
                    }
                }
            } catch (XPathExpressionException e) {
                status = AssertionStatus.FAILED;
                auditor.logAndAudit(
                        MTOM_VALIDATE_ERROR,
                        new String[]{"Error matching elements to validate '"+ ExceptionUtils.getMessage(e)+"'"},
                        ExceptionUtils.getDebugException(e) );
                break;
            } catch (XOPUtils.XOPException e) {
                status = AssertionStatus.FALSIFIED;
                auditor.logAndAudit( MTOM_VALIDATE_ERROR, "Error checking rules: " + ExceptionUtils.getMessage(e) );
            } 
        }

        return status;
    }
}
