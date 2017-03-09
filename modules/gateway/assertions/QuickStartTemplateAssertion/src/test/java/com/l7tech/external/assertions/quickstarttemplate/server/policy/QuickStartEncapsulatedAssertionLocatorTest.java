package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.google.common.collect.Sets;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QuickStartEncapsulatedAssertionLocatorTest {

    private QuickStartEncapsulatedAssertionLocator fixture;

    @Mock
    private EncapsulatedAssertionConfigManager encassConfigManager;
    @Mock
    private FolderManager folderManager;
    private Goid quickStartProvidedAssertionFolder = new Goid("ABABABABABABABABABABABABABABABAB");

    @Before
    public void setUp() {
        fixture = new QuickStartEncapsulatedAssertionLocator(encassConfigManager, folderManager, quickStartProvidedAssertionFolder);
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

        when(encassConfigManager.findAll()).thenReturn(Sets.newHashSet(nonQuickStartConfig, quickStartConfig));

        final Set<EncapsulatedAssertion> assertions = fixture.findEncapsulatedAssertions();
        assertThat(assertions.size(), equalTo(1));
        assertThat(assertions.iterator().next().config().getName(), equalTo("SomeName"));
    }

    @Test(expected = IllegalStateException.class)
    public void findEncapsulatedAssertionsShouldThrowOnMissingRootFolder() throws Exception {
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(null);
        fixture.findEncapsulatedAssertions();
    }

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

}