package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.external.assertions.amqpassertion.server.AmqpSupportServer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 10/04/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class AmqpSupportAssertion extends Assertion {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final AmqpSupportServer supportInstance = AmqpSupportServer.getInstance(appContext);
                return supportInstance.getExtensionInterfaceBindings();
            }
        });
        return meta;
    }
}
