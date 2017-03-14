package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.google.common.base.Joiner;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionStringEncoding;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuickStartMapper {
    private final QuickStartEncapsulatedAssertionLocator assertionLocator;

    public QuickStartMapper(final QuickStartEncapsulatedAssertionLocator assertionLocator) {
        this.assertionLocator = assertionLocator;
    }

    /**
     * For each name
     *    - look up encass by name to get guid
     *    - if applicable set encass argument(s)
     */
    @NotNull
    public List<EncapsulatedAssertion> getEncapsulatedAssertions(@NotNull final Service service)
            throws QuickStartPolicyBuilderException, FindException {
        final List<EncapsulatedAssertion> encapsulatedAssertions = new ArrayList<>();
        for (final Map<String, Map<String, ?>> policyMap : service.policy) {
            // We know there is only one thing in this map, we've previously validated this.
            final String name = policyMap.keySet().iterator().next();
            final EncapsulatedAssertion encapsulatedAssertion = assertionLocator.findEncapsulatedAssertion(name);
            if (encapsulatedAssertion == null) {
                throw new QuickStartPolicyBuilderException("Unable to find encapsulated assertion template named : " + name);
            }
            setEncassArguments(encapsulatedAssertion, policyMap.get(name));
            encapsulatedAssertions.add(encapsulatedAssertion);
        }
        return encapsulatedAssertions;
    }


    private void setEncassArguments(@NotNull final EncapsulatedAssertion encapsulatedAssertion, @NotNull final Map<String, ?> properties) throws QuickStartPolicyBuilderException {
        if (encapsulatedAssertion.config() == null) {
            throw new IllegalStateException("Unable to obtain the encapsulated assertion config object.");
        }

        for (final Map.Entry<String, ?> entry : properties.entrySet()) {
            final EncapsulatedAssertionArgumentDescriptor descriptor = findArgumentDescriptor(entry.getKey(), encapsulatedAssertion);
            if (descriptor == null) {
                throw new QuickStartPolicyBuilderException("Incorrect encapsulated assertion property: " + entry.getKey() + ", for encapsulated assertion: " + encapsulatedAssertion.config().getName());
            }
            // Don't know the type... Can't, so we have to check a number of different types.
            final Object propertyValue = entry.getValue();
            String resultingValue;
            if (propertyValue instanceof Iterable) {
                // If it's an iterable, we cannot pass arrays to encapsulated assertions, so we merge them together
                // like this into a semicolon delimited string.
                resultingValue = Joiner.on(";").join((Iterable) propertyValue);
            } else {
                // Convert the value using the encapsulated assertion encoding type.
                resultingValue = EncapsulatedAssertionStringEncoding.encodeToString(descriptor.dataType(), propertyValue);
                // If we couldn't convert it, try a string as a last resort.
                if (resultingValue == null) {
                    resultingValue = propertyValue.toString();
                }
            }
            encapsulatedAssertion.putParameter(entry.getKey(), resultingValue);
        }
    }

    @Nullable
    private static EncapsulatedAssertionArgumentDescriptor findArgumentDescriptor(@NotNull final String name, @NotNull final EncapsulatedAssertion ea) {
        assert ea.config() != null;
        assert ea.config().getArgumentDescriptors() != null;
        return ea.config().getArgumentDescriptors().stream()
                .filter(ad -> name.equals(ad.getArgumentName()))
                .findFirst()
                .orElse(null);
    }

}
