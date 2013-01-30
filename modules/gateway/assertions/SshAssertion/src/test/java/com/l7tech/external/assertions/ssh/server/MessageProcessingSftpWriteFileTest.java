package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessingSuspendedException;
import com.l7tech.server.MethodNotAllowedException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.IoUtils;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

public class MessageProcessingSftpWriteFileTest extends AbstractMessageProcessingSftpTest {

    //Test putting a file when LIST and STAT are not enabled.
    @Test(timeout = 10000)
    public void testWrite() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(true));

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        byte[] data = "My File Contents".getBytes();
        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putBytes(data);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mockMessageProcessing(AssertionStatus.NONE, bout);

        out.reset();
        sftpSubsystem.process(buff);

        closeHandle(id++, handle.handle);

        String theString = bout.toString();

        Assert.assertEquals("My File Contents", theString);
        bout.close();
    }

    //Test calling SSH_FXP_WRITE twice to make sure the data gets there.
    @Test(timeout = 10000)
    public void testWrite2Times() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        String contents1 = "My File Contents.";
        String contents2 = " More File Contents";
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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(true));

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        byte[] data = contents1.getBytes();
        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putBytes(data);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mockMessageProcessing(AssertionStatus.NONE, bout);

        out.reset();
        sftpSubsystem.process(buff);

        data = contents2.getBytes();
        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(contents1.length());
        buff.putBytes(data);

        out.reset();
        sftpSubsystem.process(buff);

        closeHandle(id++, handle.handle);

        String theString = bout.toString();

        Assert.assertEquals(contents1 + contents2, theString);
        bout.close();
    }

    // Call SSH_FXP_WRITE 2 but read the stream in between calls to confirm that streaming happens.
    @Test(timeout = 10000)
    public void testWrite2TimesTestStreaming() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        String contents1 = "My File Contents.";
        String contents2 = " More File Contents";
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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(true));

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        byte[] data = contents1.getBytes();
        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putBytes(data);

        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        mockMessageProcessing(AssertionStatus.NONE, pout);

        out.reset();
        sftpSubsystem.process(buff);

        byte [] dataOut = new byte[32];
        int length = pin.read(dataOut, 0, contents1.length());
        Assert.assertEquals(contents1.length(), length);
        Assert.assertEquals(contents1, new String(dataOut, 0, length));

        data = contents2.getBytes();
        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(contents1.length());
        buff.putBytes(data);

        out.reset();
        sftpSubsystem.process(buff);

        closeHandle(id++, handle.handle);

        dataOut = new byte[32];
        length = pin.read(dataOut, 0, contents2.length());
        Assert.assertEquals(contents2.length(), length);
        Assert.assertEquals(contents2, new String(dataOut, 0, length));

        pin.close();
        pout.close();
    }

    @Test(timeout = 10000)
    public void testWritePartialWritesOn() throws Throwable, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        final byte[] data = "My File Contents".getBytes();
        final byte[] data2 = "Some More file contents are here!".getBytes();

        final AtomicReference<Throwable> messageProcessingAssertionFailure = new AtomicReference<Throwable>(null);

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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS, String.valueOf(true));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT, String.valueOf(true));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
        MessageProcessingProperties messageProcessing1 = new MessageProcessingProperties(AssertionStatus.NONE, new EmptyInputStream(), bout, new Functions.UnaryVoid<PolicyEnforcementContext>() {
            @Override
            public void call(PolicyEnforcementContext policyEnforcementContext) {
                try{
                    String offset = (String)policyEnforcementContext.getVariable("request.command.parameter.offset");
                    Assert.assertEquals("0", offset);
                    String length = (String)policyEnforcementContext.getVariable("request.command.parameter.length");
                    Assert.assertEquals(String.valueOf(data.length), length);
                } catch (Throwable t){
                    messageProcessingAssertionFailure.set(t);
                }
            }
        });
        MessageProcessingProperties messageProcessing2 = new MessageProcessingProperties(AssertionStatus.NONE, new EmptyInputStream(), bout2, new Functions.UnaryVoid<PolicyEnforcementContext>() {
            @Override
            public void call(PolicyEnforcementContext policyEnforcementContext) {
                try{
                    String offset = (String)policyEnforcementContext.getVariable("request.command.parameter.offset");
                    Assert.assertEquals(String.valueOf(data.length), offset);
                    String length = (String)policyEnforcementContext.getVariable("request.command.parameter.length");
                    Assert.assertEquals(String.valueOf(data2.length), length);
                } catch (Throwable t){
                    messageProcessingAssertionFailure.set(t);
                }
            }
        });
        mockMessageProcessing(messageProcessing1,messageProcessing2);

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putBytes(data);

        out.reset();
        sftpSubsystem.process(buff);
        messageProcessing1.finishedProcessingLatch.await();
        if(messageProcessingAssertionFailure.get()!=null){
            throw messageProcessingAssertionFailure.get();
        }

        Assert.assertArrayEquals(data, bout.toByteArray());
        bout.close();

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_WRITE);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(data.length);
        buff.putBytes(data2);

        out.reset();
        sftpSubsystem.process(buff);
        messageProcessing2.finishedProcessingLatch.await();
        if(messageProcessingAssertionFailure.get()!=null){
            throw messageProcessingAssertionFailure.get();
        }

        Assert.assertArrayEquals(data2, bout2.toByteArray());
        bout2.close();

        closeHandle(id++, handle.handle);

    }
}
