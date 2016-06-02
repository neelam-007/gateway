package com.l7tech.server.processcontroller;

import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class PatcherPropertiesTest {
    @Rule
    public TemporaryFolder patcherPropertiesFolder = new TemporaryFolder();

    PatcherProperties patcherProperties;

    @Before
    public void setUp() throws Exception {
        patcherProperties = new PatcherPropertiesImpl(patcherPropertiesFolder.newFolder());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAutoDelete() throws Exception {
        // make sure default is false
        Assert.assertThat(patcherProperties.getBooleanProperty(PatcherProperties.PROP_L7P_AUTO_DELETE, true), Matchers.is(false));
        Assert.assertThat(patcherProperties.getBooleanProperty(PatcherProperties.PROP_L7P_AUTO_DELETE, false), Matchers.is(false));

        Assert.assertThat(patcherProperties.setProperty(PatcherProperties.PROP_L7P_AUTO_DELETE, "true"), Matchers.equalToIgnoringCase("false"));
        Assert.assertThat(patcherProperties.getBooleanProperty(PatcherProperties.PROP_L7P_AUTO_DELETE, true), Matchers.is(true));
        Assert.assertThat(patcherProperties.getBooleanProperty(PatcherProperties.PROP_L7P_AUTO_DELETE, false), Matchers.is(true));
    }

    @Test
    public void testSetProperty() throws Exception {
        Assert.assertThat(patcherProperties.getBooleanProperty("some.test.property.blah", true), Matchers.is(true));
        Assert.assertThat(patcherProperties.getBooleanProperty("some.test.property.blah", false), Matchers.is(false));
        Assert.assertThat(patcherProperties.getIntProperty("some.test.property.blah", 12), Matchers.is(12));
        Assert.assertThat(patcherProperties.getIntProperty("some.test.property.blah", 0), Matchers.is(0));
        Assert.assertThat(patcherProperties.getIntProperty("some.test.property.blah", -5), Matchers.is(-5));

        Assert.assertThat(patcherProperties.setProperty("some.test.property.blah", "TRue"), Matchers.nullValue());
        Assert.assertThat(patcherProperties.getBooleanProperty("some.test.property.blah", true), Matchers.is(true));
        Assert.assertThat(patcherProperties.getBooleanProperty("some.test.property.blah", false), Matchers.is(true));
        Assert.assertThat(patcherProperties.getProperty("some.test.property.blah", "default"), Matchers.equalTo("TRue"));
    }
}