package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.notNull;
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
    private Goid userProvidedAssertionFolder = new Goid("12121212121212121212121212121212");

    @Before
    public void setUp() {
        fixture = new QuickStartEncapsulatedAssertionLocator(encassConfigManager, folderManager, quickStartProvidedAssertionFolder, userProvidedAssertionFolder);
    }

    @Test
    public void findEncapsulatedAssertionShouldSucceedWhenFoundInUserFolder() throws Exception {
        final Folder parentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(userProvidedAssertionFolder)).thenReturn(parentFolder);
        final Folder containingFolder = mock(Folder.class);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig(mock(Policy.class), containingFolder);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        when(parentFolder.getNesting(containingFolder)).thenReturn(0);
        assertThat(fixture.findEncasulatedAssertion("SomeName"), notNullValue());
    }

    @Test
    public void findEncapsulatedAssertionShouldSucceedWhenFoundInDescendentOfUserFolder() throws Exception {
        final Folder parentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(userProvidedAssertionFolder)).thenReturn(parentFolder);
        final Folder containingFolder = mock(Folder.class);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig(mock(Policy.class), containingFolder);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        when(parentFolder.getNesting(containingFolder)).thenReturn(3);
        assertThat(fixture.findEncasulatedAssertion("SomeName"), notNullValue());
    }

    @Test
    public void findEncapsulatedAssertionShouldSucceedWhenFoundInDescendentOfQuickStartFolder() throws Exception {
        final Folder userParentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(userProvidedAssertionFolder)).thenReturn(userParentFolder);
        final Folder quickStartParentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(quickStartParentFolder);
        final Folder containingFolder = mock(Folder.class);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig(mock(Policy.class), containingFolder);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        when(userParentFolder.getNesting(containingFolder)).thenReturn(-1);
        when(quickStartParentFolder.getNesting(containingFolder)).thenReturn(3);
        assertThat(fixture.findEncasulatedAssertion("SomeName"), notNullValue());
    }

    @Test
    public void findEncapsulatedAssertionShouldFailWhenNotFoundInDescendentOfEitherFolder() throws Exception {
        final Folder userParentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(userProvidedAssertionFolder)).thenReturn(userParentFolder);
        final Folder quickStartParentFolder = mock(Folder.class);
        when(folderManager.findByPrimaryKey(quickStartProvidedAssertionFolder)).thenReturn(quickStartParentFolder);
        final Folder containingFolder = mock(Folder.class);
        final EncapsulatedAssertionConfig config = mockEncapsulatedAssertionConfig(mock(Policy.class), containingFolder);
        when(encassConfigManager.findByUniqueName("SomeName")).thenReturn(config);
        when(userParentFolder.getNesting(containingFolder)).thenReturn(-1);
        when(quickStartParentFolder.getNesting(containingFolder)).thenReturn(-1);
        assertThat(fixture.findEncasulatedAssertion("SomeName"), nullValue());
    }

    @Test(expected = FindException.class)
    public void findEncapsulatedAssertionShouldThrowOnFindException() throws Exception {
        when(folderManager.findByPrimaryKey(userProvidedAssertionFolder)).thenThrow(new FindException());
        fixture.findEncasulatedAssertion("SomeName");
    }

    @Test(expected = IllegalStateException.class)
    public void findEncapsulatedAssertionShouldThrowOnMissingRootFolder() throws Exception {
        when(folderManager.findByPrimaryKey(userProvidedAssertionFolder)).thenReturn(null);
        fixture.findEncasulatedAssertion("SomeName");
    }

    private EncapsulatedAssertionConfig mockEncapsulatedAssertionConfig(final Policy mockPolicy, final Folder mockFolder) {
        final EncapsulatedAssertionConfig config = mock(EncapsulatedAssertionConfig.class);
        when(config.getGoid()).thenReturn(new Goid("12345678901234567890123456789012"));
        when(config.getGuid()).thenReturn("12345678901234567890123456789012");
        when(config.getName()).thenReturn("SomeName");
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