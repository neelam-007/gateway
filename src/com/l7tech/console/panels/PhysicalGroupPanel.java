/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class PhysicalGroupPanel extends GroupPanel {
    protected PhysicalGroupPanel( IdentityProviderConfig ipc ) {
        super( ipc );
    }

    protected void loadedGroup( Group g ) throws RemoteException, FindException {
        groupMembers = getIdentityAdmin().getUserHeaders(config.getOid(), group.getUniqueIdentifier());
    }

    protected Group newGroup( EntityHeader groupHeader ) {
        return null;
    }

    public void initialize() {
        try {
            super.initialize();

            // Initialize form components
        } catch (Exception e) {
            log.log(Level.SEVERE, "GroupPanel()", e);
            e.printStackTrace();
        }
    }

    Set getGroupMembers() {
        if (groupMembers == null) groupMembers = new HashSet();
        return groupMembers;
    }

    protected String save() throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        return getIdentityAdmin().saveGroup(config.getOid(), group, groupMembers);
    }

    /**
     * Returns detailsPanel
     */
    protected JPanel getDetailsPanel() {
        // If panel not already created
        if (detailsPanel == null) {
            detailsPanel = new JPanel();
            detailsPanel.setLayout(new GridBagLayout());

            detailsPanel.add(new JLabel(new ImageIcon(Utilities.loadImage(GROUP_ICON_RESOURCE))),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 10, 0, 0), 0, 0));

            detailsPanel.add(getNameLabel(),
              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(10, 15, 0, 0), 0, 0));

            detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
              new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(10, 10, 0, 10), 0, 0));


            detailsPanel.add(getDescriptionLabel(),
              new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(10, 10, 0, 0), 0, 0));

            detailsPanel.add(getDescriptionTextField(),
              new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(10, 15, 0, 10), 0, 0));


            detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
              new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(15, 10, 0, 10), 0, 0));

            Component strut = Box.createVerticalStrut(8);

            detailsPanel.add(strut,
              new GridBagConstraints(0, 12, 2, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(10, 0, 0, 0), 0, 0));

            Utilities.equalizeLabelSizes(new JLabel[]{
                getDescriptionLabel(),
            });
        }
        // Return panel
        return detailsPanel;
    }

    protected JPanel getMembershipPanel() {
        if(usersPanel != null) return usersPanel;

        usersPanel = new GroupUsersPanel(this, config);

        return usersPanel;
    }


    private JPanel detailsPanel;
    private GroupUsersPanel usersPanel; // membership

    private Set groupMembers;
}
