package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessingSuspendedException;
import com.l7tech.server.MethodNotAllowedException;
import com.l7tech.server.policy.PolicyVersionException;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class MessageProcessingSftpOpenFileTest extends AbstractMessageProcessingSftpTest {

     //Test opening a file when LIST and STAT are not enabled. A file handle should be returned.
    @Test
    public void testOpen() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(false));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(false));

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    // Test opening a new nonexisting file when LIST are enabled. a file handle should be returned.
    @Test
    public void testOpenLISTOn() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile2.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    // Test opening an existing file with LIST enabled. A file handle should be returned.
    @Test
    public void testOpenLISTOnExistingFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    // Test opening a directory with LIST enabled. An error
    @Test
    public void testOpenLISTOnDirectoryFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("mydir");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "    <file file=\"false\" name=\"mydir\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_NO_SUCH_FILE, status.status);
    }

    //Test opening an existing file with LIST enabled. and given create and excl flags. This should return a file already exists status.
    @Test
    public void testOpenLISTOnExistingFileCreateAndExcl() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = SftpSubsystem.SSH_FXF_CREAT | SftpSubsystem.SSH_FXF_EXCL;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_FILE_ALREADY_EXISTS, status.status);
    }

    //Test opening an existing file with LIST enabled. and given create and trunk flags. This should return an OK status
    @Test
    public void testOpenLISTOnExistingFileCreateAndTrunk() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = SftpSubsystem.SSH_FXF_CREAT | SftpSubsystem.SSH_FXF_TRUNC;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    // Test opening a new nonexisting file when STAT are enabled. a file handle should be returned.
    @Test
    public void testOpenSTATOn() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final InputStream pin = createInputStream("<files></files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    // Test opening a directory with STAT enabled. An error
    @Test
    public void testOpenSTATOnDirectoryFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("mydir");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"false\" name=\"mydir\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_NO_SUCH_FILE, status.status);
    }

    // Test opening an existing file with STAT enabled. A file handle should be returned.
    @Test
    public void testOpenSTATOnExistingFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = 0;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    //Test opening an existing file with LIST enabled. and given create and excl flags. This should return a file already exists status.
    @Test
    public void testOpenSTATOnExistingFileCreateAndExcl() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = SftpSubsystem.SSH_FXF_CREAT | SftpSubsystem.SSH_FXF_EXCL;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_FILE_ALREADY_EXISTS, status.status);
    }

    //Test opening an existing file with LIST enabled. and given create and trunk flags. This should return OK status
    @Test
    public void testOpenSTATOnExistingFileCreateAndTrunk() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = SftpSubsystem.SSH_FXF_CREAT | SftpSubsystem.SSH_FXF_TRUNC;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    //test opening a file with append flag. PUT and partial uploads are enabled. A file handle should be returned
    @Test
    public void testOpenPUTOnExistingFileAppend() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = SftpSubsystem.SSH_FXF_APPEND;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(true));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        closeHandle(id++, handle.handle);
    }

    //test opening a file with append flag. PUT is disabled. Partial uploads are enabled. An error should be returned.
    @Test
    public void testOpenPUToffExistingFileAppendPartialFalse() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        int flags = SftpSubsystem.SSH_FXF_APPEND;
        int attrs = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPEN);
        buff.putInt((long) id);
        buff.putString("myfile.xml");
        buff.putInt((long) flags);
        buff.putInt((long) attrs);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(false));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS, String.valueOf(true));

        final InputStream pin = createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\"/>" +
                "</files>");

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_FAILURE, status.status);
    }
}
