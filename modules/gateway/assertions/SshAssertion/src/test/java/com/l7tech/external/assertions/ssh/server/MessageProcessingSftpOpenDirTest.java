package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.EmptyInputStream;
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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class MessageProcessingSftpOpenDirTest extends AbstractMessageProcessingSftpTest {

    @Test
    public void testOpenDir() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPENDIR);
        buff.putInt((long) id);
        buff.putString("/");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\" size=\"59\" lastModified=\"456789000\"/>" +
                "    <file file=\"false\" name=\"mydir\" lastModified=\"556789000\"/>" +
                "</files>").getBytes());
        pout.flush();
        pout.close();

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handleReply = getHandleReply(id);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READDIR);
        buff.putInt((long) ++id);
        buff.putString(handleReply.handle);

        out.reset();
        sftpSubsystem.process(buff);

        NameReply nameReply = getNameReply(id);

        Assert.assertEquals(2, nameReply.count);
        Assert.assertEquals("myfile.xml", nameReply.names[0].filename);
        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_SIZE | SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, nameReply.names[0].attrs.flags);
        Assert.assertEquals(59, nameReply.names[0].attrs.size);
        Assert.assertTrue("The returned file is not a file.", (nameReply.names[0].attrs.permissions & 0100000) == 0100000);
        Assert.assertEquals(456789, nameReply.names[0].attrs.atime);
        Assert.assertEquals(456789, nameReply.names[0].attrs.mtime);

        Assert.assertEquals("mydir", nameReply.names[1].filename);
        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, nameReply.names[1].attrs.flags);
        Assert.assertTrue("The returned file is not a file.", (nameReply.names[1].attrs.permissions & 0040000) == 0040000);
        Assert.assertEquals(556789, nameReply.names[1].attrs.atime);
        Assert.assertEquals(556789, nameReply.names[1].attrs.mtime);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READDIR);
        buff.putInt((long) ++id);
        buff.putString(handleReply.handle);

        out.reset();
        sftpSubsystem.process(buff);

        StatusReply statusReply = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_EOF, statusReply.status);

        closeHandle(++id, handleReply.handle);
    }

    @Test
    public void testOpenDirFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPENDIR);
        buff.putInt((long) id);
        buff.putString("/myfile.xml");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        mockMessageProcessing(AssertionStatus.FAILED, new EmptyInputStream());

        sftpSubsystem.process(buff);

        StatusReply statusReply = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_FAILURE, statusReply.status);
    }

    @Test
    public void testOpenDirEmpty() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_OPENDIR);
        buff.putInt((long) id);
        buff.putString("/");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(("<files></files>").getBytes());
        pout.flush();
        pout.close();

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        HandleReply handleReply = getHandleReply(id);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READDIR);
        buff.putInt((long) ++id);
        buff.putString(handleReply.handle);

        out.reset();
        sftpSubsystem.process(buff);

        StatusReply statusReply = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_EOF, statusReply.status);

        closeHandle(++id, handleReply.handle);
    }
}
