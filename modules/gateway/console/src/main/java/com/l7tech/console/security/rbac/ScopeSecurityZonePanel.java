package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.SecurityZonePredicate;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * GUI panel for editing a security zone RBAC scope predicate.
 */
public class ScopeSecurityZonePanel extends ValidatedPanel<SecurityZonePredicate> {
    private static final Logger logger = Logger.getLogger(ScopeSecurityZonePanel.class.getName());

    private JPanel contentPane;
    private JComboBox<SecurityZone> zoneComboBox;
    private JRadioButton specificZoneRadioButton;
    private JRadioButton noSecurityZoneRadioButton;

    private final EntityType entityType;
    private final Permission permission;
    private SecurityZonePredicate model;
    private SecurityZone initialRequiredZone;

    public ScopeSecurityZonePanel(@NotNull SecurityZonePredicate model, @NotNull EntityType entityType) {
        super("folderPredicate");
        this.model = model;
        this.permission = model.getPermission();
        this.entityType = entityType;
        this.initialRequiredZone = model.getRequiredZone();
        modelToView(model.getRequiredZone());
        final PredicateChangeListener changeListener = new PredicateChangeListener();
        specificZoneRadioButton.addChangeListener(changeListener);
        noSecurityZoneRadioButton.addChangeListener(changeListener);
        zoneComboBox.addActionListener(changeListener);
        init();
    }

    private void modelToView(final SecurityZone requiredZone) {
        if (requiredZone != null) {
            zoneComboBox.setSelectedItem(requiredZone);
            specificZoneRadioButton.setSelected(true);
        } else {
            noSecurityZoneRadioButton.setSelected(true);
            zoneComboBox.setEnabled(false);
        }
    }

    @Override
    protected SecurityZonePredicate getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
        specificZoneRadioButton.setText(MessageFormat.format(specificZoneRadioButton.getText(), entityType.getPluralName()));
        noSecurityZoneRadioButton.setText(MessageFormat.format(noSecurityZoneRadioButton.getText(), entityType.getPluralName()));
        final java.util.List<SecurityZone> zones = new ArrayList<SecurityZone>();
        final Collection<SecurityZone> serverZones = SecurityZoneUtil.getSecurityZones();
        if (serverZones != null) {
            zones.addAll(serverZones);
        }
        if (initialRequiredZone != null && !zones.contains(initialRequiredZone)) {
            zones.add(initialRequiredZone);
        }
        zoneComboBox.setModel(new DefaultComboBoxModel<SecurityZone>(zones.toArray(new SecurityZone[zones.size()])));
    }

    @Override
    public void focusFirstComponent() {
        zoneComboBox.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        final boolean selected = noSecurityZoneRadioButton.isSelected();
        model = new SecurityZonePredicate(permission, selected ? null : (SecurityZone) zoneComboBox.getSelectedItem());
    }

    @Override
    protected String getSyntaxError(final SecurityZonePredicate model) {
        String error = null;
        if (specificZoneRadioButton.isSelected() && !(zoneComboBox.getSelectedItem() instanceof SecurityZone)) {
            error = "A security zone must be selected.";
        }
        return error;
    }

    private class PredicateChangeListener implements ChangeListener, ActionListener {
        @Override
        public void stateChanged(final ChangeEvent e) {
            handleEvent();
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            handleEvent();
        }

        private void handleEvent() {
            doUpdateModel();
            checkSyntax();
            zoneComboBox.setEnabled(specificZoneRadioButton.isSelected());
        }
    }
}
