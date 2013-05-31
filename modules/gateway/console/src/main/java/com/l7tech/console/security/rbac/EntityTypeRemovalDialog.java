package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

/**
 * Dialog for displaying which EntityTypes were removed from the zone.
 */
public class EntityTypeRemovalDialog extends JDialog {
    private JPanel contentPanel;
    private OkCancelPanel okCancelPanel;
    private JTable entityTypesTable;
    private boolean confirmed;

    public EntityTypeRemovalDialog(@NotNull final Window owner, @NotNull final Map<EntityType, Integer> removedEntityTypes) {
        super(owner, "Update Security Zone");
        setContentPane(contentPanel);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
        okCancelPanel.getCancelButton().addActionListener(Utilities.createDisposeAction(this));
        okCancelPanel.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        okCancelPanel.setOkButtonText("Continue update");
        if (removedEntityTypes.containsKey(EntityType.SSG_KEY_METADATA)) {
            // display metadata as private keys
            final int numMetadata = removedEntityTypes.get(EntityType.SSG_KEY_METADATA);
            removedEntityTypes.remove(EntityType.SSG_KEY_METADATA);
            removedEntityTypes.put(EntityType.SSG_KEY_ENTRY, numMetadata);
        }
        final SimpleTableModel<EntityType> tableModel = TableUtil.configureTable(entityTypesTable,
                column("Type", 80, 250, 99999, propertyTransform(EntityType.class, "name")),
                column("Number", 40, 50, 99999, new Functions.Unary<String, EntityType>() {
                    @Override
                    public String call(final EntityType entityType) {
                        final Integer num = removedEntityTypes.get(entityType);
                        return num == null ? "unknown" : num.toString();
                    }
                }));
        Utilities.setRowSorter(entityTypesTable, tableModel);
        Utilities.centerOnParent(this);
        tableModel.setRows(new ArrayList<EntityType>(removedEntityTypes.keySet()));
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
