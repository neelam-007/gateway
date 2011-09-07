package com.l7tech.external.assertions.ssh.server;

import org.apache.commons.lang.StringUtils;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * Stores user name and password into session, passing to the Message Processor.
 */
public class MessageProcessingPasswordAuthenticator implements PasswordAuthenticator {
    public static final String ATTR_CRED_USERNAME = "com.l7tech.server.ssh.credential.username";
    public static final String ATTR_CRED_PASSWORD = "com.l7tech.server.ssh.credential.password";

    private final String[] authorizedUserPasswords;

    public MessageProcessingPasswordAuthenticator(final String[] authorizedUserPasswords) {
        this.authorizedUserPasswords = authorizedUserPasswords;
    }

    public boolean authenticate(String userName, String password, ServerSession session) {
        // by default allow all access, defer authentication to Gateway policy assertion
       boolean isAllowedAccess = true;

       // if authorizedUserPasswords list exists, perform authentication against it
       if (authorizedUserPasswords != null && authorizedUserPasswords.length > 0) {
           isAllowedAccess = false;
           for (String authorizedUserPassword : authorizedUserPasswords) {
               if (!StringUtils.isEmpty(authorizedUserPassword) && authorizedUserPassword.equals(password)) {
                   isAllowedAccess = true;
                   break;
               }
           }
       }

        if (isAllowedAccess) {
            session.getIoSession().setAttribute(ATTR_CRED_USERNAME, userName);
            session.getIoSession().setAttribute(ATTR_CRED_PASSWORD, password);
        } else {
            session.getIoSession().removeAttribute(ATTR_CRED_USERNAME);
            session.getIoSession().removeAttribute(ATTR_CRED_PASSWORD);
        }
        return isAllowedAccess;
    }
}
