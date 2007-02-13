package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlComposer;
import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * User: megery
 * Date: Jan 22, 2007
 * Time: 4:47:04 PM
 */
public class WSDLCompositionPanel extends WizardStepPanel{

    private static final Logger logger = Logger.getLogger(WSDLCompositionPanel.class.getName());

    private JLabel panelHeader;
    private JPanel mainPanel;
    private JButton addWSDL;
    private JButton deleteWSDL;

    private WsdlListModel wsdlListModel;
    private JList sourceWsdlList;

    private JTabbedPane sourceTabs;

    private JPanel sourceOperationsListPanel;
    private JList sourceOperationsList;
    private DefaultListModel sourceOperationsListModel;

    private JPanel sourceTreePanel;
    private JTree sourcePreviewTree;
    private WsdlTreeModel sourceWsdlTreeModel;

    private JTabbedPane resultTabs;
    private WsdlTreeModel resultingWsdlTreeModel;
    private JTree resultingWsdlTree;

    private JPanel resultOperationsPanel;
    private JList resultOperationsList;
    private WsdlOperationsListModel resultOperationsListModel;

    private JButton addToResultButton;
    private JButton removeFromResultButton;
    
    private ActionListener manageCompositionActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Object obj = e.getSource();
            if (obj instanceof JButton) {
                JButton jButton = (JButton) obj;
                if (jButton == addToResultButton) {
                    addToResultingWsdl();
                } else if (jButton == removeFromResultButton) {
                    removeFromResultingWsdl();
                } else {
                }
            }
        }
    };
//    private WsdlComposer wsdlComposer;
    private Icon operationIcon;

    private Definition resultingDef;

    public WSDLCompositionPanel(WizardStepPanel next) {
        super(next);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initialize();
        operationIcon = new ImageIcon(getClass().getClassLoader().getResource("com/l7tech/console/resources/methodPublic.gif"));
    }

    private void initialize() {
        setShowDescriptionPanel(false);
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));

        wsdlListModel = new WsdlListModel();
        sourceWsdlList.setModel(wsdlListModel);

        sourceOperationsListModel = new DefaultListModel();
        sourceOperationsList.setModel(sourceOperationsListModel);

