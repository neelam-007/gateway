package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * VirtualGroupPanel is the main entry point for Virtual Group Properties dialog.
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class VirtualGroupPanel extends GroupPanel {

    static Logger log = Logger.getLogger(VirtualGroupPanel.class.getName());

    private JPanel detailsPanel;
    private VirtualGroupDetailsPanel virtualGroupDetailsPanel = null;
    private VirtualGroupMembershipPanel virtualGroupMembershipPanel = null;

    private Set emptyMembers = Collections.EMPTY_SET;

    Set getGroupMembers() {
        return emptyMembers;
    }

    /**
     * constructor
     */
    protected VirtualGroupPanel(IdentityProviderConfig ipc) {
        super(ipc);
    }

    protected void loadedGroup( Group g ) {
        // do nothing - no group members will be shown
    }

    protected Group newGroup( EntityHeader groupHeader ) {
        VirtualGroup v = new VirtualGroup();
        v.setName(groupHeader.getName());
        return v;
    }

    IdentityProviderConfig getIdProviderConfig() {
        return config;
    }

    protected JPanel getMembershipPanel() {

        if(virtualGroupMembershipPanel != null) return virtualGroupMembershipPanel;

        // create the membership panel
        virtualGroupMembershipPanel = new VirtualGroupMembershipPanel();

        return virtualGroupMembershipPanel;
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

            detailsPanel.add(getVirtualGroupPanel(),
               new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(10, 10, 0, 10), 0, 0));

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

    private JPanel getVirtualGroupPanel() {

        if(virtualGroupDetailsPanel != null) return virtualGroupDetailsPanel;

        // create virtual group panel
        virtualGroupDetailsPanel = new VirtualGroupDetailsPanel();

        return virtualGroupDetailsPanel;
    }

    /**
     * Populates the form from the group bean
     *
     * @param group
     */
    protected void setData(Group group) {
        getNameLabel().setText(group.getName());
        virtualGroupDetailsPanel.getGroupDescTextField().setText(group.getDescription());
        virtualGroupDetailsPanel.getX509SubjectDNTextField().setText(((VirtualGroup) group).getX509SubjectDnPattern());
        virtualGroupDetailsPanel.getEmailTextField().setText(((VirtualGroup) group).getSamlEmailPattern());
        setModified(false);
    }


    /**
     * Collect changes from the form into the group instance.
     *
     * @return Group   the instance with changes applied
     */
    protected Group collectChanges() {
        VirtualGroup vGroup = (VirtualGroup) group;
        vGroup.getGroupBean().setDescription(virtualGroupDetailsPanel.getGroupDescTextField().getText());
        vGroup.setSamlEmailPattern(virtualGroupDetailsPanel.getEmailTextField().getText());
        vGroup.setX509SubjectDnPattern(virtualGroupDetailsPanel.getX509SubjectDNTextField().getText());
        return group;
    }

    protected String save() throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        return getIdentityAdmin().saveGroup(config.getOid(), group, emptyMembers);
    }
}



