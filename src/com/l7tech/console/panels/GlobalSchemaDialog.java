/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 8, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.gui.util.TableUtil;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * A dialog for the SSM administrator to manage the global schemas loaded on a gateway.
 *
 * @author flascelles@layer7-tech.com
 */
public class GlobalSchemaDialog extends JDialog {
    private final Logger logger = Logger.getLogger(GlobalSchemaDialog.class.getName());
    private JTable schemaTable;
    private JButton removebutton;
    private JButton editbutton;
    private JButton addbutton;
    private ArrayList globalSchemas = new ArrayList();
    private JButton helpbutton;
    private JPanel mainPanel;
    private JButton closebutton;

    public GlobalSchemaDialog(Frame owner) {
        super(owner, true);
        initialize();
    }

    public GlobalSchemaDialog(Dialog owner) {
        super(owner, true);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Manage Global Schemas");

        TableModel model = new AbstractTableModel() {
            public int getColumnCount() {
                return 2;
            }
            public int getRowCount() {
                return globalSchemas.size();
            }
            public Object getValueAt(int rowIndex, int columnIndex) {
                SchemaEntry entry = (SchemaEntry)globalSchemas.get(rowIndex);
                switch (columnIndex) {
                    case 0: return entry.getName();
                    case 1: return entry.getTns();
                    default: throw new RuntimeException("column index not supported " + columnIndex);
                }
            }
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "System ID";
                    case 1: return "Target Namespace";
                }
                return "";
            }
        };
        schemaTable.setModel(model);
        setListeners();

        // support Enter and Esc keys
        Actions.setEscKeyStrokeDisposes(this);
        Actions.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        populate();
        enableRemoveBasedOnSelection();
    }

    private void populate() {
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            logger.warning("No access to registry. Cannot populate the table.");
            return;
        }
        globalSchemas.clear();
        try {
            globalSchemas.addAll(reg.getSchemaAdmin().findAllSchemas());
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "cannot get schemas from gateway", e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get schemas from gateway", e);
        }
        ((AbstractTableModel)schemaTable.getModel()).fireTableDataChanged();
    }

    private void setListeners() {
        addbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });

        editbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removebutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        });

        helpbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        closebutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        schemaTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableRemoveBasedOnSelection();
            }
        });
    }

    private void enableRemoveBasedOnSelection() {
        // get new selection
        int selectedRow = schemaTable.getSelectedRow();
        // decide whether or not the remove button should be enabled
        if (selectedRow < 0) {
            removebutton.setEnabled(false);
            editbutton.setEnabled(false);
        } else {
            removebutton.setEnabled(true);
            editbutton.setEnabled(true);
        }
    }

    private void add() {
        SchemaEntry newEntry = new SchemaEntry();
        GlobalSchemaEntryEditor dlg = new GlobalSchemaEntryEditor(this, newEntry);
        dlg.pack();
        Dimension dim = dlg.getSize();
        dim.setSize(dim.getWidth() * 2, dim.getHeight() * 4);
        dlg.setSize(dim);
        Utilities.centerOnScreen(dlg);
        dlg.show();
        if (dlg.success) {
            // save changes to gateway
            Registry reg = Registry.getDefault();
            if (reg == null || reg.getSchemaAdmin() == null) {
                logger.warning("No access to registry. Cannot populate the table.");
                return;
            }
            try {
                reg.getSchemaAdmin().saveSchemaEntry(newEntry);
                // pickup all changes from gateway
                populate();
                TableUtil.adjustColumnWidth(schemaTable, 1);
            } catch (RemoteException e) {
                logger.log(Level.WARNING, "error saving schema entry", e);
            } catch (SaveException e) {
                logger.log(Level.WARNING, "error saving schema entry", e);
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "error saving schema entry", e);
            }
        }
    }

    private void edit() {
        SchemaEntry toedit = (SchemaEntry)globalSchemas.get(schemaTable.getSelectedRow());
        GlobalSchemaEntryEditor dlg = new GlobalSchemaEntryEditor(this, toedit);
        dlg.pack();
        Dimension dim = dlg.getSize();
        dim.setSize(dim.getWidth() * 2, dim.getHeight() * 4);
        dlg.setSize(dim);
        Utilities.centerOnScreen(dlg);
        dlg.show();
        if (dlg.success) {
            // save changes to gateway
            Registry reg = Registry.getDefault();
            if (reg == null || reg.getSchemaAdmin() == null) {
                logger.warning("No access to registry. Cannot populate the table.");
                return;
            }
            try {
                reg.getSchemaAdmin().saveSchemaEntry(toedit);
                ((AbstractTableModel)schemaTable.getModel()).fireTableDataChanged();
                TableUtil.adjustColumnWidth(schemaTable, 1);
            } catch (RemoteException e) {
                logger.log(Level.WARNING, "error saving schema entry", e);
            } catch (SaveException e) {
                logger.log(Level.WARNING, "error saving schema entry", e);
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "error saving schema entry", e);
            }
        }
    }

    private void remove() {
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            logger.warning("No access to registry. Cannot remove entry.");
            return;
        }
        SchemaEntry todelete = (SchemaEntry)globalSchemas.get(schemaTable.getSelectedRow());
        try {
            reg.getSchemaAdmin().deleteSchemaEntry(todelete);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Cannot remove global schema from gateway", e);
        } catch (DeleteException e) {
            logger.log(Level.WARNING, "Cannot remove global schema from gateway", e);
        }
        populate();
    }

    private void close() {
        GlobalSchemaDialog.this.dispose();
    }

    private void help() {
        Actions.invokeHelp(GlobalSchemaDialog.this);
    }

    public void show() {
        TableUtil.adjustColumnWidth(schemaTable, 1);
        super.show();
    }

    public static void main(String[] args) {
        GlobalSchemaDialog me = new GlobalSchemaDialog((Frame)null);
        SchemaEntry sample = new SchemaEntry();
        sample.setName("sampleSchema.xsd");
        sample.setOid(654);
        sample.setTns("http://blah.com/tns/brrpt");
        sample.setSchema("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<schema>blah</schema>");
        me.globalSchemas.add(sample);
        me.pack();
        me.show();
        System.exit(0);
    }
}
