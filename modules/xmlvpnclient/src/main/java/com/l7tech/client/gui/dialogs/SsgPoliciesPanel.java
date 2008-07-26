/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui.dialogs;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.TextEntryPanel;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wssp.PolicyConversionException;
import com.l7tech.policy.wssp.WsspReader;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;
import com.l7tech.client.gui.Gui;
import com.l7tech.client.gui.policy.PolicyTreeCellRenderer;
import com.l7tech.client.gui.policy.PolicyTreeModel;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.Constants;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreeModel;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Service Policies panel for the {@link SsgPropertyDialog}.
 */
class SsgPoliciesPanel extends JPanel {
    private static final Logger log = Logger.getLogger(SsgPoliciesPanel.class.getName());

    private static final int COL_NS = 0;
    private static final int COL_SA = 1;
    private static final int COL_PU = 2;
    private static final int COL_MATCHTYPE = 3;
    private static final int COL_LOCK = 4;

    /** How many columns in the table. */
    private static final int COL_COUNT = 5;

    private static final String MATCH_EQUALS = "exact match";
    private static final String MATCH_STARTSWITH = "begins with";
    private static final String[] MATCH_TYPES = new String[] { MATCH_EQUALS, MATCH_STARTSWITH };

    private PolicyManager policyCache; // transient policies
    private PolicyAttachmentKey lastSelectedPolicy = null;

    //   View for Service Policies pane
    private JTree policyTree;
    private JTable policyTable;
    private ArrayList displayPolicies;
    private DisplayPolicyTableModel displayPolicyTableModel;
    private JButton importButton;
    private JButton exportButton;
    private JButton changeButton;
    private JButton deleteButton;

    public SsgPoliciesPanel() {
        init();
    }

    public void setPolicyCache(PolicyManager policyCache) {
        this.policyCache = policyCache;
        updatePolicyPanel();
    }

    private boolean isConfigOnly() {
        return Gui.getInstance().isConfigOnly();
    }

