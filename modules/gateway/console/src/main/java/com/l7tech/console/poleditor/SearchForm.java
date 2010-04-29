/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.poleditor;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.EditableSearchComboBox;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.CloseIcon;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SearchForm {
    private JButton previousButton;
    private JButton nextButton;
    private JPanel searchPanel;
    private JLabel xLabel;
    private JComboBox searchComboBox;
    private JCheckBox caseSensitiveCheckBox;
    private JCheckBox includePropertiesCheckBox;

    private final KeyAdapter escapeListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                hidePanel();
            }
        }
    };

    private EditableSearchComboBox.Filter filter;

    /**
     * Create a new SearchForm.
     *
     * Note: idea's createUIComponents() is inserted into the constructor before any code below runs 
     */
    public SearchForm() {
        //todo fix any component which can get focus needs to listen for escape...there must be an easier way to do this for a group of components
        searchPanel.addKeyListener(escapeListener);
        previousButton.addKeyListener(escapeListener);
        nextButton.addKeyListener(escapeListener);
        ((EditableSearchComboBox)searchComboBox).addTextFieldKeyListener(escapeListener);

        //create list renderer
        searchComboBox.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value,  int index, boolean isSelected, boolean cellHasFocus) {
                if(!(value instanceof AssertionTreeNode)) throw new IllegalStateException("Unexpected value found in combo box: " + value.getClass().getName());

                AssertionTreeNode node = (AssertionTreeNode) value;
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }

                // Based on value type, determine cell contents
                setIcon(new ImageIcon(node.getIcon()));
                setText(AssertionTreeNode.getVirtualOrdinalString(node) + " " + node.getName());

                return this;
            }
        });

        //create comparator to sort the filtered items
        ((EditableSearchComboBox)searchComboBox).setComparator(new Comparator<AssertionTreeNode>() {
            @Override
            public int compare(AssertionTreeNode o1, AssertionTreeNode o2) {
                return o1.compareTo(o2);
            }
        });

        searchComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("comboBoxEdited") && e.getModifiers() == 0) {
                    invokeSelection();
                }

                //mouse selection detection
                if (e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() > 0) {
                    invokeSelection();
                }
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TopComponents.getInstance().fireGlobalAction(MainWindow.L7_F3, nextButton);
            }
        });

        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TopComponents.getInstance().fireGlobalAction(MainWindow.L7_SHIFT_F3, previousButton);
            }
        });

        final ActionListener checkBoxListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //if case sensitive is toggled, then update the search results
                ((EditableSearchComboBox) searchComboBox).refresh();
            }
        };
        caseSensitiveCheckBox.addActionListener(checkBoxListener);
        caseSensitiveCheckBox.setMnemonic(KeyEvent.VK_C);

        includePropertiesCheckBox.setMnemonic(KeyEvent.VK_P);
        includePropertiesCheckBox.addActionListener(checkBoxListener);
    }

    public void addEnterListener(KeyListener listener){
        searchComboBox.getEditor().getEditorComponent().addKeyListener(listener);        
    }

    /**
     * Returns the next virtual ordinal from search results.
     * @return String, next ordinal. Will be null when no results or no more results
     */
    public String getNextAssertionOrdinal(){
        String returnString = null;
        Object result = ((EditableSearchComboBox)searchComboBox).getNextSearchResult();
        if(result instanceof AssertionTreeNode){
            AssertionTreeNode treeNode = (AssertionTreeNode) result;
            returnString =  AssertionTreeNode.getVirtualOrdinalString(treeNode);
        }

        return returnString;
    }

    /**
     * Returns the previous virtual ordinal from search results.
     * @return String, next ordinal. Will be null when no results or no more results
     */
    public String getPreviousAssertionOrdinal(){
        String returnString = null;
        Object result = ((EditableSearchComboBox)searchComboBox).getPreviousAssertionOrdinal();
        if(result instanceof AssertionTreeNode){
            AssertionTreeNode treeNode = (AssertionTreeNode) result;
            returnString =  AssertionTreeNode.getVirtualOrdinalString(treeNode);
        }

        return returnString;
    }

    public void resetSearchIndex(){
        ((EditableSearchComboBox)searchComboBox).resetSearchResultIndex();
    }

    public boolean hasSearchResults(){
        //todo only return true when there is a filter present
        return ((EditableSearchComboBox)searchComboBox).hasResults();
    }

    public void setPolicyTree(PolicyTree policyTree){
        if(!searchPanel.isVisible()) return;

        final Object root = policyTree.getModel().getRoot();
        AbstractTreeNode rootNode = (AbstractTreeNode) root;

        final List<AbstractTreeNode> allAssertions = collectNodes(rootNode);

        setSearchableTreeNodes(allAssertions);
    }

    public void setSearchableTreeNodes(List<AbstractTreeNode> allAssertions){
        ((EditableSearchComboBox) searchComboBox).updateSearchableItems(allAssertions);
    }

    /**
     * @param mouseCloseListener MouseListener allows clients to know when the user has clicked on the 'x' area, to indicate
     * that the search form should no longer be shown
     */
    public void addCloseLabelListener(MouseListener mouseCloseListener){
        xLabel.addMouseListener(mouseCloseListener);
    }

    /**
     * Called from Idea forms
     */
    private void createUIComponents() {
        nextButton = new JButton(new ArrowIcon(ArrowIcon.DOWN));
        previousButton = new JButton(new ArrowIcon(ArrowIcon.UP));

        xLabel = new JLabel(new CloseIcon(18));
        xLabel.setText(null);

        filter = new EditableSearchComboBox.Filter() {
                @Override
                public boolean accept(Object obj) {
                    if (obj == null) return false;//this can't happen but leaving in for future changes

                    //match display names
                    if (!(obj instanceof AssertionTreeNode)) return false;

                    AssertionTreeNode node = (AssertionTreeNode) obj;

                    final boolean isCaseSensitive = caseSensitiveCheckBox.isSelected();
                    final boolean includeProperties = includePropertiesCheckBox.isSelected();

                    final String searchString;
                    if (includeProperties) {
                        String propsString = node.getAssertionPropsAsString();
                        if(propsString != null){
                            StringBuilder builder = new StringBuilder(node.getName()).append(propsString);
                            searchString = builder.toString();
                        } else{
                            searchString = node.getName();
                        }
                    } else {
                        searchString = node.getName();
                    }

                    if(!isCaseSensitive){
                        final String nodeNameLowerCase = searchString.toLowerCase();
                        return nodeNameLowerCase.indexOf(getFilterText().toLowerCase()) != -1;
                    }

                    return searchString.indexOf(getFilterText()) != -1;
                }
            };

        searchComboBox = new EditableSearchComboBox(filter);
    }

    public JPanel getSearchPanel() {
        return searchPanel;
    }

    /**
     * Clients control when this panel is displayed. If the panel is hidden via escape / close, then the
     * search field should be cleared and the panel set to invisible
     */
    public void hidePanel(){
        ((EditableSearchComboBox)searchComboBox).clearSearch();//casting as the ui editor requires the raw type
        searchPanel.setVisible(false);
    }

    public void showPanel(final PolicyTree policyTree){
        if(searchPanel.isVisible()) {
            //If the panel is snown, then simply place focus back in the search field. Don't want existing text to be lost
            //case sensitive may have been changed, so we need to cause the set of filtered items to be updated
            searchComboBox.requestFocusInWindow();
            ((EditableSearchComboBox) searchComboBox).refresh();
            return;
        }

        searchPanel.setVisible(true);
        searchComboBox.requestFocusInWindow();
        setPolicyTree(policyTree);
    }

    /**
     * Bring the user to the selected assertion
     */
    private void invokeSelection() {
        final EditableSearchComboBox.SearchFieldEditor fieldEditor = (EditableSearchComboBox.SearchFieldEditor) searchComboBox.getEditor();
        final AssertionTreeNode selectedNode = fieldEditor.getSelectedNode();

        if(selectedNode == null) return;
        //user selected a specific result
        final String vOrdinal = AssertionTreeNode.getVirtualOrdinalString(selectedNode);
        PolicyTree policyTree = (PolicyTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
        policyTree.goToAssertionTreeNode(vOrdinal);

    }

    private List<AbstractTreeNode> collectNodes(AbstractTreeNode node){
        List<AbstractTreeNode> nodes = new ArrayList<AbstractTreeNode>();
        //dont add the current node here, don't want the root node added
        for(int i = 0; i < node.getChildCount(); i++){
            nodes.add((AbstractTreeNode) node.getChildAt(i));//add the node here
            nodes.addAll(collectNodes((AbstractTreeNode) node.getChildAt(i)));
        }

        return nodes;
    }
}
