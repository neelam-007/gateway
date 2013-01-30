package com.l7tech.external.assertions.ssh.server;

import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MessageProcessingSftpCommonCallsTest extends AbstractMessageProcessingSftpTest {

    /********************************** TEST INIT ********************************/
    /* Test to make sure only version 3 of the protocol is allowed */
    @Test
    public void testInit() throws IOException {
        Buffer buff = new Buffer();
        buff.putInt(5L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_INIT);
        buff.putInt(3L);

        sftpSubsystem.process(buff);

        Buffer rtn = new Buffer(out.toByteArray());
        int length = rtn.getInt();
        byte type = rtn.getByte();
        int version = rtn.getInt();

        Assert.assertEquals(SftpSubsystem.SSH_FXP_VERSION, type);
        Assert.assertEquals((byte)3, version);
    }

    //try connecting with the version 2 protocol. make sure an error is returned.
    @Test
    public void testInitv2() throws IOException {
        Buffer buff = new Buffer();
        buff.putInt(5L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_INIT);
        buff.putInt(2L);

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(2);

        Assert.assertEquals(SftpSubsystem.SSH_FX_OP_UNSUPPORTED, status.status);
    }

    //Try connecting with the version 6 protocol. Make sure the version 3 protocol is returned.
    @Test
    public void testInitv6() throws IOException {
        Buffer buff = new Buffer();
        buff.putInt(5L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_INIT);
        buff.putInt(6L);

        sftpSubsystem.process(buff);

        Buffer rtn = new Buffer(out.toByteArray());
        int length = rtn.getInt();
        byte type = rtn.getByte();
        int version = rtn.getInt();

        Assert.assertEquals(SftpSubsystem.SSH_FXP_VERSION, type);
        Assert.assertEquals((byte)3, version);
    }
    /****************************** END TEST INIT ********************************/

    //try to close a non existing file handle. Make sure an error is returned.
    @Test
    public void testCloseBadHandle() throws IOException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_CLOSE);
        buff.putInt(id);
        buff.putString("Bad_Handle");

        sftpSubsystem.process(buff);

        StatusReply status = getStatusReply(id);

        Assert.assertEquals(SftpSubsystem.SSH_FX_INVALID_HANDLE, status.status);
    }

    @Test
    public void testRealPath() throws IOException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_REALPATH);
        buff.putInt(id);
        buff.putString("myfile.xml");

        sftpSubsystem.process(buff);

        NameReply nameReply = getNameReply(id);

        Assert.assertEquals(1, nameReply.count);
        Assert.assertEquals("/myfile.xml", nameReply.names[0].filename);
        Assert.assertEquals(0, nameReply.names[0].attrs.flags);
    }

    @Test
    public void testRealPathRoot() throws IOException {
        int id = 0;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_REALPATH);
        buff.putInt(id);
        buff.putString("");

        sftpSubsystem.process(buff);

        NameReply nameReply = getNameReply(id);

        Assert.assertEquals(1, nameReply.count);
        Assert.assertEquals("/", nameReply.names[0].filename);
        Assert.assertEquals(0, nameReply.names[0].attrs.flags);
    }
}
