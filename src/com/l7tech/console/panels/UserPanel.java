package com.l7tech.console.panels;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.Set;
import java.util.HashSet;

/**
 * A abstract class for handling common tasks of User Panel. A subclass derived from this
 * abstract class will handle different type of users.
 * 
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
abstract public class UserPanel extends EntityEditorPanel {
    final static String USER_ICON_RESOURCE = "com/l7tech/console/resources/user16.png";
    private final String USER_DOES_NOT_EXIST_MSG = "This user no longer exists";

    // user
    protected EntityHeader userHeader;
    protected UserBean user;
    protected Set userGroups;

    protected boolean formModified;
    protected IdentityProvider idProvider;
    protected final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();

    abstract public boolean certExist();

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
     * @param idProvider
     */
    public void edit(EntityHeader userHeader, IdentityProvider idProvider) {
        this.idProvider = idProvider;
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

    public IdentityProvider getProvider() {
        return idProvider;
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


