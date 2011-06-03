package com.l7tech.external.assertions.sftp;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO SFTP put/route assertion
 */
public class SftpAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {

    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);


    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    /**
     * Set the target message (or variable) into which the Response shall be saved.
     *
     * @param responseTarget Response Message, or null if no response is expected.
     */
    public void setResponseTarget(MessageTargetableSupport responseTarget) {
        this.responseTarget = responseTarget;
    }

    @Override
    public boolean initializesRequest() {
        return responseTarget != null && TargetMessageType.REQUEST == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedRequest() {
        return requestTarget == null || TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return responseTarget != null && TargetMessageType.RESPONSE == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedResponse() {
        // Response must be initialized only in the extremely unlikely event that we are sending the default response as the routing request
        return requestTarget != null && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    /**
     * Set the target message (or variable) to use as the Request.
     *
     * @param requestTarget  Request Message.  Required.
     */
    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    private static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SftpAssertion>(){
        @Override
        public String getAssertionName( final SftpAssertion assertion, final boolean decorate) {
            if (!decorate)
                return "TODO Route via SFTP";
            StringBuilder sb = new StringBuilder("TODO SFTP Put");

            return AssertionUtils.decorateName(assertion, sb.toString());
        }
    };

    //
    // Metadata
    //
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.sftp.server.ModuleLoadListener");

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "TODO Put via SFTP");
        meta.put(AssertionMetadata.DESCRIPTION, "TODO Put a request over SFTP");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.sftp.console.SftpAssertionPropertiesDialog");
        // meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SFTP Put Properties");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        ret.addAll(Arrays.asList(requestTarget.getVariablesUsed()));
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return responseTarget.mergeVariablesSet(requestTarget.getVariablesSet());
    }
}
