package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PolicyEntity {
    private String policyEntityUuid;
    private List<PolicyTemplateArgument> policyTemplateArguments = new ArrayList<PolicyTemplateArgument>();

    @JsonProperty( value = "PolicyEntityUuid")
    public String getPolicyEntityUuid() {
        return policyEntityUuid;
    }

    public void setPolicyEntityUuid(String policyEntityUuid) {
        this.policyEntityUuid = policyEntityUuid;
    }

    @JsonProperty( value = "Arguments")
    public List<PolicyTemplateArgument> getPolicyTemplateArguments() {
        return policyTemplateArguments;
    }

    public void setPolicyTemplateArguments(List<PolicyTemplateArgument> policyTemplateArguments) {
        this.policyTemplateArguments = policyTemplateArguments;
    }
}
