package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.google.common.collect.ImmutableSet;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.AssertionMapper;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.AssertionSupport;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.util.Pair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QuickStartAssertionLocatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private QuickStartAssertionLocator fixture;

    @Mock
    private EncapsulatedAssertionConfigManager encassConfigManager;
    @Mock
    private FolderManager folderManager;
    private Goid quickStartProvidedAssertionFolder = new Goid("ABABABABABABABABABABABABABABABAB");

    @Mock
    private AssertionRegistry assertionRegistry;
    private final Map<String, Assertion> assertionRegistryMap = new HashMap<>();

    @Mock
    private AssertionMapper assertionMapper;

    @Before
    public void setUp() {
        Mockito.doReturn(Collections.<String, AssertionSupport>emptyMap()).when(assertionMapper).getSupportedAssertions();
        fixture = new QuickStartAssertionLocator(encassConfigManager, assertionMapper, folderManager, quickStartProvidedAssertionFolder);
    }

    @Test
    public void findAssertion() throws Exception {
        final String validAssertionName = "ValidAssertionName";
        final Assertion validAssertion = new Assertion() {};
        fixture.setAssertionRegistry(assertionRegistry);
        when(assertionRegistry.findByExternalName(validAssertionName)).thenReturn(validAssertion);

        // assertion found
        assertNotNull(fixture.findAssertion(validAssertionName));

        // assertion not found
        assertNull(fixture.findAssertion("invalidAssertionName"));

        // don't mess with assertion registry's copy
        final Assertion validAssertionCopy = fixture.findAssertion(validAssertionName);
        assert validAssertionCopy != null;
        validAssertionCopy.setEnabled(false);
        validAssertionCopy.setAssertionComment(new Assertion.Comment());
        assertNotEquals(validAssertion.isEnabled(), validAssertionCopy.isEnabled());
        assertNotEquals(validAssertion.getAssertionComment(), validAssertionCopy.getAssertionComment());

        assertionRegistryMap.clear();
    }

    @Test
    public void findEncapsulatedAssertionShouldSucceedWhenFoundInFolder() throws Exception {
        final Pair<Folder, Folder> parentChildFolder = mockFolderHierarchy(0);
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(parentChildFolder.left);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig("SomeName", mock(Policy.class), parentChildFolder.right);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        assertThat(fixture.findEncapsulatedAssertion("SomeName").config().getName(), equalTo("SomeName"));
    }

    @Test
    public void findEncapsulatedAssertionShouldSucceedWhenFoundInDescendentOfFolder() throws Exception {
        final Pair<Folder, Folder> parentChildFolder = mockFolderHierarchy(3);
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(parentChildFolder.left);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig("SomeName", mock(Policy.class), parentChildFolder.right);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        assertThat(fixture.findEncapsulatedAssertion("SomeName").config().getName(), equalTo("SomeName"));
    }

    @Test
    public void findEncapsulatedAssertionShouldFailWhenNotFoundInDescendentOfFolder() throws Exception {
        final Pair<Folder, Folder> parentChildFolder = mockFolderHierarchy(-1);
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(parentChildFolder.left);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig("SomeName", mock(Policy.class), parentChildFolder.right);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        assertThat(fixture.findEncapsulatedAssertion("SomeName"), nullValue());
    }

    @Test(expected = FindException.class)
    public void findEncapsulatedAssertionShouldThrowOnFindException() throws Exception {
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenThrow(new FindException());
        fixture.findEncapsulatedAssertion("SomeName");
    }

    @Test(expected = IllegalStateException.class)
    public void findEncapsulatedAssertionShouldThrowOnMissingRootFolder() throws Exception {
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(null);
        fixture.findEncapsulatedAssertion("SomeName");
    }

    @Test
    public void findEncapsulatedAssertionsShouldReturnAssertionsInQuickStartFolderOnly() throws Exception {
        final Folder parentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(parentFolder);

        // This one is contained in the tree...
        final Folder containedFolder = mockPossibleChildFolder(parentFolder, 4);
        final EncapsulatedAssertionConfig quickStartConfig = mockEncapsulatedAssertionConfig("SomeName", mock(Policy.class), containedFolder);

        // This one is not contained in the tree...
        final Folder uncontainedFolder = mockPossibleChildFolder(parentFolder, -1);
        final EncapsulatedAssertionConfig nonQuickStartConfig = mockEncapsulatedAssertionConfig("OtherName", mock(Policy.class), uncontainedFolder);

        when(encassConfigManager.findAll()).thenReturn(ImmutableSet.of(nonQuickStartConfig, quickStartConfig));

        final Set<EncapsulatedAssertion> assertions = fixture.findEncapsulatedAssertions();
        assertThat(assertions.size(), equalTo(1));
        assertThat(assertions.iterator().next().config().getName(), equalTo("SomeName"));
    }

    @Test(expected = IllegalStateException.class)
    public void findEncapsulatedAssertionsShouldThrowOnMissingRootFolder() throws Exception {
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(null);
        fixture.findEncapsulatedAssertions();
    }

    // TODO: unit testing will follow
