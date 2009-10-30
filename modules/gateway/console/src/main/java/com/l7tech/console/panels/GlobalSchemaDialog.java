/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 8, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.SCHEMA_ENTRY;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private ArrayList<SchemaEntry> globalSchemas = new ArrayList<SchemaEntry>();
    private JButton helpbutton;
    private JPanel mainPanel;
    private JButton closebutton;
    private final PermissionFlags flags;

    public GlobalSchemaDialog(Frame owner) {
        super(owner, true);
        flags = PermissionFlags.get(SCHEMA_ENTRY);
        initialize();
    }

    public GlobalSchemaDialog(Dialog owner) {
        super(owner, true);
        flags = PermissionFlags.get(SCHEMA_ENTRY);
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
                SchemaEntry entry = globalSchemas.get(rowIndex);
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
        schemaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        schemaTable.getTableHeader().setReorderingAllowed(false);
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
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get schemas from gateway", e);
        }
        ((AbstractTableModel)schemaTable.getModel()).fireTableDataChanged();
    }

    private void setListeners() {
        addbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    add((String)null, (java.util.List<SchemaEntry>)null);
                } catch (IOException e1) {
                    // wont happen
                }
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
        // Set F1 funcation for the help button
        setF1HelpFunction();

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
        schemaTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    edit();
            }
        });
        schemaTable.addKeyListener(new KeyListener () {
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
                helpbutton.doClick();
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

    private void enableRemoveBasedOnSelection() {
        // get new selection
        int selectedRow = schemaTable.getSelectedRow();
        // decide whether or not the remove button should be enabled
        boolean validSelection = selectedRow >= 0;
        boolean isSystem = validSelection && globalSchemas.get(selectedRow).isSystem();

        addbutton.setEnabled(flags.canCreateSome());
        removebutton.setEnabled(flags.canDeleteSome() && validSelection && !isSystem);
        editbutton.setText(flags.canUpdateSome() && !isSystem ? "Edit" : "View");
        editbutton.setEnabled(validSelection);
    }

    private void add(String systemid, java.util.List<SchemaEntry> deps) throws IOException {
        final SchemaEntry newEntry = new SchemaEntry();
        if (systemid != null) {
            newEntry.setName(systemid);
        }
        final GlobalSchemaEntryEditor dlg = new GlobalSchemaEntryEditor(this, newEntry, flags.canUpdateSome());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        if (dlg.success) {
            // save changes to gateway
            Registry reg = Registry.getDefault();
            if (reg == null || reg.getSchemaAdmin() == null) {
                logger.warning("No access to registry. Cannot save.");
                return;
            }
            try {
                checkEntryForUnresolvedImports(newEntry, deps);
            } catch (CircularReferenceException e) {
                JOptionPane.showMessageDialog(this, "You are trying to import XML Schemas that refer to " +
                                                    "each other. Circular referencing is currently not " +
                                                    "supported in XML Schemas.",
                                              "Circular Reference Error", JOptionPane.ERROR_MESSAGE);
                throw new IOException(e.getMessage());
            } catch (IOException e) {
                logger.log(Level.INFO, "circular ref caught", e);
                return;
            }
            Throwable err = null;
            try {
                reg.getSchemaAdmin().saveSchemaEntry(newEntry);
            } catch (SaveException e) {
                err = e;
            } catch (UpdateException e) {
                err = e;
            }

            if (err != null) {
                String text = "Unable to save schema entry: " + ExceptionUtils.getMessage(err);
                logger.log(Level.WARNING, text, err);
                JOptionPane.showMessageDialog(this, text, "Unable to save schema entry", JOptionPane.ERROR_MESSAGE);
            }

            // pickup all changes from gateway
            populate();
            TableUtil.adjustColumnWidth(schemaTable, 1);
        }
    }

    private void showErrorMessage(String text) {
        JTextPane tp = new JTextPane();
        JLabel proto = new JLabel(" ");
        tp.setBackground(proto.getBackground());
        tp.setForeground(proto.getForeground());
        tp.setFont(proto.getFont());
        tp.setEditable(false);
        tp.setText(text);
        JScrollPane sp = new JScrollPane(tp);
        sp.setPreferredSize(new Dimension(800, -1));
        JOptionPane.showMessageDialog(GlobalSchemaDialog.this,
                                      sp,
                                      "Unable to save schema entry",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private class CircularReferenceException extends Exception {
        public CircularReferenceException(String msg) {
            super(msg);
        }
    }

    /**
     * returns true if there was at least one unresolved import
     *
     * NOTE this code is reused (copy/paste) in SchemaValidationPropertiesDialog
     * @param schemaTobeSaved the schema that is about to be saved
     * @return true if there was at least one unresolved import
     */
    private boolean checkEntryForUnresolvedImports(SchemaEntry schemaTobeSaved, java.util.List<SchemaEntry> dependants) throws CircularReferenceException, IOException {
        final Document schemaDoc;
        try {
            String schemaString = schemaTobeSaved.getSchema();
            if (schemaString == null) schemaString = "";
            schemaDoc = XmlUtil.stringToDocument(schemaString);
        } catch (SAXException e) {
            String msg = "cannot get xml doc from schema property";
            logger.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg);
        }
        Element schemael = schemaDoc.getDocumentElement();

        //noinspection unchecked
        java.util.List<Element> listofimports = DomUtils.findChildElementsByName(schemael, schemael.getNamespaceURI(), "import");
        if (listofimports.isEmpty())
            return false;

        ArrayList<String> unresolvedImportsList = new ArrayList<String>();
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }
        for (Element importEl : listofimports) {
            String importns = importEl.getAttribute("namespace");
            String importloc = importEl.getAttribute("schemaLocation");

            if (importns == null || importloc == null) {
                throw new IllegalStateException("The Element method, getAttribute should never return null.");
            } else {
                importloc = importloc.trim();
                importns = importns.trim();
            }

            // first, check dependants
            if (dependants != null) {
                boolean foundindep = false;
                for (SchemaEntry schemadep : dependants) {
                    if (schemadep.getName().equals(importloc)) {
                        foundindep = true;
                        break;
                    }
                    if (schemadep.getTns().equals(importns)) {
                        foundindep = true;
                        break;
                    }
                }
                if (foundindep) throw new CircularReferenceException("Namespace " + importns + " involved in circular reference");
            }

            // then check on SSG
            try {
                final boolean tnsFound = !(importns.isEmpty() || reg.getSchemaAdmin().findByTNS(importns).isEmpty());
                final boolean locFound = !(importloc.isEmpty() || reg.getSchemaAdmin().findByName(importloc).isEmpty());
                if ( !locFound ) {
                    if (! importloc.isEmpty()) {
                        unresolvedImportsList.add(importloc);
                    } else if (reg.getSchemaAdmin().findByTNS(importns).isEmpty()) {
                        unresolvedImportsList.add(importns);
                    }
                } else if ( !tnsFound ) {
                    unresolvedImportsList.add(importns);
                }
            } catch (ObjectModelException e) {
                String msg = "Error trying to look for import schema in global schema";
                logger.log(Level.SEVERE, msg, e);
                throw new RuntimeException(msg);
            }
        }
        if (!unresolvedImportsList.isEmpty()) {
            for (String unresolvedimportname : unresolvedImportsList) {
                boolean isurl = true;
                try {
                    new URL(unresolvedimportname);
                } catch (MalformedURLException e) {
                    isurl = false;
                }
                String msg;
                if (isurl) {
                    msg = "This schema has an unresolved import (" + unresolvedimportname + "). Click 'yes' " +
                          "to import this missing schema now, 'no' if you want the SecureSpan Gateway to try to " +
                          "try import it from the URL.";
                } else {
                    msg = "This schema has an unresolved import (" + unresolvedimportname + "). Click 'yes' " +
                          "to import this missing schema now, 'no' to abort.";
                }
                msg = TextUtils.breakOnMultipleLines(msg, 30);
                if (JOptionPane.showConfirmDialog(this, msg, "Unresolved Imports", JOptionPane.YES_NO_OPTION) ==
                    JOptionPane.YES_OPTION) {
                    if (dependants == null) dependants = new ArrayList<SchemaEntry>();
                    dependants.add(schemaTobeSaved);
                    add(unresolvedimportname, dependants);
                } else if (!isurl) {
                    // if user wants to abort, then we're all done
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private void edit() {
        int selectedRow = schemaTable.getSelectedRow();
        if (selectedRow < 0) return;
        final SchemaEntry toedit = globalSchemas.get(selectedRow);
        final GlobalSchemaEntryEditor dlg = new GlobalSchemaEntryEditor(this, toedit, flags.canUpdateSome() && !toedit.isSystem());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.success) {
                    // save changes to gateway
                    Registry reg = Registry.getDefault();
                    if (reg == null || reg.getSchemaAdmin() == null) {
                        logger.warning("No access to registry. Cannot save.");
                        return;
                    }
                    Throwable err = null;
                    try {
                        checkEntryForUnresolvedImports(toedit, null);
                        reg.getSchemaAdmin().saveSchemaEntry(toedit);
                        populate();
                    } catch (Exception e) {
                        err = e;
                    }

                    if (err != null) {
                        String mess = "Unable to save edited schema: " + ExceptionUtils.getMessage(err);
                        logger.log(Level.WARNING, mess, err);
                        showErrorMessage(mess);
                    }
                }
            }
        });
    }

    private void remove() {
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            logger.warning("No access to registry. Cannot remove entry.");
            return;
        }
        SchemaEntry todelete = globalSchemas.get(schemaTable.getSelectedRow());
        try {
            reg.getSchemaAdmin().deleteSchemaEntry(todelete);
        } catch (DeleteException e) {
            logger.log(Level.WARNING, "Cannot remove global schema from gateway", e);
        }
        populate();
    }

    private void close() {
        // check the state of all schemas. make sure none of them contain unresolved exports
        boolean okToClose = true;
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getSchemaAdmin() != null) {
            try {
                Collection<SchemaEntry> allschemas = reg.getSchemaAdmin().findAllSchemas();
                for (SchemaEntry schemaEntry : allschemas) {
                    if (checkEntryForUnresolvedImports(schemaEntry, null)) {
                        okToClose = false;
                        break;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not get schemas", e);
            }
        } else {
            logger.warning("No access to registry. Cannot check for unresolved exports.");
        }

        // prevent closing this dialog if the state is known to be dirty
        if (okToClose) {
            GlobalSchemaDialog.this.dispose();
        }
    }

    private void help() {
        Actions.invokeHelp(GlobalSchemaDialog.this);
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
        me.setVisible(true);
        System.exit(0);
    }
}