//        resultOperationsListModel = new WsdlOperationsListModel();
//        resultOperationsList.setModel(resultOperationsListModel);
                        
        sourceTabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                enableAddRemoveOperationsButtons();
            }
        });
        resultTabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                enableAddRemoveOperationsButtons();
            }
        });

        addWSDL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAddWsdl();
            }
        });

        deleteWSDL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doDeleteWsdl();
            }
        });

        sourceWsdlList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                updateSourceWsdlInfo();
            }
        });

        addToResultButton.addActionListener(manageCompositionActionListener);
        addToResultButton.setToolTipText("Add to Resulting WSDL");

        removeFromResultButton.addActionListener(manageCompositionActionListener);
        removeFromResultButton.setToolTipText("Remove from Resulting WSDL");

        
        sourceOperationsList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BindingOperation) {
                    BindingOperation bindingOperation = (BindingOperation) value;
                    label.setText(bindingOperation.getName());
                    label.setIcon(operationIcon);
                }
                return label;
            }
        });

        resultOperationsList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BindingOperationHolder) {
                    BindingOperationHolder bindingOperation = (BindingOperationHolder) value;
                    label.setText(bindingOperation.bindingOperation.getName());
                    label.setIcon(operationIcon);
                }
                return label;
            }
        });
        
        sourceWsdlTreeModel = new WsdlTreeModel(null);
        sourcePreviewTree.setRootVisible(true);
        sourcePreviewTree.setModel(sourceWsdlTreeModel);
        sourcePreviewTree.setCellRenderer(new EntityTreeCellRenderer());

        resultingWsdlTreeModel = new WsdlTreeModel(null);
        resultingWsdlTree.setRootVisible(true);
        resultingWsdlTree.setModel(resultingWsdlTreeModel);
        resultingWsdlTree.setCellRenderer(new EntityTreeCellRenderer());
    }

    private void enableAddRemoveOperationsButtons() {
        JPanel selectedSourcePanel = (JPanel)sourceTabs.getSelectedComponent();
        addToResultButton.setEnabled(selectedSourcePanel == sourceOperationsListPanel);

        JPanel selectedResultPanel = (JPanel)resultTabs.getSelectedComponent();
        removeFromResultButton.setEnabled(selectedResultPanel == resultOperationsPanel);

    }

    private void doDeleteWsdl() {
        int selectedIndex = sourceWsdlList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < wsdlListModel.getSize()) {
            Object selObj = sourceWsdlList.getModel().getElementAt(selectedIndex);
            if (selObj instanceof WsdlComposer.WsdlHolder) {
                WsdlComposer.WsdlHolder holder = (WsdlComposer.WsdlHolder) selObj;
                String s = new String(holder.wsdl.toString());
                JOptionPane.showMessageDialog(WSDLCompositionPanel.this, "<code>" + s+ "</code>");
            }
        }
    }

    private void doAddWsdl() {
        ChooseWsdlDialog dlg = new ChooseWsdlDialog(getOwner(), logger, "Choose WSDL");
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        if (!dlg.wasCancelled()) {
            Wsdl wsdl = dlg.getSelectedWsdl();
            Document wsdlDoc = dlg.getWsdlDocument();
            String wsdlLocation = dlg.getWsdlLocation();
            if (wsdlDoc != null) {
                wsdlListModel.addWsdl(wsdl, wsdlLocation);
                ensureSourceSelected();
            }
        }
    }

    private void ensureSourceSelected() {
        if (sourceWsdlList.getSelectedValue() == null && sourceWsdlList.getModel().getSize() > 0)
            sourceWsdlList.setSelectedIndex(0);
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        WsdlComposer wsdlComposer = (WsdlComposer) settings;

        resultOperationsListModel = new WsdlOperationsListModel(wsdlComposer);
        resultOperationsList.setModel(resultOperationsListModel);

        resultingDef = wsdlComposer.getOutputWsdl();
//        if (wsdlComposer == null)
//            wsdlComposer = new WsdlComposer(resultingDef);
        updateResultingWsdlView();
    }

    private void updateResultingWsdlView() {
        updateResultOperations();
        updateResultTree();
    }

    private void updateResultTree() {
        WsdlTreeNode.Options opts = new WsdlTreeNode.Options();
        WsdlTreeNode rootResultingWsdlTreeNode = WsdlTreeNode.newInstance(new Wsdl(resultingDef), opts);
        resultingWsdlTreeModel.setRoot(rootResultingWsdlTreeNode);
    }

    private void updateResultOperations() {
    }

    private void updateSourceWsdlInfo() {
        updateSourceOperationsList();
        prepareSourceWsdlTree();
    }

    private void updateSourceOperationsList() {
        sourceOperationsListModel.clear();

        WsdlComposer.WsdlHolder currentHolder = getSelectedSourceWsdl();
        Definition currentDef = currentHolder.wsdl.getDefinition();
        Map bindings = currentDef.getBindings();
        Set keys = bindings.keySet();

        for (Object key : keys) {
            Object bindingObj = bindings.get(key);
            javax.wsdl.Binding binding = (javax.wsdl.Binding) bindingObj;
            java.util.List ops = binding.getBindingOperations();
            for (Object op : ops) {
                sourceOperationsListModel.addElement(op);
            }
        }
    }

    private void prepareSourceWsdlTree() {
        WsdlTreeNode rootSourceWsdlTreeNode = WsdlTreeNode.newInstance(getSelectedSourceWsdl().wsdl);
        sourceWsdlTreeModel.setRoot(rootSourceWsdlTreeNode);
        sourcePreviewTree.setRootVisible(true);
    }

    public String getStepLabel() {
        return "Compose WSDL";
    }

    private void addToResultingWsdl() {
        Object[] selected = sourceOperationsList.getSelectedValues();
        if (selected != null && selected.length > 0) {
            for (Object o : selected) {
                BindingOperation bo = (BindingOperation) o;
                resultOperationsListModel.addBindingOperation(bo);
            }
        }
        updateResultingWsdlView();
    }

    private void removeFromResultingWsdl() {
        Object[] selected = resultOperationsList.getSelectedValues();
        if (selected != null && selected.length > 0) {
            for (Object o : selected) {
                BindingOperationHolder boh = (BindingOperationHolder) o;
                resultOperationsListModel.removeBindingOperation(boh);
            }
        }
        updateResultingWsdlView();
    }

    private WsdlComposer.WsdlHolder getSelectedSourceWsdl() {
        return (WsdlComposer.WsdlHolder) sourceWsdlList.getSelectedValue();
    }

    private class WsdlListModel extends AbstractListModel {

        java.util.List<WsdlComposer.WsdlHolder> wsdlList = new ArrayList<WsdlComposer.WsdlHolder>();

        public int getSize() {
            return wsdlList.size();
        }

        public Object getElementAt(int index) {
            return wsdlList.get(index);
        }

        public void addWsdl(Wsdl wsdl, String wsdlLocation) {
            if (wsdl != null) {
                WsdlComposer.WsdlHolder holder = new WsdlComposer.WsdlHolder(wsdl, wsdlLocation);
                if (!wsdlList.contains(holder)) {
                    try {
                        wsdlList.add(holder);
                    } finally {
                        fireContentsChanged(sourceWsdlList, 0, wsdlList.size());
                    }
                }
            }
        }
    }

    public static class ChooseWsdlDialog extends JDialog {

        private JPanel contentPane;
        private JPanel mainPanel;
        private JButton buttonOK;
        private JButton buttonCancel;

        private Logger logger;
        private WsdlLocationPanel wlp;
        private boolean wasCancelled;

        public ChooseWsdlDialog(Dialog owner, Logger logger, String title) throws HeadlessException {
            super(owner, title, true);
            this.logger = logger;
            initialise();
        }

        private void initialise() {
            buttonOK.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onOk();
                }
            });

            buttonCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            });

            wlp = new WsdlLocationPanel(this, logger, true, false);
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add(wlp, BorderLayout.CENTER);

            setLayout(new BorderLayout());
            add(contentPane, BorderLayout.CENTER);
            pack();

        }

        private void onCancel() {
            wasCancelled = true;
            dispose();
        }

        private void onOk() {
            wasCancelled = false;
            dispose();
        }


        public boolean wasCancelled() {
            return wasCancelled;
        }

        public Wsdl getSelectedWsdl() {
            return wlp.getWsdl();
        }

        public String getWsdlLocation() {
            return wlp.getWsdlUrl();
        }

        public Document getWsdlDocument() {
            return wlp.getWsdlDocument();
        }
    }

    private class WsdlTreeModel extends DefaultTreeModel {

        Map<String,java.util.List<String>> wsdlMap;

        public WsdlTreeModel(TreeNode root) {
            super(root);
            wsdlMap = new TreeMap<String, java.util.List<String>>();
        }

        public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
            if (!(newChild instanceof WsdlTreeNode)) {
                throw new IllegalArgumentException("Expected " + WsdlTreeNode.class.getName());
            }

            if (!(parent instanceof WsdlTreeNode)) {
                throw new IllegalArgumentException("Expected " + WsdlTreeNode.class.getName());
            }

            String childName = ((WsdlTreeNode)newChild).getName();
            String parentName = ((WsdlTreeNode)parent).getName();
            java.util.List<String> whichPart = wsdlMap.get(parentName);
            if (whichPart == null) {
                whichPart = new ArrayList<String>();
                wsdlMap.put(parentName, whichPart);
            }
            whichPart.add(childName);

            super.insertNodeInto(newChild, parent, index);
        }
    }

    private class BindingOperationHolder {
        private BindingOperation bindingOperation;
        private WsdlComposer.WsdlHolder sourceWsdlHolder;

        public BindingOperationHolder(BindingOperation bo, WsdlComposer.WsdlHolder sourceWsdl) {
            this.bindingOperation = bo;
            this.sourceWsdlHolder = sourceWsdl;
        }

        public String toString() {
            return sourceWsdlHolder.toString();
        }
    }

    private class WsdlOperationsListModel extends DefaultComboBoxModel {
        private WsdlComposer wsdlComposer;

        public WsdlOperationsListModel(WsdlComposer wsdlComposer) {
            super();
            this.wsdlComposer = wsdlComposer;
        }

        public void addBindingOperation(BindingOperation realOperation) {
            try {
                WsdlComposer.WsdlHolder sourceWsdl = getSelectedSourceWsdl();
                BindingOperationHolder operationHolder = new BindingOperationHolder(realOperation, sourceWsdl);
                this.addElement(operationHolder);
                wsdlComposer.addBindingOperation(realOperation, sourceWsdl);
            } catch (WSDLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }

        public void removeBindingOperation(BindingOperationHolder operationHolder) {
            this.removeElement(operationHolder);
            wsdlComposer.removeBindingOperation(operationHolder.bindingOperation, operationHolder.sourceWsdlHolder);
        }
    }
}
