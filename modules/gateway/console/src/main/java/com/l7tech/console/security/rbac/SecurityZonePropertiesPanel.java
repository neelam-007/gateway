package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

/**
 * A panel which displays a read-only view of a Security Zone's properties.
 */
public class SecurityZonePropertiesPanel extends JPanel {
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(SecurityZonePropertiesPanel.class.getName());
    private static final String ANY_ENTITY_TYPE_LABEL = "entityType.any";
    private static final String PERMITTED_ENTITY_TYPES_LABEL = "entityTypes.permitted";
    private JPanel contentPanel;
    private JTextField nameField;
    private JTextField descriptionField;
    private JTable entityTypesTable;
    private JScrollPane scrollPane;
    private JLabel entityTypesLabel;
    private SimpleTableModel<EntityType> entityTypesTableModel;

    /**
     * Configure the Security Zone displayed in the panel.
     *
     * @param securityZone the SecurityZone to display or null if the display should display blank values.
     */
    public void configure(@Nullable final SecurityZone securityZone) {
        if (securityZone != null) {
            nameField.setText(securityZone.getName());
            descriptionField.setText(securityZone.getDescription());
            final Set<EntityType> permittedTypes = securityZone.getPermittedEntityTypes();
            if (permittedTypes.contains(EntityType.ANY)) {
                scrollPane.setVisible(false);
                entityTypesLabel.setText(RESOURCES.getString(ANY_ENTITY_TYPE_LABEL));
            } else {
                scrollPane.setVisible(true);
                entityTypesLabel.setText(RESOURCES.getString(PERMITTED_ENTITY_TYPES_LABEL));
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
        } else {
            nameField.setText(StringUtils.EMPTY);
            descriptionField.setText(StringUtils.EMPTY);
            scrollPane.setVisible(false);
        }
    }
}
