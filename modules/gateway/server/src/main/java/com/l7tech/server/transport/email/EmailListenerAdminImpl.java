package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.variable.GatewaySecurePasswordReferenceExpander;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.util.ExceptionUtils;
import com.sun.mail.pop3.POP3Store;
import com.sun.mail.imap.IMAPStore;

import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.Folder;
import javax.mail.MessagingException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An implementation of the EmailListenerAdmin interface.
 */
public class EmailListenerAdminImpl implements EmailListenerAdmin {
    private static final Logger log = Logger.getLogger(EmailListenerAdminImpl.class.getName());

    private final EmailListenerManager emailListenerManager;
    private final String clusterNodeId;

    private static final String SOCKET_FACTORY_CLASSNAME = SslClientHostnameAwareSocketFactory.class.getName();

    public EmailListenerAdminImpl(EmailListenerManager emailListenerManager, String clusterNodeId) {
        this.emailListenerManager = emailListenerManager;
        this.clusterNodeId = clusterNodeId;
    }

    @Override
    public EmailListener findEmailListenerByPrimaryKey(Goid goid) throws FindException {
        return emailListenerManager.findByPrimaryKey(goid);
    }

    @Override
    public Collection<EmailListener> findAllEmailListeners() throws FindException {
        return emailListenerManager.findAll();
    }

    @Override
    public Goid saveEmailListener(EmailListener emailListener) throws SaveException, UpdateException {
        if (emailListener.getGoid().equals(EmailListener.DEFAULT_GOID)) {
            emailListener.createEmailListenerState(clusterNodeId, System.currentTimeMillis(), 0);
            return emailListenerManager.save(emailListener);
        } else {
            emailListenerManager.update(emailListener);
            return emailListener.getGoid();
        }
    }

    @Override
    public void deleteEmailListener(Goid goid) throws DeleteException, FindException {
        emailListenerManager.delete(goid);
    }

    @Override
    public boolean testEmailAccount(EmailServerType serverType,
                                    String hostname,
                                    int port,
                                    String username,
                                    String password,
                                    boolean useSSL,
                                    String folderName)
    {
        if(serverType == EmailServerType.POP3) {
            try {
                Properties props = new Properties();
                props.setProperty("mail.pop3.connectiontimeout", "30000");
                props.setProperty("mail.pop3.timeout", "30000");
                if(useSSL) {
                    props.setProperty("mail.pop3s.socketFactory.fallback", "false");
                    props.setProperty("mail.pop3s.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
                }
                Session session = Session.getInstance(props);
                final GatewaySecurePasswordReferenceExpander passwordExpander = new GatewaySecurePasswordReferenceExpander(new LoggingAudit(log));
                POP3Store store = (POP3Store)session.getStore(new URLName(useSSL ? "pop3s" : "pop3",
                                                                          hostname,
                                                                          port,
                                                                          null,
                                                                          username,
                                                                          new String(passwordExpander.expandPasswordReference(password))));
                store.connect();
                store.getFolder("INBOX");
                store.close();
                return true;
            } catch(Exception e) {
                log.log(Level.WARNING,
                        "Testing email server \"" + username + "@" + hostname + "\", failed: " + ExceptionUtils.getMessage(e),
                        ExceptionUtils.getDebugException(e));
                return false;
            }
        } else if(serverType == EmailServerType.IMAP) {
            try {
                Properties props = new Properties();
                props.setProperty("mail.imap.connectiontimeout", "30000");
                props.setProperty("mail.imap.timeout", "30000");
                if(useSSL) {
                    props.setProperty("mail.imaps.socketFactory.fallback", "false");
                    props.setProperty("mail.imaps.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
                }
                Session session = Session.getInstance(props);
                IMAPStore store = (IMAPStore)session.getStore(new URLName(useSSL ? "imaps" : "imap",
                                                                          hostname,
                                                                          port,
                                                                          null,
                                                                          username,
                                                                          password));
                store.connect();
                if(store.getFolder(folderName) != null) {
                    store.close();
                    return true;
                } else {
                    store.close();
                    return false;
                }
            } catch(Exception e) {
                log.log(Level.WARNING,
                        "Testing email server \"" + username + "@" + hostname + "\", failed: " + ExceptionUtils.getMessage(e),
                        ExceptionUtils.getDebugException(e));
                return false;
            }
        }

        return true;
    }

    @Override
    public IMAPFolder getIMAPFolderList(String hostname, int port, String username, String password, boolean useSSL) {
        try {
            Properties props = new Properties();
            props.setProperty("mail.imap.connectiontimeout", "30000");
            props.setProperty("mail.imap.timeout", "30000");
            if(useSSL) {
                props.setProperty("mail.imaps.socketFactory.fallback", "false");
                props.setProperty("mail.imaps.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            }
            Session session = Session.getInstance(props);
            IMAPStore store = (IMAPStore)session.getStore(new URLName(useSSL ? "imaps" : "imap",
                                                                      hostname,
                                                                      port,
                                                                      null,
                                                                      username,
                                                                      password));
            store.connect();
            IMAPFolder folder = getIMAPFolders(store.getDefaultFolder());
            store.close();
            return folder;
        } catch(Exception e) {
            return null;
        }
    }

    private IMAPFolder getIMAPFolders(Folder folder) throws MessagingException {
        IMAPFolder imapFolder = new IMAPFolder(folder.getName(), folder.getFullName());
        for(Folder child : folder.list()) {
            imapFolder.addChild(getIMAPFolders(child));
        }

        return imapFolder;
    }
}
