package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.jetbrains.annotations.NotNull;

public class ServiceContainer {

    public final Service service;

    @JsonCreator
    public ServiceContainer(@JsonProperty("Service") @NotNull final Service service) {
        this.service = service;
    }


}
