package com.l7tech.policy.assertion;

import com.l7tech.util.HexUtils;
import com.l7tech.policy.variable.Syntax;
import static com.l7tech.policy.assertion.AssertionMetadata.SHORT_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.LONG_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_ADVICE_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_DESC;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.io.IOException;

/**
 * A pseudo-routing assertion that returns a hardcoded response message, with a hardcoded
 * response status and content type.
 */
public class HardcodedResponseAssertion extends RoutingAssertion implements UsesVariables {
    private int responseStatus = 200;
    private boolean earlyResponse;
    private String responseContentType = "text/xml; charset=UTF-8";
    private String base64ResponseBody = "";

    public HardcodedResponseAssertion() {
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
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
        try {
            return new String(HexUtils.decodeBase64(base64ResponseBody, true), "UTF-8");
        } catch (IOException e) {
            return base64ResponseBody;
        }
    }

    public void responseBodyString(String responseBody) {
        setBase64ResponseBody(HexUtils.encodeBase64(HexUtils.encodeUtf8(responseBody), true));
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(responseBodyString() + responseContentType);
    }

    public String getBase64ResponseBody() {
        return base64ResponseBody;
    }

    public void setBase64ResponseBody(String body) {
        if (body == null) body = "";
        base64ResponseBody = body;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "Template Response");
        meta.put(LONG_NAME, "Return a template response message");

        meta.put(PALETTE_NODE_NAME, "Template Response");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/MessageLength-16x16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "routing" });

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME, "Template Response");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/MessageLength-16x16.gif");

        meta.put(PROPERTIES_ACTION_NAME, "Template Response Properties");
        meta.put(PROPERTIES_ACTION_DESC, "View and edit template response properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.HardcodedResponseDialog");

        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.HardcodedResponseAssertionValidator");

        meta.put(WSP_EXTERNAL_NAME, "HardcodedResponse");
        meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.wsp.HardcodedResponseAssertionMapping");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:HardcodedResponse" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }
}
