package com.l7tech.console.security.rbac;

import com.l7tech.console.util.ConsoleEntityFinder;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.util.ExceptionUtils;
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
import static com.l7tech.util.Functions.propertyTransform;

/**
 * Panel which displays the NamedEntities in a SecurityZone.
 */
public class SecurityZoneEntitiesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(SecurityZoneEntitiesPanel.class.getName());
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
                final ConsoleEntityFinder entityFinder = Registry.getDefault().getEntityFinder();
                try {
                    entities.addAll(entityFinder.findByEntityTypeAndSecurityZoneOid(selected, securityZone.getOid()));
                    Collections.sort(entities, new NamedEntityComparator());
                    entitiesTableModel.setRows(entities);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Error retrieving entities of type " + selected + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    DialogDisplayer.showMessageDialog(this, "Unable to retrieve entities", "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        } else {
            entitiesTableModel.setRows(Collections.<NamedEntity>emptyList());
        }
    }
}
