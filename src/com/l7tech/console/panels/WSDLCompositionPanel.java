package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlComposer;
import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.TopComponents;
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
import javax.wsdl.WSDLException;
import javax.wsdl.Binding;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Jan 22, 2007
 * Time: 4:47:04 PM
 */
public class WSDLCompositionPanel extends WizardStepPanel{

    private static final Logger logger = Logger.getLogger(WSDLCompositionPanel.class.getName());
    public static final String MAX_SOURCE_WSDLS = "com.l7tech.wsdl.maxsources";
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
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
    private int maxSources;

    public WSDLCompositionPanel(WizardStepPanel next) {
        super(next);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initialize();
        operationIcon = new ImageIcon(getClass().getClassLoader().getResource(RESOURCE_PATH +  "/methodPublic.gif"));
    }

    @Override
    public String getStepLabel() {
        return "Compose WSDL";
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        wsdlComposer = (WsdlComposer) settings;
        populateSourceWsdlView();
        updateResultingWsdlView();
        if (resultOperationsListModel != null)
            resultOperationsListModel.revalidate();
    }

    private void populateSourceWsdlView() {
        if (wsdlComposer == null || wsdlComposer.getSourceWsdls(false).isEmpty())
            return;

        sourceWsdlListModel.clear();
        for (WsdlComposer.WsdlHolder wsdlHolder : wsdlComposer.getSourceWsdls(true))
            sourceWsdlListModel.addWsdl(wsdlHolder);
        ensureSourceSelected();
    }

    private void initialize() {
        maxSources = Integer.getInteger(MAX_SOURCE_WSDLS, 50);
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

        addToResultButton.setIcon(new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/Add16.gif")));
        addToResultButton.addActionListener(manageCompositionActionListener);
        addToResultButton.setToolTipText("Add to Resulting WSDL");

        removeFromResultButton.setIcon(new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/Remove16.gif")));
        removeFromResultButton.addActionListener(manageCompositionActionListener);
        removeFromResultButton.setToolTipText("Remove from Resulting WSDL");

        
        sourceOperationsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
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
            @Override
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
        if (sourceWsdlListModel.getSize() >= maxSources) {
            return;
        }
        ChooseWsdlDialog dlg = new ChooseWsdlDialog(getOwner(), logger, "Choose WSDL");
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        if (!dlg.wasCancelled()) {
            Wsdl wsdl = dlg.getSelectedWsdl();
            if ( wsdl != null ) {
                if (wsdl.getServices().isEmpty()) {
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                            "The WSDL does not contain any services.", null);

                } else {
                    String wsdlLocation = dlg.getWsdlLocation();
                    WsdlComposer.WsdlHolder holder = new WsdlComposer.WsdlHolder(wsdl, wsdlLocation);
                    sourceWsdlListModel.addWsdl(holder);
                    wsdlComposer.addSourceWsdl(holder);
                    ensureSourceSelected();
                }
            }
        }
        enableAddSourceButton();
    }

    private void enableAddSourceButton() {
        if (sourceWsdlListModel.getSize() >= maxSources) {
            addWSDL.setEnabled(false);
        } else {
            addWSDL.setEnabled(true);
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
        if (currentHolder == null || currentHolder.wsdl == null)
            return;

        Collection<Binding> bindings = currentHolder.wsdl.getBindings();
        for (Binding binding : bindings) {
            if (wsdlComposer.isSupportedSoapBinding(binding)) {
                java.util.List ops = binding.getBindingOperations();
                for (Object op : ops) {
                    sourceOperationsListModel.addElement(op);
                }
            }
        }
    }

    private void prepareSourceWsdlTree() {
        final WsdlComposer.WsdlHolder s = getSelectedSourceWsdl();
        if (s == null || s.wsdl == null) {
            sourceWsdlTreeModel.setRoot(null);
            sourcePreviewTree.setRootVisible(false);
            return;
        }
        WsdlTreeNode rootSourceWsdlTreeNode = WsdlTreeNode.newInstance(s.wsdl);
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
            if (index >= 0 && index < wsdlList.size())
                return wsdlList.get(index);
            else if (!wsdlList.isEmpty())
                return wsdlList.get(0);

            return null;
        }

        public void update() {
            fireContentsChanged(sourceWsdlList, 0, wsdlList.size());
        }
        
        public void addWsdl(WsdlComposer.WsdlHolder wsdlHolder) {
            if (wsdlHolder != null) {
                if (!wsdlList.contains(wsdlHolder)) {
                    try {
                        wsdlList.add(wsdlHolder);
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

        @Override
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

        @Override
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
                Map<WsdlComposer.WsdlHolder, Set<BindingOperation>> boMap = wsdlComposer.getBindingOperatiosnMap();
                if (boMap != null) {
                    for (WsdlComposer.WsdlHolder wsdlHolder : boMap.keySet()) {
                        for (BindingOperation bindingOperation : boMap.get(wsdlHolder)) {
                            this.addElement(new BindingOperationHolder(bindingOperation, wsdlHolder));
                        }
                    }
                }
            }
        }

        private void addBindingOperations(Collection<BindingOperation> allOps) {
            WsdlComposer.WsdlHolder sourceWsdl = getSelectedSourceWsdl();
            if (sourceWsdl == null)
                return;
            for (BindingOperation bindingOperation: allOps) {
                if (wsdlComposer.addBindingOperation(bindingOperation, sourceWsdl)) {
                    addElement(new BindingOperationHolder(bindingOperation, sourceWsdl));
                }
            }
        }

        private void removeBindingOperations(List<BindingOperationHolder> operationHolders) {
            for (BindingOperationHolder operationHolder : operationHolders) {
                WsdlComposer.WsdlHolder wsdlHolder = operationHolder.sourceWsdlHolder;
                if (wsdlComposer.removeBindingOperation(operationHolder.bindingOperation, wsdlHolder)) {
                    this.removeElement(operationHolder);
                }
            }
        }

        private void revalidate() {
            Collection<BindingOperation> bops = wsdlComposer.getBindingOperations();
            Collection<BindingOperationHolder> toRemove = new ArrayList<BindingOperationHolder>();
            for (int i=0; i<getSize(); i++) {
                BindingOperationHolder boh = (BindingOperationHolder) getElementAt(i);
                if (!hasOperation(bops, boh.bindingOperation)) {
                    toRemove.add(boh);
                }
            }
            for (BindingOperationHolder boh : toRemove) {
                this.removeElement(boh);                
            }
        }

        private boolean hasOperation(Collection<BindingOperation> operations, BindingOperation operation) {
            boolean found = false;

            for (BindingOperation bindingOperation : operations) {
                if (operation.getName().equals(bindingOperation.getName())) {
                    found = true;
                }
            }

            return found;
        }
    }
}
