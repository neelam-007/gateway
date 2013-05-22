package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
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
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
    private static final String CASE_INSENSITIVE_FLAG = "(?i)";
    private JPanel contentPanel;
    private JTable entitiesTable;
    private JScrollPane scrollPane;
    private JComboBox entityTypeComboBox;
    private JTextField filterTextField;
    private JButton filterButton;
    private JButton clearButton;
    private SimpleTableModel<Entity> entitiesTableModel;
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
        enableDisable();
    }

    private void enableDisable() {
        entityTypeComboBox.setEnabled(securityZone != null);
        entitiesTable.setEnabled(securityZone != null);
        filterTextField.setEnabled(securityZone != null);
        filterButton.setEnabled(securityZone != null && filterTextField.getText().length() > 0);
        clearButton.setEnabled(securityZone != null && filterTextField.getText().length() > 0);
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
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ((TableRowSorter) entitiesTable.getRowSorter()).setRowFilter(RowFilter.regexFilter(CASE_INSENSITIVE_FLAG + filterTextField.getText().trim(), 0));
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                filterTextField.setText(StringUtils.EMPTY);
                ((TableRowSorter) entitiesTable.getRowSorter()).setRowFilter(null);
                enableDisable();
            }
        });
        filterTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(final KeyEvent e) {
            }

            @Override
            public void keyPressed(final KeyEvent e) {
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                enableDisable();
            }
        });
    }

    private void initTable() {
        entitiesTableModel = TableUtil.configureTable(entitiesTable, column("Name", 80, 300, 99999, new Functions.Unary<String, Entity>() {
            @Override
            public String call(final Entity entity) {
                String name;
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
                } else if (entity instanceof SsgKeyMetadata) {
                    final SsgKeyMetadata metadata = (SsgKeyMetadata) entity;
                    name = metadata.getAlias();
                } else if (entity instanceof ResourceEntry) {
                    final ResourceEntry resource = (ResourceEntry) entity;
                    name = resource.getUri();
                } else if (entity instanceof HttpConfiguration) {
                    final HttpConfiguration httpConfig = (HttpConfiguration) entity;
                    name = httpConfig.getProtocol() + " " + httpConfig.getHost() + " " + httpConfig.getPort();
                } else {
                    logger.log(Level.WARNING, "Unable to determine display name for entity: " + entity);
                    name = "unknown entity";
                }
                return name;
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
            // do not support audits as there may be a LOT of them in the zone
            entityTypes.remove(EntityType.AUDIT_MESSAGE);
            // user is not aware of the UDDI entities under the hood - they inherit the security zone from the published service
            entityTypes.remove(EntityType.UDDI_PROXIED_SERVICE_INFO);
            entityTypes.remove(EntityType.UDDI_SERVICE_CONTROL);
            // user is not aware that JMS involves two entity types - they share the same security zone
            entityTypes.remove(EntityType.JMS_ENDPOINT);
        }
        entityTypeComboBox.setModel(new DefaultComboBoxModel<EntityType>(entityTypes.toArray(new EntityType[entityTypes.size()])));
        entityTypeComboBox.setSelectedItem(previouslySelected);
    }

    private void loadTable() {
        if (securityZone != null) {
            final EntityType selected = getSelectedEntityType();
            if (selected != null) {
                final List<Entity> entities = new ArrayList<>();
                final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                try {
                    if (EntityType.SSG_KEY_ENTRY == selected) {
                        entities.addAll(rbacAdmin.findEntitiesByClassAndSecurityZoneOid(SsgKeyMetadata.class, securityZone.getOid()));
                    } else {
                        entities.addAll(rbacAdmin.findEntitiesByTypeAndSecurityZoneOid(selected, securityZone.getOid()));
                    }
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
