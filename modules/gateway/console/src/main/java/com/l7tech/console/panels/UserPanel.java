package com.l7tech.console.panels;

import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.IdentityHeader;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
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
    public final static String USER_ICON_RESOURCE = "com/l7tech/console/resources/user16.png";

    // user
    protected IdentityHeader userHeader;
    protected User user;
    protected Set<IdentityHeader> userGroups;

    protected boolean formModified;
    protected IdentityProviderConfig config;
    protected final Frame topParent = TopComponents.getInstance().getTopParent();

    private final PermissionFlags userFlags;
    private final PermissionFlags groupFlags;
    abstract public boolean certExist();

    protected UserPanel() {
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        userFlags = PermissionFlags.get(EntityType.USER);
        groupFlags = PermissionFlags.get(EntityType.GROUP);
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
    public void edit(IdentityHeader userHeader, IdentityProviderConfig config) {
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
    User getUser() {
        return user;
    }

    Set<IdentityHeader> getUserGroups() {
        if (userGroups == null) userGroups = new HashSet<IdentityHeader>();
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

    protected PermissionFlags getUserFlags() {
        return userFlags;
    }

    protected PermissionFlags getGroupFlags() {
        return groupFlags;
    }

}


