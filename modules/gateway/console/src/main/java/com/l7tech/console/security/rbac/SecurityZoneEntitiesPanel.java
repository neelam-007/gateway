package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
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
    private static final int PATH_COL_INDEX = 1;
    private static final String UNAVAILABLE = "unavailable";
    private JPanel contentPanel;
    private JTable entitiesTable;
    private JScrollPane scrollPane;
    private JComboBox entityTypeComboBox;
    private JLabel countLabel;
    private FilterPanel filterPanel;
    private SimpleTableModel<EntityHeader> entitiesTableModel;
    private SecurityZone securityZone;
    private TableColumn pathColumn;
    private Map<EntityHeader, String> entityNames = new HashMap<>();

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
        filterPanel.attachRowSorter(((TableRowSorter) entitiesTable.getRowSorter()), new int[]{0});
    }

    private void loadCount() {
        final int showCount = entitiesTable.getRowCount();
        final int total = entitiesTable.getModel().getRowCount();
        countLabel.setText("showing " + showCount + " of " + total + " items");
    }

    private void initTable() {
        entitiesTableModel = TableUtil.configureTable(entitiesTable,
                column("Name", 80, 300, 99999, (Functions.Unary<String, EntityHeader>) header -> entityNames.containsKey(header) ? entityNames.get(header) : UNAVAILABLE),
                column("Path", 60, 60, 99999, (Functions.Unary<String, EntityHeader>) entityHeader -> {
                    final EntityNameResolver entityNameResolver = Registry.getDefault().getEntityNameResolver();
                    String path = StringUtils.EMPTY;
                    if (entityHeader instanceof HasFolderId) {
                        try {
                            path = entityNameResolver.getPath((HasFolderId) entityHeader);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to determine path for entity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            return "unknown path";
                        } catch (final PermissionDeniedException e) {
                            logger.log(Level.WARNING, "Unable to determine path for entity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            return "path unavailable";
                        }
                    } else if (entityHeader.getType() == EntityType.ASSERTION_ACCESS) {
                        final Assertion assertion = TopComponents.getInstance().getAssertionRegistry().findByClassName(entityHeader.getName());
                        if (assertion != null) {
                            path = entityNameResolver.getPaletteFolders(assertion);
                        }
                    }
                    return path;
                }));
        pathColumn = entitiesTable.getColumnModel().getColumn(PATH_COL_INDEX);
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
        entityTypeComboBox.setModel(new DefaultComboBoxModel<>(entityTypes.toArray(new EntityType[entityTypes.size()])));
        entityTypeComboBox.setSelectedItem(previouslySelected);
    }

    private void loadTable() {
        entityNames.clear();
        if (securityZone != null) {
            EntityType selected = getSelectedEntityType();
            if (selected != null) {
                final List<EntityHeader> entities = new ArrayList<>();
                final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                try {
                    if (EntityType.SSG_KEY_ENTRY == selected) {
                        selected = EntityType.SSG_KEY_METADATA;
                    }
                    for (final EntityHeader header : rbacAdmin.findEntitiesByTypeAndSecurityZoneGoid(selected, securityZone.getGoid())) {
                        if (EntityType.POLICY != selected || !(header instanceof PolicyHeader && PolicyType.PRIVATE_SERVICE == ((PolicyHeader) header).getPolicyType())) {
                            populateHeader(entities, header);
                        }
                    }
                    entitiesTableModel.setRows(entities);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Error retrieving entities of type " + selected + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    DialogDisplayer.showMessageDialog(this, "Unable to retrieve entities", "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        } else {
            entitiesTableModel.setRows(Collections.<EntityHeader>emptyList());
        }

        showHidePathColumn();
    }

    private void populateHeader(List<EntityHeader> entities, EntityHeader header) {
        try {
            entityNames.put(header, Registry.getDefault().getEntityNameResolver().getNameForHeader(header, false));
            entities.add(header);
        } catch (final FindException | PermissionDeniedException e) {
            // don't show entities that we cannot resolve a name for
            logger.log(Level.WARNING, "Error resolving name for header " + header.toStringVerbose() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private void showHidePathColumn() {
        final boolean pathColumnVisible = entitiesTable.getColumnCount() > 1;
        if (entitiesTableModel.getRowCount() > 0) {
            final EntityHeader firstHeader = entitiesTableModel.getRowObject(0);
            if (!pathColumnVisible && (firstHeader instanceof HasFolderId || firstHeader.getType() == EntityType.ASSERTION_ACCESS)) {
                entitiesTable.addColumn(pathColumn);
            } else if (pathColumnVisible && !(firstHeader instanceof HasFolderId || firstHeader.getType() == EntityType.ASSERTION_ACCESS)) {
                entitiesTable.removeColumn(pathColumn);
            }
        } else if (pathColumnVisible) {
            entitiesTable.removeColumn(pathColumn);
        }
    }
}
