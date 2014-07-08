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
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
        //get context variable prefix
        String variablePrefix = ExpandVariables.process(assertion.getVariablePrefix(), varMap, getAudit());
        variablePrefix = variablePrefix != null ? variablePrefix : OdataValidationAssertion.DEFAULT_PREFIX;
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
            setContextVariables(odataRequestInfo, variablePrefix, context);

        } catch (EntityProviderException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_SMD, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (OdataParser.OdataParsingException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
    }

    private void setContextVariables(OdataRequestInfo odataRequestInfo, String prefix, PolicyEnforcementContext context) {
        try {
            context.setVariable(prefix + OdataValidationAssertion.QUERY_COUNT, Boolean.toString(odataRequestInfo.isCount()));

            Set<String> filterParts = OdataParserUtil.getExpressionParts(odataRequestInfo.getFilterExpression());
            if(filterParts.size() > 0) {
                context.setVariable(prefix + OdataValidationAssertion.QUERY_FILTER, filterParts.toArray(new String[]{}));
            }

            final Integer top = odataRequestInfo.getTop();
            if(top != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_TOP, top.toString());

            final Integer skip = odataRequestInfo.getSkip();
            if(skip != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_SKIP, skip.toString());

            Set<String> orderByParts = OdataParserUtil.getExpressionParts(odataRequestInfo.getFilterExpression());
            if(orderByParts.size() > 0) {
                context.setVariable(prefix + OdataValidationAssertion.QUERY_ORDERBY, orderByParts.toArray(new String[]{}));
            }
            //TODO: set the rest of the context variables


        } catch (OdataValidationException e) {
           logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
        }

    }
}
