package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holder of service and encapsulated assertion data required to create the service.
 */
public class QuickStartEncapsulatedAssertionTemplate {
    private final PublishedService publishedService;
    private final List<EncapsulatedAssertion> encapsulatedAssertions;

    public QuickStartEncapsulatedAssertionTemplate(@NotNull final PublishedService publishedService, @NotNull final List<EncapsulatedAssertion> encapsulatedAssertions) {
        this.publishedService = publishedService;
        this.encapsulatedAssertions = encapsulatedAssertions;
    }

    PublishedService getPublishedService() {
        return publishedService;
    }

    List<EncapsulatedAssertion> getEncapsulatedAssertions() {
        return encapsulatedAssertions;
    }
}
