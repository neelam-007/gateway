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
 * User: dlee
 * Date: Jan 18, 2009
 */
public class EditableSearchComboBox extends JComboBox {
    private final FilterableComboBoxModel model;
    private final SearchFieldEditor editor;
    private final static int startIndex = -1;
    private int searchResultsIndexer = startIndex;

    /**
     * Default constructor which will provide no list of search items.
     */
    public EditableSearchComboBox() {
        model = new FilterableComboBoxModel();
        editor = new SearchFieldEditor();

        setModel(model);
        setEditor(editor);
        setEditable(true);
    }

    /**
     * Constructor which will construct the combo box with a list of searchable items.
     * @param searchableItems   The default list of searchable items
     */
    public EditableSearchComboBox(List searchableItems) {
        this();
        updateSearchableItems(searchableItems);
    }

    public void addTextFieldKeyListener(KeyListener listener){
        editor.addKeyListener(listener);
    }

    public Object getNextSearchResult(){
        if(model.getSize() < 1) return null;

        if(searchResultsIndexer < model.getSize() - 1) searchResultsIndexer++;
        else return null;//no more results

        return model.getElementAt(searchResultsIndexer);
    }

    public Object getPreviousAssertionOrdinal(){
        if(searchResultsIndexer == 0 || model.getSize() < 1) return null;

        if(searchResultsIndexer > 0) searchResultsIndexer--;
        else return null;//no previous results

        return model.getElementAt(searchResultsIndexer);
    }

    public boolean hasResults(){
        return model.getSize() > 0;
    }

    public void resetSearchResultIndex(){
        searchResultsIndexer = startIndex;
    }

    public void setSearchIndexAtEnd(){
        searchResultsIndexer = model.getSize();
    }

    /**
     * Updates the list of searchable items with this one. 
     * @param searchableItems   The list of searchable items to be used as part of the look up list.
     */
    public void updateSearchableItems(List searchableItems) {
        model.updateSearchableItems(searchableItems);
    }

    /**
     * Sets the comparator that will be used to sort the filtered items.
     * @param comparator    The comparator used for sorting
     */
    public void setComparator(Comparator comparator) {
        model.setComparator(comparator);
    }

    /**
     * Sets the filtering that will be used as part of the searching for this combo box.
     * @param filter    The implemented filtering
     */
    public void setFilter(Filter filter) {
        editor.setFilter(filter);
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
        private Filter filter;
        private Comparator comparator;

        public FilterableComboBoxModel() {
            items = new ArrayList();
            filteredItems = new ArrayList(items.size());
            updateFilteredItems();
        }

        /**
         * Constructor to initialize with a list of searchable items.
         * @param searchableItems   List of searchable items.
         */
        public FilterableComboBoxModel(List searchableItems) {
            items = new ArrayList(searchableItems);
            filteredItems = new ArrayList(searchableItems.size());
            updateFilteredItems();
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

        /**
         * Sets the filtering criteria
         * @param filter    Filtering object that will do the filtering of the searchable items
         */
        public void setFilter(Filter filter) {
            this.filter = filter;
            updateFilteredItems();
        }

        /**
         * Updates the searchable items into the filter items list.
         */
        private void updateFilteredItems() {
            //this fire will cause the action listener on the text field to fire
            fireIntervalRemoved(this, 0, filteredItems.size()); //notify clearing of the previous list
            filteredItems.clear();  //remove old filterItems items

            if (filter == null) {
                //if no filterItems is set, then we should provide the original searchable items
                filteredItems.addAll(items);
            } else {
                //else, there is a filterItems provided, we should filterItems accordingly to the filterItems policy
                for (Object obj : items) {
                    if (filter.accept(obj)) {
                        filteredItems.add(obj);
                    }
                }
            }

            //sort based on the comparator
            if (comparator != null) {
                Collections.sort(filteredItems, comparator);
            }

            fireIntervalAdded(this, 0, filteredItems.size());   //notify new list of filtered items
        }

        public void setComparator(Comparator comparator) {
            this.comparator = comparator;
        }

        public int getSearchableItemsCount(){
            return items.size();
        }
    }

    /**
     *  The text editor field which will be listened for search characters to filter the items.
     */
    public class SearchFieldEditor extends JTextField implements ComboBoxEditor, DocumentListener {
        private AssertionTreeNode selectedNode;
        private volatile boolean isFiltering = false;
        private volatile boolean isSetting = false;
        private Filter filter;

        public SearchFieldEditor() {
            getDocument().addDocumentListener(this);
            filter = null;
        }

        public SearchFieldEditor(int cols) {
            this();
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
            if (anObject == null) {
                setText("");
            } else {
                if(anObject instanceof AssertionTreeNode){
                    AssertionTreeNode node = (AssertionTreeNode) anObject;
                    //todo make this a function so caller supplied logic for what is shown
                    setText(node.getName());
                    selectedNode = node;
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
            filterItems();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filterItems();
        }

        public void setFilter(Filter filter) {
            this.filter = filter;
        }

        /**
         *  The core of the filtering to display the new items in the drop down list based on the filtering
         */
        private void filterItems() {
            if (isSetting) return;

            isFiltering = true;
            if (filter != null) {
                filter.setPrefix(getText());
            }
            ((FilterableComboBoxModel) getModel()).setFilter(filter);

            //refresh the drop down list
            setPopupVisible(false);

            //only show drop down list when there are filtered items available to be displayed
            boolean hasSearchableItems = ((FilterableComboBoxModel) getModel()).getSearchableItemsCount() > 0;
            if (hasSearchableItems) {
                if (getModel().getSize() > 0) {
                    setPopupVisible(true);
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
        protected String prefix;

        public Filter() {
            prefix = "";
        }

        public Filter(String prefix) {
            this.prefix = prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        /**
         * @param obj   Object to be filtered
         * @return  TRUE if the provide object is accepted as part of the filtering.  Otherwise, FALSE.
         */
        public abstract boolean accept(Object obj);
    }

    public static void main(String[] arg) {
        String[] list = {"test", "tetttt", " tesr3", "fdasfds", "tee2"};
        List ha = new ArrayList();
        ha.add("Denis");
        ha.add("yo");
        ha.add("Deeee");
        ha.add("denneejk");
        final EditableSearchComboBox combo = new EditableSearchComboBox(ha);

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
