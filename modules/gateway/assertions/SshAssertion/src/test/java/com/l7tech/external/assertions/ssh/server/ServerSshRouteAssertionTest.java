package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.HexUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Test SshRouteAssertion
 */
public class ServerSshRouteAssertionTest {
    private PolicyEnforcementContext context;
    private SshRouteAssertion assertion;


    private MessageTargetableSupport requestTarget = defaultRequestTarget();

    private MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    }

    @Before
    public void setUp() throws Exception {
        context = getContext();
        assertion = new SshRouteAssertion();
        assertion.setRequestTarget(requestTarget);
        assertion.setUsePublicKey(true);
        assertion.setDirectory("/home/ssgconfig");
        assertion.setFileNameSource(FtpFileNameSource.PATTERN);
        assertion.setFileNamePattern("test.txt");
        assertion.setHost("testmycompany.com");
        assertion.setPort("22");
        assertion.setUsePrivateKey(true);
        assertion.setTimeout(10000);
        assertion.setUsername("root");
    }

    private PolicyEnforcementContext getContext() {
            return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testSftpSshRSAPublicKey() throws Exception {
        assertTrue(validateHostKeyData(HostDataRSA + sshRsaPublicServerKeyData));
        assertFalse(validateHostKeyData(HostDataRSA + sshDsaPublicServerKeyData));
    }

    @Test
    public void testSftpSshDSAPublicKey() throws Exception {
        assertTrue(validateHostKeyData(HostDataDSA + sshDsaPublicServerKeyData));
        assertFalse(validateHostKeyData(HostDataDSA + sshRsaPublicServerKeyData));
    }

    @Test
    public void testSftpAssertionData() throws Exception {
        assertNotNull(assertion.getHost());
        assertNotNull(assertion.getPort());
        assertNotNull(assertion.getTimeout());
        assertNotNull(assertion.getUsername());
        assertNotNull(assertion.getFileNamePattern());
        assertNotNull(assertion.getFileNameSource());
        assertNotNull(assertion.getDirectory());
        assertTrue(assertion.isUsePrivateKey());
        assertTrue(assertion.isUsePublicKey());
    }

    private boolean validateHostKeyData(String chardata) {
        boolean isValid = false;
        try {
            Pattern p = Pattern.compile("(.*)\\s?(ssh-(dss|rsa))\\s+([a-zA-Z0-9+/]+={0,2})(?: .*|$)");
            Matcher m = p.matcher(chardata.trim());
            if(m.matches()) {
                String keyType = m.group(2);
                String keyText = m.group(4);
                byte[] key = HexUtils.decodeBase64(keyText, true);

                String decodedAlgorithmDesc = new String(key, 4, 7, "ISO8859_1");
                if (keyType.compareTo(decodedAlgorithmDesc) == 0){
                       isValid = true;
                } else {
                       isValid = false;
                }

            } else {
               isValid = false;
            }
        } catch (IOException e) {
            isValid = false;
        }

        return isValid;
    }

    private static final String sshRsaPublicServerKeyData = "AAAAB3NzaC1yc2EAAAABIwAAAQEAt9ac4kXWbTX1bAv9DjyGb3wxq/Hbf2dPs+v4jd5MlxEvqbDiQKS0MSjnSs1E3eVCh+lpL8H50q9zpsIaGS7lGxKVSWnKVVCD09g2XLSMob9jM+C/9aVvrxzZijLDlxRPc+Fdttpro6Lw8e6Vc4nFuzwvYo/hrzkFVLsmia19t/++UySh8AaBEvC3XnJYQvMF1NM6469MBLmd0xq5GzkkdoDQVh1kg9z8xydIJUpcb7qrS6XQItNY9GfV6km6dmoMw2BVwsiheD5S5YjVgaoqk0JletAPB0zfWfRmbY/+FzuHUHnMf4PHJjkk4vNIe/4sH93BNp2JazsJ3z/3KMvscw==";
    private static final String sshDsaPublicServerKeyData = "AAAAB3NzaC1kc3MAAACBALZJrosbjHQdvwxYyon5YS3mgviut5iCibrSgm0WqeUaeXLl0RSzpxxUrQzwFm3tXpDDh85NiibdBUiYy4cvmLBPtToaaqC0cwM8sojDcuJMO/hQQtruZrimmVY/SbC0MF3ohEpuyyYGQf108r4fwLRqlgZJu84/NnJLb0kz4Nh7AAAAFQCfkhVeUbMfXS9Kn77ouF1hkk68awAAAIBPB2qBvkzXNsiQzTmiC05FhfqCvVe3UlCpQbPE71YISCqIwQWZ+1beDOOMfKtXkr2Mb/s5ok92nujAgXYXS7ukIEpUy2e5CIe7RHQWjVLh4DsubnUlTGvafC4TNzmAdZmQkU1LKEts1LpYLy5VPZqRgzwCHS19Mr3rEVbgIpBJjgAAAIAKjtuWUbbjmpyNTrnxxCkLBEV3u4cvvApO3JNoukgG8n7JsHtuCNAfJSES1kttXdt8m9ps1ZJiUAUOfgUNm+oUi39gGsJfHkMYoCK2bVUrDJtbZeI/Nn6FGPDAC5YmJIl38+MbBBKeWsZWdVa8sDHF/BRZNU11o0fbRv2g1QjUKg==";

    private static final String HostDataRSA = "myhost.com ssh-rsa ";
    private static final String HostDataDSA = "myhost.com ssh-dss ";


}
