package com.l7tech.external.assertions.mqnative;

import com.l7tech.external.assertions.mqnative.server.MqNativeAdminServerSupport;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

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

                final MqNativeAdminServerSupport supportInstance = MqNativeAdminServerSupport.getInstance(appContext);
                try {
                    Class.forName("com.ibm.mq.MQException", false, MqNativeAdminServerSupport.class.getClassLoader());
                    return supportInstance.getExtensionInterfaceBindings();
                } catch (ClassNotFoundException e) {
                    ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<MqNativeAdmin>(MqNativeAdmin.class, null, new MqNativeAdmin() {
                        @Override
                        public long getDefaultMqMessageMaxBytes() {
                            return supportInstance.getDefaultMqMessageMaxBytes();
                        }

                        @Override
                        public void testSettings(SsgActiveConnector mqNativeActiveConnector) throws MqNativeTestException {
                            throw new MqNativeTestException("MQ Native jars are not installed on this Gateway.");
                        }
                    });
                    return Collections.singletonList(binding);
                }
            }
        });

        return meta;
    }
}