package com.l7tech.external.assertions.quickstarttemplate.server.documentation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.AssertionSupport;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.util.ExceptionUtils;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class QuickStartDocumentationBuilder {
    private static final Logger LOGGER = Logger.getLogger(QuickStartDocumentationBuilder.class.getName());
    private static final String TEMPLATE_RESOURCE = "documentation.html";

    public String generate(
            @NotNull final Set<EncapsulatedAssertion> encapsulatedAssertions,
            @NotNull final Set<AssertionSupport.Info> assertions
    ) throws IOException {
        try (final InputStream i = getClass().getResourceAsStream(TEMPLATE_RESOURCE);
             final Reader r = new InputStreamReader(i)) {
            return Mustache.compiler()
                    .compile(r)
                    .execute(ImmutableMap.of(
                            "encasses", orderEncassesByName(encapsulatedAssertions),
                            "assertions", orderAssertionsByName(assertions)
                    ));
        } catch (final MustacheException e) {
            // This is an internal detail of the template; rethrow as an IOException.
            LOGGER.log(Level.WARNING, "Unable to compile and populate documentation template.", ExceptionUtils.getDebugException(e));
            throw new IOException(e);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Unable to find documentation template.", ExceptionUtils.getDebugException(e));
            throw e;
        }
    }

    @VisibleForTesting
    List<EncapsulatedAssertion> orderEncassesByName(@NotNull final Collection<EncapsulatedAssertion> encapsulatedAssertions) {
        return encapsulatedAssertions.stream()
                .sorted((ea1, ea2) -> Optional.ofNullable(ea1.config()).map(EncapsulatedAssertionConfig::getName).orElse("")
                        .compareTo(Optional.ofNullable(ea2.config()).map(EncapsulatedAssertionConfig::getName).orElse("")))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    List<AssertionSupport.Info> orderAssertionsByName(@NotNull final Collection<AssertionSupport.Info> assertions) {
        //noinspection ConstantConditions
        return assertions.stream()
                .sorted((a1, a2) -> Optional.ofNullable(a1.getExternalName()).orElse("").compareTo(Optional.ofNullable(a2.getExternalName()).orElse("")))
                .collect(Collectors.toList());
    }
}
