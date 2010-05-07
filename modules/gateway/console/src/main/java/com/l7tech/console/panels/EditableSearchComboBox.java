package com.l7tech.console.panels;

import com.l7tech.console.tree.policy.AssertionTreeNode;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.awt.*;

/**
 * Editable search combo box. As the user types a dynamic pop up shows all matching items.
 *
 * actionPerformed has been enhanced to only fire ActionEvents when it is known that a user has made a valid selection.
 * This means some ActionEvents are ignored. This is due to the JComboBox being a compound component and can cause multiple
 * events to be fired for a single user action. However in some circumstances an event may be fired more than once.
 *
 * @author dlee
 * @author darmstrong
 */
public class EditableSearchComboBox<T> extends JComboBox {
    private final FilterableComboBoxModel model;
    private final SearchFieldEditor<T> editor;
    private final static int startIndex = -1;
    private int searchResultsIndexer = startIndex;
    private long lastEvent = System.currentTimeMillis();
    private List<ActionListener> actionListeners = new ArrayList<ActionListener>();

    /**
     * popUpCancelled informs us to ignore the ActionEvent caused by the pop up menu being cancelled. This happens
     * when the User chooses nothing by clicking out side of the menu pop up. In this case take no action
     * note: the selectedItem will be what ever the last element the mouse went over was
     */
    private boolean popUpCancelled = false;

    /**
     * ignoreWhen is used to know when to ignore the built in delay of 1 second between events. This is set when the
     * user types so that we can support a fast typer typing a search term and pressing enter.
     */
    private boolean ignoreWhen = false;

    /**
     * Default constructor which will provide no list of search items.
     * @param filter Filter contains the filtering logic 
     */
    public EditableSearchComboBox(final Filter filter) {
        model = new FilterableComboBoxModel(filter);
        editor = new SearchFieldEditor<T>();

        setModel(model);
        setEditor(editor);
        setEditable(true);

        super.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() > 0) ||
                        (e.getActionCommand().equals("comboBoxEdited") && e.getModifiers() == 0)) {

                    final boolean actionFromFocusEvent = e.getActionCommand().equals("comboBoxChanged") &&
                            EditableSearchComboBox.this.getSelectedItem() == null;

                    //A JComboBox is a compound component and a single user action may cause several actionPerformed events
                    //as a result we will ignore any events which occur with 1 second of the previous event
                    final long eventTime = e.getWhen();

                    final long difference = eventTime - lastEvent;
                    if (!actionFromFocusEvent &&
                            (difference > 1000 || ignoreWhen) &&
                            !"".equals(filter.getFilterText()) &&
                            !popUpCancelled) {
                        for (ActionListener actList : actionListeners) {
                            actList.actionPerformed(e);
                        }
                    }

                    lastEvent = eventTime;
                    popUpCancelled = false;
                    ignoreWhen = false;
                }
            }
        });

        this.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                popUpCancelled = true;
            }
        });
    }

    /**
     * Get the actual object which the user selected via it's textual representation in the combo box
     * @return
     */
    public T getSelectedObject() {
        return editor.selectedObject;
    }

    /**
     * Constructor which will construct the combo box with a list of searchable items.
     * @param searchableItems   The default list of searchable items
     * @param filter Filter contains the filtering logic
     */
    public EditableSearchComboBox(final List<T> searchableItems, final Filter filter) {
        this(filter);
        updateSearchableItems(searchableItems);
    }

    public void addTextFieldKeyListener(KeyListener listener){
        editor.addKeyListener(listener);
    }

    /**
     * Overridden to manage it's own set of listeners which will get notified of higher level events than the
     * ActionPerformed events. This allows clients of this class to not have to work out which events it should ignore.
     *
     * @param l ActionListener to add
     */
    @Override
    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
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

    public Object getPreviousSearchResult(){
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
    public void updateSearchableItems(List<T> searchableItems) {
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
    public void setComparator(Comparator<T> comparator) {
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
        private List<T> items;
        private List<T> filteredItems;
        private Object selectedItem;
        private final Filter filter;
        private Comparator<T> comparator;
        private String modelItemsClass;

        public FilterableComboBoxModel(final Filter filter) {
            items = new ArrayList<T>();
            filteredItems = new ArrayList<T>();
            this.filter = filter;
        }

        @Override
        public void addElement(Object obj) {
            if(obj == null) return;
            
            items.add((T) obj);
            updateFilteredItems();
        }

        @SuppressWarnings({"SuspiciousMethodCalls"})
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
            throw new UnsupportedOperationException("insertElementAt is not supported");
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
        public void updateSearchableItems(List<T> searchableItems) {
            items.clear();
            items.addAll(searchableItems);
            if(searchableItems.size() > 0){
                final Object t = searchableItems.get(0);
                modelItemsClass = t.getClass().getName();
            }
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
                for (T obj : items) {
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

        public void setComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        public int getSearchableItemsCount() {
            return filteredItems.size();
        }
    }

    /**
     *  The text editor field which will be listened for search characters to filter the items.
     */
    public class SearchFieldEditor<T> extends JTextField implements ComboBoxEditor, DocumentListener {
        private T selectedObject;
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

        @Override
        public Component getEditorComponent() {
            return this;
        }

        /**
         * setItem is called every time the user uses either the mouse keys to move through the drop down list or
         * if an item is clicked on with the mouse
         *
         * @param anObject
         */
        @Override
        public void setItem(Object anObject) {
            if(isFiltering) return;

            isSetting = true;
            //anObject comes directly from 'getSelectedItem' on the combo box. anObject is null, when nothing
            //is selected. Therefore we do not want to set "" as the text when anObject is null. We simply will do
            //nothing, which will leave any text previously entered in the text field.
            if (anObject != null) {
                //do not set the text of the text field. This overwrites what the user has typed
                //and causes various bugs where going back into the text field populates it with a previous selection
                if(model.modelItemsClass != null){
                    final String anObjectType = anObject.getClass().getName();
                    if(anObjectType.equals(model.modelItemsClass)){
                        selectedObject = (T) anObject;
                    }
                }
            }
            isSetting = false;
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
            ignoreWhen = true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filterItems(true);
            ignoreWhen = true;
        }

        /**
         *  The core of the filtering to display the new items in the drop down list based on the filtering
         * @param showPopUp boolean if there are search results and true, then the pop up menu will be shown
         */
        protected void filterItems(final boolean showPopUp) {
            if (isSetting) return;

            isFiltering = true;
            //reset the selected node, as if we are filtering it no longer applies
            selectedObject = null;
            
            model.setSearchText(getText());

            //refresh the drop down list
            setPopupVisible(false);

            //only show drop down list when there are filtered items available to be displayed
            boolean hasSearchableItems = model.getSearchableItemsCount() > 0;
            if (hasSearchableItems) {
                if (model.getSize() > 0) {
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
