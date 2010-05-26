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
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.SCHEMA_ENTRY;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.xml.DocumentReferenceProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private ArrayList<SchemaEntry> globalSchemas = new ArrayList<SchemaEntry>();
    private JButton helpButton;
    private JPanel mainPanel;
    private JButton closeButton;
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
            @Override
            public int getColumnCount() {
                return 2;
            }
            @Override
            public int getRowCount() {
                return globalSchemas.size();
            }
            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                SchemaEntry entry = globalSchemas.get(rowIndex);
                switch (columnIndex) {
                    case 0: return entry.getName();
                    case 1: return entry.getTns();
                    default: throw new RuntimeException("column index not supported " + columnIndex);
                }
            }

            @Override
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
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>( model );
        sorter.setSortKeys( Arrays.asList( new RowSorter.SortKey(0, SortOrder.ASCENDING) ) );
        schemaTable.setRowSorter( sorter );
        sorter.sort();
        setListeners();

        // support Enter and Esc keys
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setEnterAction(this, closeButton.getAction());
        addWindowListener(new WindowAdapter() {
            @Override
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
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    add((String)null, new ArrayList<SchemaEntry>());
                } catch (CircularReferenceException cre) {
                    JOptionPane.showMessageDialog(
                            GlobalSchemaDialog.this,
                            "You are trying to import XML Schemas that refer to each other. Circular referencing is " +
                            "currently not supported in XML Schemas.",
                            "Circular Reference Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        // Set F1 function for the help button
        setF1HelpFunction();

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        schemaTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableRemoveBasedOnSelection();
            }
        });
        schemaTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    edit();
            }
        });
        schemaTable.addKeyListener(new KeyListener () {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    edit();
                }
            }
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }

    /**
     * Set F1 help function
     */
    private void setF1HelpFunction() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        String actionName = "showHelpTopics";
        AbstractAction helpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                helpButton.doClick();
            }
        };

        helpAction.putValue(Action.NAME, actionName);
        helpAction.putValue(Action.ACCELERATOR_KEY, keyStroke);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        getRootPane().getActionMap().put(actionName, helpAction);
        getLayeredPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        getLayeredPane().getActionMap().put(actionName, helpAction);
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        ((JComponent)getContentPane()).getActionMap().put(actionName, helpAction);
    }

    private void enableRemoveBasedOnSelection() {
        // get new selection
        int selectedRow = schemaTable.getSelectedRow();
        // decide whether or not the remove button should be enabled
        boolean validSelection = selectedRow >= 0;
        boolean isSystem = validSelection && globalSchemas.get(schemaTable.convertRowIndexToModel(selectedRow)).isSystem();

        addButton.setEnabled(flags.canCreateSome());
        removeButton.setEnabled(flags.canDeleteSome() && validSelection && !isSystem);
        editButton.setText(flags.canUpdateSome() && !isSystem ? "Edit" : "View");
        editButton.setEnabled(validSelection);
    }

    private void add( final String systemIdentifier,
                      final java.util.List<SchemaEntry> dependencies ) throws CircularReferenceException {
        final SchemaEntry newEntry = new SchemaEntry();
        if ( systemIdentifier != null ) {
            newEntry.setName(systemIdentifier);
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

            checkEntryForUnresolvedImports(newEntry, dependencies);

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
                showErrorMessage(text);
            }

            // pickup all changes from gateway
            populate();
            TableUtil.adjustColumnWidth(schemaTable, 1);
        }
    }

    private void showErrorMessage(String text) {
        JOptionPane.showMessageDialog(GlobalSchemaDialog.this,
                                      Utilities.getTextDisplayComponent(text, 600, 100, -1, -1),
                                      "Unable to save schema entry",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /**
     * returns true if there was at least one unresolved import
     *
     * NOTE this code is reused (copy/paste) in SchemaValidationPropertiesDialog
     * @param schemaTobeSaved the schema that is about to be saved
     * @return true if there was at least one unresolved import
     */
    private boolean checkEntryForUnresolvedImports( final SchemaEntry schemaTobeSaved,
                                                    final java.util.List<SchemaEntry> dependants ) throws CircularReferenceException {
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

        final java.util.List<Element> dependencyElements = new ArrayList<Element>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaDoc, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                assert node instanceof Element;
                if ( node instanceof Element ) dependencyElements.add( (Element)node );
                return null;  
            }
        } );

        if ( dependencyElements.isEmpty() )
            return false;

        final ArrayList<String> unresolvedDependencyList = new ArrayList<String>();
        final Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }
        final SchemaAdmin schemaAdmin = reg.getSchemaAdmin();
        for ( final Element dependencyElement : dependencyElements ) {
            final boolean useNamespace = "import".equalsIgnoreCase( dependencyElement.getLocalName() );
            final String dependencyNamespace = dependencyElement.hasAttribute( "namespace" ) ? dependencyElement.getAttribute("namespace") : null;
            final String dependencyLocation = dependencyElement.hasAttribute( "schemaLocation" ) ? dependencyElement.getAttribute( "schemaLocation" ) : null;

            try {
                boolean hasTns = useNamespace && dependencyNamespace != null;
                final boolean tnsFound = useNamespace && !schemaAdmin.findByTNS(dependencyNamespace).isEmpty();
                final boolean locFound = !useNamespace && dependencyLocation != null && !matchNamespaceForIncludeOrRedefine(schemaAdmin.findByName(dependencyLocation),schemaTobeSaved.getTns()).isEmpty();
                if ( !locFound && !tnsFound ) {
                    // Check for circularity
                    if ( dependants != null ) {
                        boolean found = false;
                        for ( final SchemaEntry schemaEntry : dependants ) {
                            if ( dependencyLocation != null && dependencyLocation.equals( schemaEntry.getName() )) {
                                found = true;
                                break;
                            }
                            if ( hasTns && dependencyNamespace.equals( schemaEntry.getTns() ) ) {
                                found = true;
                                break;
                            }
                        }
                        if (found) throw new CircularReferenceException();
                    }

                    if ( dependencyLocation != null ) {
                        unresolvedDependencyList.add(dependencyLocation);
                    } else if ( !tnsFound && hasTns ) {
                        unresolvedDependencyList.add(dependencyNamespace);
                    }
                }
            } catch (ObjectModelException e) {
                String msg = "Error trying to look for import schema in global schema";
                logger.log(Level.SEVERE, msg, e);
                throw new RuntimeException(msg);
            }
        }

        if (!unresolvedDependencyList.isEmpty()) {
            for ( final String unresolvedDependency : unresolvedDependencyList ) {
                boolean isUrl = true;
                try {
                    new URL(unresolvedDependency);
                } catch (MalformedURLException e) {
                    isUrl = false;
                }
                String msg;
                if (isUrl) {
                    msg = "This schema has an unresolved dependency (" + unresolvedDependency + "). Click 'yes' " +
                          "to import this missing schema now, 'no' if you want the SecureSpan Gateway to try to " +
                          "try import it from the URL.";
                } else {
                    msg = "This schema has an unresolved dependency (" + unresolvedDependency + "). Click 'yes' " +
                          "to import this missing schema now, 'no' to abort.";
                }
                msg = TextUtils.breakOnMultipleLines(msg, 30);
                if ( JOptionPane.showConfirmDialog(this, msg, "Unresolved Dependencies", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    dependants.add(schemaTobeSaved);
                    add(unresolvedDependency, dependants);
                } else if (!isUrl) {
                    // if user wants to abort, then we're all done
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private Collection<SchemaEntry> matchNamespaceForIncludeOrRedefine( final Collection<SchemaEntry> schemaEntries,
                                                                        final String targetNamespace ) {
        return Functions.grep(schemaEntries, new Functions.Unary<Boolean, SchemaEntry>(){
            @Override
            public Boolean call( final SchemaEntry schemaEntry ) {
                return !schemaEntry.hasTns() || schemaEntry.getTns().equals(targetNamespace);
            }
        });
    }

    private void edit() {
        int selectedRow = schemaTable.getSelectedRow();
        if (selectedRow < 0) return;
        final SchemaEntry toEdit = globalSchemas.get(schemaTable.convertRowIndexToModel(selectedRow));
        final SchemaEntry originalSelectedSchema = toEdit.asCopy();
        final GlobalSchemaEntryEditor dlg = new GlobalSchemaEntryEditor(this, toEdit, flags.canUpdateSome());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.success) {
                    // save changes to gateway
                    Registry reg = Registry.getDefault();
                    if (reg == null || reg.getSchemaAdmin() == null) {
                        logger.warning("No access to registry. Cannot save.");
                        return;
                    }
                    try {
                        checkEntryForUnresolvedImports(toEdit, new ArrayList<SchemaEntry>());
                        reg.getSchemaAdmin().saveSchemaEntry(toEdit);
                        populate();
                    } catch (Exception e) {
                        String errMsg;
                        if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                            errMsg = "Unable to save schema entry: (name) must be unique";
                            JOptionPane.showMessageDialog(GlobalSchemaDialog.this, errMsg, "Unable to save schema entry", JOptionPane.ERROR_MESSAGE);
                        } else {
                            errMsg = "Unable to save edited schema entry:" + ExceptionUtils.getMessage(e);
                            showErrorMessage(errMsg);
                        }
                        logger.log(Level.WARNING, errMsg, e);

                        // Roll back
                        toEdit.setName(originalSelectedSchema.getName());
                        toEdit.setTns((originalSelectedSchema.getTns()));
                        toEdit.setSchema(originalSelectedSchema.getSchema());
                    }
                }
            }
        });
    }

    private void remove() {
        SchemaEntry schemasToBeDeleted = globalSchemas.get(schemaTable.convertRowIndexToModel(schemaTable.getSelectedRow()));
        Object[] options = {"Remove", "Cancel"};
        String message = "Are you sure you want to remove the selected schema?\n" + schemasToBeDeleted.getName();

        final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), message);
        final Object object;
        if(width > 600){
            object = Utilities.getTextDisplayComponent(message, 600, 100, -1, -1);
        }else{
            object = message;
        }

        int result = JOptionPane.showOptionDialog(GlobalSchemaDialog.this, object, "Confirm Global Schemas Removal",
                0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            Registry reg = Registry.getDefault();
            if (reg == null || reg.getSchemaAdmin() == null) {
                logger.warning("No access to registry. Cannot remove entry.");
                return;
            }
            try {
                reg.getSchemaAdmin().deleteSchemaEntry(schemasToBeDeleted);
            } catch (DeleteException e) {
                logger.log(Level.WARNING, "Cannot remove global schema from gateway", e);
            }
            populate();
        }
    }

    private void close() {
        // check the state of all schemas. make sure none of them contain unresolved exports
        boolean okToClose = true;
        Registry reg = Registry.getDefault();
        if (reg != null && reg.getSchemaAdmin() != null) {
            try {
                Collection<SchemaEntry> allSchemas = reg.getSchemaAdmin().findAllSchemas();
                for (final SchemaEntry schemaEntry : allSchemas) {
                    if (checkEntryForUnresolvedImports(schemaEntry, new ArrayList<SchemaEntry>())) {
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

    private static class CircularReferenceException extends Exception {}
}
