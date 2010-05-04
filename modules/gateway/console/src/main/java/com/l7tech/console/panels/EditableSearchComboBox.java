package com.l7tech.console.panels;

import com.l7tech.console.tree.policy.AssertionTreeNode;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.awt.*;

/**
 * Provide own implementation of a editable searching combo box.
 * It would basically dynamically update the list of options in the combo box based on the characters being typed
 * in the search textfield area.
 *
 * Implementation tried to be generic as much as possible so that it can be used for other purposes, but may be constrained
 * to AbstractTreeNodes because of the first implementation attempt.
 *
 * @author dlee
 * @author darmstrong
 */
public class EditableSearchComboBox extends JComboBox {
    private final FilterableComboBoxModel model;
    private final SearchFieldEditor editor;
    private final static int startIndex = -1;
    private int searchResultsIndexer = startIndex;

    /**
     * Default constructor which will provide no list of search items.
     * @param filter Filter contains the filtering logic 
     */
    public EditableSearchComboBox(final Filter filter) {
        model = new FilterableComboBoxModel(filter);
        editor = new SearchFieldEditor();

        setModel(model);
        setEditor(editor);
        setEditable(true);
    }

    /**
     * Constructor which will construct the combo box with a list of searchable items.
     * @param searchableItems   The default list of searchable items
     * @param filter Filter contains the filtering logic
     */
    public EditableSearchComboBox(final List searchableItems, final Filter filter) {
        this(filter);
        updateSearchableItems(searchableItems);
    }

    public void addTextFieldKeyListener(KeyListener listener){
        editor.addKeyListener(listener);
    }

    /**
     * Both okToLoopForward and okToLoopBack allow for the behaviour where the first time results are exhausted
     * in a direction, the user can be shown a message (via returning null), but the subsequent time an attempt is made
     * to move in that direction, the index will loop back to the correct index (either the start or the end)
     */
    private boolean okToLoopForward = false;
    private boolean okToLoopBack = false;

    public Object getNextSearchResult(){
        if(model.getSize() < 1) return null;

        okToLoopBack = false;
        
        if (searchResultsIndexer == model.getSize() - 1 && okToLoopForward){
            searchResultsIndexer = startIndex;//reset
            okToLoopForward = false;
        }

        if (searchResultsIndexer < model.getSize() - 1) searchResultsIndexer++;
        else {
            okToLoopForward = true;
            return null;//no more results
        }

        return model.getElementAt(searchResultsIndexer);
    }

    public Object getPreviousAssertionOrdinal(){
        if(model.getSize() < 1) return null;

        okToLoopForward = false;

        if(searchResultsIndexer <= 0 && okToLoopBack){
            searchResultsIndexer = model.getSize();//reset
            okToLoopBack = false;
        }

        if(searchResultsIndexer > 0) searchResultsIndexer--;
        else {
            okToLoopBack = true;
            return null;//no previous results
        }

        return model.getElementAt(searchResultsIndexer);
    }

    public boolean hasResults(){
        return model.getSize() > 0;
    }

    public void resetSearchResultIndex(){
        searchResultsIndexer = startIndex;
    }

    /**
     * Updates the list of searchable items with this one. 
     * @param searchableItems   The list of searchable items to be used as part of the look up list.
     */
    public void updateSearchableItems(List searchableItems) {
        model.updateSearchableItems(searchableItems);
    }

    public void refresh(){
        editor.filterItems(false);
        model.refresh();
    }

    /**
     * Sets the comparator that will be used to sort the filtered items.
     * @param comparator    The comparator used for sorting
     */
    public void setComparator(Comparator comparator) {
        model.setComparator(comparator);
    }

    /**
     * Clear search text and background colour accordingly.
     */
    public void clearSearch() {
        setSelectedItem(null);
        editor.clearSearch();
        model.updateFilteredItems();
    }

