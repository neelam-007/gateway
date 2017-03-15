package com.l7tech.external.assertions.quickstarttemplate.server.documentation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class QuickStartDocumentationBuilder {
    private static final Logger LOGGER = Logger.getLogger(QuickStartDocumentationBuilder.class.getName());
    private static final String TEMPLATE_RESOURCE = "documentation.html";

    public String generate(final Set<EncapsulatedAssertion> encapsulatedAssertions) throws IOException {
        final List<EncapsulatedAssertion> sortedAssertions = orderByName(encapsulatedAssertions);
        LOGGER.warning("JMK: Assertions after sorting: " + sortedAssertions.size());
        try (final InputStream i = getClass().getResourceAsStream(TEMPLATE_RESOURCE);
             final Reader r = new InputStreamReader(i)) {
            return Mustache.compiler()
                    .compile(r)
                    .execute(ImmutableMap.of(
                            "assertions", sortedAssertions
                    ));
        } catch (final MustacheException e) {
            // This is an internal detail of the template; rethrow as an IOException.
            LOGGER.log(Level.WARNING, "Unable to compile and populate documentation template.", e);
            throw new IOException(e);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Unable to find documentation template.", e);
            throw e;
        }
    }

    @VisibleForTesting
    List<EncapsulatedAssertion> orderByName(final Collection<EncapsulatedAssertion> encapsulatedAssertions) {
        return encapsulatedAssertions.stream()
                .sorted((ea1, ea2) -> ea1.config().getName().compareTo(ea2.config().getName()))
                .collect(Collectors.toList());
    }

}
