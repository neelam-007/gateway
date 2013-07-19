package com.l7tech.console.security.rbac;

import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class SecurityZoneManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(SecurityZoneManagerWindow.class.getName());
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(SecurityZoneManagerWindow.class.getName());
    private static final String DELETE_CONFIRMATION_PROERPTY = "delete.confirmation";
    private static final String DELETE_CONFIRMATION_NAME_MAX_CHARS = "delete.confirmation.name.max.chars";

    private JPanel contentPane;
    private JButton closeButton;
    private JButton createButton;
    private JButton editButton;
    private JButton removeButton;
    private JTable securityZonesTable;
    private JTabbedPane tabbedPanel;
    private SecurityZonePropertiesPanel propertiesPanel;
    private SecurityZoneEntitiesPanel entitiesPanel;
    private JButton assignButton;
    private SecurityProvider securityProvider;

    private SimpleTableModel<SecurityZone> securityZonesTableModel;
    private boolean canCreate;

    public SecurityZoneManagerWindow(Window owner) {
        super(owner, "Manage Security Zones", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        setModal(true);
        securityProvider = Registry.getDefault().getSecurityProvider();
        canCreate = securityProvider.hasPermission(new AttemptedCreate(EntityType.SECURITY_ZONE));

        closeButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeButton);

        final RunOnChangeListener enableOrDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisable();
                reloadTabbedPanels();
            }
        });

        securityZonesTableModel = TableUtil.configureTable(securityZonesTable,
                column("Name", 40, 140, 99999, propertyTransform(SecurityZone.class, "name")),
                column("Description", 80, 300, 99999, propertyTransform(SecurityZone.class, "description")));
        securityZonesTableModel.addTableModelListener(enableOrDisableListener);
        Utilities.setRowSorter(securityZonesTable, securityZonesTableModel);
        loadSecurityZonesTable();

        securityZonesTable.getSelectionModel().addListSelectionListener(enableOrDisableListener);

        EntityCrudController<SecurityZone> ecc = new EntityCrudController<>();
        ecc.setEntityTable(securityZonesTable);
        ecc.setEntityTableModel(securityZonesTableModel);
        ecc.setEntityCreator(new EntityCreator<SecurityZone>() {
            @Override
            public SecurityZone createNewEntity() {
                final SecurityZone securityZone = new SecurityZone();
                securityZone.getPermittedEntityTypes().addAll(EnumSet.allOf(EntityType.class));
                return securityZone;
            }
        });
        ecc.setEntityDeleter(new EntityDeleter<SecurityZone>() {
            @Override
            public void deleteEntity(SecurityZone entity) throws DeleteException {
                Registry.getDefault().getRbacAdmin().deleteSecurityZone(entity);
                flushCachedZones();
                refreshTrees();
            }

            @Override
            public void displayDeleteDialog(final SecurityZone zone, final Functions.UnaryVoid<SecurityZone> afterDeleteListener) {
                final Integer maxNameChars = Integer.valueOf(RESOURCES.getString(DELETE_CONFIRMATION_NAME_MAX_CHARS));
                final String displayName = TextUtils.truncateStringAtEnd(zone.getName(), maxNameChars);
                final String confirmation = RESOURCES.getString(DELETE_CONFIRMATION_PROERPTY);
                final String msg = MessageFormat.format(confirmation, displayName);
                DialogDisplayer.showOptionDialog(
                        SecurityZoneManagerWindow.this,
                        WordUtils.wrap(msg, DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                        "Remove " + zone.getName(),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Remove " + displayName, "Cancel"},
                        null,
                        new DialogDisplayer.OptionListener() {
                            @Override
                            public void reportResult(int option) {
                                if (option == 0) {
                                    afterDeleteListener.call(zone);
                                } else {
                                    afterDeleteListener.call(null);
                                }
                            }
                        });
            }


        });
        ecc.setEntitySaver(new EntitySaver<SecurityZone>() {
            @Override
            public SecurityZone saveEntity(SecurityZone entity) throws SaveException {
                long oid = Registry.getDefault().getRbacAdmin().saveSecurityZone(entity);
                flushCachedZones();
                refreshTrees();
                try {
                    return Registry.getDefault().getRbacAdmin().findSecurityZoneByPrimaryKey(oid);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Unable to retrieve saved entity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    entity.setOid(oid);
                    return entity;
                } catch (final PermissionDeniedException e) {
                    throw new SaveException("Cannot retrieve saved entity: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
        ecc.setEntityEditor(new EntityEditor<SecurityZone>() {
            @Override
            public void displayEditDialog(final SecurityZone zone, final Functions.UnaryVoidThrows<SecurityZone, SaveException> afterEditListener) {
                boolean create = PersistentEntity.DEFAULT_OID == zone.getOid();
                AttemptedOperation operation = create
                        ? new AttemptedCreateSpecific(EntityType.SECURITY_ZONE, zone)
                        : new AttemptedUpdate(EntityType.SECURITY_ZONE, zone);
                boolean readOnly = !securityProvider.hasPermission(operation);
                final SecurityZonePropertiesDialog dlg = new SecurityZonePropertiesDialog(SecurityZoneManagerWindow.this, zone, readOnly, afterEditListener);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg);
            }
        });

        createButton.addActionListener(ecc.createCreateAction());
        editButton.addActionListener(ecc.createEditAction());
        Utilities.setDoubleClickAction(securityZonesTable, editButton);
        removeButton.addActionListener(ecc.createDeleteAction());
        final Map<EntityType, List<SecurityZone>> modifiableEntityTypes = getModifiableEntityTypes();
        if (!modifiableEntityTypes.isEmpty()) {
            assignButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final AssignSecurityZonesDialog assignDialog = new AssignSecurityZonesDialog(SecurityZoneManagerWindow.this, modifiableEntityTypes);
                    assignDialog.pack();
                    Utilities.centerOnParentWindow(assignDialog);
                    DialogDisplayer.display(assignDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (getSelectedSecurityZone() != null) {
                                // currently selected zone's entities may have changed
                                reloadTabbedPanels();
                            }
                        }
                    });
                }
            });
        } else {
            assignButton.setVisible(false);
        }
        enableOrDisable();
    }

    /**
     * Get sorted entity types for which the user can modify at least one entity by changing its zone (must be able to switch between at least two zones).
     *
     * @return a map of sorted entity types for which the user can modify at least one entity by changing its zone.
     *         Key = entity type, value = sorted zones that can be set for the entity type.
     */
    private Map<EntityType, List<SecurityZone>> getModifiableEntityTypes() {
        final Map<EntityType, List<SecurityZone>> validEntityTypes = new TreeMap<>(EntityType.NAME_COMPARATOR);
        for (final EntityType type : SecurityZoneUtil.getNonHiddenZoneableEntityTypes()) {
            // must be able to update at least one of the entity type
            if (securityProvider.hasPermission(new AttemptedUpdateAny(type))) {
                final List<SecurityZone> validZones = SecurityZoneUtil.getSortedZonesForOperationAndEntityType(OperationType.UPDATE, Collections.singleton(type));
                // must be able to switch between at least two zones
                if (validZones.size() > 1) {
                    validEntityTypes.put(type, validZones);
                }
            }
        }
        return validEntityTypes;
    }

    private void reloadTabbedPanels() {
        final SecurityZone selected = getSelectedSecurityZone();
        propertiesPanel.configure(selected);
        entitiesPanel.configure(selected);
    }

    private static void flushCachedZones() {
        SecurityZoneUtil.flushCachedSecurityZones();
    }

    private void loadSecurityZonesTable() {
        flushCachedZones();
        final Set<SecurityZone> securityZones = new TreeSet<>(new NamedEntityComparator());
        securityZones.addAll(SecurityZoneUtil.getSecurityZones());
        securityZonesTableModel.setRows(new ArrayList<SecurityZone>(securityZones));
    }

    private void enableOrDisable() {
        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        final SecurityZone selected = getSelectedSecurityZone();
        editButton.setEnabled(selected != null && securityProvider.hasPermission(new AttemptedUpdate(EntityType.SECURITY_ZONE, selected)));
        removeButton.setEnabled(selected != null && securityProvider.hasPermission(new AttemptedDeleteSpecific(EntityType.SECURITY_ZONE, selected)));
        createButton.setEnabled(canCreate);
    }

    @Nullable
    private SecurityZone getSelectedSecurityZone() {
        SecurityZone selected = null;
        final int rowIndex = securityZonesTable.getSelectedRow();
        if (rowIndex >= 0) {
            final int modelIndex = securityZonesTable.convertRowIndexToModel(rowIndex);
            selected = securityZonesTableModel.getRowObject(modelIndex);
        }
        return selected;
    }

    private void refreshTrees() {
        final ServicesAndPoliciesTree servicesAndPoliciesTree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        servicesAndPoliciesTree.refresh();
        TopComponents.getInstance().getAssertionRegistry().updateAssertionAccess();
    }
}
