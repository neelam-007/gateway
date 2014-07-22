package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
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
import org.apache.olingo.odata2.api.uri.PathSegment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

        if(!msg.isInitialized()) {
            // Uninitialized target message
            logAndAudit(AssertionMessages.MESSAGE_NOT_INITIALIZED, targetName);
            return getBadMessageStatus();
        }
        //check if OData request is coming from HTTP
        HttpServletRequestKnob httpRequestKnob = msg.getKnob(HttpServletRequestKnob.class);
        if(httpRequestKnob == null) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI, targetName + " is not HTTP");
            return AssertionStatus.FALSIFIED;
        }

        OdataValidationAssertion.OdataOperations requestMethod =
                OdataValidationAssertion.OdataOperations.valueOf(httpRequestKnob.getMethodAsString());

        // Check HTTP request method is authorized
        switch (requestMethod) {
            case GET:
                if (!assertion.isReadOperation()) {
                    return AssertionStatus.FALSIFIED;
                }
                break;

            case POST:
                if (!assertion.isCreateOperation()) {
                    return AssertionStatus.FALSIFIED;
                }
                break;

            case PUT:
                if (!assertion.isUpdateOperation()) {
                    return AssertionStatus.FALSIFIED;
                }
                break;

            case MERGE:
                if (!assertion.isMergeOperation()) {
                    return AssertionStatus.FALSIFIED;
                }
                break;

            case PATCH:
                if (!assertion.isPartialUpdateOperation()) {
                    return AssertionStatus.FALSIFIED;
                }
                break;

            case DELETE:
                if (!assertion.isDeleteOperation()) {
                    return AssertionStatus.FALSIFIED;
                }
                break;

            default:
                return AssertionStatus.FALSIFIED;
        }

        //get context variable prefix
        String variablePrefix = ExpandVariables.process(assertion.getVariablePrefix(), varMap, getAudit());
        variablePrefix = !variablePrefix.isEmpty() ? variablePrefix : OdataValidationAssertion.DEFAULT_PREFIX;
        String metadata = ExpandVariables.process(assertion.getOdataMetadataSource(), varMap, getAudit());
        if (StringUtils.isBlank(metadata)) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_SMD, "Service Metadata Document is blank!");
            return AssertionStatus.FALSIFIED;
        }
        String resourceUrl = ExpandVariables.process(assertion.getResourceUrl(), varMap, getAudit());
        if (StringUtils.isBlank(resourceUrl)) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI, "Resource URI is blank!");
            return AssertionStatus.FALSIFIED;
        }

        String path, query = null;

        int queryIndex = resourceUrl.indexOf('?');
        if (queryIndex != -1) {
            query = resourceUrl.substring(queryIndex + 1);
            path = resourceUrl.substring(0, queryIndex);
        } else {
            path = resourceUrl;
        }

        Edm metadataEdm;
        InputStream metadataStream = new ByteArrayInputStream(metadata.getBytes());

        try {
            metadataEdm = EntityProvider.readMetadata(metadataStream, false);
        } catch (EntityProviderException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_SMD,
                    new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        OdataRequestInfo odataRequestInfo;

        //Create parser
        OdataParser parser = new OdataParser(metadataEdm);

        try {
            //get request info
            odataRequestInfo = parser.parseRequest(path, query);

            setContextVariables(odataRequestInfo, variablePrefix, context);

        } catch (OdataParser.OdataParsingException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_URI,
                    new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FALSIFIED;
        }

        // Check Permitted Actions
        if (odataRequestInfo.isMetadataRequest() &&
                (null == assertion.getActions() ||
                !assertion.getActions().contains(OdataValidationAssertion.ProtectionActions.ALLOW_METADATA))) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_REQUEST_MADE_FOR_SMD);
            return AssertionStatus.FALSIFIED;
        }

        if (odataRequestInfo.isValueRequest() &&
                (null == assertion.getActions() ||
                !assertion.getActions().contains(OdataValidationAssertion.ProtectionActions.ALLOW_RAW_VALUE))) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_REQUEST_MADE_FOR_RAW_VALUE);
            return AssertionStatus.FALSIFIED;
        }

        if (assertion.isValidatePayload()) {

            final MimeKnob mimeKnob = msg.getKnob(MimeKnob.class);

            if (mimeKnob == null) {
                logAndAudit(AssertionMessages.ODATA_VALIDATION_TARGET_INVALID_PAYLOAD, targetName, "payload is empty");
                return AssertionStatus.FALSIFIED;
            }

            //determine content type
            String contentType;
            ContentTypeHeader contentTypeHeader = mimeKnob.getOuterContentType();
            if (contentTypeHeader != null) {
                contentType = contentTypeHeader.getType() + "/" + contentTypeHeader.getSubtype();
            } else {
                contentType = "application/xml"; // this is OData default content type
            }

            try {
                //parse the payload
                parser.parsePayload(requestMethod.toString(), odataRequestInfo, mimeKnob.getEntireMessageBodyAsInputStream(), contentType);
            } catch (OdataParser.OdataParsingException | IOException e) {
                logAndAudit(AssertionMessages.ODATA_VALIDATION_TARGET_INVALID_PAYLOAD,
                        new String[]{targetName, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FALSIFIED;
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.NO_SUCH_PART, new String[]{assertion.getTargetName(),
                        e.getWhatWasMissing()}, ExceptionUtils.getDebugException(e));
                return getBadMessageStatus();
            }
        }

        return AssertionStatus.NONE;
    }

    private void setContextVariables(OdataRequestInfo odataRequestInfo, String prefix, PolicyEnforcementContext context) {
        context.setVariable(prefix + OdataValidationAssertion.QUERY_COUNT, Boolean.toString(odataRequestInfo.isCount()));
        //set filter
        try {
            Set<String> filterParts = OdataParserUtil.getExpressionParts(odataRequestInfo.getFilterExpression());
            if (filterParts.size() > 0) {
                context.setVariable(prefix + OdataValidationAssertion.QUERY_FILTER, filterParts.toArray(new String[filterParts.size()]));
            }
        } catch (OdataValidationException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_EXPRESSION_ERROR,
                    new String[]{"filter", ExceptionUtils.getMessage(e.getCause())}, ExceptionUtils.getDebugException(e));
            //set filter as string
            context.setVariable(prefix + OdataValidationAssertion.QUERY_FILTER, odataRequestInfo.getFilterExpressionString());
        }

        final Integer top = odataRequestInfo.getTop();
        if (top != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_TOP, top.toString());

        final Integer skip = odataRequestInfo.getSkip();
        if (skip != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_SKIP, skip.toString());
        try {
            Set<String> orderByParts = OdataParserUtil.getExpressionParts(odataRequestInfo.getOrderByExpression());
            if (orderByParts.size() > 0) {
                context.setVariable(prefix + OdataValidationAssertion.QUERY_ORDERBY, orderByParts.toArray(new String[orderByParts.size()]));
            }
        } catch (OdataValidationException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_EXPRESSION_ERROR,
                    new String[]{"orderby", ExceptionUtils.getMessage(e.getCause())}, ExceptionUtils.getDebugException(e));
            //set orderby as string
            context.setVariable(prefix + OdataValidationAssertion.QUERY_FILTER, odataRequestInfo.getOrderByExpressionString());
        }
        //set $expand expression
        final String expand = odataRequestInfo.getExpandExpressionString();
        if (expand != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_EXPAND, expand);
        //set $format expression
        String format = odataRequestInfo.getFormat();
        if (format != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_FORMAT, format);

        final String inlinecount = odataRequestInfo.getInlineCount();
        if (inlinecount != null)
            context.setVariable(prefix + OdataValidationAssertion.QUERY_INLINECOUNT, inlinecount);

        final String select = odataRequestInfo.getSelectExpressionString();
        if (select != null) context.setVariable(prefix + OdataValidationAssertion.QUERY_SELECT, select);

        Map<String, String> customQueryOptions = odataRequestInfo.getCustomQueryOptions();
        if (!customQueryOptions.isEmpty())
            context.setVariable(prefix + OdataValidationAssertion.QUERY_CUSTOMOPTIONS, OdataParserUtil.map2Array(customQueryOptions));

        //set path segments
        List<PathSegment> pathSegmentList = odataRequestInfo.getOdataSegments();
        if (pathSegmentList.size() > 0)
            context.setVariable(prefix + OdataValidationAssertion.QUERY_PATHSEGMENTS, OdataParserUtil.pathSegments2Strings(pathSegmentList));
    }
}
