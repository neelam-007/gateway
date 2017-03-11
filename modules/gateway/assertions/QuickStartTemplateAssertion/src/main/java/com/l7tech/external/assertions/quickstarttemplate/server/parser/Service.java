package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.l7tech.common.http.HttpMethod;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Service {

    public final String name;
    public final String gatewayUri;
    public final List<HttpMethod> httpMethods;
    public final List<Map<String, Map<String, ?>>> policy;

    @JsonCreator
    public Service(@JsonProperty("name") @NotNull final String name,
                   @JsonProperty("gatewayUri") @NotNull final String gatewayUri,
                   @JsonProperty("httpMethods") @NotNull final List<String> httpMethods,
                   @JsonProperty("policy") @NotNull final List<Map<String, Map<String, ?>>> policy) {
        this.name = name;
        this.gatewayUri = gatewayUri;
        this.httpMethods = httpMethods.stream().map(o -> HttpMethod.valueOf(o.toUpperCase())).collect(Collectors.toList());
        policy.forEach(p -> {
            if (p.size() > 1) {
                throw new IllegalArgumentException("Policy cannot have more than one element");
            }
        });
        this.policy = policy;
    }

}
