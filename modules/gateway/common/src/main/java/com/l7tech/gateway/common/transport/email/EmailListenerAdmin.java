package com.l7tech.gateway.common.transport.email;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.EMAIL_LISTENER;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing {@link EmailListener} instances on the Gateway.
 * @author Norman
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=EMAIL_LISTENER)
@Administrative
public interface EmailListenerAdmin {
    public static class IMAPFolder implements Serializable {
        private String name;
        private String path;
        private List<IMAPFolder> children;

        public IMAPFolder(String name, String path) {
            this.name = name;
            this.path = path;
            children = new ArrayList<IMAPFolder>();
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public List<IMAPFolder> getChildren() {
            return children;
        }

        public void addChild(IMAPFolder child) {
            children.add(child);
        }
    }

    /**
     * Finds a particular {@link EmailListener} with the specified GOID, or null if no such policy can be found.
     * @param goid the GOID of the EmailListener to retrieve
     * @return the EmailListener with the specified GOID, or null if no such email listener can be found.
     */
    @Secured(stereotype=FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed = false)
    EmailListener findEmailListenerByPrimaryKey(Goid goid) throws FindException;

    /**
     * Retrieve all available email listeners.
     *
     * @return a List of EmailListener instances.  Never null.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_HEADERS)
    @Administrative(licensed=false)
    Collection<EmailListener> findAllEmailListeners() throws FindException;

    /**
     * Store the specified new or existing EmailListener. If the specified {@link EmailListener} contains a
     * unique GOID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     *
     * @param emailListener  the email listener to save.  Required.
     * @return the unique global object ID that was updated or created.
     * @throws com.l7tech.objectmodel.SaveException   if the requested information could not be saved
     * @throws com.l7tech.objectmodel.UpdateException if the requested information could not be updated for some other reason
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid saveEmailListener(EmailListener emailListener) throws SaveException, UpdateException;

    /**
     * Delete a specific email listener instance identified by its primary key.
     *
     * @param goid the global object ID of the email listener instance to delete.  Required.
     * @throws com.l7tech.objectmodel.DeleteException if there is some other problem deleting the object
     * @throws FindException if the object cannot be found
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deleteEmailListener(Goid goid) throws DeleteException, FindException;

    @Secured(stereotype=TEST_CONFIGURATION)
    boolean testEmailAccount(EmailServerType serverType,
                             String hostname,
                             int port,
                             String username,
                             String password,
                             boolean useSSL,
                             String folderName);

    @Secured(stereotype=SAVE)
    IMAPFolder getIMAPFolderList(String hostname, int port, String username, String password, boolean useSSL);
}
