package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.sun.mail.pop3.POP3Store;
import com.sun.mail.imap.IMAPStore;

import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.Folder;
import javax.mail.MessagingException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * An implementation of the EmailListenerAdmin interface.
 */
public class EmailListenerAdminImpl implements EmailListenerAdmin {
    private static final Logger log = Logger.getLogger(EmailListenerAdminImpl.class.getName());

    private final EmailListenerManager emailListenerManager;
    private final String clusterNodeId;

    private static final String SOCKET_FACTORY_CLASSNAME = SslClientSocketFactory.class.getName();

    public EmailListenerAdminImpl(EmailListenerManager emailListenerManager, String clusterNodeId) {
        this.emailListenerManager = emailListenerManager;
        this.clusterNodeId = clusterNodeId;
    }

    public EmailListener findEmailListenerByPrimaryKey(long oid) throws FindException {
        return emailListenerManager.findByPrimaryKey(oid);
    }

    public Collection<EmailListener> findAllEmailListeners() throws FindException {
        return emailListenerManager.findAll();
    }

    public long saveEmailListener(EmailListener emailListener) throws SaveException, UpdateException {
        if (emailListener.getOid() == EmailListener.DEFAULT_OID) {
            emailListener.setOwnerNodeId(clusterNodeId);
            emailListener.setLastPollTime(System.currentTimeMillis());
            emailListener.setVersion(1);
            return emailListenerManager.save(emailListener);
        } else {
            emailListenerManager.update(emailListener);
            return emailListener.getOid();
        }
    }

    public void deleteEmailListener(long oid) throws DeleteException, FindException {
        emailListenerManager.delete(oid);
    }

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
                POP3Store store = (POP3Store)session.getStore(new URLName(useSSL ? "pop3s" : "pop3",
                                                                          hostname,
                                                                          port,
                                                                          null,
                                                                          username,
                                                                          password));
                store.connect();
                store.getFolder("INBOX");
                store.close();
                return true;
            } catch(Exception e) {
                log.warning("Testing email server \"" + username + "@" + hostname + "\", failed: " + e.getMessage());
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
                log.warning("Testing email server \"" + username + "@" + hostname + "\", failed: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

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
