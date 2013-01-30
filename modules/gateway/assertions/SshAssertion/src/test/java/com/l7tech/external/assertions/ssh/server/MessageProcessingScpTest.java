package com.l7tech.external.assertions.ssh.server;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessingSuspendedException;
import com.l7tech.server.MethodNotAllowedException;
import com.l7tech.server.policy.PolicyVersionException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This was created: 1/30/13 as 10:48 AM
 *
 * @author Victor Kazakov
 */
public class MessageProcessingScpTest extends AbstractMessageProcessingScpCommandTest {

    @Test
    public void writeTest() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        String fileContents = "My File Contents";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(fileContents.getBytes());
        bout.write(0);
        InputStream in = createInputStream(bout);
        messageProcessingScpCommand.setInputStream(in);

        ByteArrayOutputStream processingOut = new ByteArrayOutputStream();
        mockMessageProcessing(AssertionStatus.NONE, processingOut);
        messageProcessingScpCommand.writeFile("C     " + fileContents.length() + " test.txt", new VirtualSshFile("/test.txt", true));

        String theString = processingOut.toString();
        Assert.assertEquals(fileContents, theString);
        processingOut.close();

        byte[] bytes = out.toByteArray();
        Assert.assertArrayEquals(new byte[]{0, 0}, bytes);
    }

    @Test
    public void readTest() throws IOException, MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {
        String fileContents = "My File Contents";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(0);
        bout.write(0);
        InputStream in = createInputStream(bout);
        messageProcessingScpCommand.setInputStream(in);

        final InputStream pin = createInputStream(fileContents);
        mockMessageProcessing(AssertionStatus.NONE, pin);
        messageProcessingScpCommand.readFile(new VirtualSshFile("/test.txt", true));
        pin.close();

        byte[] returned = out.toByteArray();
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(("C0644 "+fileContents.length()+" test.txt\n"+fileContents).getBytes());
        expected.write(0);
        Assert.assertArrayEquals(expected.toByteArray(), returned);
    }
}
