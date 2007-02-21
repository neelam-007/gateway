package com.l7tech.skunkworks;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.RoutingAssertion;

/**
 * Very simple assertion, for example purposes.
 * This allows the Console to generate a default properties editor for its properties.
 * See {@link com.l7tech.skunkworks.server.ServerHelloWorldAssertion} for the server implementation.
 */
public class HelloWorldAssertion extends RoutingAssertion {
    private String nameToGreet;

    public String getNameToGreet() {
        return nameToGreet;
    }

    public void setNameToGreet(String nameToGreet) {
        this.nameToGreet = nameToGreet;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });

        return meta;
    }
}
