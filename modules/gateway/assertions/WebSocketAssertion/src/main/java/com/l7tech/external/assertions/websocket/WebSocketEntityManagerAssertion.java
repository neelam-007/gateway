package com.l7tech.external.assertions.websocket;

import com.l7tech.external.assertions.websocket.server.WebSocketEntityManagerServerSupport;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/4/12
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketEntityManagerAssertion extends Assertion {

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        //Register Action class for task framework
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.websocket.console.WebSocketManagerAction"});

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return WebSocketEntityManagerServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }
}
