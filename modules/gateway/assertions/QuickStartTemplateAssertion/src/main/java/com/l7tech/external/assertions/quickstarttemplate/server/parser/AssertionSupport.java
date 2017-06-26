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
 * Holds assertion support information
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class AssertionSupport {
    /**
     * Assertion external name used to search through {@link com.l7tech.policy.AssertionRegistry assertion registry}
     */
    private final String externalName;
    /**
     * The fully qualified name of the desired class
     */
    private final String className;
    /**
     * Argument mappings
     */
    @Nullable
    private final Map<String, PropertyAttribute> properties;
    /**
     * Sample payload
     */
    @Nullable
    private final String samplePayload;
    /**
     * JSON schema
     */
    @Nullable
    private final String jsonSchema;

    public static final class PropertyAttribute {
        @NotNull
        private final String mapTo;
        @NotNull
        private final Class<?> type;
        @Nullable
        private final Map<String, String> values;

        @SuppressWarnings("NullableProblems")
        @JsonCreator
        public PropertyAttribute(
                @JsonProperty("mapTo") final String mapTo,
                @JsonProperty("type") final Class<?> type,
                @JsonProperty("values") @Nullable final Map<String, String> values
        ) {
            if (StringUtils.isBlank(mapTo)){
                throw new IllegalArgumentException("Assertion mapping must provide mapTo.");
            }
            this.mapTo = mapTo;

            if (type == null){
                throw new IllegalArgumentException("Assertion mapping must provide type.");
            }
            this.type = type;

            this.values = values != null ? Collections.unmodifiableMap(values) : Collections.emptyMap();
        }

        @NotNull
        public String getMapTo() {
            return mapTo;
        }

        @NotNull
        public Class<?> getType() {
            return type;
        }

        @Nullable
        public Map<String, String> getValues() {
            return values;
        }
    }

    public static final class MethodAttribute {
        private final String method;
        private final List<Class<?>> parameters;

        @JsonCreator
        public MethodAttribute(
                @JsonProperty("method") final String method,
                @JsonProperty("parameters") @Nullable final List<Class<?>> parameters
        ) {
            if (StringUtils.isBlank(method)){
                throw new IllegalArgumentException("Assertion mapping must provide method.");
            }
            this.method = method;
            this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
        }

        public String getMethod() {
            return method;
        }

        public List<Class<?>> getParameters() {
            return parameters;
        }
    }

    @JsonCreator
    public AssertionSupport(
            @JsonProperty("externalName") final String externalName,
            @JsonProperty("className") final String className,
            @JsonProperty("properties") @Nullable final Map<String, PropertyAttribute> properties,
            @JsonProperty("samplePayload") @Nullable final String samplePayload,
            @JsonProperty("jsonSchema") @Nullable final String jsonSchema
    ) {
        if (StringUtils.isBlank(externalName)){
            throw new IllegalArgumentException("Assertion mapping must provide externalName.");
        }
        this.externalName = externalName;
        // todo: perhaps made className mandatory
        this.className = className;
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : Collections.emptyMap();
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

    @Nullable
    public Map<String, PropertyAttribute> getProperties() {
        return properties;
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
        public Map<String, PropertyAttribute> getProperties() {
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

        // TODO: expose more attributes here
    }
}