    /**
     * The model implementation that will model the editable search combo box.
     */
    public class FilterableComboBoxModel extends AbstractListModel implements MutableComboBoxModel {
        private List items;
        private List filteredItems;
        private Object selectedItem;
        private final Filter filter;
        private Comparator comparator;

        public FilterableComboBoxModel(final Filter filter) {
            items = new ArrayList();
            filteredItems = new ArrayList();
            this.filter = filter;
        }

        @Override
        public void addElement(Object obj) {
            if(obj == null) return;
            
            items.add(obj);
            updateFilteredItems();
        }

        @Override
        public void removeElement(Object obj) {
            items.remove(obj);
            updateFilteredItems();
        }

        @Override
        public void removeElementAt(int obj) {
            items.remove(obj);
            updateFilteredItems();
        }

        @Override
        public void insertElementAt(Object obj, int index) {
            //TODO: Provide implementation body
        }

        @Override
        public int getSize() {
            //returned the size of the filtered items because we want this to be actual display list size
            return filteredItems.size();
        }

        @Override
        public Object getElementAt(int index) {
            return filteredItems.get(index);
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (selectedItem != null && anItem != null){
                if(selectedItem.equals(anItem)) return;
            }
            
            if (anItem != null) {
                //only accept selection based on the recognized type from our searchable items
                boolean recognizedType = false;
                for (Object item : items) {
                    if (item.getClass().equals(anItem.getClass())) {
                        recognizedType = true;
                        break;
                    }
                }
                if (!recognizedType) return;
            }

            selectedItem = anItem;
            fireContentsChanged(this, -1, -1);  //notify of new selected item
        }

        /**
         * Update the list of searchable items
         * @param searchableItems   The new updated searchable item list
         */
        public void updateSearchableItems(List searchableItems) {
            items.clear();
            items.addAll(searchableItems);
            updateFilteredItems();
        }

        public void setSearchText(final String text){
            filter.setFilterText(text);
            updateFilteredItems();
        }

        public void refresh(){
            updateFilteredItems();
        }
        
        /**
         * Updates the searchable items into the filter items list.
         */
        private void updateFilteredItems() {
            //this fire will cause the action listener on the text field to fire
            fireIntervalRemoved(this, 0, filteredItems.size()); //notify clearing of the previous list
            filteredItems.clear();  //remove old filterItems items

            //if there is no filter, then there are no search results
            if (!"".equals(filter.getFilterText())) {
                for (Object obj : items) {
                    if (filter.accept(obj)) {
                        filteredItems.add(obj);
                    }
                }
                //sort based on the comparator
                if (comparator != null) {
                    Collections.sort(filteredItems, comparator);
                }

                fireIntervalAdded(this, 0, filteredItems.size());   //notify new list of filtered items
            }
        }

        public void setComparator(Comparator comparator) {
            this.comparator = comparator;
        }

        public int getSearchableItemsCount() {
            return filteredItems.size();
        }
    }

    /**
     *  The text editor field which will be listened for search characters to filter the items.
     */
    public class SearchFieldEditor extends JTextField implements ComboBoxEditor, DocumentListener {
        private AssertionTreeNode selectedNode;
        private volatile boolean isFiltering = false;
        private volatile boolean isSetting = false;


