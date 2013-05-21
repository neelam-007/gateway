package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Panel which displays the NamedEntities in a SecurityZone.
 */
public class SecurityZoneEntitiesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(SecurityZoneEntitiesPanel.class.getName());
    private JPanel contentPanel;
    private JTable entitiesTable;
    private JScrollPane scrollPane;
    private JComboBox entityTypeComboBox;
    private SimpleTableModel<Entity> entitiesTableModel;
    private SecurityZone securityZone;

    public SecurityZoneEntitiesPanel() {
        final List<EntityType> entityTypes = new ArrayList<>();
        entityTypes.add(null);
        entityTypes.addAll(SecurityZoneUtil.getAllZoneableEntityTypes());
        // do not support audits as there may be a LOT of them in the zone
        entityTypes.remove(EntityType.AUDIT_MESSAGE);
        entityTypeComboBox.setModel(new DefaultComboBoxModel<EntityType>(entityTypes.toArray(new EntityType[entityTypes.size()])));
        entityTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadTable();
            }
        });
        entitiesTableModel = TableUtil.configureTable(entitiesTable, column("Name", 80, 300, 99999, new Functions.Unary<String, Entity>() {
            @Override
            public String call(final Entity entity) {
                String name = StringUtils.EMPTY;
                if (entity instanceof NamedEntity) {
                    name = ((NamedEntity) entity).getName();
                } else if (entity instanceof PublishedServiceAlias) {
                    final PublishedServiceAlias alias = (PublishedServiceAlias) entity;
                    try {
                        final PublishedService owningService = Registry.getDefault().getServiceManager().findServiceByID(String.valueOf(alias.getEntityOid()));
                        name = owningService.getName() + " alias";
                    } catch (final FindException e) {
                        name = "service id " + alias.getEntityOid() + " alias";
                    }
                } else if (entity instanceof PolicyAlias) {
                    final PolicyAlias alias = (PolicyAlias) entity;
                    try {
                        final Policy owningPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(alias.getEntityOid());
                        name = owningPolicy.getName() + " alias";
                    } catch (final FindException e) {
                        name = "policy id " + alias.getEntityOid() + " alias";
                    }
                }
                return name;
            }
        }));
        Utilities.setRowSorter(entitiesTable, entitiesTableModel);
    }

    public void configure(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
        loadTable();
    }

    @Nullable
    private EntityType getSelectedEntityType() {
        final Object selected = entityTypeComboBox.getSelectedItem();
        if (selected != null) {
            return (EntityType) selected;
        }
        return null;
    }

    private void loadTable() {
        if (securityZone != null) {
            final EntityType selected = getSelectedEntityType();
            if (selected != null) {
                final List<Entity> entities = new ArrayList<>();
                final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                try {
                    entities.addAll(rbacAdmin.findEntitiesByTypeAndSecurityZoneOid(selected, securityZone.getOid()));
                    entitiesTableModel.setRows(entities);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Error retrieving entities of type " + selected + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    DialogDisplayer.showMessageDialog(this, "Unable to retrieve entities", "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        } else {
            entitiesTableModel.setRows(Collections.<Entity>emptyList());
        }
    }
}