//    @Test
//    public void findSupportedAssertion() throws Exception {
//        // extremely abusing chaining pattern, well it is unit test :-)
//        QuickStartMapper.getSupportedAssertionNames().forEach(
//                name -> Mockito.doReturn(
//                            Optional.of(name)
//                                .map(s -> Optional.of(Mockito.mock(Assertion.class))
//                                        .map(a -> {
//                                            Mockito.doReturn(a).when(a).getCopy();
//                                            // todo add additional mocks for the assertion here is needed
//                                            return a;
//                                        })
//                                        .orElseThrow(() -> new RuntimeException("Cannot happen, but happened!!!!")))
//                                .map(ass -> {
//                                    assertionRegistryMap.put(name, ass);
//                                    return ass;
//                                })
//                                .orElseThrow(() -> new RuntimeException("Cannot happen, but happened!!!!"))
//                ).when(assertionRegistry).findByExternalName(name));
//        Assert.assertThat(assertionRegistryMap, MapKeysMatcher.mapKeysMatcher(QuickStartMapper.getSupportedAssertionNames()));
//
//        final Set<Assertion> assertions = fixture.findSupportedAssertions();
//        Assert.assertThat(assertions, Matchers.containsInAnyOrder(assertionRegistryMap.values().stream().toArray(Assertion[]::new)));
//    }
//
//    @Test
//    public void findSupportedAssertionShouldThrowOnUnknownAssertion() throws Exception {
//        expectedException.expect(FindException.class);
//        expectedException.expectMessage(L7Matchers.matchesRegex("(?s)Assertion with name.*cannot be found.*"));
//
//        Mockito.doReturn(null).when(assertionRegistry).findByExternalName(Mockito.anyString());
//        fixture.findSupportedAssertions();
//    }

    private Pair<Folder, Folder> mockFolderHierarchy(final int nestingLevel) {
        final Folder parentFolder = mock(Folder.class);
        final Folder possibleChildFolder = mockPossibleChildFolder(parentFolder, nestingLevel);
        return Pair.pair(parentFolder, possibleChildFolder);
    }

    private Folder mockPossibleChildFolder(final Folder parentFolder, final int nestingLevel) {
        final Folder possibleChildFolder = mock(Folder.class);
        when(parentFolder.getNesting(possibleChildFolder)).thenReturn(nestingLevel);
        return possibleChildFolder;
    }

    private EncapsulatedAssertionConfig mockEncapsulatedAssertionConfig(final String name, final Policy mockPolicy, final Folder mockFolder) {
        final EncapsulatedAssertionConfig config = mock(EncapsulatedAssertionConfig.class);
        when(config.getGoid()).thenReturn(new Goid("12345678901234567890123456789012"));
        when(config.getGuid()).thenReturn("12345678901234567890123456789012");
        when(config.getName()).thenReturn(name);
        when(config.getVersion()).thenReturn(42);
        if (mockPolicy != null) {
            when(config.getPolicy()).thenReturn(mockPolicy);
            if (mockFolder != null) {
                when(mockPolicy.getFolder()).thenReturn(mockFolder);
            }
        }
        return config;
    }

    /**
     * Custom Matcher to match set of keys
     */
    private static final class MapKeysMatcher<K> extends BaseMatcher<Map<K, ?>> {
        @NotNull
        private final Set<K> expectedKeys;

        private MapKeysMatcher(final Set<K> expectedKeys) {
            Assert.assertNotNull(expectedKeys);
            this.expectedKeys = Collections.unmodifiableSet(expectedKeys);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(toSortedSet(expectedKeys));
        }

        @Override
        public void describeMismatch(final Object item, final Description description) {
            description.appendText("was ");
            if (item instanceof Map) {
                //noinspection unchecked
                description.appendValue(toSortedMap((Map<K, ?>)item));
            } else {
                description.appendValue(item);
            }
        }

        @Override
        public boolean matches(final Object obj) {
            Assert.assertNotNull(obj);
            Assert.assertThat(obj, Matchers.instanceOf(Map.class));
            //noinspection unchecked
            final Map<K, ?> mapToVerify = (Map<K, ?>) obj;
            return matches(mapToVerify.keySet(), expectedKeys);
        }

        private boolean matches(final Set<K> toVerify, final Set<K> expected) {
            return toVerify == expected || (toVerify != null && expected != null && toVerify.equals(expected));
        }

        private static <K> Set<K> toSortedSet(final Set<K> set) {
            if (set == null)
                return null;
            else if (set instanceof SortedSet)
                return set;
            return new TreeSet<>(set);
        }

        private static <K> Map<K, ?> toSortedMap(final Map<K, ?> map) {
            if (map == null)
                return null;
            else if (map instanceof SortedMap)
                return map;
            return new TreeMap<>(map);
        }

        private static <K> MapKeysMatcher<K> mapKeysMatcher(final Set<K> expectedKeys) {
            return new MapKeysMatcher<>(expectedKeys);
        }
    }
}