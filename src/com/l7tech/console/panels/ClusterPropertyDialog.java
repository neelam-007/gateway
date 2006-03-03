/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.common.gui.util.TableUtil;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

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
    private final ArrayList properties = new ArrayList();
    private final Logger logger = Logger.getLogger(ClusterPropertyDialog.class.getName());

    public ClusterPropertyDialog(Frame owner) {
        super(owner, true);
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
                ClusterProperty entry = (ClusterProperty)properties.get(rowIndex);
                switch (columnIndex) {
                    case 0: return entry.getName();
                    case 1: return entry.getValue();
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
        propsTable.setModel(model);
        propsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propsTable.getTableHeader().setReorderingAllowed(false);

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(comp instanceof JComponent) {
                    ((JComponent)comp).setToolTipText(((ClusterProperty)properties.get(row)).getDescription());
                }
                return comp;
            }
        };
        propsTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

        setListeners();

        // support Enter and Esc keys
        Actions.setEscKeyStrokeDisposes(this);
        Actions.setEnterAction(this, new AbstractAction() {
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
                if (e.getClickCount() == 2)
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

    private void add() {
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {

            CaptureProperty dlg = new CaptureProperty(this, "New Cluster Property", null, null, null);
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            dlg.setVisible(true);
            if (!dlg.wasOked()) {
                return;
            }

            try {
                reg.getClusterStatusAdmin().setProperty(dlg.newKey(), dlg.newValue());
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (SaveException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (UpdateException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            }

            populate();
            TableUtil.adjustColumnWidth(propsTable, 1);
        } else {
            logger.severe("cannot get cluster status admin for removing property");
        }
    }

    private void edit() {
        ClusterProperty prop = (ClusterProperty)properties.get(propsTable.getSelectedRow());
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {

            CaptureProperty dlg = new CaptureProperty(this, "Edit Cluster Property",
                    prop.getDescription(), prop.getName(), prop.getValue());
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            dlg.setVisible(true);
            if (!dlg.wasOked()) {
                return;
            }

            try {
                if (dlg.newKey().equals(prop.getName())) {
                    reg.getClusterStatusAdmin().setProperty(dlg.newKey(), dlg.newValue());
                } else {
                    reg.getClusterStatusAdmin().setProperty(prop.getName(), null);
                    reg.getClusterStatusAdmin().setProperty(dlg.newKey(), dlg.newValue());
                }
            }  catch (RemoteException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (SaveException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (UpdateException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            }

            populate();
            TableUtil.adjustColumnWidth(propsTable, 1);
        } else {
            logger.severe("cannot get cluster status admin for editing property");
        }
    }

    private void remove() {
        ClusterProperty prop = (ClusterProperty)properties.get(propsTable.getSelectedRow());
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {
            try {
                reg.getClusterStatusAdmin().setProperty(prop.getName(), null);
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (SaveException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (UpdateException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "exception setting property", e);
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
        if (selectedRow < 0) {
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
        } else {
            removeButton.setEnabled(true);
            editButton.setEnabled(true);
        }
    }

    private void populate() {
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getClusterStatusAdmin() != null) {
            properties.clear();
            try {
                Collection allProperties = reg.getClusterStatusAdmin().getAllProperties();
                for (Iterator i = allProperties.iterator(); i.hasNext();) {
                    ClusterProperty property = (ClusterProperty)i.next();
                    if (!property.isHiddenInGui())
                        properties.add(property);
                }
                
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "exception getting properties", e);
            } catch (FindException e) {
                logger.log(Level.SEVERE, "exception getting properties", e);
            }
        } else {
            logger.severe("cannot get cluster status admin for populating dlg");
        }
        ((AbstractTableModel)propsTable.getModel()).fireTableDataChanged();
    }

    public void setVisible(boolean visible) {
        if(visible) {
            TableUtil.adjustColumnWidth(propsTable, 1);
        }
        super.setVisible(visible);
    }

    public static void main(String[] args) {
        ClusterPropertyDialog me = new ClusterPropertyDialog((Frame)null);
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
