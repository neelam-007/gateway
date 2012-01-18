package com.l7tech.external.assertions.mqnative;

import com.l7tech.external.assertions.mqnative.server.MqNativeAdminServerSupport;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * A hidden assertion that registers the MQ Native Admin extension interface and GUI action.
 *
 * @author ghuang
 */
public class MqNativeSupportAssertion extends Assertion {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return MqNativeAdminServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        return meta;
    }
}