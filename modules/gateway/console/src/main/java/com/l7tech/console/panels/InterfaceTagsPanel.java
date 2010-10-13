package com.l7tech.console.panels;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class InterfaceTagsPanel extends ValidatedPanel<Set<InterfaceTag>> {
    private static final Logger logger = Logger.getLogger(InterfaceTagsPanel.class.getName());
    private final int INDEX_VALUE = 0;
    private final int INDEX_DONE = 1;

    private JPanel mainPanel;
    private JButton createTagButton;
    private JButton deleteTagButton;
    private JButton addAddressButton;
    private JButton removeAddressButton;
    private JButton editAddressButton;
    private JList tagList;
    private SortedListModel<InterfaceTag> tagListModel;
    private JList patternList;
    private JSplitPane splitPane;

    private final Set<InterfaceTag> model;
    private Collection<SsgConnector> connectors;

    public InterfaceTagsPanel(Set<InterfaceTag> model) {
        this.model = new LinkedHashSet<InterfaceTag>(model);
        init();
    }

    public InterfaceTagsPanel(String propertyName, Set<InterfaceTag> model) {
        super(propertyName);
        this.model = model;
        init();
    }

    @Override
    protected Set<InterfaceTag> getModel() {
        return model;
    }

    private Collection<SsgConnector> loadAllConnectors() {
        try {
            final TransportAdmin transportAdmin = Registry.getDefault().getTransportAdmin();
            return transportAdmin == null ? Collections.<SsgConnector>emptyList() : transportAdmin.findAllSsgConnectors();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to load connector list to check if interface is in use: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Collections.emptyList();
        }
    }
    
    @Override
    protected void initComponents() {
        Utilities.deuglifySplitPane(splitPane);
        Utilities.equalizeButtonSizes(createTagButton, deleteTagButton, addAddressButton, removeAddressButton, editAddressButton);

        connectors = Functions.grep(loadAllConnectors(), new Functions.Unary<Boolean, SsgConnector>() {
            @Override
            public Boolean call(SsgConnector ssgConnector) {
                return ssgConnector.isEnabled();
            }
        });

        final DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof InterfaceTag) {
                    InterfaceTag tag = (InterfaceTag) value;
                    value = tag.getName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        };
        tagList.setCellRenderer(cellRenderer);
        tagListModel = new SortedListModel<InterfaceTag>(new Comparator<InterfaceTag>() {
            @Override
            public int compare(InterfaceTag a, InterfaceTag b) {
                return a.getName().compareTo(b.getName());
            }
        });
        tagListModel.addAll(model.toArray(new InterfaceTag[model.size()]));
        tagList.setModel(tagListModel);
        tagList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                populatePatternList(getSelectedTag());
            }
        });
        populatePatternList(null);

        Utilities.enableGrayOnDisabled(patternList);
        patternList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnableState();
            }
        });

        createTagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tagList.clearSelection();
                promptForInterfaceName(new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(final String interfaceName) {
                        promptForAddressPattern(null, new Functions.UnaryVoid<String>() {
                            @Override
                            public void call(String addressPattern) {
                                final InterfaceTag added = new InterfaceTag(interfaceName, new LinkedHashSet<String>(Arrays.asList(addressPattern)));
                                tagListModel.add(added);
                                tagList.setSelectedValue(added, true);
                                populatePatternList(added);
                            }
                        });
                    }
                });
            }
        });

        deleteTagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InterfaceTag tag = getSelectedTag();
                if (tag == null)
                    return;

                if (warnIfInUse(tag))
                    return;

                DialogDisplayer.showConfirmDialog(InterfaceTagsPanel.this,
                        "Are you sure you wish to delete the interface " + tag.getName() + "?",
                        "Delete Interface",
                        JOptionPane.OK_CANCEL_OPTION,
                        new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (JOptionPane.OK_OPTION != option)
                            return;
                        tagListModel.removeElement(tag);
                        populatePatternList(null);
                    }
                });
            }
        });

        addAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InterfaceTag tag = getSelectedTag();
                if (tag == null)
                    return;

                if (warnIfInUse(tag))
                    return;

                promptForAddressPattern(null, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String address) {
                        tag.getIpPatterns().add(address);
                        populatePatternList(tag);
                        patternList.setSelectedValue(address, true);
                    }
                });
            }
        });

        removeAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InterfaceTag tag = getSelectedTag();
                if (tag == null)
                    return;

                final String address = getSelectedAddress();
                if (address == null)
                    return;

                if (warnIfInUse(tag))
                    return;

                tag.getIpPatterns().remove(address);
                populatePatternList(tag);
            }
        });

        editAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InterfaceTag tag = getSelectedTag();
                if (tag == null)
                    return;

                final String oldAddress = getSelectedAddress();
                if (oldAddress == null)
                    return;

                if (warnIfInUse(tag))
                    return;

                promptForAddressPattern(oldAddress, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String address) {
                        if (oldAddress.equals(address))
                            return;
                        tag.getIpPatterns().remove(oldAddress);
                        tag.getIpPatterns().add(address);
                        populatePatternList(tag);
                        patternList.setSelectedValue(address, true);
                    }
                });
            }
        });

        add(mainPanel, BorderLayout.CENTER);
        updateEnableState();
    }

    private boolean warnIfInUse(InterfaceTag tag) {
        SsgConnector connector = getFirstConnectorUsingTag(tag.getName());
        if (connector != null) {
            DialogDisplayer.showMessageDialog(this,
                    "Unable to Change or Remove In-Use Interface",
                    "The specified interface is currently in use by the listen port named " + connector.getName(),
                    null);
            return true;
        }
        return false;
    }

    private SsgConnector getFirstConnectorUsingTag(String tagName) {
        for (SsgConnector connector : connectors)
            if (tagName.equalsIgnoreCase(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS)))
                return connector;
        return null;
    }

    private void promptForInterfaceName(final Functions.UnaryVoid<String> nameUser) {
        final Object[] info = new Object[] {
            null, // Input value with an initial value, null.
            false // Flag to indicate if the input process is finished or not.
        };
        while (! (Boolean)info[INDEX_DONE]) {
            DialogDisplayer.showInputDialog(InterfaceTagsPanel.this, "Please enter a name for the new interface.", "Interface Name", JOptionPane.PLAIN_MESSAGE, null, null, info[INDEX_VALUE], new DialogDisplayer.InputListener() {
                @Override
                public void reportResult(Object option) {
                    if (option == null || option.toString().length() < 1) {
                        info[INDEX_DONE] = true;
                        return;
                    }
                    final String name = option.toString();
                    if (!InterfaceTag.isValidName(name)) {
                        DialogDisplayer.showMessageDialog(InterfaceTagsPanel.this, "An interface name must start with a letter or underscore, and can contain only ASCII letters, underscores, or numbers.",
                            "Invalid Interface Name", JOptionPane.ERROR_MESSAGE, null);
                    } else if (isDuplicateInterfaceName(name)) {
                        DialogDisplayer.showMessageDialog(InterfaceTagsPanel.this, "The interface '" + name + "' already exists.  Please use a new interface name to try again.",
                            "Duplicate Interface Name", JOptionPane.ERROR_MESSAGE, null);
                    } else {
                        info[INDEX_DONE] = true;
                        nameUser.call(name);
                    }
                    info[INDEX_VALUE] = name;
                }
            });
        }
    }

    /**
     * Check if the name has been used by any interface tag.
     * @param interfaceTagName: the name of an interface tag.
     * @return true if the name has beens used by some interface tag.
     */
    private boolean isDuplicateInterfaceName(String interfaceTagName) {
        if (interfaceTagName == null) throw new IllegalArgumentException("Interface Tag Name is not specified.");

        Collection<InterfaceTag> tags = tagListModel.toList();
        if (tags == null || tags.isEmpty()) return false;

        for (InterfaceTag tag: tags) {
            if (tag.getName().equals(interfaceTagName)) {
                return true;
            }
        }

        return false;
    }

    private void promptForAddressPattern(String initialValue, final Functions.UnaryVoid<String> patternUser) {
        final Object[] info = new Object[] {
            initialValue, // Input value with a given initial value, initialValue.
            false         // Flag to indicate if the input process is finished or not.
        };
        while (! (Boolean)info[INDEX_DONE]) {
            DialogDisplayer.showInputDialog(InterfaceTagsPanel.this, "Please enter an address pattern.", "Address Pattern", JOptionPane.PLAIN_MESSAGE, null, null, info[INDEX_VALUE], new DialogDisplayer.InputListener() {
                @Override
                public void reportResult(Object option) {
                    if (option == null || option.toString().length() < 1) {
                        info[INDEX_DONE] = true;
                        return;
                    }
                    final String pattern = option.toString();
                    if (!InterfaceTag.isValidPattern(pattern)) {
                        DialogDisplayer.showMessageDialog(InterfaceTagsPanel.this,
                            "Address patterns should be in this format: 127.0.0/24" + (Registry.getDefault().getTransportAdmin().isUseIpv6() ? " or 2222::/64" :""),
                            "Invalid Address Pattern", JOptionPane.ERROR_MESSAGE, null);
                    } else if (isDuplicateAddressPattern(pattern)) {
                        DialogDisplayer.showMessageDialog(InterfaceTagsPanel.this, "The address pattern '" + pattern + "' already exists.  Please use a new pattern to try again.",
                            "Duplicate Address Pattern", JOptionPane.ERROR_MESSAGE, null);
                    }
                    else {
                        info[INDEX_DONE] = true;
                        patternUser.call(pattern);
                    }
                    info[INDEX_VALUE] = pattern;
                }
            });
        }
    }

    /**
     * Check if the address pattern already exists in the pattern list.
     * @param addressPattern: the address pattern to be checked.
     * @return true if the address pattern already exists.
     */
    private boolean isDuplicateAddressPattern(String addressPattern) {
        if (addressPattern == null) throw new IllegalArgumentException("Address Pattern is not specified.");

        for (int i = 0; i < patternList.getModel().getSize(); i++) {
            if (addressPattern.equals(patternList.getModel().getElementAt(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean isSelectedInRange(JList list) {
        int index = list.getMinSelectionIndex();
        return !(index < 0 || index >= list.getModel().getSize());
    }

    private InterfaceTag getSelectedTag() {
        return isSelectedInRange(tagList) ? (InterfaceTag)tagList.getSelectedValue() : null;
    }

    private String getSelectedAddress() {
        return isSelectedInRange(patternList) ? (String)patternList.getSelectedValue() : null;
    }

    private void populatePatternList(InterfaceTag tag) {
        if (tag != null)
            patternList.setListData(tag.getIpPatterns().toArray());
        else
            patternList.setListData(new Object[0]);
        updateEnableState();
    }

    private void updateEnableState() {
        boolean haveTag = getSelectedTag() != null;
        boolean haveAddress = getSelectedAddress() != null;

        deleteTagButton.setEnabled(haveTag);
        addAddressButton.setEnabled(haveTag);
        patternList.setEnabled(haveTag);

        removeAddressButton.setEnabled(haveTag && haveAddress);
        editAddressButton.setEnabled(haveTag && haveAddress);

        checkSyntax();
        checkSemantic();
    }

    @Override
    public void focusFirstComponent() {
        tagList.requestFocus();
    }

    @Override
    protected void doUpdateModel() {
        model.clear();
        model.addAll(tagListModel.toList());
    }
}
