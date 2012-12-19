package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.sftp.SftpFile;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.*;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static org.mockito.Mockito.mock;

/**
 * This was created: 12/12/12 as 5:43 PM
 *
 * @author Victor Kazakov
 */
@RunWith(Parameterized.class)
public class SftpPollingListenerTest {

    @Mock
    private Sftp sftp;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SecurePasswordManager securePasswordManager;

    private static com.jscape.inet.sftp.SftpClient sftpClient = mock(com.jscape.inet.sftp.SftpClient.class);

    SftpPollingListener sftpPollingListener;
    private List<SftpFile> fileList;
    private List<SftpFile> expectedList;
    private String regularExpression;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        Arrays.asList(new MockSftpFile("testFile.xml")),
                        Arrays.asList(new MockSftpFile("testFile.xml")),
                        null
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("anotherFile.txt"), new MockSftpFile("noExtension"), new MockSftpFile(".hidden")),
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("anotherFile.txt"), new MockSftpFile("noExtension"), new MockSftpFile(".hidden")),
                        null
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml.processing")),
                        Collections.<SftpFile>emptyList(),
                        null
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile.xml.processing"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX"), new MockSftpFile("fileX.response"),
                                new MockSftpFile("fileZ"), new MockSftpFile("fileZ.processed")),
                        Arrays.asList(new MockSftpFile("testFile.xml"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX")),
                        null
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile(".response"), new MockSftpFile(".processing"), new MockSftpFile("..processed"), new MockSftpFile("testFile.processed.processing"), new MockSftpFile("testFile2.processed.processed"), new MockSftpFile("testFile2"), new MockSftpFile("testFile.processed.response"), new MockSftpFile("testFileA.response.xml"), new MockSftpFile("testFileB.processed.xml")),
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile2"), new MockSftpFile("testFileA.response.xml"), new MockSftpFile("testFileB.processed.xml")),
                        null
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml")),
                        Arrays.asList(new MockSftpFile("testFile.xml")),
                        ".*"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("anotherFile.txt"), new MockSftpFile("noExtension"), new MockSftpFile(".hidden")),
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("anotherFile.txt"), new MockSftpFile("noExtension"), new MockSftpFile(".hidden")),
                        ".*"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml.processing")),
                        Collections.<SftpFile>emptyList(),
                        ".*"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile.xml.processing"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX"), new MockSftpFile("fileX.response"),
                                new MockSftpFile("fileZ"), new MockSftpFile("fileZ.processed")),
                        Arrays.asList(new MockSftpFile("testFile.xml"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX")),
                        ".*"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile(".response"), new MockSftpFile(".processing"), new MockSftpFile("..processed"), new MockSftpFile("testFile.processed.processing"), new MockSftpFile("testFile2.processed.processed"), new MockSftpFile("testFile2"), new MockSftpFile("testFile.processed.response"), new MockSftpFile("testFileA.response.xml"), new MockSftpFile("testFileB.processed.xml")),
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile2"), new MockSftpFile("testFileA.response.xml"), new MockSftpFile("testFileB.processed.xml")),
                        ".*"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile.xml.processing"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX"), new MockSftpFile("fileX.response"),
                                new MockSftpFile("fileZ"), new MockSftpFile("fileZ.processed")),
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile")),
                        "test.*"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile.xml.processing"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX"), new MockSftpFile("fileX.response"),
                                new MockSftpFile("fileZ.xml"), new MockSftpFile("fileZ.processed")),
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("fileZ.xml")),
                        ".*\\.xml"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile.xml.processing"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX"), new MockSftpFile("fileX.response"),
                                new MockSftpFile("fileZ"), new MockSftpFile("fileZ.processed")),
                        Collections.<SftpFile>emptyList(),
                        "fileZ"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml"), new MockSftpFile("testFile.xml.processing"),
                                new MockSftpFile("anotherFile"), new MockSftpFile("testFile"), new MockSftpFile("fileX"), new MockSftpFile("fileX.response"),
                                new MockSftpFile("fileZ"), new MockSftpFile("fileZ.processed")),
                        Arrays.asList(new MockSftpFile("fileX")),
                        "fileX"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile01.xml"), new MockSftpFile("testFile01.xml.processing"),
                                new MockSftpFile("testFile02.xml"), new MockSftpFile("testFile02.xml.processed"),
                                new MockSftpFile("testFile03.xml"), new MockSftpFile("testFile03.xml.response"),
                                new MockSftpFile("testFile24.xml"), new MockSftpFile("testFile14.xml"),
                                new MockSftpFile("testFile67.xml"), new MockSftpFile("testFiles23.xml"),
                                new MockSftpFile("testFile99.xml"), new MockSftpFile("testFile99.xml.processed"),
                                new MockSftpFile("testFile100.xml"), new MockSftpFile("testFile765.xml"),
                                new MockSftpFile("testFiles08.xml"), new MockSftpFile("xtestFile08.xml"),
                                new MockSftpFile("testFile09.xmls"), new MockSftpFile("testFile09\\.xml"),
                                new MockSftpFile("testFile4.xml"), new MockSftpFile("testFile14.xml")),
                        //Note, I know testFile14.xml is there twice!!!
                        Arrays.asList(new MockSftpFile("testFile01.xml"), new MockSftpFile("testFile03.xml"),
                                new MockSftpFile("testFile24.xml"), new MockSftpFile("testFile14.xml"),
                                new MockSftpFile("testFile67.xml"), new MockSftpFile("testFile14.xml")),
                        "testFile(\\d\\d)\\.xml"
                },
                {
                        Arrays.asList(new MockSftpFile("testFile.xml", true), new MockSftpFile("testFile.xml")),
                        Arrays.asList(new MockSftpFile("testFile.xml")),
                        null
                },
                {
                        Arrays.asList(new MockSftpFile("testFile", true), new MockSftpFile("testFile.xml")),
                        Arrays.asList(new MockSftpFile("testFile.xml")),
                        null
                },
        });
    }

    public SftpPollingListenerTest(List<SftpFile> fileList, List<SftpFile> expectedList, String regularExpression) {
        this.fileList = fileList;
        this.expectedList = expectedList;
        this.regularExpression = regularExpression;
    }

    @Before
    public void setup() throws SftpPollingListenerConfigException {
        //Process mockito annotations
        MockitoAnnotations.initMocks(this);

        SsgActiveConnector ssgActiveConnector = new SsgActiveConnector();
        ssgActiveConnector.setProperty(PROPERTIES_KEY_SFTP_HOST, "host");
        ssgActiveConnector.setProperty(PROPERTIES_KEY_SFTP_PORT, "-1");
        ssgActiveConnector.setProperty(PROPERTIES_KEY_SFTP_DIRECTORY, "testDir");
        ssgActiveConnector.setProperty(PROPERTIES_KEY_SFTP_USERNAME, "testUser");
        ssgActiveConnector.setProperty(PROPERTIES_KEY_SFTP_FILE_NAME_PATTERN, regularExpression);

        sftpPollingListener = new SftpPollingListener(ssgActiveConnector, eventPublisher, securePasswordManager) {
            @Override
            void handleFile(SftpFile file) throws SftpPollingListenerException {
            }
        };
    }

    @Test
    public void scanDirectoryForFilesTest() throws IOException {
        mockGetDirListing(fileList);

        Collection<SftpFile> rtn = sftpPollingListener.scanDirectoryForFilesSetProcessing(sftp);

        compareCollections(expectedList, rtn);
    }

    private void mockGetDirListing(final List<SftpFile> fileListToReturn) throws SftpException {
        //use then answer instead of then return because Mockito caches the return, which is a problem with Enumerations
        Mockito.when(sftp.getDirListing()).thenAnswer(new Answer<Enumeration>() {
            @Override
            public Enumeration answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.enumeration(fileListToReturn);
            }
        });
    }

    private void compareCollections(Collection<SftpFile> expected, Collection<SftpFile> returned) {
        Assert.assertEquals("The returned file list size does not match the expected file list size", expected.size(), returned.size());
        for (SftpFile file : expected) {
            Assert.assertTrue("The returned list does not contain an expected file: " + file, returned.contains(file));
        }
    }

    public static class MockSftpFile extends SftpFile {
        private boolean isDirectory;
        private String fileName;
        private boolean exists;

        public MockSftpFile(String fileName) {
            this(fileName, false);
        }

        public MockSftpFile(String fileName, boolean isDirectory) {
            this(fileName, isDirectory, true);
        }

        public MockSftpFile(String fileName, boolean isDirectory, boolean exists) {
            super(fileName, sftpClient);
            this.fileName = fileName;
            this.isDirectory = isDirectory;
            this.exists = exists;
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public boolean exists() {
            return exists;
        }

        @Override
        public boolean isDirectory() {
            return isDirectory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MockSftpFile that = (MockSftpFile) o;

            if (exists != that.exists) return false;
            if (isDirectory != that.isDirectory) return false;
            if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (isDirectory ? 1 : 0);
            result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
            result = 31 * result + (exists ? 1 : 0);
            return result;
        }
    }
}
