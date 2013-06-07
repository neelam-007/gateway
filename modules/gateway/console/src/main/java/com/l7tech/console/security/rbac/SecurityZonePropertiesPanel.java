package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final int PERMITTED_COL_INDEX = 1;
    private JPanel contentPanel;
    private JTextField nameField;
    private JTable entityTypesTable;
    private JScrollPane scrollPane;
    private JLabel entityTypesLabel;
    private JTextArea descriptionTextArea;
    private SimpleTableModel<EntityType> entityTypesTableModel;
    private SecurityZone securityZone;

    public SecurityZonePropertiesPanel() {
        entityTypesTableModel = TableUtil.configureTable(entityTypesTable,
                column("Type", 80, 300, 99999, propertyTransform(EntityType.class, "name")),
                column("Permitted", 40, 140, 99999, new Functions.Unary<Boolean, EntityType>() {
                    @Override
                    public Boolean call(final EntityType entityType) {
                        final Set<EntityType> permittedTypes = securityZone == null ? Collections.<EntityType>emptySet() : securityZone.getPermittedEntityTypes();
                        return permittedTypes.contains(entityType);
                    }
                }));
        entityTypesTable.getColumnModel().getColumn(PERMITTED_COL_INDEX).setCellRenderer(new CheckOrXCellRenderer());
        Utilities.setRowSorter(entityTypesTable, entityTypesTableModel);
    }

    /**
     * Configure the Security Zone displayed in the panel.
     *
     * @param securityZone the SecurityZone to display or null if the display should display blank values.
     */
    public void configure(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
        if (securityZone != null) {
            nameField.setText(securityZone.getName());
            descriptionTextArea.setText(securityZone.getDescription());
            if (securityZone.getPermittedEntityTypes().contains(EntityType.ANY)) {
                entityTypesLabel.setText(RESOURCES.getString(ANY_ENTITY_TYPE_LABEL));
                entityTypesTableModel.setRows(Collections.<EntityType>emptyList());
                scrollPane.setVisible(false);
            } else {
                entityTypesLabel.setText(RESOURCES.getString(PERMITTED_ENTITY_TYPES_LABEL));
                entityTypesTableModel.setRows(new ArrayList<EntityType>(SecurityZoneUtil.getNonHiddenZoneableEntityTypes()));
                scrollPane.setVisible(true);
            }
        } else {
            nameField.setText(StringUtils.EMPTY);
            descriptionTextArea.setText(StringUtils.EMPTY);
            entityTypesTableModel.setRows(Collections.<EntityType>emptyList());
            entityTypesLabel.setText(RESOURCES.getString(PERMITTED_ENTITY_TYPES_LABEL));
            scrollPane.setVisible(true);
        }
    }

    private class CheckOrXCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(final Object value) {
            if (value != null) {
                if (Boolean.valueOf(value.toString())) {
                    setIcon(ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/check16.gif"));
                    setToolTipText("Permitted");
                } else {
                    setIcon(ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/RedCrossSign16.gif"));
                    setToolTipText("Not Permitted");
                }
            } else {
                super.setValue(value);
            }
        }

        @Override
        public int getHorizontalAlignment() {
            return JLabel.CENTER;
        }
    }
}
