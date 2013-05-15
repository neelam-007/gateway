package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Set;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

/**
 * A panel which displays a read-only view of a Security Zone's properties.
 */
public class SecurityZonePropertiesPanel extends JPanel {
    private JPanel contentPanel;
    private JTextField nameField;
    private JTextField descriptionField;
    private JTable entityTypesTable;
    private JScrollPane scrollPane;
    private JLabel entityTypesLabel;
    private SimpleTableModel<EntityType> entityTypesTableModel;

    public void configure(final SecurityZone securityZone) {
        nameField.setText(securityZone.getName());
        descriptionField.setText(securityZone.getDescription());
        final Set<EntityType> permittedTypes = securityZone.getPermittedEntityTypes();
        if (permittedTypes.contains(EntityType.ANY)) {
            scrollPane.setVisible(false);
            entityTypesLabel.setText("Any entity type is permitted in this zone");
        } else {
            scrollPane.setVisible(true);
            entityTypesLabel.setText("Entity types permitted in this zone:");
            entityTypesTableModel = TableUtil.configureTable(entityTypesTable,
                    column("Type", 80, 300, 99999, propertyTransform(EntityType.class, "name")),
                    column("Permitted", 40, 140, 99999, new Functions.Unary<Object, EntityType>() {
                        @Override
                        public Object call(final EntityType entityType) {
                            return permittedTypes.contains(entityType) ? "yes" : "no";
                        }
                    }));

            entityTypesTableModel.setRows(new ArrayList<EntityType>(SecurityZoneUtil.getAllZoneableEntityTypes()));
        }
    }
}
