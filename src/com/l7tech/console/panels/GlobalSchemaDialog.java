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
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();

            }
        });
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

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
                logger.warning("No access to registry. Cannot save.");
                return;
            }
            try {
                checkEntryForUnresolvedImports(newEntry);
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

    /**
     * returns true if there was at least one unresolved import
     */
    private boolean checkEntryForUnresolvedImports(SchemaEntry schemaTobeSaved) {
        Document schemaDoc = null;
        try {
            schemaDoc = XmlUtil.stringToDocument(schemaTobeSaved.getSchema());
        } catch (IOException e) {
            String msg = "cannot get xml doc from schema property";
            logger.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg);
        } catch (SAXException e) {
            String msg = "cannot get xml doc from schema property";
            logger.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg);
        }
        Element schemael = schemaDoc.getDocumentElement();
        java.util.List listofimports = XmlUtil.findChildElementsByName(schemael, schemael.getNamespaceURI(), "import");
        if (listofimports.isEmpty()) return false;
        ArrayList unresolvedImportsList = new ArrayList();
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }
        for (Iterator iterator = listofimports.iterator(); iterator.hasNext();) {
            Element importEl = (Element) iterator.next();
            String importns = importEl.getAttribute("namespace");
            String importloc = importEl.getAttribute("schemaLocation");
            try {
                if (importloc == null || reg.getSchemaAdmin().findByName(importloc).isEmpty()) {
                    if (importns == null || reg.getSchemaAdmin().findByTNS(importns).isEmpty()) {
                        if (importloc != null) {
                            unresolvedImportsList.add(importloc);
                        } else {
                            unresolvedImportsList.add(importns);
                        }
                    }
                }
            } catch (ObjectModelException e) {
                String msg = "Error trying to look for import schema in global schema";
                logger.log(Level.SEVERE, msg, e);
                throw new RuntimeException(msg);
            }  catch (RemoteException e) {
                String msg = "Error trying to look for import schema in global schema";
                logger.log(Level.SEVERE, msg, e);
                throw new RuntimeException(msg);
            }
        }
        if (!unresolvedImportsList.isEmpty()) {
            StringBuffer msg = new StringBuffer("This schema contains the following unresolved imported schemas:\n");
            for (Iterator iterator = unresolvedImportsList.iterator(); iterator.hasNext();) {
                msg.append(iterator.next());
                msg.append("\n");
            }
            msg.append("You must add those unresolved schemas now.");
            JOptionPane.showMessageDialog(this, msg, "Unresolved Imports", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
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
                logger.warning("No access to registry. Cannot save.");
                return;
            }
            try {
                checkEntryForUnresolvedImports(toedit);
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
        // check the state of all schemas. make sure none of them contain unresolved exports
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getSchemaAdmin() != null) {
            Collection allschemas = null;
            try {
                allschemas = reg.getSchemaAdmin().findAllSchemas();
            } catch (RemoteException e) {
                logger.log(Level.WARNING, "could not get schemas", e);
                GlobalSchemaDialog.this.dispose();
            } catch (FindException e) {
                logger.log(Level.WARNING, "could not get schemas", e);
                GlobalSchemaDialog.this.dispose();
            }
            boolean okToClose = true;
            for (Iterator iterator = allschemas.iterator(); iterator.hasNext();) {
                SchemaEntry schemaEntry = (SchemaEntry) iterator.next();
                if (checkEntryForUnresolvedImports(schemaEntry)) {
                    okToClose = false;
                    break;
                }
            }
            // prevent closing this dialog if the state is dirty
            if (!okToClose) return;
        } else {
            logger.warning("No access to registry. Cannot check for unresolved exports.");
        }
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
