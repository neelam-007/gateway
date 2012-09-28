package com.l7tech.external.assertions.ahttp;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;

import java.util.logging.Logger;

/**
 * 
 */
public class SubmitAsyncHttpResponseAssertion extends MessageTargetableAssertion {
    protected static final Logger logger = Logger.getLogger(SubmitAsyncHttpResponseAssertion.class.getName());

    public SubmitAsyncHttpResponseAssertion() {
        super(TargetMessageType.RESPONSE);
    }

    private String correlationId;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( correlationId );
    }

    private static final String META_INITIALIZED = SubmitAsyncHttpResponseAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.ahttp.server.AsyncHttpTransportModule");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
