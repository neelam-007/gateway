package com.l7tech.external.assertions.quickstarttemplate.server.documentation;

import com.google.common.collect.ImmutableSet;
import com.l7tech.external.assertions.quickstarttemplate.server.QuickStartTestBase;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

public class QuickStartDocumentationBuilderTest extends QuickStartTestBase {

    private QuickStartDocumentationBuilder fixture;

    @Before
    public void setUp() {
        fixture = new QuickStartDocumentationBuilder();
    }

    @Test
    public void generateWithMultipleCompleteAssertions() throws Exception {
        final String result = fixture.generate(
                ImmutableSet.of(
                    mockEncapsulatedAssertion("MyAssertionA", "MyDescriptionA", "MySchemaA", "MySampleA"),
                    mockEncapsulatedAssertion("MyAssertionB", "MyDescriptionB", "MySchemaB", "MySampleB"),
                    mockEncapsulatedAssertion("MyAssertionC", "MyDescriptionC", "MySchemaC", "MySampleC")
                ),
                Collections.emptySet()
        );
        // Difficult to validate HTML formatting (and subject to change), so just validate that all of the above
        // elements are in the resulting content.
        assertThat(result, containsString("MyAssertionA"));
        assertThat(result, containsString("MyAssertionB"));
        assertThat(result, containsString("MyAssertionC"));
        assertThat(result, containsString("MyDescriptionA"));
        assertThat(result, containsString("MyDescriptionB"));
        assertThat(result, containsString("MyDescriptionC"));
        assertThat(result, containsString("MySchemaA"));
        assertThat(result, containsString("MySchemaB"));
        assertThat(result, containsString("MySchemaC"));
        assertThat(result, containsString("MySampleA"));
        assertThat(result, containsString("MySampleB"));
        assertThat(result, containsString("MySampleC"));
    }

    @Test
    public void generateWithAssertionMissingDescription() throws Exception {
        final String result = fixture.generate(
                ImmutableSet.of(mockEncapsulatedAssertion("MyAssertionA", null, "MySchemaA", "MySampleA")),
                Collections.emptySet()
        );
        assertThat(result, containsString("MyAssertionA"));
        assertThat(result, not(containsString("<p></p>"))); // No description section. Not a great test.
        assertThat(result, containsString("MySchemaA"));
        assertThat(result, containsString("MySampleA"));
    }

    @Test
    public void generateWithAssertionMissingSchema() throws Exception {
        final String result = fixture.generate(
                ImmutableSet.of(mockEncapsulatedAssertion("MyAssertionA", "MyDescriptionA", null, "MySampleA")),
                Collections.emptySet()
        );
        assertThat(result, containsString("MyAssertionA"));
        assertThat(result, containsString("MyDescriptionA"));
        assertThat(result, not(containsString("<summary>Schema</summary>"))); // No schema section.
        assertThat(result, containsString("MySampleA"));
    }

    @Test
    public void generateWithAssertionMissingSample() throws Exception {
        final String result = fixture.generate(
                ImmutableSet.of(mockEncapsulatedAssertion("MyAssertionA", "MyDescriptionA", "MySchemaA", null)),
                Collections.emptySet()
        );
        assertThat(result, containsString("MyAssertionA"));
        assertThat(result, containsString("MyDescriptionA"));
        assertThat(result, containsString("MySchemaA"));
        assertThat(result, not(containsString("<summary>Sample</summary>"))); // No sample section.
    }

    @Test
    public void findEncapsulatedAssertionsOrderedByNameShouldReturnListOrderedByName() {
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion("MyAssertionA", "MyDescription", "MySchema", "MySample");
        final EncapsulatedAssertion ea2 = mockEncapsulatedAssertion("MyAssertionB", "MyDescription", "MySchema", "MySample");
        final EncapsulatedAssertion ea3 = mockEncapsulatedAssertion("MyAssertionC", "MyDescription", "MySchema", "MySample");
        final List<EncapsulatedAssertion> result = fixture.orderEncassesByName(ImmutableSet.of(
                ea2,
                ea3,
                ea1
        ));
        assertThat(result.stream()
                .map(e -> e.config().getName())
                .collect(Collectors.toList()), contains("MyAssertionA", "MyAssertionB", "MyAssertionC"));
    }

    // TODO: assertions testing pending
//    @Test
//    public void findAssertionsOrderedByNameShouldReturnListOrderedByName() {
//        final Assertion a1 = mockAssertion("MyAssertionA", "MyAssertionA", "MyAssertionA Description");
//        final Assertion a2 = mockAssertion("MyAssertionB", "MyAssertionB", "MyAssertionB Description");
//        final Assertion a3 = mockAssertion("MyAssertionC", "MyAssertionC", "MyAssertionC Description");
//        List<AssertionSupport.Info> result = fixture.orderAssertionsByName(
//                ImmutableSet.of(
//                        a2,
//                        a3,
//                        a1
//                ));
//        Assert.assertThat(result.stream().map(a -> a.assertion).collect(Collectors.toList()), Matchers.contains(a1, a2, a3));
//    }
//
//    @Test
//    public void generateWithAssertions() throws Exception {
//        final String result = fixture.generate(
//                Collections.emptySet(),
//                ImmutableSet.of(
//                        mockAssertion("MyAssertionA", "MyAssertionA", "MyAssertionA Description"),
//                        mockAssertion("MyAssertionB", "MyAssertionB", "MyAssertionB Description"),
//                        mockAssertion("MyAssertionC", "MyAssertionC", "MyAssertionC Description"))
//        );
//        Assert.assertThat(result, Matchers.containsString("MyAssertionA"));
//        Assert.assertThat(result, Matchers.containsString("MyAssertionB"));
//        Assert.assertThat(result, Matchers.containsString("MyAssertionC"));
//    }


}