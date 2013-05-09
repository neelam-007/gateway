package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test SshRouteAssertion
 */
public class SshRouteAssertionTest {
    private SshRouteAssertion assertion;

    private MessageTargetableSupport requestTarget = defaultRequestTarget();

    private MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    }

    @Before
    public void setUp() throws Exception {
        assertion = new SshRouteAssertion();
        assertion.setRequestTarget(requestTarget);
        assertion.setUsePublicKey(true);
        assertion.setDirectory("/home/ssgconfig");
        assertion.setFileName("testFileName.txt");
        assertion.setHost("testmycompany.com");
        assertion.setPort("22");
        assertion.setUsePrivateKey(true);
        assertion.setConnectTimeout(10);
        assertion.setReadTimeout(10);
        assertion.setUsername("root");
    }

    @Test
    public void testSshPublicKeyFingerprintValidation() throws Exception {
        assertFalse(SshKeyUtil.validateSshPublicKeyFingerprint("54:45:5e:ec:80:cb:d3:a8:01:a1:b0:7b:9f:82:80:9e").isSome());
        assertFalse(SshKeyUtil.validateSshPublicKeyFingerprint("95:53:22:67:42:28:54:0d:86:0e:98:73:05:3f:d1:84").isSome());
        assertFalse(SshKeyUtil.validateSshPublicKeyFingerprint("85:d9:2d:45:cf:50:ed:0f:1e:61:d5:38:9a:18:4d:c0").isSome());
        assertFalse(SshKeyUtil.validateSshPublicKeyFingerprint("${contextVariable}").isSome());
        assertTrue(SshKeyUtil.validateSshPublicKeyFingerprint("54:45:5e:zz:80:cb:d3:a8:01:a1:b0:7b:9f:82:80:9e").isSome());
        assertTrue(SshKeyUtil.validateSshPublicKeyFingerprint("54:45:5e:ec:80").isSome());
        assertTrue(SshKeyUtil.validateSshPublicKeyFingerprint("").isSome());
        assertTrue(SshKeyUtil.validateSshPublicKeyFingerprint(null).isSome());
    }

    @Test
    public void testSftpAssertionData() throws Exception {
        assertNotNull(assertion.getHost());
        assertNotNull(assertion.getPort());
        assertNotNull(assertion.getConnectTimeout());
        assertNotNull(assertion.getReadTimeout());
        assertNotNull(assertion.getUsername());
        assertNotNull(assertion.getFileName());
        assertNotNull(assertion.getDirectory());
        assertTrue(assertion.isUsePrivateKey());
        assertTrue(assertion.isUsePublicKey());
    }
}
