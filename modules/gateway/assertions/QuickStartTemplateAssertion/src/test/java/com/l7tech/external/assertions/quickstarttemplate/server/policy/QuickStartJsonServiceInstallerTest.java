package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Functions;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.Path;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickStartJsonServiceInstallerTest {

    @Rule
    public final TemporaryFolder jsonBootstrapFolder = new TemporaryFolder();

    @Mock
    private QuickStartServiceBuilder serviceBuilder;

    @Mock
    private ServiceManager serviceManager;

    @Mock
    private PolicyVersionManager policyVersionManager;

    private QuickStartJsonServiceInstaller serviceInstaller;

    @Before
    public void setUp() throws Exception {
        serviceInstaller = Mockito.spy(new QuickStartJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager));
        // mock our temporary folder as the bootstrap folder
        Mockito.doReturn(jsonBootstrapFolder.getRoot().toPath()).when(serviceInstaller).getBootstrapFolder();
    }

    @After
    public void tearDown() throws Exception {
        jsonBootstrapFolder.delete();
    }

    @Test
    public void test_installJsonServices_BootstrapIgnoresSubfolder() throws Exception {
        jsonBootstrapFolder.newFile("test1.json");
        jsonBootstrapFolder.newFile("test2.json");

        final File testFolder = jsonBootstrapFolder.newFolder("test");
        final File testJsonFile1 = new File(testFolder, "test_test1.json");
        Assert.assertTrue(testJsonFile1.createNewFile());
        final File testJsonFile2 = new File(testFolder, "test_test2.json");
        Assert.assertTrue(testJsonFile2.createNewFile());

        doMockInstallJsonService(path -> { /* DO NOTHING */ });
        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).installJsonService(Mockito.any(Path.class));
    }

    @Test
    public void test_installJsonServices_BootstrapFilesAreSorted() throws Exception {
        final File file1 = jsonBootstrapFolder.newFile("test2.json");
        final File file2 = jsonBootstrapFolder.newFile("test1.json");
        final File file3 = jsonBootstrapFolder.newFile("test4.json");
        final File file4 = jsonBootstrapFolder.newFile("test3.json");

        final File testFolder = jsonBootstrapFolder.newFolder("test");
        final File testJsonFile1 = new File(testFolder, "test_test1.json");
        Assert.assertTrue(testJsonFile1.createNewFile());
        final File testJsonFile2 = new File(testFolder, "test_test2.json");
        Assert.assertTrue(testJsonFile2.createNewFile());

        doMockInstallJsonService(path -> { /* DO NOTHING */ });
        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(4)).installJsonService(Mockito.any(Path.class));
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file2.toPath()));
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file1.toPath()));
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file4.toPath()));
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file3.toPath()));
    }

    @Test
    public void test_installJsonServices_BootstrapIgnoresNonJsonFiles() throws Exception {
        jsonBootstrapFolder.newFile("test1.json");
        jsonBootstrapFolder.newFile("test2.json");
        jsonBootstrapFolder.newFile("test3json");
        jsonBootstrapFolder.newFile("test.json.blah");
        jsonBootstrapFolder.newFile("testjson.blah");
        jsonBootstrapFolder.newFile("test.jsonblah");
        jsonBootstrapFolder.newFile("test.j.s.o.n");

        final File testFolder = jsonBootstrapFolder.newFolder("test");
        final File testJsonFile1 = new File(testFolder, "test_test1.json");
        Assert.assertTrue(testJsonFile1.createNewFile());
        final File testJsonFile2 = new File(testFolder, "test_test2.json");
        Assert.assertTrue(testJsonFile2.createNewFile());
        final File testNonJsonFile = new File(testFolder, "test_test_blah.blah");
        Assert.assertTrue(testNonJsonFile.createNewFile());

        doMockInstallJsonService(path -> { /* DO NOTHING */ });
        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).installJsonService(Mockito.any(Path.class));
    }

    private void doMockInstallJsonService(final Functions.UnaryVoid<Path> callable) {
        Assert.assertNotNull(callable);

        Mockito.doAnswer(invocation -> {
            Assert.assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
            final Object param1 = invocation.getArguments()[0];
            Assert.assertThat("First Param is Path", param1, Matchers.instanceOf(Path.class));
            final Path path = (Path)param1;
            Assert.assertThat(path, Matchers.notNullValue());
            // get the module from our repository
            callable.call(path);
            return null;
        }).when(serviceInstaller).installJsonService(Mockito.any(Path.class));
    }
}