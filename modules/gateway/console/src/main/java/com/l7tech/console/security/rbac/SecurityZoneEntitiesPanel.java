package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntityHeader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Panel which displays the Entities in a SecurityZone.
 */
public class SecurityZoneEntitiesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(SecurityZoneEntitiesPanel.class.getName());
    private JPanel contentPanel;
    private JTable entitiesTable;
    private JScrollPane scrollPane;
    private JComboBox entityTypeComboBox;
    private JLabel countLabel;
    private FilterPanel filterPanel;
    private SimpleTableModel<ZoneableEntityHeader> entitiesTableModel;
    private SecurityZone securityZone;

    public SecurityZoneEntitiesPanel() {
        initTable();
        initComboBox();
        initFiltering();
        enableDisable();
    }

    public void configure(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
        loadTable();
        loadComboBox();
        loadCount();
        enableDisable();
    }

    private void enableDisable() {
        entityTypeComboBox.setEnabled(securityZone != null);
        entitiesTable.setEnabled(securityZone != null);
        filterPanel.allowFiltering(securityZone != null);
        countLabel.setVisible(securityZone != null);
    }

    @Nullable
    private EntityType getSelectedEntityType() {
        final Object selected = entityTypeComboBox.getSelectedItem();
        if (selected != null) {
            return (EntityType) selected;
        }
        return null;
    }

    private void initFiltering() {
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        filterPanel.registerClearCallback(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                loadCount();
            }
        });
        filterPanel.attachRowSorter(((TableRowSorter) entitiesTable.getRowSorter()), new int[]{0});
    }

    private void loadCount() {
        final int showCount = entitiesTable.getRowCount();
        final int total = entitiesTable.getModel().getRowCount();
        countLabel.setText("showing " + showCount + " of " + total + " items");
    }

    private void initTable() {
        entitiesTableModel = TableUtil.configureTable(entitiesTable, column("Name", 80, 300, 99999, new Functions.Unary<String, ZoneableEntityHeader>() {
            @Override
            public String call(final ZoneableEntityHeader header) {
                try {
                    return Registry.getDefault().getEntityNameResolver().getNameForHeader(header);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Unable to determine name for entity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    return "unknown entity";
                }
            }
        }));
        Utilities.setRowSorter(entitiesTable, entitiesTableModel);
    }

    private void initComboBox() {
        entityTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof EntityType) {
                    final EntityType type = (EntityType) value;
                    value = type.getName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        entityTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadTable();
                loadCount();
            }
        });
        loadComboBox();
    }

    private void loadComboBox() {
        final EntityType previouslySelected = securityZone == null ? null : (EntityType) entityTypeComboBox.getSelectedItem();
        final List<EntityType> entityTypes = new ArrayList<>();
        if (securityZone != null) {
            // first option is null to force user to select something before any entities are loaded
            entityTypes.add(null);
            if (securityZone.getPermittedEntityTypes().contains(EntityType.ANY)) {
                // already sorted
                entityTypes.addAll(SecurityZoneUtil.getAllZoneableEntityTypes());
            } else {
                final Set<EntityType> sortedSubset = new TreeSet<>(EntityType.NAME_COMPARATOR);
                sortedSubset.addAll(securityZone.getPermittedEntityTypes());
                entityTypes.addAll(sortedSubset);
            }
            entityTypes.removeAll(SecurityZoneUtil.getHiddenZoneableEntityTypes());
        }
        entityTypeComboBox.setModel(new DefaultComboBoxModel<EntityType>(entityTypes.toArray(new EntityType[entityTypes.size()])));
        entityTypeComboBox.setSelectedItem(previouslySelected);
    }

    private void loadTable() {
        if (securityZone != null) {
            EntityType selected = getSelectedEntityType();
            if (selected != null) {
                final List<ZoneableEntityHeader> entities = new ArrayList<>();
                final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                try {
                    if (EntityType.SSG_KEY_ENTRY == selected) {
                        selected = EntityType.SSG_KEY_METADATA;
                    }
                    entities.addAll(rbacAdmin.findEntitiesByTypeAndSecurityZoneOid(selected, securityZone.getOid()));
                    entitiesTableModel.setRows(entities);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Error retrieving entities of type " + selected + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    DialogDisplayer.showMessageDialog(this, "Unable to retrieve entities", "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        } else {
            entitiesTableModel.setRows(Collections.<ZoneableEntityHeader>emptyList());
        }
    }
}
