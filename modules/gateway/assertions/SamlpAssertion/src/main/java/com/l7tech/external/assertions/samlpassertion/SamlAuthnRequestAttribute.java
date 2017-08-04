package com.l7tech.external.assertions.samlpassertion;

public final class SamlAuthnRequestAttribute {
    public static final String ASSERTION_CONSUMER_SERVICE_URL = "AssertionConsumerServiceURL";
    public static final String ASSERTION_CONSUMER_SERVICE_INDEX = "AssertionConsumerServiceIndex";
    public static final String PROTOCOL_BINDING = "ProtocolBinding";
    public static final String PROVIDER_NAME = "ProviderName";
    public static final String ATTRIBUTE_CONSUMING_SERVICE_INDEX = "AttributeConsumerServiceIndex";

    private SamlAuthnRequestAttribute() {
        // restrict instantiation for class declaring constants only
    }
}
