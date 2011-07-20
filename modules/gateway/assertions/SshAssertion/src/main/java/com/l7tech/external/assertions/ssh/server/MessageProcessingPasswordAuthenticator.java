package com.l7tech.external.assertions.ssh.server;

import org.apache.commons.lang.StringUtils;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * Stores user name and password to pass to the Message Processor.
 */
public class MessageProcessingPasswordAuthenticator implements PasswordAuthenticator {

    private String userName;
    private String password;
    String[] authorizedUserPasswordKeys;

    public boolean authenticate(String userName, String password, ServerSession session) {
        this.userName = userName;
        this.password = password;

        // by default allow all access, defer authentication to Gateway policy assertion
       boolean isAllowedAccess = true;

       // if authorizedUserPasswordKeys list exists, perform authentication against it
       if (authorizedUserPasswordKeys != null && authorizedUserPasswordKeys.length > 0) {
           isAllowedAccess = false;
           for (String authorizedUserPasswordKey : authorizedUserPasswordKeys) {
               if (!StringUtils.isEmpty(authorizedUserPasswordKey) && authorizedUserPasswordKey.equals(password)) {
                   isAllowedAccess = true;
                   break;
               }
           }
       }

        return isAllowedAccess;
    }

    public String getUserName() {
        return userName;
    }
    public String getPassword() {
        return password;
    }
    public String[] getAuthorizedUserPasswordKeys() {
        return authorizedUserPasswordKeys;
    }
    public void setAuthorizedUserPasswordKeys(String[] authorizedUserPasswordKeys) {
        this.authorizedUserPasswordKeys = authorizedUserPasswordKeys;
    }
}
