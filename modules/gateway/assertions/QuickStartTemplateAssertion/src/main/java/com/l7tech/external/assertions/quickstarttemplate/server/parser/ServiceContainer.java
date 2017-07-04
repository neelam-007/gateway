package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public class ServiceContainer {

    public final Service service;

    @JsonCreator
    public ServiceContainer(@JsonProperty("Service") @NotNull final Service service) {
        this.service = service;
    }


}
