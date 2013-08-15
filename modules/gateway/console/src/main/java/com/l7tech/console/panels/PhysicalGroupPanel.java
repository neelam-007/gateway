/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.Group;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author alex
 */
public class PhysicalGroupPanel extends GroupPanel {
    protected PhysicalGroupPanel( IdentityProviderConfig ipc ) {
        super( ipc );
    }

    protected void loadedGroup(Group g) throws FindException {
        groupMembers = getIdentityAdmin().getUserHeaders(config.getGoid(), group.getId());
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

    Set<IdentityHeader> getGroupMembers() {
        if (groupMembers == null) groupMembers = new HashSet<IdentityHeader>();
        return groupMembers;
    }

    protected String save() throws SaveException, UpdateException, ObjectNotFoundException {
        return getIdentityAdmin().saveGroup(config.getGoid(), group, groupMembers);
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
              new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(10, 15, 0, 0), 0, 0));

            detailsPanel.add(getEnabledCheckBox(),
              new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(10, 15, 0, 10), 0, 0));

            detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
              new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(10, 10, 0, 10), 0, 0));


            detailsPanel.add(getDescriptionLabel(),
              new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(10, 10, 0, 0), 0, 0));

            detailsPanel.add(getDescriptionTextField(),
              new GridBagConstraints(1, 3, 3, 1, 1.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(10, 15, 0, 10), 0, 0));


            detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
              new GridBagConstraints(0, 11, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(15, 10, 0, 10), 0, 0));

            Component strut = Box.createVerticalStrut(8);

            detailsPanel.add(strut,
              new GridBagConstraints(0, 12, 3, 1, 1.0, 1.0,
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

    /**
     * @return enable checkbox
     */
    private JCheckBox getEnabledCheckBox(){
        // If scroll pane not already created
        if(enabledCheckBox != null) return enabledCheckBox;

        // create
        enabledCheckBox = new JCheckBox("Enabled");

        if (group instanceof InternalGroup)
        {
            InternalGroup ig = (InternalGroup)group;

            enabledCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setModified(true);
                }
            });
            enabledCheckBox.setSelected(ig.isEnabled());
            enabledCheckBox.setEnabled(config.isWritable() && canUpdate);


            return enabledCheckBox;
        }
        enabledCheckBox.setVisible(false);
        return enabledCheckBox;
    }
    protected JPanel getMembershipPanel() {
        if(usersPanel != null) return usersPanel;

        usersPanel = new GroupUsersPanel(this, config, config.isWritable() && canUpdate);

        return usersPanel;
    }

    @Override
    protected Group collectChanges() {
        if (group instanceof PersistentGroup) {
            PersistentGroup pg = (PersistentGroup) group;
            pg.setDescription(getDescriptionTextField().getText());
        }
        if (group instanceof InternalGroup){
            InternalGroup ig = (InternalGroup) group;
            ig.setEnabled(getEnabledCheckBox().isSelected());
        }
        return group;
    }

    private JPanel detailsPanel;
    private GroupUsersPanel usersPanel; // membership
    private JCheckBox enabledCheckBox;

    private Set<IdentityHeader> groupMembers;
}
