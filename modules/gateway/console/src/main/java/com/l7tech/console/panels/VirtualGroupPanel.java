package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.common.io.CertUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.ResourceBundle;

/**
 * VirtualGroupPanel is the main entry point for Virtual Group Properties dialog.
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class VirtualGroupPanel extends GroupPanel<VirtualGroup> {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewVirtualGroupDialog");
    
    private JPanel detailsPanel;
    private VirtualGroupDetailsPanel virtualGroupDetailsPanel = null;
    private VirtualGroupMembershipPanel virtualGroupMembershipPanel = null;

    private Set<IdentityHeader> emptyMembers = Collections.emptySet();

    Set<IdentityHeader> getGroupMembers() {
        return emptyMembers;
    }

    /**
     * constructor
     */
    protected VirtualGroupPanel(IdentityProviderConfig ipc) {
        super(ipc);
    }

    protected void loadedGroup(Group g) {
        // do nothing - no group members will be shown
    }

    protected VirtualGroup newGroup(EntityHeader groupHeader) {
        return new VirtualGroup(config.getOid(), groupHeader.getName());
    }

    IdentityProviderConfig getIdentityProviderConfig() {
        return config;
    }

    protected JPanel getMembershipPanel() {

        if (virtualGroupMembershipPanel != null) return virtualGroupMembershipPanel;

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

        if (virtualGroupDetailsPanel != null) return virtualGroupDetailsPanel;

        // create virtual group panel
        virtualGroupDetailsPanel = new VirtualGroupDetailsPanel(canUpdate);

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
        virtualGroupDetailsPanel.getX509SubjectDNTextField().setText(((VirtualGroup)group).getX509SubjectDnPattern());
        virtualGroupDetailsPanel.getEmailTextField().setText(((VirtualGroup)group).getSamlEmailPattern());
        setModified(false);
    }

    @Override
    protected boolean validateForm() {
        String dn = virtualGroupDetailsPanel.getX509SubjectDNTextField().getText();
        if ( dn != null && dn.trim().length() > 0 && !CertUtils.isValidDN(dn)) {
            String message = CertUtils.getDNValidationMessage(dn);
            if ( message == null ) {
                message = "";
            } else {
                message = "\n" + message;
            }
            return JOptionPane.showConfirmDialog(this,
                            resources.getString("x509DNPatternTextField.warning.invalid") + message,
                            resources.getString("x509DNPatternTextField.warning.title"),
                            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        }

        return true;
    }

    /**
     * Collect changes from the form into the group instance.
     *
     * @return Group   the instance with changes applied
     */
    protected Group collectChanges() {
        VirtualGroup vGroup = group;
        vGroup.setProviderId(config.getOid());
        vGroup.setDescription(virtualGroupDetailsPanel.getGroupDescTextField().getText());
        vGroup.setSamlEmailPattern(virtualGroupDetailsPanel.getEmailTextField().getText());
        vGroup.setX509SubjectDnPattern(virtualGroupDetailsPanel.getX509SubjectDNTextField().getText());
        return group;
    }

    protected String save() throws SaveException, UpdateException, ObjectNotFoundException {
        return getIdentityAdmin().saveGroup(config.getOid(), group, emptyMembers);
    }
}



