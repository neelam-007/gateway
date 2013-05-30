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
import com.l7tech.util.Functions;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class SecurityZoneManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(SecurityZoneManagerWindow.class.getName());
    private static final String DELETE_CONFIRMATION_FORMAT = "Are you sure you want to remove the security zone {0}? All entities currently assigned to this zone will switch to the \"no security zone\" state if you continue. This action cannot be undone.";

    private JPanel contentPane;
    private JButton closeButton;
    private JButton createButton;
    private JButton editButton;
    private JButton removeButton;
    private JTable securityZonesTable;
    private JTabbedPane tabbedPanel;
    private SecurityZonePropertiesPanel propertiesPanel;
    private SecurityZoneEntitiesPanel entitiesPanel;

    private SimpleTableModel<SecurityZone> securityZonesTableModel;
    private boolean canCreate;

    public SecurityZoneManagerWindow(Window owner) {
        super(owner, "Manage Security Zones", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        setModal(true);
        canCreate = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedCreateSpecific(EntityType.SECURITY_ZONE, new SecurityZone()));

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
                final String msg = MessageFormat.format(DELETE_CONFIRMATION_FORMAT, zone.getName());
                DialogDisplayer.showOptionDialog(
                        SecurityZoneManagerWindow.this,
                        WordUtils.wrap(msg, DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                        "Remove " + zone.getName(),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Remove " + zone.getName(), "Cancel"},
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
                reloadTabbedPanels();
                refreshTrees();
                try {
                    return Registry.getDefault().getRbacAdmin().findSecurityZoneByPrimaryKey(oid);
                } catch (FindException e) {
                    throw new SaveException(e);
                }
            }
        });
        ecc.setEntityEditor(new EntityEditor<SecurityZone>() {
            @Override
            public void displayEditDialog(final SecurityZone entity, final Functions.UnaryVoid<SecurityZone> afterEditListener) {
                boolean create = PersistentEntity.DEFAULT_OID == entity.getOid();
                AttemptedOperation operation = create
                        ? new AttemptedCreateSpecific(EntityType.SECURITY_ZONE, entity)
                        : new AttemptedUpdate(EntityType.SECURITY_ZONE, entity);
                boolean readOnly = !Registry.getDefault().getSecurityProvider().hasPermission(operation);
                final SecurityZonePropertiesDialog dlg = new SecurityZonePropertiesDialog(SecurityZoneManagerWindow.this, entity, readOnly);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (dlg.isConfirmed()) {
                            SecurityZone copy = new SecurityZone();
                            copy(entity, copy);
                            afterEditListener.call(dlg.getData(copy));
                        } else {
                            afterEditListener.call(null);
                        }
                    }
                });
            }
        });

        createButton.addActionListener(ecc.createCreateAction());
        editButton.addActionListener(ecc.createEditAction());
        Utilities.setDoubleClickAction(securityZonesTable, editButton);
        removeButton.addActionListener(ecc.createDeleteAction());

        enableOrDisable();
    }

    private void reloadTabbedPanels() {
        final SecurityZone selected = getSelectedSecurityZone();
        propertiesPanel.configure(selected);
        entitiesPanel.configure(selected);
    }

    private static void flushCachedZones() {
        SecurityZoneUtil.flushCachedSecurityZones();
    }

    private static void copy(SecurityZone src, SecurityZone dest) {
        dest.setOid(src.getOid());
        dest.setVersion(src.getVersion());
        dest.setName(src.getName());
        dest.setDescription(src.getDescription());
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