        public SearchFieldEditor() {

            getDocument().addDocumentListener(this);
            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    //don't filter with no filter text entered
                    if (!"".equals(getText())){
                        filterItems(true);
                        SearchFieldEditor.this.setSelectionStart(0);
                        SearchFieldEditor.this.setSelectionEnd(SearchFieldEditor.this.getText().length());
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    //nothing to do
                }
            });
        }

        public SearchFieldEditor(int cols) {
            setColumns(cols);
        }

        @Override
        public Component getEditorComponent() {
            return this;
        }

        @Override
        public void setItem(Object anObject) {
            if(isFiltering) return;

            isSetting = true;

            //anObject comes directly from 'getSelectedItem' on the combo box. anObject is null, when nothing
            //is selected. Therefore we do not want to set "" as the text when anObject is null. We simply will do
            //nothing, which will leave any text previously entered in the text field.
            if (anObject != null) {
                if(anObject instanceof AssertionTreeNode){
                    //do not set the text of the text field. This overwrites what the user has typed
                    //and causes various bugs where going back into the text field populates it with a previous selection
                    selectedNode = (AssertionTreeNode) anObject;
                } else{
                    setText(anObject.toString());
                }
            }
            isSetting = false;
        }

        public AssertionTreeNode getSelectedNode() {
            return selectedNode;
        }

        @Override
        public Object getItem() {
            return getText();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {}

        @Override
        public void insertUpdate(DocumentEvent e) {
            filterItems(true);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filterItems(true);
        }

        /**
         *  The core of the filtering to display the new items in the drop down list based on the filtering
         * @param showPopUp boolean if there are search results and true, then the pop up menu will be shown
         */
        protected void filterItems(final boolean showPopUp) {
            if (isSetting) return;

            isFiltering = true;
            //reset the selected node, as if we are filtering it no longer applies
            selectedNode = null;
            
            ((FilterableComboBoxModel) getModel()).setSearchText(getText());

            //refresh the drop down list
            setPopupVisible(false);

            //only show drop down list when there are filtered items available to be displayed
            boolean hasSearchableItems = ((FilterableComboBoxModel) getModel()).getSearchableItemsCount() > 0;
            if (hasSearchableItems) {
                if (getModel().getSize() > 0) {
                    setPopupVisible(showPopUp);
                    setBackground(Color.white);
                } else {
                    if (!"".equals(getText())) {
                        setBackground(new Color(0xFF, 0xFF, 0xe1));
                    }
                }
            } else {
                if (!"".equals(getText())) {
                    setBackground(new Color(0xFF, 0xFF, 0xe1));
                } else {
                    setBackground(Color.white);
                }
            }
            isFiltering = false;
        }

        /**
         * Clears the text field and sets background back to white.
         */
        public void clearSearch() {
            setText("");
            setBackground(Color.white);
        }
    }

    /**
     * Abstract filtering class to filter out the searchable items.
     */
    public static abstract class Filter {
        private String filterText;

        public Filter() {
            filterText = "";
        }

        public Filter(String filterText) {
            this.filterText = filterText;
        }

        /**
         * @param obj   Object to be filtered
         * @return  TRUE if the provide object is accepted as part of the filtering.  Otherwise, FALSE.
         */
        public abstract boolean accept(Object obj);

        public String getFilterText() {
            return filterText;
        }

        public void setFilterText(String filterText) {
            if (filterText == null){
                this.filterText = "";
            } else{
                this.filterText = filterText;    
            }

        }
    }

    public static void main(String[] arg) {
        String[] list = {"test", "tetttt", " tesr3", "fdasfds", "tee2"};
        List ha = new ArrayList();
        ha.add("Denis");
        ha.add("yo");
        ha.add("Deeee");
        ha.add("denneejk");
        final EditableSearchComboBox combo = new EditableSearchComboBox(ha, new Filter() {
            @Override
            public boolean accept(Object obj) {
                return true;
            }
        });

        combo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println(e.getActionCommand() + " " + e.getModifiers());
                if (e.getActionCommand().equals("comboBoxEdited") && e.getModifiers() == 0) {
                    System.out.println("Key board selected -->" + combo.getSelectedItem() + " " + combo.getSelectedIndex());
                }

                if (e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() > 0) {
                    System.out.println("mouse selected -->" + combo.getSelectedItem());
                }

            }

        });

        JFrame f = new JFrame();
        JPanel p = new JPanel(new BorderLayout());
        p.add(combo, BorderLayout.CENTER);
        f.add(p);
        f.pack();
        f.setVisible(true);
    }
}
