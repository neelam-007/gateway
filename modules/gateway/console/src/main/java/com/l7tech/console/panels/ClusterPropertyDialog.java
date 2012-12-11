/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A dialog to view/edit/add/remove cluster-wide properties
 *
 * @author flascelles@layer7-tech.com
 */
public class ClusterPropertyDialog extends JDialog {
    private JPanel mainPanel;
    private JTable propsTable;
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private JButton helpButton;
    private JButton closeButton;
    private final ArrayList<ClusterProperty> properties = new ArrayList<ClusterProperty>();
    private final Logger logger = Logger.getLogger(ClusterPropertyDialog.class.getName());
    private Collection<ClusterPropertyDescriptor> knownProperties;
    private final PermissionFlags flags;

    public ClusterPropertyDialog(Frame owner) {
        super(owner, true);
        flags = PermissionFlags.get(EntityType.CLUSTER_PROPERTY);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Manage Cluster-Wide Properties");

        TableModel model = new AbstractTableModel() {
            public int getColumnCount() {
                return 2;
            }
            public int getRowCount() {
                return properties.size();
            }
            public Object getValueAt(int rowIndex, int columnIndex) {
                ClusterProperty entry = properties.get(rowIndex);
                switch (columnIndex) {
                    case 0: return entry.getName();
                    case 1:
                        String value = entry.getValue();
                        if (value != null) {
                            value = value.replace('\n', ' '); // because newlines get truncated
                        }
                        return value;
                    default: throw new RuntimeException("column index not supported " + columnIndex);
                }
            }
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "Key";
                    case 1: return "Value";
                }
                return "";
            }
        };
        TableColumnModel columnModel = new DefaultTableColumnModel();
        columnModel.addColumn(new TableColumn(0, 100));
        columnModel.addColumn(new TableColumn(1, 150));
        // Set headers
        for(int i = 0; i < columnModel.getColumnCount(); i++){
            TableColumn tc = columnModel.getColumn(i);
            tc.setMinWidth(50);
            tc.setHeaderValue(model.getColumnName(tc.getModelIndex()));
        }

        propsTable.setModel(model);
        propsTable.setColumnModel(columnModel);
        propsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propsTable.getTableHeader().setReorderingAllowed(false);

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(comp instanceof JComponent) {
                    ((JComponent)comp).setToolTipText(properties.get(row).getProperty(ClusterProperty.DESCRIPTION_PROPERTY_KEY));
                }
                return comp;
            }
        };
        propsTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

        //Set up tool tips for the value cells.
        DefaultTableCellRenderer renderer2 = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(comp instanceof JComponent) {
                    ClusterProperty clusterProperty = properties.get(row);
                    String cpvalue = clusterProperty.getValue();
                    if (cpvalue != null && cpvalue.length() > 20) {
                        ((JComponent)comp).setToolTipText(Utilities.toTooltip(TextUtils.escapeHtmlSpecialCharacters(cpvalue), true));
                    }
                    else {
                        ((JComponent)comp).setToolTipText(null);
                    }
                }
                return comp;
            }
        };
        propsTable.getColumnModel().getColumn(1).setCellRenderer(renderer2);

        setListeners();

        // support Enter and Esc keys
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();

            }
        });
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        Utilities.equalizeButtonSizes(new JButton[] {
                addButton,
                editButton,
                removeButton,
                helpButton,
                closeButton
        });

        populate();
        enableRemoveBasedOnSelection();
    }

    private void close() {
        dispose();
    }

    private void setListeners() {
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        // Set F1 funcation for the help button
        setF1HelpFunction();

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        propsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableRemoveBasedOnSelection();
            }
        });
        propsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == 1)
                    edit();
            }
        });
        propsTable.addKeyListener(new KeyListener () {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    edit();
                }
            }
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });
    }

    /**
     * Set F1 help function
     */
    private void setF1HelpFunction() {
        KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        String actionName = "showHelpTopics";
        AbstractAction helpAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                helpButton.doClick();
            }
        };

        helpAction.putValue(Action.NAME, actionName);
        helpAction.putValue(Action.ACCELERATOR_KEY, accel);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, actionName);
        getRootPane().getActionMap().put(actionName, helpAction);
        getLayeredPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, actionName);
        getLayeredPane().getActionMap().put(actionName, helpAction);
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, actionName);
        ((JComponent)getContentPane()).getActionMap().put(actionName, helpAction);
    }

    private void add() {
        final Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {

            if (knownProperties == null) {
                knownProperties = reg.getClusterStatusAdmin().getAllPropertyDescriptors();
            }

            final CaptureProperty dlg = new CaptureProperty(this, "New Cluster Property", null, new ClusterProperty(), knownProperties, flags.canUpdateSome());
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            DialogDisplayer.display(dlg, new Runnable() {
                public void run() {
                    if (!dlg.wasOked()) {
                        return;
                    }

                    try {
                        reg.getClusterStatusAdmin().saveProperty(dlg.getProperty());
                    } catch (DuplicateObjectException e) {
                        DialogDisplayer.showMessageDialog(ClusterPropertyDialog.this, "Cluster-Wide Property Error",
                                "Cannot save duplicate property '" + dlg.getProperty().getName() + "'.", null);
                    } catch (SaveException e) {
                        logger.log(Level.SEVERE, "exception setting property", e);
                    } catch (UpdateException e) {
                        logger.log(Level.SEVERE, "exception setting property", e);
                    } catch (DeleteException e) {
                        logger.log(Level.SEVERE, "exception setting property", e);
                    }

                    populate();
                }
            });
        } else {
            logger.severe("cannot get cluster status admin for removing property");
        }
    }

    private void edit() {
        if (!propsTable.isRowSelected(propsTable.getSelectedRow())) // double-right-clicked
            return;
        final ClusterProperty prop = properties.get(propsTable.getSelectedRow());
        final Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {

            final boolean canEdit = flags.canUpdateSome();

            String title = canEdit ? "Edit Cluster Property" : "View Cluster Property";

            if (knownProperties == null && canEdit) {
                knownProperties = reg.getClusterStatusAdmin().getAllPropertyDescriptors();
            }

            final CaptureProperty dlg = new CaptureProperty(this, title,
                    prop.getProperty(ClusterProperty.DESCRIPTION_PROPERTY_KEY), prop, knownProperties, canEdit);
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            DialogDisplayer.display(dlg, new Runnable() {
                public void run() {
                    if (!dlg.wasOked()) {
                        return;
                    }

                    if (canEdit) {
                        try {
                            reg.getClusterStatusAdmin().saveProperty(prop);
                        } catch (SaveException e) {
                            logger.log(Level.SEVERE, "exception setting property", e);
                        } catch (UpdateException e) {
                            logger.log(Level.SEVERE, "exception setting property", e);
                        } catch (DeleteException e) {
                            logger.log(Level.SEVERE, "exception setting property", e);
                        }
                    }

                    populate();
                }
            });
        } else {
            logger.severe("cannot get cluster status admin for editing property");
        }
    }

    private void remove() {
        ClusterProperty prop = properties.get(propsTable.getSelectedRow());
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {
            try {
                reg.getClusterStatusAdmin().deleteProperty(prop);
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "exception removing property", e);
            }
            populate();
        } else {
            logger.severe("cannot get cluster status admin for removing property");
        }
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void enableRemoveBasedOnSelection() {
        // get new selection
        int selectedRow = propsTable.getSelectedRow();
        // decide whether or not the remove button should be enabled
        boolean removeAndEditEnabled = selectedRow >= 0;

        addButton.setEnabled(flags.canCreateSome());
        removeButton.setEnabled(flags.canDeleteSome() && removeAndEditEnabled);

        if (flags.canUpdateSome()) editButton.setText("Edit");
        else editButton.setText("View");

        editButton.setEnabled(removeAndEditEnabled);
    }

    private void populate() {
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {
            properties.clear();
            try {
                Collection<ClusterProperty> allProperties = reg.getClusterStatusAdmin().getAllProperties();
                for (ClusterProperty property : allProperties) {
                    if (!property.isHiddenProperty())
                        properties.add(property);
                }
                Collections.sort(properties);

            } catch (FindException e) {
                logger.log(Level.SEVERE, "exception getting properties", e);
            }
        } else {
            logger.severe("cannot get cluster status admin for populating dlg");
        }
        ((AbstractTableModel)propsTable.getModel()).fireTableDataChanged();
    }

    public static void main(String[] args) {
        ClusterPropertyDialog me = new ClusterPropertyDialog(null);
        ClusterProperty sample = new ClusterProperty();
        sample.setName("com.l7tech.gateway.enablePhotonSequencer");
        sample.setValue("true");
        me.properties.add(sample);
        sample = new ClusterProperty();
        sample.setName("com.l7tech.gateway.reloadTargetOnHBXTrigger");
        sample.setValue("false");
        me.properties.add(sample);
        me.pack();
        me.setVisible(true);
        System.exit(0);
    }
}
