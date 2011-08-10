package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.keyprovider.PemSshKeyUtil;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;

/**
 * Stores user public key for the Message Processor.
 */
public class MessageProcessingPublicKeyAuthenticator implements PublickeyAuthenticator {

    private String userName;
    PublicKey publicKey;
    String[] authorizedUserPublicKeys;

   public boolean authenticate(String username, PublicKey publicKey, ServerSession session) {
        this.userName = username;
        this.publicKey = publicKey;

       // fail by default, force user to password authenticate
       // ssh authentication order: public key authentication first, if it fails, then password authentication
       // public key and password are passed to the Gateway policy assertion to determine final authentication
       boolean isAllowedAccess = false;

       // if authorizedUserPublicKeys list exists, perform authentication against it
       if (authorizedUserPublicKeys != null && authorizedUserPublicKeys.length > 0 && publicKey != null) {
           String publicKeyString = PemSshKeyUtil.writeKey(publicKey);
           if (!StringUtils.isEmpty(publicKeyString)) {
               publicKeyString = publicKeyString.replace( SyspropUtil.getProperty( "line.separator" ), "" );
               for (String authorizedUserPublicKey : authorizedUserPublicKeys) {
                   if (publicKeyString.equals(authorizedUserPublicKey)) {
                       isAllowedAccess = true;
                       break;
                   }
               }
           }
       }

       return isAllowedAccess;
    }

    public String getUserName() {
        return userName;
    }
    public PublicKey getPublicKey() {
        return publicKey;
    }
    public String[] getAuthorizedUserPublicKeys() {
        return authorizedUserPublicKeys;
    }
    public void setAuthorizedUserPublicKeys(String[] authorizedUserPublicKeys) {
        this.authorizedUserPublicKeys = authorizedUserPublicKeys;
    }
}
