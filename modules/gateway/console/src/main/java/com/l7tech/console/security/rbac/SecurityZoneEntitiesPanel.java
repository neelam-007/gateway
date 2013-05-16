package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

/**
 * Panel which displays the NamedEntities in a SecurityZone.
 */
public class SecurityZoneEntitiesPanel extends JPanel {
    private JPanel contentPanel;
    private JTable entitiesTable;
    private JScrollPane scrollPane;
    private JComboBox entityTypeComboBox;
    private SimpleTableModel<NamedEntity> entitiesTableModel;
    private SecurityZone securityZone;

    public SecurityZoneEntitiesPanel() {
        final List<EntityType> entityTypes = new ArrayList<>();
        entityTypes.add(null);
        entityTypes.addAll(SecurityZoneUtil.getAllZoneableEntityTypes());
        entityTypeComboBox.setModel(new DefaultComboBoxModel<EntityType>(entityTypes.toArray(new EntityType[entityTypes.size()])));
        entityTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadTable();
            }
        });
        entitiesTableModel = TableUtil.configureTable(entitiesTable, column("Name", 80, 300, 99999, propertyTransform(NamedEntity.class, "name")));
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
                final List<NamedEntity> entities = new ArrayList<>();
                switch (selected) {
                    case POLICY:
                        entities.addAll(Registry.getDefault().getPolicyAdmin().findBySecurityZoneOid(securityZone.getOid()));
                    default:
                        Collections.sort(entities, new NamedEntityComparator());
                        entitiesTableModel.setRows(entities);
                }
            }
        } else {
            entitiesTableModel.setRows(Collections.<NamedEntity>emptyList());
        }
    }
}
