package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable object holding assertion support information.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class AssertionSupport {
    /**
     * Mandatory assertion external name used to search through {@link com.l7tech.policy.AssertionRegistry assertion registry}
     */
    private final String externalName;
    /**
     * Optional fully qualified name of the desired class
     */
    private final String className;
    /**
     * Optional property mappings
     */
    @NotNull
    private final Map<String, String> properties;
    /**
     * Optional indicator that a property is base 64 encoded on the Gateway
     */
    @NotNull
    private final Map<String, Boolean> propertiesIsBase64Encoded;
    /**
     * Optional Sample payload
     */
    @Nullable
    private final String samplePayload;
    /**
     * Optional JSON schema
     */
    @Nullable
    private final String jsonSchema;

    @JsonCreator
    public AssertionSupport(
            @JsonProperty("externalName") final String externalName,
            @JsonProperty("className") final String className,
            @JsonProperty("properties") @Nullable final Map<String, String> properties,
            @JsonProperty("propertiesIsBase64Encoded") @Nullable final Map<String, Boolean> propertiesIsBase64Encoded,
            @JsonProperty("samplePayload") @Nullable final String samplePayload,
            @JsonProperty("jsonSchema") @Nullable final String jsonSchema
    ) {
        if (StringUtils.isBlank(externalName)){
            throw new IllegalArgumentException("Assertion mapping must provide externalName.");
        }
        this.externalName = externalName;
        this.className = className;
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : Collections.emptyMap();
        this.propertiesIsBase64Encoded = propertiesIsBase64Encoded != null ? Collections.unmodifiableMap(propertiesIsBase64Encoded) : Collections.emptyMap();
        this.samplePayload = samplePayload;
        this.jsonSchema = jsonSchema;
    }

    @NotNull
    public String getExternalName() {
        return externalName;
    }

    public String getClassName() {
        return className;
    }

    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    @NotNull
    public Map<String, Boolean> getPropertiesIsBase64Encoded() {
        return propertiesIsBase64Encoded;
    }

    @Nullable
    public String getSamplePayload() {
        return samplePayload;
    }

    @Nullable
    public String getJsonSchema() {
        return jsonSchema;
    }

    public Info withAssertion(@NotNull final Assertion assertion) {
        return new Info(assertion);
    }

    /**
     * Immutable assertion info
     */
    public final class Info {
        @NotNull
        private final Assertion assertion;

        private Info(@NotNull final Assertion assertion) {
            this.assertion = assertion;
        }

        @NotNull
        public Assertion getAssertion() {
            return assertion;
        }

        @NotNull
        public String getExternalName() {
            assert AssertionSupport.this.getExternalName().equals(assertion.meta().get(AssertionMetadata.WSP_EXTERNAL_NAME));
            return AssertionSupport.this.getExternalName();
        }

        public String getClassName() {
            return AssertionSupport.this.getClassName();
        }

        @Nullable
        public Map<String, String> getProperties() {
            return AssertionSupport.this.getProperties();
        }

        @Nullable
        public String getSamplePayload() {
            return AssertionSupport.this.getSamplePayload();
        }

        @Nullable
        public String getSchema() {
            return AssertionSupport.this.getJsonSchema();
        }

        @Nullable
        public String getName() {
            return assertion.meta().get(AssertionMetadata.BASE_NAME);
        }

        @Nullable
        public String getShortName() {
            return assertion.meta().get(AssertionMetadata.SHORT_NAME);
        }

        @Nullable
        public String getLongName() {
            return assertion.meta().get(AssertionMetadata.LONG_NAME);
        }

        @Nullable
        public String getDescription() {
            return assertion.meta().get(AssertionMetadata.DESCRIPTION);
        }

        @NotNull
        public List<String> getVariablesUsed() {
            return Optional.of(assertion)
                    .filter(a -> a instanceof UsesVariables)
                    .map(a -> ((UsesVariables)a).getVariablesUsed())
                    .map(Arrays::stream)
                    .orElse(Stream.empty())
                    .sorted()
                    .collect(Collectors.toList());
        }

        @NotNull
        public List<Triple<String, String, Boolean>> getVariablesSet() {
            return Optional.of(assertion)
                    .filter(a -> a instanceof SetsVariables)
                    .map(a -> ((SetsVariables)a).getVariablesSet())
                    .map(Arrays::stream)
                    .orElse(Stream.empty())
                    .sorted(Comparator.nullsLast((o1, o2) -> Optional.ofNullable(o1.getName()).orElse("").compareTo(Optional.ofNullable(o2.getName()).orElse(""))))
                    .map(meta -> Triple.triple(meta.getName(), Optional.ofNullable(meta.getType()).map(DataType::getName).orElse(null), meta.isMultivalued()))
                    .collect(Collectors.toList());
        }
    }
}
