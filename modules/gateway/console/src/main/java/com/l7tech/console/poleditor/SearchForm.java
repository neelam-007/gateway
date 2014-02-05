/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.poleditor;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.EditableSearchComboBox;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.CloseIcon;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class SearchForm {
    @Deprecated // used in pre-Icefish
    private static final String PREF_PREFIX = "policy.editor.search";
    @Deprecated // used in pre-Icefish
    public static final String SHOW = PREF_PREFIX + ".show";
    @Deprecated // used in pre-Icefish
    private static final String CHECK_SHOW_DISABLED = PREF_PREFIX + ".checkShowDisabled";
    @Deprecated // used in pre-Icefish
    private static final String CHECK_CASE_SENSITIVE = PREF_PREFIX + ".checkCaseSensitive";
    @Deprecated // used in pre-Icefish
    private static final String CHECK_INCULDE_PROPERTIES = PREF_PREFIX + ".checkIncludeProperties";

    private JButton previousButton;
    private JButton nextButton;
    private JPanel searchPanel;
    private JLabel xLabel;
    private EditableSearchComboBox<AssertionTreeNode> searchComboBox;
    private JCheckBox caseSensitiveCheckBox;
    private JCheckBox includePropertiesCheckBox;
    private JCheckBox showDisabledCheckBox;
    private PolicyEditorPanel policyEditorPanel;

    /**
     * Create a new SearchForm.
     *
     * Note: idea's createUIComponents() is inserted into the constructor before any code below runs 
     */
    public SearchForm(final PolicyEditorPanel policyEditorPanel) {
        this.policyEditorPanel = policyEditorPanel;

        //todo fix any component which can get focus needs to listen for escape...there must be an easier way to do this for a group of components
        KeyAdapter escapeListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePanel();
                }
            }
        };
        addKeyListeners(escapeListener);

        final Functions.Unary<String, AssertionTreeNode> accessorFunction = new Functions.Unary<String, AssertionTreeNode>() {
            @Override
            public String call(AssertionTreeNode assertionTreeNode) {
                String assertionOrdinal = AssertionTreeNode.getVirtualOrdinalString(assertionTreeNode);
                String assertionName = assertionTreeNode.getName();

                if (assertionName != null && assertionName.startsWith("<html>")) {
                    return assertionName.replace("<html>", "<html>" + assertionOrdinal + " "); // "<html>" should be put in the beginning of the returned string. 
                } else {
                    return assertionOrdinal + " " + assertionName;
                }
            }
        };

        final Functions.Unary<Icon, AssertionTreeNode> iconAccessorFunction = new Functions.Unary<Icon, AssertionTreeNode>() {
            @Override
            public Icon call(AssertionTreeNode assertionTreeNode) {
                if (assertionTreeNode.asAssertion().isEnabled()) {
                    return new ImageIcon(assertionTreeNode.getIcon());
                } else {
                    Image crossImage = ImageCache.getInstance().getIcon("com/l7tech/console/resources/RedCrossSign16.gif");
                    if (crossImage != null) {
                        final Image ret = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
                        final Graphics g = ret.getGraphics();
                        g.drawImage(assertionTreeNode.getIcon() , 0, 0, null );
                        g.drawImage(crossImage, 0, 0, null );
                        return new ImageIcon(ret);
                    } else {
                        return new ImageIcon(assertionTreeNode.getIcon());
                    }
                }
            }
        };

        //Create a renderer and configure it to clip. Text which is too large will automatically get '...' added to it
        //and the jlabel will not grow to accommodate it, if it is larger thatn the size of the combo box component
        TextListCellRenderer<AssertionTreeNode> comboBoxRenderer =
                new TextListCellRenderer<AssertionTreeNode>(accessorFunction, null, iconAccessorFunction, false);
        comboBoxRenderer.setRenderClipped(true);

        searchComboBox.setRenderer(comboBoxRenderer);

        //create comparator to sort the filtered items
        searchComboBox.setComparator(new Comparator<AssertionTreeNode>() {
            @Override
            public int compare(AssertionTreeNode o1, AssertionTreeNode o2) {
                return o1.compareTo(o2);
            }
        });

        searchComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                invokeSelection();
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
                searchComboBox.refresh();
                if (e.getSource() == caseSensitiveCheckBox) {
                    policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_CASE_SENSITIVE, String.valueOf(caseSensitiveCheckBox.isSelected()));
                } else if (e.getSource() == includePropertiesCheckBox) {
                    policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_INCLUDE_PROPERTIES, String.valueOf(includePropertiesCheckBox.isSelected()));
                } else if (e.getSource() == showDisabledCheckBox) {
                    policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_SHOW_DISABLED, String.valueOf(showDisabledCheckBox.isSelected()));
                }
            }
        };
        caseSensitiveCheckBox.addActionListener(checkBoxListener);
        caseSensitiveCheckBox.setMnemonic(KeyEvent.VK_C);
        if (Boolean.parseBoolean(policyEditorPanel.getTabSettingFromPolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_CASE_SENSITIVE, CHECK_CASE_SENSITIVE, "false"))) {
            caseSensitiveCheckBox.setSelected(true);
        }

        includePropertiesCheckBox.setMnemonic(KeyEvent.VK_P);
        includePropertiesCheckBox.addActionListener(checkBoxListener);
        if (Boolean.parseBoolean(policyEditorPanel.getTabSettingFromPolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_INCLUDE_PROPERTIES, CHECK_INCULDE_PROPERTIES, "true"))) {
            includePropertiesCheckBox.setSelected(true);
        }

        showDisabledCheckBox.addActionListener(checkBoxListener);
        if (Boolean.parseBoolean(policyEditorPanel.getTabSettingFromPolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_SHOW_DISABLED, CHECK_SHOW_DISABLED, "true"))) {
            showDisabledCheckBox.setSelected(true);
        }
    }

    /**
     * Add a keylistener to each component on the search bar which can have focus.
     * @param keyListener KeyListener
     */
    public void addKeyListeners(KeyListener keyListener) {
        searchPanel.addKeyListener(keyListener);
        previousButton.addKeyListener(keyListener);
        nextButton.addKeyListener(keyListener);
        searchComboBox.addTextFieldKeyListener(keyListener);
    }

    /**
     * Returns the next virtual ordinal from search results.
     * @return String, next ordinal. Will be null when no results or no more results
     */
    public String getNextAssertionOrdinal(){
        String returnString = null;
        AssertionTreeNode treeNode = searchComboBox.getNextSearchResult();
        if(treeNode != null){
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
        Object result = searchComboBox.getPreviousSearchResult();
        if(result instanceof AssertionTreeNode){
            AssertionTreeNode treeNode = (AssertionTreeNode) result;
            returnString =  AssertionTreeNode.getVirtualOrdinalString(treeNode);
        }

        return returnString;
    }

    public void resetSearchIndex(){
        searchComboBox.resetSearchResultIndex();
    }

    public boolean hasSearchResults(){
        return searchComboBox.hasResults();
    }

    public void setPolicyTree(PolicyTree policyTree){
        if(!searchPanel.isVisible()) return;

        final Object root = policyTree.getModel().getRoot();
        AssertionTreeNode rootNode = (AssertionTreeNode) root;

        final List<AssertionTreeNode> allAssertions = collectNodes(rootNode);

        setSearchableTreeNodes(allAssertions);
    }

    public void setSearchableTreeNodes(List<AssertionTreeNode> allAssertions){
        searchComboBox.updateSearchableItems(allAssertions);
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

        EditableSearchComboBox.Filter filter = new EditableSearchComboBox.Filter() {
            @Override
            public boolean accept(Object obj) {
                if (obj == null) return false;//this can't happen but leaving in for future changes

                //match display names
                if (!(obj instanceof AssertionTreeNode)) return false;

                AssertionTreeNode node = (AssertionTreeNode) obj;

                final boolean origDecorateCommentStatus = AssertionTreeNode.isDecorateComment();
                // Set the decorateComment as false, so the comment searching will ignore searching any characters contained in the HTML tags.
                AssertionTreeNode.setDecorateComment(false);

                final boolean isCaseSensitive = caseSensitiveCheckBox.isSelected();
                final boolean includeProperties = includePropertiesCheckBox.isSelected();
                final boolean showDisabled = showDisabledCheckBox.isSelected();

                //Do not include in the result list if showDisable is not checked
                if (!showDisabled) {
                    if (!node.asAssertion().isEnabled()) {
                        return false;
                    }
                }

                final String searchString;
                if (includeProperties) {
                    String propsString = node.getAssertionPropsAsString();
                    if (propsString != null) {
                        StringBuilder builder = new StringBuilder(node.getName()).append(propsString);
                        searchString = builder.toString();
                    } else {
                        searchString = node.getName();
                    }
                } else {
                    searchString = node.getName();
                }

                // Set the flag "decorateComment" back to the original status
                AssertionTreeNode.setDecorateComment(origDecorateCommentStatus);

                if (!isCaseSensitive) {
                    final String nodeNameLowerCase = searchString.toLowerCase();
                    return nodeNameLowerCase.indexOf(getFilterText().toLowerCase()) != -1;
                }

                return searchString.indexOf(getFilterText()) != -1;
            }
        };

        searchComboBox = new EditableSearchComboBox<AssertionTreeNode>(filter){};
    }

    public JPanel getSearchPanel() {
        return searchPanel;
    }

    /**
     * Clients control when this panel is displayed. If the panel is hidden via escape / close, then the
     * search field should be cleared and the panel set to invisible
     */
    public void hidePanel(){
        searchComboBox.clearSearch();
        searchPanel.setVisible(false);
        policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_SHOW, "false");
        //put focus into the policy tree
        TopComponents.getInstance().getPolicyTree().requestFocusInWindow();
    }

    public void showPanel(final PolicyTree policyTree){
        policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_SHOW, "true");

        if(searchPanel.isVisible()) {
            //If the panel is snown, then simply place focus back in the search field. Don't want existing text to be lost
            //case sensitive may have been changed, so we need to cause the set of filtered items to be updated
            searchComboBox.requestFocusInWindow();
            searchComboBox.refresh();
            return;
        }

        searchPanel.setVisible(true);
        searchComboBox.requestFocusInWindow();
        setPolicyTree(policyTree);
    }

    /**
     * Copy policy tab settings for the current policy based on the status of UI components such as searchPanel,
     * caseSensitiveCheckBox, includePropertiesCheckBox, and showDisabledCheckBox
     */
    public void copyAllSearchPropertiesBasedOnUI() {
        policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_SHOW, String.valueOf(searchPanel.isVisible()));
        policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_CASE_SENSITIVE, String.valueOf(caseSensitiveCheckBox.isSelected()));
        policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_INCLUDE_PROPERTIES, String.valueOf(includePropertiesCheckBox.isSelected()));
        policyEditorPanel.updatePolicyTabProperty(PolicyEditorPanel.POLICY_TAB_PROPERTY_SEARCH_SHOW_DISABLED, String.valueOf(showDisabledCheckBox.isSelected()));
    }

    public boolean isSearchVisible(){
        return searchPanel.isVisible();
    }

    /**
     * Bring the user to the selected assertion
     */
    private void invokeSelection() {
        final AssertionTreeNode selectedNode = searchComboBox.getSelectedObject();

        if(selectedNode == null){
            //user pressed 'enter' in the search box
            resetSearchIndex();
            TopComponents.getInstance().fireGlobalAction(MainWindow.L7_F3, this.searchPanel);
            return;
        }

        //user selected a specific result
        final String vOrdinal = AssertionTreeNode.getVirtualOrdinalString(selectedNode);
        PolicyTree policyTree = (PolicyTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
        policyTree.goToAssertionTreeNode(vOrdinal);

    }

    private List<AssertionTreeNode> collectNodes(AssertionTreeNode node){
        List<AssertionTreeNode> nodes = new ArrayList<AssertionTreeNode>();
        //dont add the current node here, don't want the root node added
        for(int i = 0; i < node.getChildCount(); i++){
            nodes.add((AssertionTreeNode) node.getChildAt(i));//add the node here
            nodes.addAll(collectNodes((AssertionTreeNode) node.getChildAt(i)));
        }

        return nodes;
    }
}