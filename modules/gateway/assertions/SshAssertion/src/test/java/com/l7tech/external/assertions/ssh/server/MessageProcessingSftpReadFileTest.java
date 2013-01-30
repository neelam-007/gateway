package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessingSuspendedException;
import com.l7tech.server.MethodNotAllowedException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.util.Functions;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class MessageProcessingSftpReadFileTest extends AbstractMessageProcessingSftpTest {

    //Test putting a file when LIST and STAT are not enabled.
    @Test(timeout = 10000)
    public void testRead() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET, String.valueOf(true));

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);

        byte[] dataBytes = "My File Contents".getBytes();
        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READ);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putInt(dataBytes.length);

        final InputStream pin = createInputStream("My File Contents");
        mockMessageProcessing(AssertionStatus.NONE, pin);

        out.reset();
        sftpSubsystem.process(buff);
        pin.close();

        DataReply data = getDataReply(id);
        Assert.assertEquals(16, data.data.length);
        Assert.assertArrayEquals("My File Contents".getBytes(), data.data);

        closeHandle(id++, handle.handle);
    }

    @Test(timeout = 10000)
    public void testReadStreaming() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        byte[] dataBytes1 = "My File Contents".getBytes();
        byte[] dataBytes2 = "Some More File Contents".getBytes();

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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET, String.valueOf(true));

        sftpSubsystem.process(buff);

        HandleReply handle = getHandleReply(id);


        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(dataBytes1);
        mockMessageProcessing(AssertionStatus.NONE, pin);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READ);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putInt(dataBytes1.length);

        out.reset();
        sftpSubsystem.process(buff);

        DataReply data = getDataReply(id);
        Assert.assertEquals(dataBytes1.length, data.data.length);
        Assert.assertArrayEquals(dataBytes1, data.data);

        pout.write(dataBytes2);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READ);
        buff.putInt((long) ++id);
        buff.putString(handle.handle);
        buff.putLong(dataBytes1.length);
        buff.putInt(dataBytes2.length);

        out.reset();
        sftpSubsystem.process(buff);

        data = getDataReply(id);
        Assert.assertEquals(dataBytes2.length, data.data.length);
        Assert.assertArrayEquals(dataBytes2, data.data);

        pout.flush();
        pout.close();
        pin.close();
        closeHandle(id++, handle.handle);
    }

    //Test putting a file when LIST and STAT are not enabled.
    @Test(timeout = 10000)
    public void testReadLISTOn() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        byte[] dataBytes = "My File Contents".getBytes();
        final InputStream pin = createInputStream(new String(dataBytes));

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
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(false));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET, String.valueOf(true));

        mockMessageProcessing(new MessageProcessingProperties(AssertionStatus.NONE, createInputStream("<files>\n" +
                "    <file file=\"true\" name=\"myfile.xml\" size=\"" + dataBytes.length + "\" lastModified=\"456789000\"/>" +
                "    <file file=\"false\" name=\"mydir\" lastModified=\"556789000\"/>" +
                "</files>"), new NullOutputStream()), new MessageProcessingProperties(AssertionStatus.NONE, pin, new NullOutputStream()));

        sftpSubsystem.process(buff);


        HandleReply handle = getHandleReply(id);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READ);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putInt(dataBytes.length);

        out.reset();
        sftpSubsystem.process(buff);
        pin.close();

        DataReply data = getDataReply(id);
        Assert.assertEquals(16, data.data.length);
        Assert.assertArrayEquals("My File Contents".getBytes(), data.data);

        closeHandle(id++, handle.handle);
    }

    @Test(timeout = 10000)
    public void testReadPartialReadsOn() throws Throwable, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        final byte[] dataBytes = "My File Contents. Where You can read From multiple parts of the File.".getBytes();
        final InputStream pin1 = createInputStream(new String(dataBytes));
        final byte[] dataBytes2 = Arrays.copyOfRange(dataBytes, 10, 45);
        final InputStream pin2 = createInputStream(new String(dataBytes2));

        final AtomicReference<Throwable> messageProcessingAssertionFailure = new AtomicReference<Throwable>(null);

        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST, String.valueOf(false));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT, String.valueOf(false));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS, String.valueOf(true));
        connector.putProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET, String.valueOf(true));

        mockMessageProcessing(new MessageProcessingProperties(AssertionStatus.NONE, pin1, new NullOutputStream(), new Functions.UnaryVoid<PolicyEnforcementContext>() {
            @Override
            public void call(PolicyEnforcementContext policyEnforcementContext) {
                try {
                    String offset = (String) policyEnforcementContext.getVariable("request.command.parameter.offset");
                    Assert.assertEquals("0", offset);
                    String length = (String) policyEnforcementContext.getVariable("request.command.parameter.length");
                    Assert.assertEquals(String.valueOf(dataBytes.length), length);
                } catch (Throwable t) {
                    messageProcessingAssertionFailure.set(t);
                }
            }
        }),
                new MessageProcessingProperties(AssertionStatus.NONE, pin2, new NullOutputStream(), new Functions.UnaryVoid<PolicyEnforcementContext>() {
                    @Override
                    public void call(PolicyEnforcementContext policyEnforcementContext) {
                        try {
                            String offset = (String) policyEnforcementContext.getVariable("request.command.parameter.offset");
                            Assert.assertEquals("10", offset);
                            String length = (String) policyEnforcementContext.getVariable("request.command.parameter.length");
                            Assert.assertEquals(String.valueOf(dataBytes2.length), length);
                        } catch (Throwable t) {
                            messageProcessingAssertionFailure.set(t);
                        }
                    }
                }));


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

        sftpSubsystem.process(buff);


        HandleReply handle = getHandleReply(id);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READ);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(0);
        buff.putInt(dataBytes.length);

        out.reset();
        sftpSubsystem.process(buff);
        pin1.close();
        if (messageProcessingAssertionFailure.get() != null) {
            throw messageProcessingAssertionFailure.get();
        }

        DataReply data = getDataReply(id);
        Assert.assertEquals(dataBytes.length, data.data.length);
        Assert.assertArrayEquals(dataBytes, data.data);

        buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_READ);
        buff.putInt((long) id);
        buff.putString(handle.handle);
        buff.putLong(10);
        buff.putInt(dataBytes2.length);

        out.reset();
        sftpSubsystem.process(buff);
        pin2.close();
        if (messageProcessingAssertionFailure.get() != null) {
            throw messageProcessingAssertionFailure.get();
        }

        data = getDataReply(id);
        Assert.assertEquals(dataBytes2.length, data.data.length);
        Assert.assertArrayEquals(dataBytes2, data.data);

        closeHandle(id++, handle.handle);
    }
}
