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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class MessageProcessingSftpStatTest extends AbstractMessageProcessingSftpTest {

    @Test
    public void testStat() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("myfile.xml");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(false));

        sftpSubsystem.process(buff);

        AttrsReply attrsReply = getAttrsReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_SIZE | SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, attrsReply.attrs.flags);

        Assert.assertTrue("The returned file is not a file.", (attrsReply.attrs.permissions & 0100000) == 0100000);
    }

    @Test
    public void testStatRootDir() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("/");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(false));

        sftpSubsystem.process(buff);

        AttrsReply attrsReply = getAttrsReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, attrsReply.attrs.flags);

        Assert.assertTrue("The returned file is not a directory.", (attrsReply.attrs.permissions & 0040000) == 0040000);
    }

    @Test
     public void testStatLISTon() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("myfile.xml");

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

        AttrsReply attrsReply = getAttrsReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_SIZE | SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, attrsReply.attrs.flags);

        Assert.assertEquals(59, attrsReply.attrs.size);
        Assert.assertTrue("The returned file is not a file.", (attrsReply.attrs.permissions & 0100000) == 0100000);
        Assert.assertEquals(456789, attrsReply.attrs.atime);
        Assert.assertEquals(456789, attrsReply.attrs.mtime);
    }

    @Test
    public void testStatLISTOnDir() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("mydir");

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

        AttrsReply attrsReply = getAttrsReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, attrsReply.attrs.flags);

        Assert.assertTrue("The returned file is not a directory.", (attrsReply.attrs.permissions & 0040000) == 0040000);
        Assert.assertEquals(556789, attrsReply.attrs.atime);
        Assert.assertEquals(556789, attrsReply.attrs.mtime);
    }

    @Test
    public void testStatLISTOnNoFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("badFile.xml");

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

        StatusReply statusReply = getStatusReply(id);
        Assert.assertEquals(SftpSubsystem.SSH_FX_NO_SUCH_FILE, statusReply.status);

    }

    @Test
    public void testStatSTATOn() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("myfile.xml");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\" size=\"59\" lastModified=\"456789000\"/>" +
                "</files>").getBytes());
        pout.flush();
        pout.close();

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        AttrsReply attrsReply = getAttrsReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_SIZE | SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, attrsReply.attrs.flags);

        Assert.assertEquals(59, attrsReply.attrs.size);
        Assert.assertTrue("The returned file is not a file.", (attrsReply.attrs.permissions & 0100000) == 0100000);
        Assert.assertEquals(456789, attrsReply.attrs.atime);
        Assert.assertEquals(456789, attrsReply.attrs.mtime);
    }

    @Test
    public void testStatSTATOnDir() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("mydir");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(true));

        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(("<files>\n" +
                "    <file file=\"false\" name=\"mydir\" lastModified=\"556789000\"/>" +
                "</files>").getBytes());
        pout.flush();
        pout.close();

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        AttrsReply attrsReply = getAttrsReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS | SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME, attrsReply.attrs.flags);

        Assert.assertTrue("The returned file is not a directory.", (attrsReply.attrs.permissions & 0040000) == 0040000);
        Assert.assertEquals(556789, attrsReply.attrs.atime);
        Assert.assertEquals(556789, attrsReply.attrs.mtime);
    }

    @Test
    public void testStatSTATOnNoFile() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_STAT);
        buff.putInt((long) id);
        buff.putString("badFile.xml");

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(true));

        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(("<files></files>").getBytes());
        pout.flush();
        pout.close();

        mockMessageProcessing(AssertionStatus.NONE, pin);

        sftpSubsystem.process(buff);

        StatusReply statusReply = getStatusReply(id);
        Assert.assertEquals(SftpSubsystem.SSH_FX_NO_SUCH_FILE, statusReply.status);

    }

}
