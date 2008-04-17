/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.util.*;
import com.l7tech.internal.license.LicenseSpec;
import com.l7tech.server.GatewayFeatureSet;
import com.l7tech.server.GatewayFeatureSets;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Panel for editing a LicenseSpec in the internal L7 license builder GUI.
 */
public class LicenseSpecPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(LicenseSpecPanel.class.getName());

    /**
     * Property whose PropertyChangeEvent signals that a field has been edited.  Call getSpec() to pick up the changes.
     */
    public static final String PROPERTY_LICENSE_SPEC = "licenseSpec";
    public static final Color BAD_FIELD_BG = new Color(255, 200, 200);

    /**
     * We fire an update when they've made a change then been idle for two seconds.
     * We wait this long because firing an update can cause the cursor to move to the end of the text field.
     */
    private static final long INACTIVITY_UPDATE_MILLIS = 2000;

    private JPanel rootPanel;
    private JTextField idField;
    private JButton randomIdButton;
    private JTextField descriptionField;
    private JTextField licenseeEmailField;
    private JTextField licenseeNameField;
    private JTextField startField;
    private JButton startTodayButton;
    private JTextField expiryField;
    private JButton expireNextYearButton;
    private JTextField productField;
    private JButton currentProductButton;
    private JButton anyProductButton;
    private JTextField majorVersionField;
    private JButton currentMajorVersionButton;
    private JButton anyMajorVersionButton;
    private JTextField minorVersionField;
    private JButton currentMinorVersionButton;
    private JButton anyMinorVersionButton;
    private JTextField hostField;
    private JButton anyHostButton;
    private JTextField ipField;
    private JButton anyIpButton;
    private JTree featureTree;
    private JButton featureExpandAllButton;
    private JButton featureCollapseAllButton;
    private JButton featureClearAll;
    private JComboBox eulaComboBox;
    private JButton eulaDefaultButton;
    private JButton eulaCustomButton;
    private JLabel detailsLabel;
    private JTextField featureLabelField;
    private JButton defaultFeatureLabelButton;
    private JList attributesList;

    private JTextField defaultTextField = new JTextField();
    private JComboBox defaultComboBox = new JComboBox();
    private FocusListener focusListener;
    private DocumentListener documentListener;
    private Map<JTextField, String> oldFieldValues = new HashMap<JTextField, String>();
    private Random random = new SecureRandom();
    private TreeModel featureTreeModel;
    private TreeCellEditor featureTreeEditor;
    private FeatureTreeRenderer featureTreeRenderer;
    private Set<String> featureNamesChecked = new HashSet<String>();
    private Set<String> featureNamesWithCheckedKids = new HashSet<String>();
    private Map<String, Set<GatewayFeatureSet>> featureParents = new HashMap<String, Set<GatewayFeatureSet>>();
    private String customEulaText = null;

    private boolean fieldChanged = false;
    private long fieldChangedWhen = 0;
    private boolean featureCheckChangedSinceLastOptimize = false;

    private final List<GatewayFeatureSet> profilesForward;
    private final List<GatewayFeatureSet> profilesReverse;
    {
        List<GatewayFeatureSet> fwd = new ArrayList<GatewayFeatureSet>(GatewayFeatureSets.getProductProfiles().values());
        fwd.remove(GatewayFeatureSets.PROFILE_LICENSE_NAMES_NO_FEATURES); // don't show this as an option in GUI

        List<GatewayFeatureSet> rev = new ArrayList<GatewayFeatureSet>(fwd);
        Collections.reverse(rev);

        profilesForward = Collections.unmodifiableList(fwd);
        profilesReverse = Collections.unmodifiableList(rev);
    }

    private GatewayFeatureSet rootSet = new GatewayFeatureSet("set:ROOT", "All registered feature sets",
                                                              "Holds all registered feature sets",
                                                              profilesForward.toArray(new GatewayFeatureSet[0]));

    private final String EULA_NONE = "<None>";
    private final String EULA_CUSTOM = "<Custom>";
    private final String[] eulaChoices = new String[] {
            EULA_NONE,
            EULA_CUSTOM
    };
    private final Set<String> allLicAttrNames;

    public LicenseSpecPanel() {
        ResourceBundle resources = ResourceBundle.getBundle("com/l7tech/internal/license/gui/resources/licenseAttributes");
        allLicAttrNames = resources.keySet();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(rootPanel);
        init();
    }

    private void init() {
        DefaultListModel attributesListModel = new DefaultListModel();
        for (String attr: allLicAttrNames) attributesListModel.addElement(attr);
        attributesList.setModel(attributesListModel);
        attributesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        attributesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fireUpdate();
            }
        });

        idField.addFocusListener(getFocusListener());
        descriptionField.addFocusListener(getFocusListener());
        licenseeEmailField.addFocusListener(getFocusListener());
        licenseeNameField.addFocusListener(getFocusListener());
        startField.addFocusListener(getFocusListener());
        expiryField.addFocusListener(getFocusListener());
        productField.addFocusListener(getFocusListener());
        majorVersionField.addFocusListener(getFocusListener());
        minorVersionField.addFocusListener(getFocusListener());
        hostField.addFocusListener(getFocusListener());
        ipField.addFocusListener(getFocusListener());
        featureLabelField.addFocusListener(getFocusListener());
        eulaComboBox.addFocusListener(getFocusListener());

        idField.getDocument().addDocumentListener(getDocumentListener());
        descriptionField.getDocument().addDocumentListener(getDocumentListener());
        licenseeEmailField.getDocument().addDocumentListener(getDocumentListener());
        licenseeNameField.getDocument().addDocumentListener(getDocumentListener());
        startField.getDocument().addDocumentListener(getDocumentListener());
        expiryField.getDocument().addDocumentListener(getDocumentListener());
        productField.getDocument().addDocumentListener(getDocumentListener());
        majorVersionField.getDocument().addDocumentListener(getDocumentListener());
        minorVersionField.getDocument().addDocumentListener(getDocumentListener());
        hostField.getDocument().addDocumentListener(getDocumentListener());
        ipField.getDocument().addDocumentListener(getDocumentListener());
        featureLabelField.getDocument().addDocumentListener(getDocumentListener());
        eulaComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (ItemEvent.SELECTED != e.getStateChange())
                    return;
                if (EULA_CUSTOM.equals(e.getItem()) && customEulaText == null)
                    eulaCustomButton.doClick();
                else
                    fireUpdate();
            }
        });

        eulaComboBox.setModel(new DefaultComboBoxModel(eulaChoices));
        eulaComboBox.setSelectedItem(EULA_NONE);

        featureTree.setModel(getFeatureTreeModel());
        featureTree.setCellRenderer(getFeatureTreeRenderer());
        featureTree.setCellEditor(getFeatureTreeEditor());
        featureTree.setRootVisible(false);
        featureTree.setShowsRootHandles(true);
        featureTree.setEditable(true);
        featureTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                String note = null;
                TreePath path = featureTree.getSelectionPath();
                if (path != null) {
                    FeatureNode fn = (FeatureNode)path.getLastPathComponent();
                    GatewayFeatureSet fs = (GatewayFeatureSet)fn.getUserObject();
                    note = fs.getNote();
                }
                String str = note == null || note.length() < 1 ? "&nbsp;" : note;
                str = TextUtils.wrapString(str, 70, 5, "<br>\n");
                detailsLabel.setText("<HTML>" + str);
            }
        });
        featureExpandAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTree tree = featureTree;
                Utilities.expandTree(tree);
            }
        });
        featureCollapseAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTree tree = featureTree;
                Utilities.collapseTree(tree);
            }
        });
        featureClearAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                featureNamesChecked.clear();
                featureNamesWithCheckedKids.clear();
                featureTree.repaint();
                fireUpdate();
            }
        });

        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                if (!fieldChanged) return;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!fieldChanged)
                            return;
                        final long now = System.currentTimeMillis();
                        if (now - fieldChangedWhen < INACTIVITY_UPDATE_MILLIS)
                            return;
                        checkForChangedField();
                    }
                });
            }
        }, 500, 500);

        randomIdButton.addActionListener(new RandomIdAction());
        startTodayButton.addActionListener(new StartTodayAction());
        expireNextYearButton.addActionListener(new ExpireNextYearAction());
        currentProductButton.addActionListener(new CurrentProductAction());
        anyProductButton.addActionListener(blankFieldAction(productField));
        currentMajorVersionButton.addActionListener(new CurrentMajorVersionAction());
        anyMajorVersionButton.addActionListener(blankFieldAction(majorVersionField));
        currentMinorVersionButton.addActionListener(new CurrentMinorVersionAction());
        anyMinorVersionButton.addActionListener(blankFieldAction(minorVersionField));
        anyHostButton.addActionListener(blankFieldAction(hostField));
        anyIpButton.addActionListener(blankFieldAction(ipField));
        defaultFeatureLabelButton.addActionListener(blankFieldAction(featureLabelField));
        eulaDefaultButton.addActionListener(selectItemAction(eulaComboBox, EULA_NONE));
        eulaCustomButton.addActionListener(new CustomEulaAction());

        setSpec(null);
    }

    private TreeModel getFeatureTreeModel() {
        if (featureTreeModel == null) {
            FeatureNode rootNode = new FeatureNode(rootSet);
            featureTreeModel = new DefaultTreeModel(rootNode, true);
        }
        return featureTreeModel;
    }

    private FeatureTreeRenderer getFeatureTreeRenderer() {
        if (featureTreeRenderer == null) {
            featureTreeRenderer = new FeatureTreeRenderer();
        }
        return featureTreeRenderer;
    }

    private TreeCellEditor getFeatureTreeEditor() {
        if (featureTreeEditor == null) {
            featureTreeEditor = new FeatureCellEditor(new JCheckBox());
        }
        return featureTreeEditor;
    }

    private void checkForChangedField() {
        if (!fieldChanged)
            return;
        fieldChangedWhen = System.currentTimeMillis();
        fireUpdate();
        fieldChanged = false;
    }

    private DocumentListener getDocumentListener() {
        if (documentListener != null) return documentListener;
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changeCheck();
            }

            public void removeUpdate(DocumentEvent e) {
                changeCheck();
            }

            public void changedUpdate(DocumentEvent e) {
                changeCheck();
            }

            private void changeCheck() {
                fieldChanged = true;
                fieldChangedWhen = System.currentTimeMillis();
            }
        };
        return documentListener = listener;
    }

    private ActionListener blankFieldAction(final JTextField field) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                field.setText("");
                fireUpdate();
            }
        };
    }

    private ActionListener selectItemAction(final JComboBox cb, final String item) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cb.setSelectedItem(item);
                fireUpdate();
            }
        };
    }

    /** Initialize all fields with useful default values, except licensee name which has to be set. */
    public void setDefaults() {
        setSpec(new LicenseSpec());
        new RandomIdAction().actionPerformed(null);
        new StartTodayAction().actionPerformed(null);
        new ExpireNextYearAction().actionPerformed(null);
        new CurrentProductAction().actionPerformed(null);
        new CurrentMajorVersionAction().actionPerformed(null);
    }

    private FocusListener getFocusListener() {
        if (focusListener != null) return focusListener;
        FocusListener listener = new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                Component c = e.getComponent();
                // See if this is one of our text fields changing.  If so, we'll need to fire an update
                if (c instanceof JTextField) {
                    // Heh, this is such a smelly hack.  I'm so proud of it
                    JTextField field = (JTextField)c;
                    String currentValue = field.getText();
                    String oldValue = oldFieldValues.get(field);
                    if (oldValue == null || !(oldValue.equals(currentValue))) {
                        oldFieldValues.put(field, currentValue);
                        fireUpdate();
                    }
                }
            }
        };
        return focusListener = listener;
    }

    private void fireUpdate() {
        firePropertyChange(PROPERTY_LICENSE_SPEC, null, null);
    }

    /**
     * Updates the view to correspond with this license spec.
     * @param spec the new LicenseSpec.  Required.
     */
    public void setSpec(LicenseSpec spec) {
        fieldChanged = false;
        if (spec == null) spec = new LicenseSpec();
        setText(idField, tt(spec.getLicenseId()));
        setText(descriptionField, tt(spec.getDescription()));
        updateLicenseAttributeList(spec);
        setText(licenseeEmailField, tt(spec.getLicenseeContactEmail()));
        setText(licenseeNameField, tt(spec.getLicenseeName()));

        String start = tt(spec.getStartDate());
        String expiry = tt(spec.getExpiryDate());
        setText(startField, start);
        setText(expiryField, expiry);
        // Check if "Start Date" or "Expiry Date" is specified or not for a special license attribute, "Subscription".
        Set<String> attrs = spec.getAttributes();
        if (attrs.contains("Subscription")) {
            if ("".equals(start) && "".equals(expiry)) {
                throw new RuntimeException("For a Subscription license, Start Date and Expiry Date are mandatory requirements.",
                    new InvalidLicenseException());
            } else if ("".equals(start)) {
                throw new RuntimeException("For a Subscription license, Start Date is a mandatory requirement.",
                    new InvalidLicenseException());
            } else if ("".equals(expiry)) {
                throw new RuntimeException("For a Subscription license, Expiry Date is a mandatory requirement.",
                    new InvalidLicenseException());
            }
        }

        setText(productField, tt(spec.getProduct()));
        setText(majorVersionField, tt(spec.getVersionMajor()));
        setText(minorVersionField, tt(spec.getVersionMinor()));

        setText(hostField, tt(spec.getHostname()));
        setText(ipField, tt(spec.getIp()));
        setText(featureLabelField, tt(spec.getFeatureLabel()));

        featureNamesChecked.clear();
        featureNamesWithCheckedKids.clear();
        Set<String> rootFeatures = spec.getRootFeatures();
        Map<String, GatewayFeatureSet> byname = GatewayFeatureSets.getAllFeatureSets();
        for (String name : rootFeatures) {
            GatewayFeatureSet fs = byname.get(name);
            if (fs == null) continue; // ignore unrecognized feature name
            setFeatureChecked(fs, true);
        }
        optimizeCheckedFeatures(true);
        featureTree.repaint();

        fieldChanged = false;
        getSpec(); // update all field colors
    }

    /**
     * Update the attribute list in the spec panel.
     * @param spec: the license spec
     * @return an error message.  Null if no errors exist.
     */
    private void updateLicenseAttributeList(LicenseSpec spec) {
        Set<String> newAttrList = spec.getAttributes();
        List<String> notSuchAttrs = new ArrayList<String>();
        // Remove all list-selection listeners.  Otherwise, spec panel will update xml and cause infinite loop.
        ListSelectionListener listeners[] = attributesList.getListSelectionListeners();
        for (ListSelectionListener listener: listeners) attributesList.removeListSelectionListener(listener);
        // Before updating the attribute JList, clean all selections in the JList.
        attributesList.clearSelection();

        // Check if new attribute list is empty or not.  If not, update the attribute JList in the license spec panel.
        if (! newAttrList.isEmpty()) {
            Vector<Integer> indexVector = new Vector<Integer>(newAttrList.size());
            DefaultListModel model = (DefaultListModel)attributesList.getModel();
            for (String attr: newAttrList) {
                int index = model.indexOf(attr);
                // No such attribute exists
                if (index == -1) {
                    notSuchAttrs.add(attr);
                }
                // This is one that will be selected in the attribute JList.
                else {
                    indexVector.add(index);
                }
            }

            // Update the attribute JList in the spec panel if applicable.
            if (!indexVector.isEmpty()) {
                int indices[] = new int[indexVector.size()];
                for (int i = 0; i < indexVector.size(); i++) indices[i] = indexVector.get(i);
                attributesList.setSelectedIndices(indices);
            }

            // Check if there are existing some unknown attributes.  If so, report an error.
            if (!notSuchAttrs.isEmpty()) {
                StringBuilder errMsg = new StringBuilder();
                for (int i = 0; i < notSuchAttrs.size(); i++) {
                    errMsg.append(" ").append(notSuchAttrs.get(i));
                    if (i < notSuchAttrs.size() - 1) {
                        errMsg.append(",");
                    }
                }
                // Before throwing an exception, add back all list-selection listeners.
                for (ListSelectionListener listener: listeners) attributesList.addListSelectionListener(listener);

                throw new RuntimeException("The license attribute(s)," + errMsg.toString()  + " not existing.",
                        new InvalidLicenseException());
            }
        }
        // Add back all list-selection listeners, since we remove them at the beginning of the method.
        for (ListSelectionListener listener: listeners) attributesList.addListSelectionListener(listener);
    }

    private void setText(JTextField field, String val) {
        field.setText(val);
        oldFieldValues.put(field, val);
    }

    /**
     * Read the view and build a LicenseSpec out of it.
     * @return a LicenseSpec built out of the current view.
     */
    public LicenseSpec getSpec() {
        LicenseSpec spec = new LicenseSpec();
        spec.setLicenseId(ftid(idField));
        spec.setDescription(fts(descriptionField));
        spec.setLicenseeName(ftsReq(licenseeNameField));
        spec.setLicenseeContactEmail(fts(licenseeEmailField));
        spec.setStartDate(ftd(startField));
        spec.setExpiryDate(ftd(expiryField));
        spec.setProduct(fts(productField));
        spec.setVersionMajor(fts(majorVersionField));
        spec.setVersionMinor(fts(minorVersionField));
        spec.setHostname(fts(hostField));
        spec.setIp(fts(ipField));
        spec.setFeatureLabel(fts(featureLabelField));
        spec.setAttributes(getAttributes());

        Object ecb = eulaComboBox.getSelectedItem();
        if (EULA_CUSTOM.equals(ecb)) {
            spec.setEulaText(customEulaText);
            eulaComboBox.setBackground(defaultTextField.getBackground());
        } else {
            spec.setEulaText(null);
            eulaComboBox.setBackground(BAD_FIELD_BG);
        }

        addCheckedFeaturesToSpec(spec);
        return spec;
    }

    /**
     * Get all names of selected attributes from the JList
     * @return a list of the names of selected attributes.
     */
    private Set<String> getAttributes() {
        Set<String> attributes = new HashSet<String>(allLicAttrNames.size());
        ListModel model = attributesList.getModel();
        for(int i = 0; i < model.getSize(); i++) {
            if (attributesList.isSelectedIndex(i)) {
                attributes.add((String)model.getElementAt(i));
            }
        }
        return attributes;
    }
    
    /**
     *
     * Find all checked features and make sure they get added to spec.
     * @param spec the LicenseSpec to update.  Required.
     */
    private void addCheckedFeaturesToSpec(LicenseSpec spec) {
        // make sure features are optimized first if they need to be
        optimizeCheckedFeatures(true);

        // Scan roots from bottom of list up, then each root from top of tree down, adding all checked features
        // and stopping, without adding features already implied by features already added.
        Set<String> topEnabled = new LinkedHashSet<String>();
        Set<String> allEnabled = new LinkedHashSet<String>();

        int maxDepth = 1;
        for (;;) {
            boolean atLeastOneDepthLimitReached = false;
            for (GatewayFeatureSet fs : profilesReverse) {
                if (recursiveAdd(maxDepth, fs, topEnabled, allEnabled))
                    atLeastOneDepthLimitReached = true;
            }
            if (atLeastOneDepthLimitReached)
                maxDepth++; // search deeper
            else
                break; // done
        }

        logger.finer("Search got to depth: " + maxDepth);

        for (String name : topEnabled)
            spec.addRootFeature(name);
    }

    private boolean recursiveAdd(int maxDepth, GatewayFeatureSet fs, Set<String> topEnabled, Set<String> allEnabled) {
        if (maxDepth < 1) return true; // ask to try again with more depth
        maxDepth--;

        String name = fs.getName();
        if (allEnabled.contains(name))
            return false; // already enabled, skip it

        if (isFeatureChecked(fs)) {
            // Enable this parent explicitly, then mark it and all kids as implicitly enabled
            topEnabled.add(name);
            allEnabled.add(name);
            for (GatewayFeatureSet kid : fs.getChildren())
                markAllEnabled(kid, allEnabled);
            return false;
        } else {
            // Feature not checked -- check children
            boolean anyBottomedOut = false;
            for (GatewayFeatureSet kid : fs.getChildren()) {
                if (recursiveAdd(maxDepth, kid, topEnabled, allEnabled))
                    anyBottomedOut = true;
            }
            return anyBottomedOut;
        }
    }

    private void markAllEnabled(GatewayFeatureSet fs, Set<String> allEnabled) {
        allEnabled.add(fs.getName());
        for (GatewayFeatureSet kid : fs.getChildren())
            markAllEnabled(kid, allEnabled);
    }

    /* from text to date. */
    private Date ftd(JTextField f) {
        boolean good = true;
        try {
            String s = f.getText();
            if (s == null || s.length() < 1) return null;
            final Date date = ISO8601Date.parse(s);
            if (date.getTime() == 0 || date.getTime() == -1) {
                good = false; // flag this invalid date in red
                return null;
            }
            return date;
        } catch (ParseException e) {
            good = false;
            return null;
        } finally {
            f.setBackground(good ? defaultTextField.getBackground() : BAD_FIELD_BG);
        }
    }

    /* from text to string. */
    private String fts(JTextField f) {
        String s = f.getText();
        return s == null || s.trim().length() < 1 ? null : s.trim();
    }

    /* from text to required string. */
    private String ftsReq(JTextField f) {
        boolean good = false;
        try {
            String s = f.getText();
            if (s == null || s.trim().length() < 1) {
                return null;
            } else {
                good = true;
                return s.trim();
            }
        } finally {
            f.setBackground(good ? defaultTextField.getBackground() : BAD_FIELD_BG);
        }
    }

    /* from text to license id. */
    private long ftid(JTextField f) {
        boolean good = false;
        try {
            final String s = f.getText();
            if (s == null || s.length() < 1)
                return 0;
            final long n = Long.parseLong(s);
            if (n < 1)
                return 0;
            good = true;
            return n;
        } catch (NumberFormatException nfe) {
            return 0;
        } finally {
            f.setBackground(good ? defaultTextField.getBackground() : BAD_FIELD_BG);
        }
    }

    /* to text from date. */
    private String tt(Date d) {
        return d == null ? "" : ISO8601Date.format(d);
    }

    /* to text from long. */
    private String tt(long n) {
        return n == 0 ? "" : String.valueOf(n);
    }

    /* to text from string. */
    private String tt(String s) {
        return s == null || "*".equals(s) ? "" : s;
    }

    private class RandomIdAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            long rand;
            do {
                rand = Math.abs(random.nextLong());
            } while (rand == 0); // reroll zeros
            idField.setText(String.valueOf(rand));
            fireUpdate();
        }
    }

    private class StartTodayAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            startField.setText(ISO8601Date.format(new Date()));
            fireUpdate();
        }
    }

    private class ExpireNextYearAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1);
            expiryField.setText(ISO8601Date.format(cal.getTime()));
            fireUpdate();
        }
    }

    private class CurrentProductAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            productField.setText(BuildInfo.getProductName());
            fireUpdate();
        }
    }

    private class CurrentMajorVersionAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            majorVersionField.setText(BuildInfo.getProductVersionMajor());
            fireUpdate();
        }
    }

    private class CurrentMinorVersionAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            minorVersionField.setText(BuildInfo.getProductVersionMinor());
            fireUpdate();
        }
    }

    private class CustomEulaAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = FileChooserUtil.createJFileChooser();
            fc.setDialogTitle("Select custom EULA text file");
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            fc.addChoosableFileFilter(buildFileFilter(".txt", "(*.txt) Text files."));
            fc.setMultiSelectionEnabled(false);
            int result = fc.showDialog(LicenseSpecPanel.this, "Load");
            if (JFileChooser.APPROVE_OPTION != result) return;
            File file = fc.getSelectedFile();
            if (file == null) return;

            final byte[] bytes;
            try {
                bytes = HexUtils.slurpFile(file);
            } catch (IOException e1) {
                err("Unable to read file", e1);
                return;
            }

            boolean assumeAscii = true;
            for (byte b : bytes) {
                if (b == '\r' || b == '\n' || b == '\t')
                    continue;
                if (b < 21) {
                    assumeAscii = false;
                    break;
                }
            }

            final String encoding;
            if (assumeAscii) {
                encoding = "ASCII";
            } else {
                encoding = chooseEncoding("File contains non-ASCII characters.  What encoding should be used to read it?");
                if (encoding == null)
                    return;
            }

            try {
                customEulaText = new String(bytes, encoding);
            } catch (UnsupportedEncodingException e1) {
                err("error", e1);
            }

            eulaComboBox.setSelectedItem(EULA_CUSTOM);            
            fireUpdate();
        }
    }

    /**
     * @param prompt the prompt to display.  Required
     * @return the chosen encoding or null if it was canceled.
     */
    private String chooseEncoding(String prompt) {
        String dflt = "ISO8859-1";
        String[] opts = new String[] {
                dflt,
                "UTF-8"
        };
        int result = JOptionPane.showOptionDialog(this,
                                                  prompt,
                                                  "Select Character Encoding",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE,
                                                  null,
                                                  opts,
                                                  dflt);
        return result < 0 ? null : opts[result];

    }

    private void err(String what, Throwable t) {
        JOptionPane.showMessageDialog(this, what + ": " + ExceptionUtils.getMessage(t), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static FileFilter buildFileFilter(final String extension, final String description) {
        return new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
            }
            public String getDescription() {
                return description;
            }
        };
    }

    /*
     * Set feature check status and trigger optimization if anything changes.
     */
    private void setFeatureChecked(GatewayFeatureSet fs, boolean checked) {
        boolean changeMade = setFeatureCheckedNoRecurse(fs, checked);
        if (!changeMade) return;

        // Check or uncheck kids
        List<GatewayFeatureSet> kids = fs.getChildren();
        for (GatewayFeatureSet kid : kids) {
            setFeatureChecked(kid, checked);
        }

        // Request an optimization of the feature tree
        featureCheckChangedSinceLastOptimize = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                optimizeCheckedFeatures(false);
            }
        });
    }

    /*
     * Set feature check status but never trigger any optimization.
     *
     * @return true if the check status was changed.
     */
    private boolean setFeatureCheckedNoRecurse(GatewayFeatureSet fs, boolean checked) {
        boolean wasChecked = isFeatureChecked(fs);

        // Bail early if this one's state hasn't changed
        if (wasChecked == checked)
            return false;

        if (checked) {
            featureNamesChecked.add(fs.getName());
        } else {
            featureNamesChecked.remove(fs.getName());
        }

        return true;
    }

    private boolean isFeatureChecked(GatewayFeatureSet fs) {
        return featureNamesChecked.contains(fs.getName());
    }

    private boolean isFeatureHaveAnyCheckedKids(String featureName) {
        return featureNamesWithCheckedKids.contains(featureName);
    }

    /*
     * Analyze the checked feature tree and ensure that parents are on if and only if all their children are on.
     */
    private void optimizeCheckedFeatures(boolean suppressUpdate) {
        if (!featureCheckChangedSinceLastOptimize)
            return;
        Map<String,GatewayFeatureSet> profiles = GatewayFeatureSets.getProductProfiles();
        for (GatewayFeatureSet set : profiles.values()) {
            optimizeCheckedFeatures(set);
        }
        featureCheckChangedSinceLastOptimize = false;
        featureTree.repaint();
        if (!suppressUpdate)
            fireUpdate();
    }

    /*
     * @return the check status of this feature set after optimization:
     *           Boolean.TRUE:   feature and all children (if any) checked
     *           Boolean.FALSE:  neither feature nor any children checked
     *           null:           feature not checked, but at least one child was TRUE or null
     */
    private Boolean optimizeCheckedFeatures(GatewayFeatureSet set) {
        List<GatewayFeatureSet> kids = set.getChildren();

        // If no kids, status will never change -- it's always whatever was set by the last user action
        if (kids.isEmpty()) {
            featureNamesWithCheckedKids.remove(set.getName());
            return isFeatureChecked(set);
        }

        // Otherwise, status will be whatever is implied by the union of all child statuses.
        boolean anyCheckedOrMaybe = false;  // true if at least one child returned TRUE or null
        boolean anyUncheckedOrMaybe = false;      // true if at least one child returned FALSE or null
        boolean noneChecked = true;
        for (GatewayFeatureSet kid : kids) {
            Boolean kidChecked = optimizeCheckedFeatures(kid);
            if (kidChecked == null) {
                anyCheckedOrMaybe = true;
                anyUncheckedOrMaybe = true;
                noneChecked = false;
            } else {
                if (kidChecked) {
                    anyCheckedOrMaybe = true;
                    noneChecked = false;
                } else
                    anyUncheckedOrMaybe = true;
            }
        }

        // All kids checked as long as none are unchecked
        boolean allchecked = !anyUncheckedOrMaybe;

        // Propagate removal up to parents, but not auto-adds
        if (!allchecked)
            setFeatureCheckedNoRecurse(set, allchecked);
        if (anyCheckedOrMaybe)
            featureNamesWithCheckedKids.add(set.getName());
        else
            featureNamesWithCheckedKids.remove(set.getName());

        if (allchecked) return Boolean.TRUE;
        if (noneChecked) return Boolean.FALSE;
        return null;
    }


    /* Register a child->parent relationship. */
    private void addFeatureParent(GatewayFeatureSet kid, GatewayFeatureSet parent) {
        // The rootSet doesn't really exist, it's a fiction for the benefit of the JTree, so don't let it bung up
        // our record of which are the root-most feature sets.
        if (parent == rootSet) return;

        Set<GatewayFeatureSet> parents = featureParents.get(kid.getName());
        if (parents == null) {
            parents = new HashSet<GatewayFeatureSet>();
            featureParents.put(kid.getName(), parents);
        }
        parents.add(parent);
    }

    private class FeatureNode extends DefaultMutableTreeNode {
        public FeatureNode(GatewayFeatureSet fs) {
            super(null, fs.getChildren().size() > 0);
            setUserObject(fs);
            for (GatewayFeatureSet kid : fs.getChildren()) {
                addFeatureParent(kid, fs);
                add(new FeatureNode(kid));
            }
        }

        public void setUserObject(Object userObject) {
            if (userObject == null) throw new NullPointerException("No userObject provided");
            if (userObject instanceof GatewayFeatureSet) {
                super.setUserObject(userObject);
            } else if (userObject instanceof Boolean) {
                // Consume this and translate it into the correct behavior
                Boolean b = (Boolean)userObject;
                GatewayFeatureSet fs = (GatewayFeatureSet)getUserObject();
                LicenseSpecPanel.this.setFeatureChecked(fs, b);
            } else
                throw new ClassCastException("Unacceptable user object type: " + userObject.getClass());
        }
    }

    private void setText(JCheckBox cb, GatewayFeatureSet fs) {
        cb.setText("<HTML><B>" + fs.getName() + "</B>: " + fs.getDescription());
    }

    private static final GreyableCheckBox cbtristate = new GreyableCheckBox();

    private static class GreyableCheckBox extends JCheckBox {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
    }

    /** A JCheckBox that will display itself as a grey box if its unchecked but at least one child feature is checked. */
    private class SuspiciousCheckBox extends JCheckBox {

        protected void paintComponent(Graphics g) {
            // Check for a feature name
            if (!isSelected()) {
                String text = getText();
                if (text != null && text.startsWith("<HTML><B>")) {
                    int slash = text.indexOf('/');
                    String featureName = text.substring(9, slash - 1);
                    if (LicenseSpecPanel.this.isFeatureHaveAnyCheckedKids(featureName)) {
                        drawPartialCheck(g);
                        return;
                    }
                }
            }

            super.paintComponent(g);
        }

        private void drawPartialCheck(Graphics g) {
            cbtristate.setSize(getSize());
            cbtristate.setLocation(getLocation());
            cbtristate.setText(getText());
            cbtristate.setForeground(getForeground());
            cbtristate.setBackground(getBackground());
            cbtristate.setOpaque(isOpaque());
            cbtristate.getModel().setRollover(true);
            cbtristate.getModel().setArmed(true);
            cbtristate.paintComponent(g);
        }
    }

    private class FeatureTreeRenderer extends DefaultTreeCellRenderer {
        final Color[] gcol = new Color[] { Color.CYAN };

        JCheckBox comp = new SuspiciousCheckBox();

        /** @return the renderer, as it is currently configured. */
        public JCheckBox getCheckBox() {
            return comp;
        }


        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (!(value instanceof FeatureNode))
                throw new ClassCastException("Not a FeatureNode: " + value);

            FeatureNode fn = (FeatureNode)value;
            GatewayFeatureSet fs = (GatewayFeatureSet)fn.getUserObject();
            LicenseSpecPanel.this.setText(comp, fs);
            comp.setSelected(LicenseSpecPanel.this.isFeatureChecked(fs));
            if (sel) {
                comp.setForeground(getTextSelectionColor());
                comp.setBackground(getBackgroundSelectionColor());
            } else {
                comp.setForeground(getTextNonSelectionColor());
                comp.setBackground(getBackgroundNonSelectionColor());
            }
            return comp;
        }
    }

    private class FeatureCellEditor extends DefaultCellEditor {
        FeatureTreeRenderer renderer = new FeatureTreeRenderer();
        GatewayFeatureSet lastFs = null;

        public FeatureCellEditor(JCheckBox checkBox) {
            super(checkBox);
        }

        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            FeatureNode fn = (FeatureNode)value;
            GatewayFeatureSet gfs = (GatewayFeatureSet)fn.getUserObject();
            boolean b = LicenseSpecPanel.this.isFeatureChecked(gfs);
            JCheckBox editor = (JCheckBox)super.getTreeCellEditorComponent(tree, b, isSelected, expanded, leaf, row);
            LicenseSpecPanel.this.setText(editor, gfs);
            return editor;
        }
    }

}
