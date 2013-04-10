package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.SecurityZonePredicate;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI panel for editing a security zone RBAC scope predicate.
 */
public class ScopeSecurityZonePanel extends ValidatedPanel<SecurityZonePredicate> {
    private static final Logger logger = Logger.getLogger(ScopeSecurityZonePanel.class.getName());

    private JPanel contentPane;
    private JLabel label;
    private JComboBox<SecurityZone> zoneComboBox;

    private final EntityType entityType;
    private final Permission permission;
    private SecurityZonePredicate model;

    public ScopeSecurityZonePanel(@NotNull SecurityZonePredicate model, @NotNull EntityType entityType) {
        super("folderPredicate");
        this.model = model;
        this.permission = model.getPermission();
        this.entityType = entityType;
        loadZonesComboBox(model.getRequiredZone());
        init();
    }

    private void loadZonesComboBox(SecurityZone requiredZone) {
        try {
            java.util.List<SecurityZone> zones = new ArrayList<SecurityZone>();
            Collection<SecurityZone> serverZones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
            if (serverZones != null)
                zones.addAll(serverZones);
            if (requiredZone != null && !zones.contains(requiredZone))
                zones.add(requiredZone);
            zoneComboBox.setModel(new DefaultComboBoxModel<SecurityZone>(zones.toArray(new SecurityZone[zones.size()])));
            if (requiredZone != null)
                zoneComboBox.setSelectedItem(requiredZone);
        } catch (FindException e) {
            final String msg = "Unable to load security zones: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    protected SecurityZonePredicate getModel() {
        doUpdateModel();
        return model;
    }

    @Override
    protected void initComponents() {
        label.setText(MessageFormat.format(label.getText(), entityType.getPluralName()));

        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        zoneComboBox.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        model = new SecurityZonePredicate(permission, (SecurityZone) zoneComboBox.getSelectedItem());
    }

    @Override
    protected String getSyntaxError(final SecurityZonePredicate model) {
        String error = null;

        if (!(zoneComboBox.getSelectedItem() instanceof SecurityZone)) {
            error = "A security zone must be selected.";
        }

        return error;
    }
}
