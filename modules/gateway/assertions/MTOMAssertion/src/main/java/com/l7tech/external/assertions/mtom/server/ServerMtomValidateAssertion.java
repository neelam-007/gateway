package com.l7tech.external.assertions.mtom.server;

import com.l7tech.external.assertions.mtom.MtomValidateAssertion;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultNodeSet;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathVariableFinder;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

/**
 *
 */
public class ServerMtomValidateAssertion extends AbstractMessageTargetableServerAssertion<MtomValidateAssertion> {

    //- PUBLIC

    public ServerMtomValidateAssertion( final MtomValidateAssertion assertion ) throws PolicyAssertionException {
        super( assertion, assertion );
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
                logAndAudit( MTOM_VALIDATE_ERROR, ExceptionUtils.getMessage(e) );
            } catch (SAXException e) {
                status = getBadMessageStatus();
                logAndAudit(
                        MTOM_VALIDATE_ERROR,
                        new String[]{"Error parsing message for validation '"+ ExceptionUtils.getMessage(e)+"'"},
                        ExceptionUtils.getDebugException(e) );
            }

            if ( status == AssertionStatus.NONE ) {
                if ( compiledXpaths == null ) {
                    status = AssertionStatus.FAILED;
                    logAndAudit( MTOM_VALIDATE_ERROR, "Invalid XPath expression" );
                } else {
                    try {
                        final ElementCursor cursor = message.getXmlKnob().getElementCursor();
                        status = processRules( context, message, cursor );
                    } catch (SAXException e) {
                        status = getBadMessageStatus();
                        logAndAudit(
                                MTOM_VALIDATE_ERROR,
                                new String[]{"Error parsing message when matching elements to validate '"+ ExceptionUtils.getMessage(e)+"'"},
                                ExceptionUtils.getDebugException(e) );
                    }
                }
            }
        } else if ( assertion.isRequireEncoded() ) {
            status = getBadMessageStatus();
            logAndAudit( MTOM_VALIDATE_ERROR, "Message not encoded" );
        } else {
            status = AssertionStatus.NONE;
        }

        return status;
    }


    //- PRIVATE

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
                logAndAudit( MTOM_ENCODE_INVALID_XPATH, xpathExpression.getExpression() );
            }
        }

        return xpaths;
    }

    private AssertionStatus processRules( final PolicyEnforcementContext context,
                                          final Message message,
                                          final ElementCursor cursor ) throws IOException{
        AssertionStatus status = AssertionStatus.NONE;

        final MtomValidateAssertion.ValidationRule[] rules = assertion.getValidationRules();
        if ( rules != null ) {
            final XpathVariableFinder xpathVariableFinder = new PolicyEnforcementContextXpathVariableFinder(context);

            outer:
            for ( int i=0 ; i<rules.length; i++ ) {
                final MtomValidateAssertion.ValidationRule rule = rules[i];
                final CompiledXpath compiledXpath = compiledXpaths[i];

                try {
                    cursor.moveToRoot();
                    final XpathResult result = cursor.getXpathResult( compiledXpath, xpathVariableFinder, true );
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
                                        logAndAudit( MTOM_VALIDATE_ERROR, "Size exceeded" );
                                        break outer;
                                    }
                                }
                            }

                            if ( rule.getCount() != 0 && elementCount > rule.getCount() ) {
                                status = getBadMessageStatus();
                                logAndAudit( MTOM_VALIDATE_ERROR, "Count exceeded" );
                                break;
                            }
                        }
                    }
                } catch (XPathExpressionException e) {
                    status = AssertionStatus.FAILED;
                    logAndAudit(
                            MTOM_VALIDATE_ERROR,
                            new String[]{"Error matching elements to validate '"+ ExceptionUtils.getMessage(e)+"'"},
                            ExceptionUtils.getDebugException(e) );
                    break;
                } catch (XOPUtils.XOPException e) {
                    status = AssertionStatus.FALSIFIED;
                    logAndAudit( MTOM_VALIDATE_ERROR, "Error checking rules: " + ExceptionUtils.getMessage(e) );
                }
            }
        }

        return status;
    }
}
