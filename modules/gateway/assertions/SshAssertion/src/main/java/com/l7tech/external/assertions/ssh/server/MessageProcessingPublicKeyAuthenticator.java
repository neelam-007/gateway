package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;

/**
 * Stores user public key into session for the Message Processor.
 */
public class MessageProcessingPublicKeyAuthenticator implements PublickeyAuthenticator {
    public static final String ATTR_CRED_USERNAME = "com.l7tech.server.ssh.credential.username";
    public static final String ATTR_CRED_PUBLIC_KEY = "com.l7tech.server.ssh.credential.key";

    private final String[] authorizedUserPublicKeys;

    public MessageProcessingPublicKeyAuthenticator(final String[] authorizedUserPublicKeys) {
        this.authorizedUserPublicKeys = authorizedUserPublicKeys;
    }

    @Override
    public boolean authenticate(String userName, PublicKey publicKey, ServerSession session) {
        // by default allow all access, defer authentication to Gateway policy assertion
        boolean isAllowedAccess = true;

        // if authorizedUserPublicKeys list exists, perform authentication against it
        if (authorizedUserPublicKeys != null && authorizedUserPublicKeys.length > 0 && publicKey != null) {
            isAllowedAccess = false;
            String publicKeyString = SshKeyUtil.writeKey(publicKey);
            if (!StringUtils.isEmpty(publicKeyString)) {
                publicKeyString = publicKeyString.replace( SyspropUtil.getProperty("line.separator"), "" );
                for (String authorizedUserPublicKey : authorizedUserPublicKeys) {
                    if (publicKeyString.equals(authorizedUserPublicKey)) {
                        isAllowedAccess = true;
                        break;
                    }
                }
            }
        }

        if (isAllowedAccess) {
            session.getIoSession().setAttribute(ATTR_CRED_USERNAME, userName);

            String publicKeyStr = SshKeyUtil.writeKey(publicKey);
            if (!StringUtils.isEmpty(publicKeyStr)) {
                publicKeyStr = publicKeyStr.replace(SyspropUtil.getProperty("line.separator"), "");
            }
            session.getIoSession().setAttribute(ATTR_CRED_PUBLIC_KEY, publicKeyStr);
        } else {
            session.getIoSession().removeAttribute(ATTR_CRED_USERNAME);
            session.getIoSession().removeAttribute(ATTR_CRED_PUBLIC_KEY);
        }
        return isAllowedAccess;
    }
}
