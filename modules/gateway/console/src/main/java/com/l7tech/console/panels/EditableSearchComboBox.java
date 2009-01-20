package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
    private FilterableComboBoxModel model;
    private SearchFieldEditor editor;

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

    /**
     * Updates the list of searchable items with this one. 
     * @param searchableItems   The list of searchable items to be used as part of the look up list.
     */
    public void updateSearchableItems(List searchableItems) {
        if (model != null) {
            model.updateSearchableItems(searchableItems);
        } 
    }

    /**
     * Sets the comparator that will be used to sort the filtered items.
     * @param comparator    The comparator used for sorting
     */
    public void setComparator(Comparator comparator) {
        if (model != null) {
            model.setComparator(comparator);
        }
    }

    /**
     * Sets the filtering that will be used as part of the searching for this combo box.
     * @param filter    The implemented filtering
     */
    public void setFilter(Filter filter) {
        if (editor != null) {
            editor.setFilter(filter);
        }
    }

    /**
     * Clear search text and background colour accordingly.
     */
    public void clearSearch() {
        if (editor != null) {
            editor.clearSearch();
        }
    }

    /**
     * The model implementation that will model the editable search combo box.
     */
    private class FilterableComboBoxModel extends AbstractListModel implements MutableComboBoxModel {
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

        public void addElement(Object obj) {
            items.add(obj);
            updateFilteredItems();
        }

        public void removeElement(Object obj) {
            items.remove(obj);
            updateFilteredItems();
        }

        public void removeElementAt(int obj) {
            items.remove(obj);
            updateFilteredItems();
        }

        public void insertElementAt(Object obj, int index) {
            //TODO: Provide implementation body
        }

        public int getSize() {
            //returned the size of the filtered items because we want this to be actual display list size
            return filteredItems.size();
        }

        public Object getElementAt(int index) {
            return filteredItems.get(index);
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public void setSelectedItem(Object anItem) {
            if ((selectedItem == null) && (anItem == null)) return;
            if ((selectedItem != null) && selectedItem.equals(anItem)) return;
            if ((anItem != null) && anItem.equals(selectedItem)) return;
            if (anItem == null) return;

            //only accept selection based on the recognized type from our searchable items
            boolean recognizedType = false;
            for (Object item : items) {
                if (item.getClass().equals(anItem.getClass())) {
                    recognizedType = true;
                }
            }
            if (!recognizedType) return;


            selectedItem = anItem;
            fireContentsChanged(this, -1, -1);  //notify of new selected item
        }

        /**
         * Update the list of searchable items
         * @param searchableItems   The new updated searchable item list
         */
        public void updateSearchableItems(List searchableItems) {
            items = searchableItems;
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
    private class SearchFieldEditor implements ComboBoxEditor, DocumentListener {
        public JTextField textField;
        private volatile boolean isFiltering = false;
        private volatile boolean isSetting = false;
        private Filter filter;


        public SearchFieldEditor() {
            textField = new JTextField();
            textField.getDocument().addDocumentListener(this);
            filter = null;
        }

        public SearchFieldEditor(int cols) {
            this();
            textField.setColumns(cols);
        }

        public Component getEditorComponent() {
            return textField;
        }

        public void setItem(Object anObject) {
            if(isFiltering) return;

            isSetting = true;
            if (anObject == null) {
                textField.setText("");
            } else {
                textField.setText(anObject.toString());
            }
            isSetting = false;
        }


        public Object getItem() {
            return textField.getText();
        }

        public void selectAll() {
            textField.selectAll();
        }

        public void addActionListener(ActionListener l) {
            textField.addActionListener(l);
        }

        public void removeActionListener(ActionListener l) {
            textField.removeActionListener(l);
        }

        public void changedUpdate(DocumentEvent e) {}

        public void insertUpdate(DocumentEvent e) {
            filterItems();
        }

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
                filter.setPrefix(textField.getText());
            }
            ((FilterableComboBoxModel) getModel()).setFilter(filter);

            //refresh the drop down list
            setPopupVisible(false);

            //only show drop down list when there are filtered items available to be displayed
            boolean hasSearchableItems = ((FilterableComboBoxModel) getModel()).getSearchableItemsCount() > 0;
            if (hasSearchableItems) {
                if (getModel().getSize() > 0) {
                    setPopupVisible(true);
                    textField.setBackground(Color.white);
                } else {
                    if (!"".equals(textField.getText())) {
                        textField.setBackground(new Color(0xFF, 0xFF, 0xe1));
                    }
                }
            } else {
                if (!"".equals(textField.getText())) {
                    textField.setBackground(new Color(0xFF, 0xFF, 0xe1));
                } else {
                    textField.setBackground(Color.white);
                }
            }
            isFiltering = false;
        }

        /**
         * Clears the text field and sets background back to white.
         */
        public void clearSearch() {
            textField.setText("");
            textField.setBackground(Color.white);;
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
