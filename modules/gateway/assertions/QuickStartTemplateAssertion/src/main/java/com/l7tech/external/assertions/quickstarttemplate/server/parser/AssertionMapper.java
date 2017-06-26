package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
@SuppressWarnings("WeakerAccess")
public class AssertionMapper {
    private static final Logger logger = Logger.getLogger(AssertionMapper.class.getName());

    /**
     * QuickStart Assertion mapper YAML resource
     */
    private static final String QS_MAPPER_YAML = "com/l7tech/external/assertions/quickstarttemplate/server/parser/qs_mapper.yaml";

    /**
     * Holds a read-only view of supported Assertions
     */
    @NotNull
    private final Map<String, AssertionSupport> supportedAssertions;

    /**
     * Constructs the assertion mapper with {@link #QS_MAPPER_YAML} as source.
     */
    public AssertionMapper() {
        Map<String, AssertionSupport> assertions = null;
        try (final InputStream inputStream = getMapperResourceStream()) {
            assertions = loadMappings(inputStream);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        this.supportedAssertions = assertions != null ? assertions : Collections.emptyMap();
    }

    /**
     * Constructs the assertion mapper from the specified {@code source}.
     *
     * @param source    an {@link InputStream} of the assertions source.  Mandatory and cannot be {@code null}.
     */
    public AssertionMapper(@NotNull final InputStream source) {
        supportedAssertions = loadMappings(source);
    }

    /**
     * Loads the mappings from the specified YAML {@link InputStream input stream}.
     *
     * @param inputStream    {@link InputStream} of the YAML content.  Mandatory and cannot be {@code null}.
     * @return a {@link Map map} of the Assertion mappings, never {@code null}.
     */
    @NotNull
    private Map<String, AssertionSupport> loadMappings(@NotNull final InputStream inputStream) {
        try {
            return Collections.unmodifiableMap(new ObjectMapper(new YAMLFactory()).readValue(inputStream, new TypeReference<Map<String, AssertionSupport>>() { }));
        } catch (final JsonParseException e) {
            logger.log(Level.WARNING, "Unable to parse assertion mapper YAML content: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (final JsonMappingException e) {
            final IllegalArgumentException arg = ExceptionUtils.getCauseIfCausedBy(e, IllegalArgumentException.class);
            if (arg != null) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(arg), ExceptionUtils.getDebugException(e));
            } else {
                final JsonParseException parseException = ExceptionUtils.getCauseIfCausedBy(e, JsonParseException.class);
                if (parseException != null) {
                    logger.log(Level.WARNING, "Unable to parse JSON service payload: " + ExceptionUtils.getMessage(parseException), ExceptionUtils.getDebugException(parseException));
                } else {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error while reading assertion mapper YAML content: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return Collections.emptyMap();
    }

    /**
     * Convenient method for getting {@link #QS_MAPPER_YAML Assertion mapper YAML} resource stream.<br/>
     * It is the responsibility of the caller to properly close the stream.
     *
     * @return {@link InputStream} of the {@link #QS_MAPPER_YAML Assertion mapper YAML} resource, never {@code null}.
     * @throws IOException if the resource stream cannot be opened.
     */
    @NotNull
    private InputStream getMapperResourceStream() throws IOException {
        return Optional.ofNullable(getClassLoader())
                .map(cl -> cl.getResourceAsStream(QS_MAPPER_YAML))
                .orElseThrow(() -> new IOException("Failed to load Assertion mapper yaml resource."));
    }

    private ClassLoader getClassLoader() {
        return AssertionMapper.class.getClassLoader();
    }

    /**
     * Read-only list of supported assertions.
     */
    @NotNull
    public static Map<String, AssertionSupport> getSupportedAssertions() {
        return new AssertionMapper().supportedAssertions;
    }
}
