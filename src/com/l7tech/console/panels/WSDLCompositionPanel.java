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
import javax.wsdl.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
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

    private SourceWsdlListModel sourceWsdlListModel;
    private JList sourceWsdlList;

    private JTabbedPane sourceTabs;

    private JPanel sourceOperationsListPanel;
    private JList sourceOperationsList;
    private DefaultListModel sourceOperationsListModel;

    private JTree sourcePreviewTree;
    private WsdlTreeViewModel sourceWsdlTreeModel;

    private JTabbedPane resultTabs;
    private WsdlTreeViewModel resultingWsdlTreeModel;
    private JTree resultingWsdlTree;

    private JPanel resultOperationsPanel;
    private JList resultOperationsList;
    private ResultingOperationsListModel resultOperationsListModel;

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
    private WsdlComposer wsdlComposer;
    private Icon operationIcon;

    public WSDLCompositionPanel(WizardStepPanel next) {
        super(next);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initialize();
        operationIcon = new ImageIcon(getClass().getClassLoader().getResource("com/l7tech/console/resources/methodPublic.gif"));
    }

    public String getStepLabel() {
        return "Compose WSDL";
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        wsdlComposer = (WsdlComposer) settings;
        populateSourceWsdlView();
        updateResultingWsdlView();
    }

    private void populateSourceWsdlView() {
        if (wsdlComposer == null || wsdlComposer.getSourceWsdls().isEmpty())
            return;
        
        sourceWsdlListModel.clear();
        for (WsdlComposer.WsdlHolder wsdlHolder : wsdlComposer.getSourceWsdls())
            sourceWsdlListModel.addWsdl(wsdlHolder.wsdl, wsdlHolder.toString());
    }

    private void initialize() {
        setShowDescriptionPanel(false);
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));

        sourceWsdlListModel = new SourceWsdlListModel();
        sourceWsdlList.setModel(sourceWsdlListModel);

        sourceOperationsListModel = new DefaultListModel();
        sourceOperationsList.setModel(sourceOperationsListModel);

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
                doAddSourceWsdl();
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
        
        sourceWsdlTreeModel = new WsdlTreeViewModel(null);
        sourcePreviewTree.setRootVisible(true);
        sourcePreviewTree.setModel(sourceWsdlTreeModel);
        sourcePreviewTree.setCellRenderer(new EntityTreeCellRenderer());

        resultingWsdlTreeModel = new WsdlTreeViewModel(null);
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

    private void doAddSourceWsdl() {
        ChooseWsdlDialog dlg = new ChooseWsdlDialog(getOwner(), logger, "Choose WSDL");
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        if (!dlg.wasCancelled()) {
            Wsdl wsdl = dlg.getSelectedWsdl();
            Document wsdlDoc = dlg.getWsdlDocument();
            String wsdlLocation = dlg.getWsdlLocation();
            if (wsdlDoc != null) {
                sourceWsdlListModel.addWsdl(wsdl, wsdlLocation);
                ensureSourceSelected();
            }
        }
    }

    private void ensureSourceSelected() {
        if (sourceWsdlList.getSelectedValue() == null && sourceWsdlList.getModel().getSize() > 0)
            sourceWsdlList.setSelectedIndex(0);
    }

    private void updateResultingWsdlView() {
        updateResultOperationsList();
        updateResultTree();
    }

    private void updateResultOperationsList() {
        if (resultOperationsListModel == null) {
            resultOperationsListModel = new ResultingOperationsListModel(wsdlComposer);
            resultOperationsList.setModel(resultOperationsListModel);
        }
    }

    private void updateResultTree() {
        WsdlTreeNode.Options opts = new WsdlTreeNode.Options();
        WsdlTreeNode rootResultingWsdlTreeNode = null;
        try {
            rootResultingWsdlTreeNode = WsdlTreeNode.newInstance(new Wsdl(wsdlComposer.buildOutputWsdl()), opts);
        } catch (WSDLException e) {
            logger.warning("Could not parse WSDL: " + e.getMessage());
        } catch (IOException e) {
            logger.warning("Could not parse WSDL: " + e.getMessage());
        } catch (SAXException e) {
            logger.warning("Could not parse WSDL: " + e.getMessage());
        }
        resultingWsdlTreeModel.setRoot(rootResultingWsdlTreeNode);
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

    private void addToResultingWsdl() {
        Object[] selected = sourceOperationsList.getSelectedValues();
        if (selected != null && selected.length > 0) {
            List<BindingOperation> ops = new ArrayList<BindingOperation>();
            for (Object o : selected) {
                ops.add((BindingOperation) o);
            }
            resultOperationsListModel.addBindingOperations(ops);
        }
        updateResultingWsdlView();
    }

    private void removeFromResultingWsdl() {
        Object[] selected = resultOperationsList.getSelectedValues();
        if (selected != null && selected.length > 0) {
            List<BindingOperationHolder> ops = new ArrayList<BindingOperationHolder>();
            for (Object o : selected) {
                ops.add((BindingOperationHolder) o);
            }
            resultOperationsListModel.removeBindingOperations(ops);
        }
        updateResultingWsdlView();
    }

    private WsdlComposer.WsdlHolder getSelectedSourceWsdl() {
        return (WsdlComposer.WsdlHolder) sourceWsdlList.getSelectedValue();
    }

    private class SourceWsdlListModel extends AbstractListModel {

        java.util.List<WsdlComposer.WsdlHolder> wsdlList = new ArrayList<WsdlComposer.WsdlHolder>();

        public int getSize() {
            return wsdlList.size();
        }

        public Object getElementAt(int index) {
            return wsdlList.get(index);
        }

        public void update() {
            fireContentsChanged(sourceWsdlList, 0, wsdlList.size());
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

        public void clear() {
            wsdlList.clear();
            fireContentsChanged(sourceWsdlList, 0, wsdlList.size());            
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
    }

    private class WsdlTreeViewModel extends DefaultTreeModel {

        Map<String,java.util.List<String>> wsdlMap;

        public WsdlTreeViewModel(TreeNode root) {
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
        public BindingOperation bindingOperation;
        public WsdlComposer.WsdlHolder sourceWsdlHolder;

        public BindingOperationHolder(BindingOperation bo ,WsdlComposer.WsdlHolder sourceWsdl) {
            this.bindingOperation = bo;
            this.sourceWsdlHolder = sourceWsdl;
        }

        public String toString() {
            return bindingOperation.getName();
        }
    }

    private class ResultingOperationsListModel extends DefaultComboBoxModel {
        private WsdlComposer wsdlComposer;

        public ResultingOperationsListModel(WsdlComposer wsdlComposer) {
            super();
            this.wsdlComposer = wsdlComposer;
            populate();
        }

        private void populate() {
            if (wsdlComposer != null) {
                Set<BindingOperation> allOps = new HashSet<BindingOperation>();
//                Collection<BindingOperation> bindingOperations = wsdlComposer.getBindingOperations();
//                if (bindingOperations != null) {
//                    allOps.addAll(bindingOperations);
//                }

                Binding b = wsdlComposer.getBinding();
                if (b != null) {
                    List others = b.getBindingOperations();
                    if (others != null) {
                        for (Object other : others) {
                            allOps.add((BindingOperation) other);
                        }
                    }
                }
                for (BindingOperation anOp : allOps) {
                    addElement(new BindingOperationHolder(anOp, null));
                }
            }
        }

        public void addBindingOperations(Collection<BindingOperation> operations) {
            WsdlComposer.WsdlHolder sourceWsdl = getSelectedSourceWsdl();
            for (BindingOperation bindingOperation: operations) {
                if (wsdlComposer.addBindingOperation(bindingOperation, sourceWsdl)) {
                    this.addElement(new BindingOperationHolder(bindingOperation, sourceWsdl));
                }
            }
        }

        public void removeBindingOperations(List<BindingOperationHolder> operationHolders) {
            for (BindingOperationHolder operationHolder : operationHolders) {
                WsdlComposer.WsdlHolder wsdlHolder = operationHolder.sourceWsdlHolder;
                wsdlComposer.removeBindingOperation(operationHolder.bindingOperation, wsdlHolder);
                this.removeElement(operationHolder);
            }
        }
    }
}
