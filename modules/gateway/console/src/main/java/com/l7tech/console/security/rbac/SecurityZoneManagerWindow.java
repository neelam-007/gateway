package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class SecurityZoneManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(SecurityZoneManagerWindow.class.getName());

    private JPanel contentPane;
    private JButton closeButton;
    private JButton createButton;
    private JButton editButton;
    private JButton removeButton;
    private JTable securityZonesTable;
    private JTabbedPane tabbedPanel;
    private SecurityZonePropertiesPanel propertiesPanel;

    private SimpleTableModel<SecurityZone> securityZonesTableModel;
    private final PermissionFlags flags = PermissionFlags.get(EntityType.SECURITY_ZONE);

    public SecurityZoneManagerWindow(Window owner) {
        super(owner, "Manage Security Zones", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        setModal(true);

        closeButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeButton);

        final RunOnChangeListener enableOrDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisable();
                final SecurityZone selected = getSelectedSecurityZone();
                propertiesPanel.configure(selected);
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
                TopComponents.getInstance().getAssertionRegistry().updateAssertionAccess();
            }
        });
        ecc.setEntitySaver(new EntitySaver<SecurityZone>() {
            @Override
            public SecurityZone saveEntity(SecurityZone entity) throws SaveException {
                long oid = Registry.getDefault().getRbacAdmin().saveSecurityZone(entity);
                flushCachedZones();
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
        removeButton.addActionListener(ecc.createDeleteAction(EntityType.SECURITY_ZONE, SecurityZoneManagerWindow.this));

        enableOrDisable();
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
        boolean haveSelection = securityZonesTable.getSelectedRow() >= 0;
        editButton.setEnabled(haveSelection);
        removeButton.setEnabled(flags.canDeleteSome() && haveSelection);
        createButton.setEnabled(flags.canCreateSome());
    }

    private SecurityZone getSelectedSecurityZone() {
        final int rowIndex = securityZonesTable.getSelectedRow();
        final int modelIndex = securityZonesTable.convertRowIndexToModel(rowIndex);
        return securityZonesTableModel.getRowObject(modelIndex);
    }
}
