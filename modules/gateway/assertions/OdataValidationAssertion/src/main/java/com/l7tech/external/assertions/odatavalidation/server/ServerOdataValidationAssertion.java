package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Server side implementation of the OdataValidationAssertion.
 *
 * @see com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion
 */
public class ServerOdataValidationAssertion extends AbstractMessageTargetableServerAssertion<OdataValidationAssertion> {
    private final String[] variablesUsed;

    public ServerOdataValidationAssertion(final OdataValidationAssertion assertion) {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                          final Message msg,
                                          final String targetName,
                                          final AuthenticationContext authContext) throws IOException {
        Map<String, Object> varMap = context.getVariableMap(variablesUsed, getAudit());
        String metadata = ExpandVariables.process(assertion.getOdataMetadataSource(), varMap, getAudit());
        if(StringUtils.isBlank(metadata)) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_SMD, metadata);
            return AssertionStatus.FALSIFIED;
        }
        String resourceUrl = ExpandVariables.process(assertion.getResourceUrl(), varMap, getAudit());
        if(StringUtils.isBlank(resourceUrl)) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI);
            return AssertionStatus.FALSIFIED;
        }

        String path, query = null;

        int queryIndex = resourceUrl.indexOf('?');
        if (queryIndex != -1) {
            query = resourceUrl.substring(queryIndex+1);
            path = resourceUrl.substring(0, queryIndex);
        } else {
            path = resourceUrl;
        }
        try {
            InputStream metadataStream = new ByteArrayInputStream(metadata.getBytes());
            Edm medatadaEdm = EntityProvider.readMetadata(metadataStream, false);
            //Create parser
            OdataParser parser = new OdataParser(medatadaEdm);
            //get request info
            OdataRequestInfo odataRequestInfo = parser.parseRequest(path, query);
            setContextVariables(odataRequestInfo, context);

        } catch (EntityProviderException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_SMD, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (OdataParser.OdataParsingException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
    }

    private void setContextVariables(OdataRequestInfo odataRequestInfo, PolicyEnforcementContext context) {
        try {
            Set<String> filterParts = OdataParserUtil.getExpressionParts(odataRequestInfo.getFilterExpression());
            context.setVariable("odata.query.filter", filterParts.toArray(new String[]{}));
        } catch (OdataValidationException e) {
            e.printStackTrace();
        }

    }

    private ByteArrayInputStream getMetadataDocumentStream(PolicyEnforcementContext context) { // TODO: clean up, audit
        String variable;

        variable = assertion.getOdataMetadataSource();

        return new ByteArrayInputStream(variable.getBytes());
    }
}
