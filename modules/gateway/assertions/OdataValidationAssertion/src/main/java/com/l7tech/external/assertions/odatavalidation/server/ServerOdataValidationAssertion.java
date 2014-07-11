package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
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

import javax.servlet.http.HttpServletRequest;
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

    public static final String METADATA_SUFFIX = "$metadata";
    public static final String VALUE_SUFFIX = "$value";

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

        OdataValidationAssertion.OdataOperations requestMethod =
                OdataValidationAssertion.OdataOperations.valueOf(context.getRequest().getHttpRequestKnob().getMethodAsString());

        String inboundURL = context.getRequest().getHttpRequestKnob().getRequestUrl();

        // Check HTTP request method is authorized
        switch (requestMethod) {
            case GET:
                if (!assertion.isReadOperation()) {
                    return AssertionStatus.UNAUTHORIZED;
                }
                break;

            case POST:
                if (!assertion.isCreateOperation()) {
                    return AssertionStatus.UNAUTHORIZED;
                }
                break;

            case PUT:
                if (!assertion.isUpdateOperation()) {
                    return AssertionStatus.UNAUTHORIZED;
                }
                break;

            case MERGE:
                if (!assertion.isMergeOperation()) {
                    return AssertionStatus.UNAUTHORIZED;
                }
                break;

            case PATCH:
                if (!assertion.isPartialUpdateOperation()) {
                    return AssertionStatus.UNAUTHORIZED;
                }
                break;

            case DELETE:
                if (!assertion.isDeleteOperation()) {
                    return AssertionStatus.UNAUTHORIZED;
                }
                break;

            default:
                return AssertionStatus.UNAUTHORIZED;
        }

        // Check Permitted Actions
        boolean isMetadataRequest = inboundURL.endsWith(METADATA_SUFFIX);
        boolean isValueRequest = inboundURL.endsWith(VALUE_SUFFIX);
        if ( isMetadataRequest && ! assertion.getActions().contains(OdataValidationAssertion.ProtectionActions.ALLOW_METADATA) ) {
            return AssertionStatus.UNAUTHORIZED;
        }
        if ( isValueRequest && ! assertion.getActions().contains(OdataValidationAssertion.ProtectionActions.ALLOW_RAW_VALUE) ) {
            return AssertionStatus.UNAUTHORIZED;
        }

        //get context variable prefix
        String variablePrefix = ExpandVariables.process(assertion.getVariablePrefix(), varMap, getAudit());
        variablePrefix = variablePrefix != null ? variablePrefix : OdataValidationAssertion.DEFAULT_PREFIX; // TODO ExpandVariables.process() never returns null, change this check
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
            Edm metadataEdm = EntityProvider.readMetadata(metadataStream, false);
            //Create parser
            OdataParser parser = new OdataParser(metadataEdm);
            //get request info
            OdataRequestInfo odataRequestInfo = parser.parseRequest(path, query);

            if ( assertion.isValidatePayload() ) {
                HttpServletRequest httpServletRequest = context.getRequest().getKnob(HttpServletRequestKnob.class).getHttpServletRequest();
                OdataPayloadInfo odataPayloadInfo = parser.parsePayload(requestMethod.toString(),
                        odataRequestInfo, httpServletRequest.getInputStream(),
                        httpServletRequest.getContentType());
            }

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
                context.setVariable(prefix + OdataValidationAssertion.QUERY_FILTER, filterParts.toArray(new String[filterParts.size()]));
            }

            final Integer top = odataRequestInfo.getTop();
            if(top != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_TOP, top.toString());

            final Integer skip = odataRequestInfo.getSkip();
            if(skip != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_SKIP, skip.toString());

            Set<String> orderByParts = OdataParserUtil.getExpressionParts(odataRequestInfo.getFilterExpression());
            if(orderByParts.size() > 0) {
                context.setVariable(prefix + OdataValidationAssertion.QUERY_ORDERBY, orderByParts.toArray(new String[orderByParts.size()]));
            }
            //set $expand expression
            final String expand = odataRequestInfo.getExpandExpressionString();
            if(expand != null)context.setVariable(prefix + OdataValidationAssertion.QUERY_EXPAND, expand);
            //set $format expression
            String format = odataRequestInfo.getFormat();
            if(format != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_FORMAT, format);

            final String inlinecount = odataRequestInfo.getInlineCount();
            if(inlinecount != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_INLINECOUNT, inlinecount);

            final String select = odataRequestInfo.getSelectExpressionString();
            if(select != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_SELECT, select);

            Map<String,String> customQueryOptions = odataRequestInfo.getCustomQueryOptions();
            if(!customQueryOptions.isEmpty()) context.setVariable(prefix + OdataValidationAssertion.QUERY_CUSTOMOPTIONS, customQueryOptions);



            //TODO: set the rest of the context variables


        } catch (OdataValidationException e) {
           logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
        }

    }
}
