package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Step panel which allows the user to configure the scope of role permissions.
 */
public class PermissionScopeSelectionPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionScopeSelectionPanel.class.getName());
    private static final int NAME_COL_INDEX = 1;
    private static final String OBJECT_SELECTION = "Object selection";
    private static final int CHECK_COL_INDEX = 0;
    private static final String NO_SECURITY_ZONE = "(no security zone)";
    private static final String UNAVAILABLE = "unavailable";
    private static final String NAME = "Name";
    private static final String PATH = "Path";
    private static final String ZONE = "Zone";
    private static final String DESCRIPTION = "Description";
    private static final int MAX_WIDTH = 99999;
    private static final int CHECK_BOX_WIDTH = 30;
    private static final String ATTRIBUTE = "Attribute";
    private static final String COMPARISON = "Comparison";
    private static final String VALUE = "Value";
    private static final String EQUALS = "equals";
    private static final String STARTS_WITH = "starts with";
    private static final String EQ = "eq";
    private static final String SW = "sw";
    private static final String ENTITIES_WITH_NO_ZONE = "All entities that are not assigned a security zone";
    private static final int ZONE_TAB_INDEX = 2;
    private static final int FOLDER_TAB_INDEX = 1;
    private static final String SELECT_OPTIONS_FOR_THESE_PERMISSIONS = "Select options for these permissions";
    private JPanel contentPanel;
    private JTabbedPane tabPanel;
    private JPanel zonesPanel;
    private SelectableFilterableTablePanel zonesTablePanel;
    private JPanel folderPanel;
    private JCheckBox transitiveCheckBox;
    private JCheckBox ancestryCheckBox;
    private SelectableFilterableTablePanel foldersTablePanel;
    private JPanel attributesPanel;
    private JTable attributePredicatesTable;
    private JButton removeButton;
    private JButton addButton;
    private JPanel criteriaPanel;
    private JComboBox attributeComboBox;
    private JComboBox comparisonComboBox;
    private JTextField attributeValueTextField;
    private JLabel conditionsLabel;
    private JLabel specificObjectsLabel;
    private SelectableFilterableTablePanel specificObjectsTablePanel;
    private JPanel specificObjectsPanel;
    private JPanel conditionsPanel;
    private JLabel header;
    // use this check box for entity type specific input
    private JCheckBox customCheckBox;
    private CheckBoxSelectableTableModel<SecurityZone> zonesModel;
    private CheckBoxSelectableTableModel<FolderHeader> foldersModel;
    private SimpleTableModel<AttributePredicate> attributesModel;
    private CheckBoxSelectableTableModel<EntityHeader> specificObjectsModel;
    private static Map<String, String> validComparisons = new HashMap<>();
    private PermissionsConfig config;

    static {
        validComparisons.put(EQUALS, EQ);
        validComparisons.put(STARTS_WITH, SW);
    }

    public PermissionScopeSelectionPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
        initTables();
    }

    @Override
    public String getStepLabel() {
        return OBJECT_SELECTION;
    }

    @Override
    public boolean canAdvance() {
        return (config.getScopeType() == PermissionsConfig.ScopeType.SPECIFIC_OBJECTS && !specificObjectsModel.getSelected().isEmpty()) ||
                (config.getScopeType() == PermissionsConfig.ScopeType.CONDITIONAL && (!zonesModel.getSelected().isEmpty() || !foldersModel.getSelected().isEmpty() || attributesModel.getRowCount() > 0));
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canSkip(final Object settings) {
        boolean canSkip = false;
        if (settings instanceof PermissionsConfig) {
            final PermissionsConfig config = (PermissionsConfig) settings;
            canSkip = !config.hasScope();
        } else if (settings != null) {
            logger.log(Level.WARNING, "Cannot handle settings because received invalid settings object: " + settings);
        }
        return canSkip;
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof PermissionsConfig) {
            config = (PermissionsConfig) settings;
            final EntityType type = config.getType();
            if (config.getScopeType() != null) {
                switch (config.getScopeType()) {
                    case CONDITIONAL:
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                conditionsPanel.setVisible(true);
                                specificObjectsPanel.setVisible(false);
                                header.setText(SELECT_OPTIONS_FOR_THESE_PERMISSIONS);
                                tabPanel.setEnabledAt(FOLDER_TAB_INDEX, type == EntityType.ANY || type.isFolderable());
                                tabPanel.setEnabledAt(ZONE_TAB_INDEX, type == EntityType.ANY || type.isSecurityZoneable());
                                reloadAttributeComboBox();
                            }
                        });
                        break;
                    case SPECIFIC_OBJECTS:
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                specificObjectsPanel.setVisible(true);
                                conditionsPanel.setVisible(false);
                                final String typePlural = config.getType().getPluralName().toLowerCase();
                                header.setText("Select " + typePlural);
                                specificObjectsLabel.setText("Permissions will only apply to the selected " + typePlural + ".");
                                customCheckBox.setText(type == EntityType.FOLDER ? "Grant read access to the ancestors of the selected folders." : StringUtils.EMPTY);
                                customCheckBox.setVisible(type == EntityType.FOLDER);
                                if (config.getSelectedEntities().isEmpty()) {
                                    final List<EntityHeader> entities = new ArrayList<>();
                                    try {
                                        entities.addAll(EntityUtils.getEntities(config.getType()));
                                    } catch (final FindException e) {
                                        logger.log(Level.WARNING, "Unable to retrieve entities: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                    }
                                    specificObjectsModel.setSelectableObjects(entities);
                                }
                            }
                        });
                        break;
                    default:
                        throw new IllegalStateException("Unsupported scope type: " + config.getScopeType());
                }
            }
        }

        setSkipped(canSkip(settings)

        );
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof PermissionsConfig) {
            final PermissionsConfig config = (PermissionsConfig) settings;
            switch (config.getScopeType()) {
                case CONDITIONAL:
                    config.setSelectedZones(new HashSet<>(zonesModel.getSelected()));
                    config.setSelectedFolders(new HashSet<>(foldersModel.getSelected()));
                    config.setFolderTransitive(transitiveCheckBox.isSelected());
                    config.setFolderAncestry(ancestryCheckBox.isSelected());
                    config.setAttributePredicates(new HashSet<>(attributesModel.getRows()));
                    break;
                case SPECIFIC_OBJECTS:
                    config.setSelectedEntities(new HashSet<>(specificObjectsModel.getSelected()));
                    if (config.getType() == EntityType.FOLDER && customCheckBox.isSelected()) {
                        config.setSpecificFolderAncestry(true);
                    } else {
                        config.setSpecificFolderAncestry(false);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported scope type: " + config.getScopeType());
            }
        } else {
            logger.log(Level.WARNING, "Cannot store settings because received invalid settings object: " + settings);
        }
    }

    private void initTables() {
        final TableListener tableListener = new TableListener();
        initZonesTable(tableListener);
        initFoldersTable(tableListener);
        initAttributes(tableListener);
        initSpecificObjects(tableListener);
    }

    private void initSpecificObjects(final TableListener tableListener) {
        specificObjectsModel = TableUtil.configureSelectableTable(specificObjectsTablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(final EntityHeader header) {
                        return specificObjectsModel.isSelected(header);
                    }
                }),
                column(NAME, 30, 600, MAX_WIDTH, new Functions.Unary<String, EntityHeader>() {
                    @Override
                    public String call(final EntityHeader header) {
                        String name = UNAVAILABLE;
                        try {
                            name = Registry.getDefault().getEntityNameResolver().getNameForHeader(header, true);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve name for header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return name;
                    }
                }));
        specificObjectsModel.addTableModelListener(tableListener);
        specificObjectsModel.setSelectableObjects(Collections.<EntityHeader>emptyList());
        specificObjectsTablePanel.configure(specificObjectsModel, new int[]{NAME_COL_INDEX}, null);
    }

    private void initAttributes(final TableListener tableListener) {
        attributesModel = TableUtil.configureTable(attributePredicatesTable,
                column(ATTRIBUTE, 30, 200, MAX_WIDTH, new Functions.Unary<String, AttributePredicate>() {
                    @Override
                    public String call(final AttributePredicate predicate) {
                        return predicate.getAttribute();
                    }
                }),
                column(COMPARISON, 30, 200, MAX_WIDTH, new Functions.Unary<String, AttributePredicate>() {
                    @Override
                    public String call(final AttributePredicate predicate) {
                        return predicate.getMode();
                    }
                }),
                column(VALUE, 30, 200, MAX_WIDTH, new Functions.Unary<String, AttributePredicate>() {
                    @Override
                    public String call(final AttributePredicate predicate) {
                        return predicate.getValue();
                    }
                }));
        attributesModel.addTableModelListener(tableListener);
        attributesModel.setRows(new ArrayList<AttributePredicate>());
        attributePredicatesTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        attributePredicatesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    removeButton.setEnabled(attributePredicatesTable.getSelectedRows().length > 0);
                }
            }
        });
        Utilities.setRowSorter(attributePredicatesTable, attributesModel, new int[]{0, 1, 2}, new boolean[]{true, true, true}, new Comparator[]{null, null, null});

        reloadAttributeComboBox();
        comparisonComboBox.setModel(new DefaultComboBoxModel(validComparisons.keySet().toArray()));

        addButton.setEnabled(false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Object selectedAttribute = attributeComboBox.getSelectedItem();
                final Object selectedComparison = comparisonComboBox.getSelectedItem();
                if (selectedAttribute instanceof String && selectedComparison instanceof String) {
                    final String mode = validComparisons.get(selectedComparison);
                    if (mode != null) {
                        final AttributePredicate predicate = new AttributePredicate(null, selectedAttribute.toString(), attributeValueTextField.getText().trim());
                        predicate.setMode(mode);
                        if (!attributesModel.getRows().contains(predicate)) {
                            attributesModel.addRow(predicate);
                        }
                    } else {
                        logger.log(Level.WARNING, "Unrecognized comparison mode: " + selectedComparison);
                    }
                }
            }
        });

        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int[] selectedRows = attributePredicatesTable.getSelectedRows();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    final int selectedRow = selectedRows[i];
                    if (selectedRow >= 0) {
                        final int modelIndex = attributePredicatesTable.convertRowIndexToModel(selectedRow);
                        if (modelIndex >= 0) {
                            attributesModel.removeRowAt(modelIndex);
                        }
                    }
                }
            }
        });

        attributeValueTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(final KeyEvent e) {
            }

            @Override
            public void keyPressed(final KeyEvent e) {
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                addButton.setEnabled(attributeComboBox.getSelectedItem() != null &&
                        comparisonComboBox.getSelectedItem() != null &&
                        StringUtils.isNotBlank(attributeValueTextField.getText()));
            }
        });
    }

    private void reloadAttributeComboBox() {
        attributeComboBox.setModel(new DefaultComboBoxModel(findAttributeNames(config == null ? EntityType.ANY : config.getType()).toArray()));
    }

    private void initFoldersTable(final TableListener tableListener) {
        foldersModel = TableUtil.configureSelectableTable(foldersTablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, FolderHeader>() {
                    @Override
                    public Boolean call(final FolderHeader folder) {
                        return foldersModel.isSelected(folder);
                    }
                }),
                column(NAME, 30, 200, MAX_WIDTH, new Functions.Unary<String, FolderHeader>() {
                    @Override
                    public String call(final FolderHeader folder) {
                        String name = UNAVAILABLE;
                        try {
                            name = Registry.getDefault().getEntityNameResolver().getNameForHeader(folder, false);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve name for folder: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return name;
                    }
                }),
                column(PATH, 30, 400, MAX_WIDTH, new Functions.Unary<String, FolderHeader>() {
                    @Override
                    public String call(final FolderHeader folder) {
                        String path = UNAVAILABLE;
                        try {
                            path = Registry.getDefault().getEntityNameResolver().getPath(folder);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve path for folder header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return path;
                    }
                }),
                column(ZONE, 30, 200, MAX_WIDTH, new Functions.Unary<String, FolderHeader>() {
                    @Override
                    public String call(final FolderHeader folder) {
                        SecurityZone zone = null;
                        if (folder.getSecurityZoneGoid() != null) {
                            zone = SecurityZoneUtil.getSecurityZoneByGoid(folder.getSecurityZoneGoid());
                        }
                        return zone == null ? NO_SECURITY_ZONE : zone.getName();
                    }
                }));
        foldersModel.addTableModelListener(tableListener);
        try {
            final Collection<FolderHeader> folders = Registry.getDefault().getFolderAdmin().findAllFolders();
            foldersModel.setSelectableObjects(new ArrayList<>(folders));
        } catch (final FindException e) {
            foldersModel.setSelectableObjects(Collections.<FolderHeader>emptyList());
        }
        foldersTablePanel.configure(foldersModel, new int[]{NAME_COL_INDEX}, "folders");
    }

    private void initZonesTable(final TableListener tableListener) {
        zonesModel = TableUtil.configureSelectableTable(zonesTablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, SecurityZone>() {
                    @Override
                    public Boolean call(final SecurityZone zone) {
                        return zonesModel.isSelected(zone);
                    }
                }),
                column(NAME, 30, 200, MAX_WIDTH, new Functions.Unary<String, SecurityZone>() {
                    @Override
                    public String call(final SecurityZone zone) {
                        return zone.equals(SecurityZoneUtil.getNullZone()) ? NO_SECURITY_ZONE : zone.getName();
                    }
                }),
                column(DESCRIPTION, 30, 400, MAX_WIDTH, new Functions.Unary<String, SecurityZone>() {
                    @Override
                    public String call(final SecurityZone zone) {
                        return zone.equals(SecurityZoneUtil.getNullZone()) ? ENTITIES_WITH_NO_ZONE : zone.getDescription();
                    }
                }));
        final ArrayList<SecurityZone> zones = new ArrayList<>();
        zones.add(SecurityZoneUtil.getNullZone());
        zones.addAll(SecurityZoneUtil.getSortedReadableSecurityZones());
        zonesModel.setSelectableObjects(zones);
        zonesModel.addTableModelListener(tableListener);
        zonesTablePanel.configure(zonesModel, new int[]{NAME_COL_INDEX}, "zones");
    }

    private Collection<String> findAttributeNames(@NotNull final EntityType entityType) {
        final Collection<String> names = new ArrayList<>();
        final Class eClazz = entityType.getEntityClass();
        if (eClazz != null) {
            try {
                final BeanInfo info = Introspector.getBeanInfo(eClazz);
                final PropertyDescriptor[] props = info.getPropertyDescriptors();
                for (final PropertyDescriptor propertyDescriptor : props) {
                    final Method getter = propertyDescriptor.getReadMethod();
                    if (getter != null) {
                        final Class rtype = getter.getReturnType();
                        if (Number.class.isAssignableFrom(rtype) ||
                                rtype == Long.TYPE ||
                                rtype == Integer.TYPE ||
                                rtype == Byte.TYPE ||
                                rtype == Short.TYPE ||
                                CharSequence.class.isAssignableFrom(rtype) ||
                                rtype == Boolean.TYPE ||
                                Boolean.class.isAssignableFrom(rtype) ||
                                Enum.class.isAssignableFrom(rtype)) {
                            //there is a getter for this property, so use it in the list
                            names.add(propertyDescriptor.getName());
                        }
                    }
                }

                // Allow attempts to use Name for ANY entity, since in practice most will be NamedEntity subclasses
                if (EntityType.ANY.equals(entityType)) {
                    names.add("name");
                }
            } catch (final IntrospectionException e) {
                logger.log(Level.WARNING, "Unable to introspect " + entityType, e);
                JOptionPane.showMessageDialog(this, "Unable to determine available attributes", "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
        return names;
    }

    private class TableListener implements TableModelListener {
        @Override
        public void tableChanged(final TableModelEvent e) {
            final int type = e.getType();
            if (type == TableModelEvent.UPDATE || type == TableModelEvent.INSERT || type == TableModelEvent.DELETE) {
                notifyListeners();
            }
        }
    }
}
