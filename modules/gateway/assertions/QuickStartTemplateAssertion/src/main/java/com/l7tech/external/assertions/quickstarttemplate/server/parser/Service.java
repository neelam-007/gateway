package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    public Service(@JsonProperty("name") final String name,
                   @JsonProperty("gatewayUri") final String gatewayUri,
                   @JsonProperty("httpMethods") @NotNull final List<String> httpMethods,
                   @JsonProperty("policy") @NotNull final List<Map<String, Map<String, ?>>> policy) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Service must have a name.");
        }
        this.name = name;
        if (!ValidationUtils.isValidUri(gatewayUri) || !gatewayUri.startsWith("/")) {
            throw new IllegalArgumentException("Service gatewayUri is invalid. Service must have a valid URI starting with a '/'.");
        }
        this.gatewayUri = gatewayUri;
        if (httpMethods == null) {
            throw new IllegalArgumentException("HTTP methods are required.");
        }
        this.httpMethods = httpMethods.stream().map(o -> HttpMethod.valueOf(o.toUpperCase())).collect(Collectors.toList());
        if (policy == null) {
            throw new IllegalArgumentException("Policy list is required.");
        }
        policy.forEach(p -> {
            if (p.size() > 1) {
                throw new IllegalArgumentException("Policy elements cannot have more than one child.");
            }
        });
        this.policy = policy;
    }

}
