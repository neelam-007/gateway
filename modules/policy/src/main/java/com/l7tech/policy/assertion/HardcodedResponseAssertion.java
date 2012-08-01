package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.annotation.Base64Value;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * A pseudo-routing assertion that returns a hardcoded response message, with a hardcoded
 * response status and content type.
 */
public class HardcodedResponseAssertion extends RoutingAssertion implements UsesVariables, RoutingAssertionDoesNotRoute {
    public static final String DEFAULT_STATUS = "200";
    private String responseStatus = DEFAULT_STATUS;
    private boolean earlyResponse;
    private String responseContentType = "text/xml; charset=UTF-8";
    private String base64ResponseBody = "";

    public HardcodedResponseAssertion() {
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(@NotNull final String responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * Int setter required for backwards compatibility.
     */
    public void setResponseStatus(final int responseStatus) {
        this.responseStatus = String.valueOf(responseStatus);
    }

    public boolean isEarlyResponse() {
        return earlyResponse;
    }

    public void setEarlyResponse(boolean earlyResponse) {
        this.earlyResponse = earlyResponse;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String responseBodyString() {
        return new String(HexUtils.decodeBase64(base64ResponseBody, true), Charsets.UTF8);
    }

    public void responseBodyString(String responseBody) {
        setBase64ResponseBody(HexUtils.encodeBase64(HexUtils.encodeUtf8(responseBody), true));
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(responseBodyString(), responseContentType, responseStatus);
    }

    @Base64Value(decodeMethodName = "responseBodyString")
    public String getBase64ResponseBody() {
        return base64ResponseBody;
    }

    public void setBase64ResponseBody(String body) {
        if (body == null) body = "";
        base64ResponseBody = body;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return false;
    }

    @Override
    public boolean initializesResponse() {
        return true;
    }

    @Override
    public boolean needsInitializedResponse() {
        return false;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "Return Template Response to Requestor");
        meta.put(DESCRIPTION, "Return a template response message.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/MessageLength-16x16.gif");
        meta.put(PALETTE_FOLDERS, new String[]{"routing"});

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/MessageLength-16x16.gif");

        meta.put(PROPERTIES_ACTION_NAME, "Template Response Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.HardcodedResponseDialog");

        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.HardcodedResponseAssertionValidator");

        meta.put(WSP_EXTERNAL_NAME, "HardcodedResponse");
        meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.wsp.HardcodedResponseAssertionMapping");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:HardcodedResponse" rather than "set:modularAssertions"
        meta.put(FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }
}