    private void init() {
        int y = 0;
        setLayout(new GridBagLayout());
        JPanel pane = this;
        setBorder(BorderFactory.createEmptyBorder());

        pane.add(new JLabel("<HTML><h4>Service Policies Being Cached by "+ Constants.APP_NAME +"</h4></HTML>"),
                 new GridBagConstraints(0, y, 1, 1, 1.0, 0.0,
                                        GridBagConstraints.NORTHWEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(14, 6, 0, 6), 0, 0));

        JButton buttonFlushPolicies = new JButton("Clear Policy Cache");
        buttonFlushPolicies.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                policyCache.clearPolicies();
                updatePolicyPanel();
            }
        });
        buttonFlushPolicies.setVisible(!isConfigOnly());
        pane.add(Box.createGlue(),
                 new GridBagConstraints(1, y, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.EAST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(0, 0, 0, 6), 0, 0));
        pane.add(buttonFlushPolicies,
                 new GridBagConstraints(2, y++, 2, 1, 0.0, 0.0,
                                        GridBagConstraints.EAST,
                                        GridBagConstraints.NONE,
                                        new Insets(14, 6, 0, 6), 0, 0));

        pane.add(new JLabel("Services with Cached Policies:"),
                 new GridBagConstraints(0, y, 2, 1, 0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(6, 6, 0, 6), 0, 0));
        pane.add(Box.createGlue(),
                 new GridBagConstraints(2, y++, 1, 1, 1.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(6, 6, 0, 6), 0, 0));

        displayPolicies = new ArrayList();
        displayPolicyTableModel = new DisplayPolicyTableModel();
        policyTable = new JTable(displayPolicyTableModel);
        policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        policyTable.setCellSelectionEnabled(false);
        policyTable.setRowSelectionAllowed(true);
        policyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        policyTable.setAutoCreateColumnsFromModel(true);
        final TableColumn colNs = policyTable.getColumnModel().getColumn(COL_NS);
        final TableColumn colSa = policyTable.getColumnModel().getColumn(COL_SA);
        final TableColumn colPu = policyTable.getColumnModel().getColumn(COL_PU);
        final TableColumn colMt = policyTable.getColumnModel().getColumn(COL_MATCHTYPE);
        final TableColumn colLk = policyTable.getColumnModel().getColumn(COL_LOCK);
        colNs.setHeaderValue("Body Namespace");
        colSa.setHeaderValue("SOAPAction");
        colPu.setHeaderValue("Proxy URI");
        colMt.setHeaderValue("Match Type");
        colMt.setCellRenderer(new ComboBoxCellRenderer(MATCH_TYPES));
        colMt.setCellEditor(new ComboBoxCellEditor(MATCH_TYPES));
        setColumnSize(colMt, " Match Type ", new JComboBox(MATCH_TYPES));
        colLk.setHeaderValue("Lock");
        colLk.setCellRenderer(new LockCellRenderer());
        colLk.setCellEditor(policyTable.getDefaultEditor(Boolean.class));
        setColumnSize(colLk, "  Lock  ", new JCheckBox());
        policyTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane policyTableSp = new JScrollPane(policyTable);
        policyTableSp.setPreferredSize(new Dimension(120, 120));
        pane.add(policyTableSp,
                 new GridBagConstraints(0, y, 3, 1, 100.0, 1.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(0, 6, 3, 3), 0, 0));
        JPanel policyButtons = new JPanel();
        policyButtons.setLayout(new BoxLayout(policyButtons, BoxLayout.Y_AXIS));
        policyButtons.add(getChangeButton());
        policyButtons.add(getDeleteButton());
        policyButtons.add(Box.createGlue());
        policyButtons.add(getImportButton());
        policyButtons.add(getExportButton());
        Utilities.equalizeButtonSizes(new AbstractButton[] { getImportButton(),
                                                             getExportButton(),
                                                             getChangeButton(),
                                                             getDeleteButton() });
        pane.add(policyButtons,
                 new GridBagConstraints(3, y++, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.NORTHWEST,
                                        GridBagConstraints.VERTICAL,
                                        new Insets(0, 0, 3, 6), 0, 0));

        pane.add(new JLabel("Associated Policy:"),
                 new GridBagConstraints(0, y++, GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(4, 6, 0, 6), 0, 0));

        policyTree = new JTree((TreeModel)null);
        policyTree.setCellRenderer(new PolicyTreeCellRenderer());
        JScrollPane policyTreeSp = new JScrollPane(policyTree);
        policyTreeSp.setPreferredSize(new Dimension(120, 120));
        pane.add(policyTreeSp,
                 new GridBagConstraints(0, y++, GridBagConstraints.REMAINDER, 1, 100.0, 100.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(2, 6, 6, 6), 3, 3));

        policyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                displaySelectedPolicy();
            }
        });
    }

    private void setColumnSize(TableColumn col, String columnTitle, JComponent cellComponent) {
        int lockWidth = (int)new JLabel(columnTitle).getPreferredSize().getWidth() + 8;
        int matchWidth = (int)cellComponent.getPreferredSize().getWidth() + 8;
        int width = Math.max(lockWidth, matchWidth);
        col.setMinWidth(width);
        col.setMaxWidth(width);
    }

    private void importPolicyFromWsdl() {
        UrlPanel p = new UrlPanel();
        OkCancelDialog dialog =
                OkCancelDialog.createOKCancelDialog(getRootPane(),"Enter a WSDL URL",true, p);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.setVisible(true);

        String urlString = (String)dialog.getValue();
        if (urlString == null) return; // Canceled

        try {
            final Document wsdlDoc = XmlUtil.parse(new URL(urlString).openStream());
            log.fine("Downloaded WSDL document: " + XmlUtil.nodeToFormattedString(wsdlDoc));
            log.fine("Scanning WSDL document for policies...");
            final Wsdl wsdl = Wsdl.newInstance(urlString, wsdlDoc);
            Binding binding = chooseBinding(wsdl);
            if (binding == null) return; // canceled

            // Find all already-used proxy URIs
            Set usedUris = new HashSet();
            Set paks = policyCache.getPolicyAttachmentKeys();
            for (Iterator i = paks.iterator(); i.hasNext();) {
                PolicyAttachmentKey pak = (PolicyAttachmentKey)i.next();
                usedUris.add(pak.getProxyUri());
            }

            List operations = binding.getBindingOperations();
            for (Iterator i = operations.iterator(); i.hasNext();) {
                BindingOperation operation = (BindingOperation)i.next();

                org.apache.ws.policy.Assertion wsspIn  = wsdl.getEffectiveInputPolicy(binding, operation);
                org.apache.ws.policy.Assertion wsspOut = wsdl.getEffectiveOutputPolicy(binding, operation);

                WsspReader converter = new WsspReader();
                try {
                    Assertion all = converter.convertFromWssp(
                            (org.apache.ws.policy.Policy)wsspIn.normalize(wsdl.getPolicyRegistry()),
                            (org.apache.ws.policy.Policy)wsspOut.normalize(wsdl.getPolicyRegistry()));

                    Policy newPolicy = new Policy(all, null);

                    // Make a new unique uri
                    String proxyUriBase = "/" + binding.getQName().getLocalPart();
                    String proxyUri = proxyUriBase;
                    int num = 1;
                    while (usedUris.contains(proxyUri))
                        proxyUri = proxyUriBase + num++;

                    // attach this policy at the appropriate location
                    String soapAction = SoapUtil.findSoapAction(operation);
                    String uri = SoapUtil.findTargetNamespace(wsdl.getDefinition(), operation);
                    PolicyAttachmentKey pak = new PolicyAttachmentKey(uri, soapAction, proxyUri);
                    pak.setBeginsWithMatch(false);
                    pak.setPersistent(true);
                    try {
                        policyCache.setPolicy(pak, newPolicy);
                    } catch (PolicyLockedException e1) {
                        // extremely unlikely to happen -- proxyUri was unique when we created it a second ago
                        // Can only happen if a client request thread causes this exact PAK to be created during this
                        // time window
                        throw new PolicyConversionException(e1);
                    }
                    updatePolicyPanel();
                    log.log(Level.INFO, "Successfully imported policy for binding=" + binding.getQName() + " operation=" + operation);

                } catch (PolicyConversionException e1) {
                    log.log(Level.WARNING, "Unable to import policy", e1);
                    JOptionPane.showMessageDialog(getRootPane(),
                                                  "Unable to convert policy for binding " +
                                                          binding.getQName().getLocalPart() + ": " + e1.getMessage() +
                                                  "\n\nThis binding will not be imported.",
                                                  "Unable to import policy",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (IOException e1) {
            log.log(Level.WARNING, "Error reading WSDL URL", e1);
            JOptionPane.showMessageDialog(getRootPane(),
                                          "Unable to read the specified URL: " + e1.getMessage(),
                                          "Unable to read URL",
                                          JOptionPane.ERROR_MESSAGE);
        } catch (SAXException e1) {
            log.log(Level.WARNING, "Error parsing WSDL XML", e1);
            JOptionPane.showMessageDialog(getRootPane(),
                                          "Unable to parse the specified WSDL: " + e1.getMessage(),
                                          "Unable to parse WSDL",
                                          JOptionPane.ERROR_MESSAGE);
        } catch (WSDLException e1) {
            log.log(Level.WARNING, "Error parsing WSDL", e1);
            JOptionPane.showMessageDialog(getRootPane(),
                                          "Unable to parse the specified WSDL: " + e1.getMessage(),
                                          "Unable to parse WSDL",
                                          JOptionPane.ERROR_MESSAGE);
        } catch (Wsdl.BadPolicyReferenceException e1) {
            // Can't actually happen -- we'd have caught this earlier on
            log.log(Level.WARNING, "Error importing policy from WSDL", e1);
            JOptionPane.showMessageDialog(getRootPane(),
                                          "Unable to import policy from the specified WSDL: " + e1.getMessage(),
                                          "Unable to parse WSDL",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private Binding chooseBinding(Wsdl wsdl) throws SAXException {
        List bindings = new ArrayList(wsdl.getBindings());
        List bindingsWithPolicies = new ArrayList();
        List badBindings = new ArrayList();
        for (Iterator i = bindings.iterator(); i.hasNext();) {
            Binding binding = (Binding)i.next();
            List ops = binding.getBindingOperations();
            for (Iterator j = ops.iterator(); j.hasNext();) {
                BindingOperation op = (BindingOperation)j.next();
                try {
                    if (wsdl.getEffectiveInputPolicy(binding, op) != null ||
                            wsdl.getEffectiveOutputPolicy(binding, op) != null)
                        bindingsWithPolicies.add(binding);
                } catch (Wsdl.BadPolicyReferenceException e) {
                    // Ignore this binding -- it has a bad reference
                    log.log(Level.WARNING, "Unable to follow policy reference in WSDL: ", e);
                    badBindings.add(binding);
                }
            }
        }

        if (badBindings.size() > 0) {
            // Warn about bad bindings
            StringBuffer sb = new StringBuffer();
            for (Iterator i = badBindings.iterator(); i.hasNext();) {
                Binding binding = (Binding)i.next();
                sb.append(binding.getQName().getLocalPart()).append(" ");
            }
            JOptionPane.showMessageDialog(getRootPane(),
                                          "The following bindings had unresolvable policy references, and were ignored:\n"
                                                  + sb.toString(),
                                          "Unable to resolve attached policies",
                                          JOptionPane.ERROR_MESSAGE);
        }

        if (bindingsWithPolicies.size() < 1)
            throw new SAXException("The specified WSDL does not contain any bindings with attached policies.");

        if (bindingsWithPolicies.size() < 2)
            return (Binding)bindingsWithPolicies.get(0);

        Binding[] bindingsArray = (Binding[]) bindingsWithPolicies.toArray(new Binding[0]);
        String[] bindingNames = new String[bindingsArray.length];
        for (int i = 0; i < bindingNames.length; i++) {
            Binding binding = (Binding) bindingsArray[i];
            bindingNames[i] = binding.getQName().getLocalPart() + " (" + binding.getQName().getNamespaceURI() + ")";
        }

        Object bindingName = JOptionPane.showInputDialog(getRootPane(),
                                                         "Select the binding whose policy you wish to import.",
                                                         "Select a binding.",
                                                         JOptionPane.QUESTION_MESSAGE,
                                                         null,
                                                         bindingNames,
                                                         null);

        for (int i = 0; i < bindingNames.length; i++) {
            String name = bindingNames[i];
            if (name != null && name.equals(bindingName))
                return bindingsArray[i];
        }

        return null;
    }

    private JButton getImportButton() {
        if (importButton == null) {
            importButton = new JButton("Import");
            importButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    SsgPolicyImportDialog policyImportDialog =
                            SsgPolicyImportDialog.createSsgPolicyImportDialog(getRootPane());
                    policyImportDialog.pack();
                    Utilities.centerOnScreen(policyImportDialog);
                    policyImportDialog.setVisible(true);
                    if (policyImportDialog.isCancelled())
                        return;

                    if (policyImportDialog.isWsdlImportSelected()) {
                        importPolicyFromWsdl();
                        return;
                    }

                    InputStream policyInputStream = null;
                    try {
                        policyInputStream = policyImportDialog.getPolicyInputStream();
                        if (policyInputStream == null) {
                            JFileChooser fc = FileChooserUtil.createJFileChooser();
                            fc.setFileFilter(createPolicyFileFilter());
                            fc.setDialogType(JFileChooser.OPEN_DIALOG);

                            if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(getRootPane())) {
                                File got = fc.getSelectedFile();
                                policyInputStream = new FileInputStream(got);
                            }
                        }

                        if (policyInputStream != null) {
                            Assertion rootAssertion = WspReader.getDefault().parsePermissively(XmlUtil.parse(policyInputStream).getDocumentElement());
                            // Filter out all disabled assertions that are ignored by the SSB.  Also, after filtering,
                            // check if a composite assertin is empty.  If so, filter out the composite assertion as well.
                            filterAllDisabledDescendantAssertions(rootAssertion, null);

                            // TODO filter out assertions that aren't implemented by the SSB
                            Policy newPolicy = new Policy(rootAssertion, null);
                            PolicyAttachmentKeyDialog pakDlg = new PolicyAttachmentKeyDialog(Gui.getInstance().getFrame(),
                                                                                             "Import Policy: Policy Resolution",
                                                                                             true, isConfigOnly());
                            PolicyAttachmentKey oldPak = getSelectedPolicy();
                            if (oldPak != null) {
                                oldPak = new PolicyAttachmentKey(oldPak); // make copy first
                                oldPak.setPersistent(true);
                                oldPak.setBeginsWithMatch(true);
                            }
                            pakDlg.setPolicyAttachmentKey(oldPak);

                            Utilities.centerOnScreen(pakDlg);
                            pakDlg.setVisible(true);

                            PolicyAttachmentKey newPak = pakDlg.getPolicyAttachmentKey();
                            if (newPak != null) {
                                policyCache.setPolicy(newPak, newPolicy);
                                updatePolicyPanel();
                            }
                            log.log(Level.INFO, "Policy import successful");
                        }                        
                    } catch (NullPointerException nfe) {
                        // TODO Figure out: which third-party bug was this awful catch block put here to work around, and is it safe to remove yet?
                        log.log(Level.WARNING, "Error importing policy", nfe);
                        JOptionPane.showMessageDialog(getRootPane(),
                                                      "Unable to import the specified file: " + nfe.getMessage(),
                                                      "Unable to read file",
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Error importing policy", e);
                        JOptionPane.showMessageDialog(getRootPane(),
                                                      "Unable to import the specified file: " + e.getMessage(),
                                                      "Unable to read file",
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (PolicyLockedException e) {
                        log.log(Level.WARNING, "Error saving policy", e);
                        JOptionPane.showMessageDialog(getRootPane(),
                                                      "Unable to save the new policy: " + e.getMessage() + "\nPlease try again.",
                                                      "Unable to save policy",
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (SAXException e) {
                        log.log(Level.WARNING, "Error importing policy", e);
                        JOptionPane.showMessageDialog(getRootPane(),
                                                      "Unable to import the specified file due to malformed XML: " + e.getMessage(),
                                                      "Unable to read file",
                                                      JOptionPane.ERROR_MESSAGE);
                    } finally {
                        ResourceUtils.closeQuietly(policyInputStream);
                    }
                }
            });
        }
        return importButton;
    }

    /**
     * Filter out all disabled child assertions in a composite assertion.  The assertion has been upgraded after filtering.
     * @param assertion a assertion to be processed.
     * @param iterator an iterator associated with the assertion.
     */
    private void filterAllDisabledDescendantAssertions(Assertion assertion, Iterator iterator) {
        // Apply filter on this one
        if (assertion instanceof CompositeAssertion) {
            // Apply filter to children
            CompositeAssertion root = (CompositeAssertion)assertion;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                if (kid.isEnabled()) filterAllDisabledDescendantAssertions(kid, i);
                else i.remove();
            }
            // If all children of this composite were removed, we have to remove it from it's parent
            if (root.getChildren().isEmpty() && iterator != null) {
                iterator.remove();
            }
        } else {
            if (!assertion.isEnabled() && iterator != null) {
                iterator.remove();
            }
            if (iterator == null) {
                throw new RuntimeException("Invalid policy, all policies must have a composite assertion at the root");
            }
        }
    }

    private JButton getExportButton() {
        if (exportButton == null) {
            exportButton = new JButton("Export");
            exportButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    PolicyAttachmentKey pak = getSelectedPolicy();
                    if (pak == null) return;
                    Policy policy = policyCache.getPolicy(pak);
                    if (policy == null) return;

                    JFileChooser fc = FileChooserUtil.createJFileChooser();
                    fc.setFileFilter(createPolicyFileFilter());
                    fc.setDialogType(JFileChooser.SAVE_DIALOG);

                    if (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(getRootPane())) {
                        String name = fc.getSelectedFile().getPath();
                        // add extension if not present (bugzilla #1673)
                        if (!name.endsWith(".xml") && !name.endsWith(".XML")) {
                            name = name + ".xml";
                        }
                        //File got = fc.getSelectedFile();
                        File got = new File(name);
                        FileOutputStream os = null;
                        try {
                            os = new FileOutputStream(got);
                            WspWriter.writePolicy(policy.getAssertion(), os);
                            os.close();
                            os = null;
                        } catch (NullPointerException nfe) {
                            log.log(Level.WARNING, "NullPointerException", nfe);
                        } catch (IOException e) {
                            log.log(Level.WARNING, "Error exporting policy", e);
                            JOptionPane.showMessageDialog(getRootPane(),
                                                          "Unable to export to the specified file: " + e.getMessage(),
                                                          "Unable to export file",
                                                          JOptionPane.ERROR_MESSAGE);
                        } finally {
                            if (os != null) try { os.close(); } catch (IOException e) { }
                        }
                    }
                }
            });
        }
        return exportButton;
    }

    private FileFilter createPolicyFileFilter() {
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;
                String name = f.getName();
                int dot = name.lastIndexOf('.');
                if (dot < 0)
                    return false;
                String ext = name.substring(dot);
                return ext.equalsIgnoreCase(".xml");
            }

            public String getDescription() {
                return "Policy document (*.xml)";
            }
        };
        return filter;
    }

    private JButton getChangeButton() {
        if (changeButton == null) {
            changeButton = new JButton("Edit");
            changeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PolicyAttachmentKey pak = getSelectedPolicy();
                    if (pak == null) return;
                    Policy policy = policyCache.getPolicy(pak);
                    if (policy == null) return;
                    PolicyAttachmentKeyDialog pakDlg = new PolicyAttachmentKeyDialog(Gui.getInstance().getFrame(),
                                                                                     "Configure Policy Resolution",
                                                                                     true, isConfigOnly());
                    pakDlg.setPolicyAttachmentKey(pak);
                    Utilities.centerOnScreen(pakDlg);
                    pakDlg.setVisible(true);
                    PolicyAttachmentKey newPak = pakDlg.getPolicyAttachmentKey();
                    if (newPak == null)
                        return;
                    policyCache.flushPolicy(pak);
                    if (!newPak.isPersistent()) {
                        policyCache.flushPolicy(newPak); // avoid trying to overwrite persistent with transient
                        policy.setAlwaysValid(false);
                    }
                    try {
                        policyCache.setPolicy(newPak, policy);
                    } catch (PolicyLockedException e1) {
                        log.log(Level.WARNING, "Error saving policy", e1);
                        JOptionPane.showMessageDialog(getRootPane(),
                                                      "Unable to save the edited policy: " + e1.getMessage() + "\nPlease try again.",
                                                      "Unable to save policy",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                    lastSelectedPolicy = newPak;
                    updatePolicyPanel();
                }
            });
        }
        return changeButton;
    }

    private JButton getDeleteButton() {
        if (deleteButton == null) {
            deleteButton = new JButton("Delete");
            deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PolicyAttachmentKey pak = getSelectedPolicy();
                    if (pak == null)
                        return;
                    if (!pak.isPersistent())
                        policyCache.flushPolicy(pak); // avoid trying to overwrite persistent with transient
                    policyCache.flushPolicy(pak);
                    updatePolicyPanel();
                }
            });
        }
        return deleteButton;
    }

    private class DisplayPolicyTableModel extends AbstractTableModel {
        public int getRowCount() {
            return displayPolicies.size();
        }

        public int getColumnCount() {
            return COL_COUNT;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case COL_LOCK:
                    return !isConfigOnly();
                case COL_MATCHTYPE:
                    return true;
            }
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PolicyAttachmentKey pak;
            switch (columnIndex) {
                case COL_NS:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getUri();
                case COL_SA:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getSoapAction();
                case COL_PU:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getProxyUri();
                case COL_MATCHTYPE:
                    pak = ((PolicyAttachmentKey)displayPolicies.get(rowIndex));
                    return pak == null ? null : (pak.isBeginsWithMatch() ? MATCH_STARTSWITH
                                                                         : MATCH_EQUALS);
                case COL_LOCK:
                    pak = ((PolicyAttachmentKey)displayPolicies.get(rowIndex));
                    return pak == null ? null : Boolean.valueOf(pak.isPersistent());
            }
            log.log(Level.WARNING, "SsgPropertyDialog: policyTable: invalid columnIndex: " + columnIndex);
            return null;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Boolean p;
            switch (columnIndex) {
                case COL_MATCHTYPE:
                    String v = (String)aValue;
                    if (v != null) {
                        p = Boolean.valueOf(MATCH_STARTSWITH.equals(v));
                        PolicyAttachmentKey pak = ((PolicyAttachmentKey)displayPolicies.get(rowIndex));
                        Policy policy = policyCache.getPolicy(pak);
                        if (policy != null) {
                            pak.setBeginsWithMatch(p.booleanValue());
                            if (!pak.isPersistent())
                                policyCache.flushPolicy(pak);
                            try {
                                policyCache.setPolicy(pak, policy);
                            } catch (PolicyLockedException e) {
                                log.log(Level.WARNING, "Unable to save policy", e);
                            }
                            updatePolicyPanel();
                        }
                    }
                    break;
                case COL_LOCK:
                    p = (Boolean)aValue;
                    if (p != null) {
                        PolicyAttachmentKey pak = ((PolicyAttachmentKey)displayPolicies.get(rowIndex));
                        Policy policy = policyCache.getPolicy(pak);
                        if (policy != null) {
                            pak.setPersistent(p.booleanValue());
                            if (!pak.isPersistent()) {
                                policyCache.flushPolicy(pak);
                                policy.setAlwaysValid(false);                                
                            }
                            try {
                                policyCache.setPolicy(pak, policy);
                            } catch (PolicyLockedException e) {
                                log.log(Level.WARNING, "Unable to save policy", e);
                            }
                            updatePolicyPanel();
                        }
                    }
                    break;
            }
        }
    }

    private PolicyAttachmentKey getSelectedPolicy() {
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size())
            return (PolicyAttachmentKey)displayPolicies.get(row);
        return null;
    }

    private void displaySelectedPolicy() {
        Policy policy = null;
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size()) {
            lastSelectedPolicy = (PolicyAttachmentKey)displayPolicies.get(row);
            if (lastSelectedPolicy != null)
                policy = policyCache.getPolicy(lastSelectedPolicy);
        }
        ClientAssertion clientAssertion = null;
        try {
            clientAssertion = policy == null ? null : policy.getClientAssertion();
        } catch (PolicyAssertionException e) {
            // fallthrough and use null
        }
        policyTree.setModel((policy == null || clientAssertion == null) ? null : new PolicyTreeModel(clientAssertion));
        int erow = 0;
        while (erow < policyTree.getRowCount()) {
            policyTree.expandRow(erow++);
        }
        getChangeButton().setEnabled(policy != null);
        getExportButton().setEnabled(policy != null);
        getDeleteButton().setEnabled(policy != null && lastSelectedPolicy != null && (isConfigOnly() || !lastSelectedPolicy.isPersistent()));
    }

    /** Update the policy display panel with information from the Ssg bean. */
    public void updatePolicyPanel() {
        PolicyAttachmentKey lastPak = lastSelectedPolicy;
        displayPolicies.clear();
        displayPolicies = new ArrayList(policyCache.getPolicyAttachmentKeys());
        displayPolicyTableModel.fireTableDataChanged();
        if (lastPak != null) {
            for (int i = 0; i < displayPolicies.size(); i++) {
                PolicyAttachmentKey pak = (PolicyAttachmentKey)displayPolicies.get(i);
                if (lastPak == pak)
                    policyTable.getSelectionModel().setSelectionInterval(i, i);
            }
        }
        displaySelectedPolicy();
    }

    private class LockCellRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate = policyTable.getDefaultRenderer(Boolean.class);

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isConfigOnly()) c.setEnabled(false);
            return c;
        }
    }

    private static class ComboBoxCellRenderer extends JComboBox implements TableCellRenderer {
        private ComboBoxCellRenderer(String[] items) {
            super(items);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            setSelectedItem(value);
            return this;
        }
    }

    private static class ComboBoxCellEditor extends DefaultCellEditor {
        private ComboBoxCellEditor(String[] items) {
            super(new JComboBox(items));
        }
    }

    public static class UrlPanel extends TextEntryPanel {
        public UrlPanel() {
            this("URL:", null);
        }

        public UrlPanel(String label, String initialValue) {
            super(label, "url", initialValue);
        }

        @Override
        protected String getSemanticError(String model) {
            if (model == null || model.length() == 0) return null;
            try {
                URL url = new URL(model);
                //noinspection ResultOfMethodCallIgnored
                InetAddress.getByName(url.getHost());
                return null;
            } catch (SecurityException se) {
                // if we are not permitted to resolve the address don't show
                // this as an error
                return null;
            } catch (Exception e) {
                return ExceptionUtils.getMessage(e);
            }
        }

        @Override
        protected String getSyntaxError(String model) {
            if (model == null || model.length() == 0) return null;
            // if the URL contains context variable, you just can't check syntax
            try {
                URL url = new URL(model);
                if (url.getHost() == null || url.getHost().length() == 0) {
                    return "no host";
                } else {
                    return null;
                }
            } catch ( MalformedURLException e) {
                return ExceptionUtils.getMessage(e);
            }
        }

    }    
}
