package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashSet;
import java.util.Set;

/**
 * A abstract class for handling common tasks of User Panel. A subclass derived from this
 * abstract class will handle different type of users.
 * <p/>
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
abstract public class UserPanel extends EntityEditorPanel {
    final static String USER_ICON_RESOURCE = "com/l7tech/console/resources/user16.png";

    // user
    protected EntityHeader userHeader;
    protected UserBean user;
    protected Set userGroups;

    protected boolean formModified;
    protected IdentityProviderConfig config;
    protected final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();
    protected FormAuthorizationPreparer securityFormAuthorizationPreparer;

    abstract public boolean certExist();

    protected UserPanel() {
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        securityFormAuthorizationPreparer = new FormAuthorizationPreparer(provider, new String[]{Group.ADMIN_GROUP_NAME});
    }

    /**
     * Enables or disables the buttons based
     * on whether or not data on the form has been changed
     */
    void setModified(boolean b) {
        formModified = b;
    }

    /**
     * Constructs the panel
     *
     * @param userHeader
     * @param config
     */
    public void edit(EntityHeader userHeader, IdentityProviderConfig config) {
        this.config = config;
        edit(userHeader);
    }

    /**
     * Retrieve the <code>USer</code> this panel is editing.
     * It is a convenience, and package private method, for
     * interested panels.
     *
     * @return the user that this panel is currently editing
     */
    UserBean getUser() {
        return user;
    }

    Set getUserGroups() {
        if (userGroups == null) userGroups = new HashSet();
        return userGroups;
    }

    public IdentityProviderConfig getProviderConfig() {
        return config;
    }


    /**
     * A listener to detect when Document components have changed. Once this is
     * done a flag is set to ensure that the apply changes/ revert buttons are
     * enabled.
     */
    protected final DocumentListener documentListener = new DocumentListener() {
        /**
         * Gives notification that there was an insert into the document.
         */
        public void insertUpdate(DocumentEvent e) {
            setModified(true);
        }

        /**
         * Gives notification that a portion of the document has been
         */
        public void removeUpdate(DocumentEvent e) {
            setModified(true);
        }

        /**
         * Gives notification that an attribute or set of attributes changed.
         */
        public void changedUpdate(DocumentEvent e) {
            setModified(true);
        }
    };

}


