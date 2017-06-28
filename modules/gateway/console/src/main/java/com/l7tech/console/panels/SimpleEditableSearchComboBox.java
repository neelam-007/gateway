package com.l7tech.console.panels;

import com.l7tech.gui.util.DocumentSizeFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Simple Editable Search ComboBox control is customized version of existing EditableSearchComboBox control.
 * It is customized for string model collection. Edited text will be automatically completed upon user selection from the drop down list.
 * By default, filter mechanism is set to FilterBy.CONTAINS. It is recommended to listen to ItemSelectedEvent event for the item selection changes.
 */
public class SimpleEditableSearchComboBox extends EditableSearchComboBox<String> {

    private Window parentWindow;
    private boolean ignoreItemChangeEvent;
    private List<ItemListener> itemSelectedListeners = new ArrayList<>();

    public SimpleEditableSearchComboBox() {
        super(new Filter() {
            public boolean accept(Object obj) {
                if (getFilterBy() == FilterBy.STARTS_WITH) {
                    return ((String)obj).toLowerCase().startsWith(getFilterText().toLowerCase());
                } else {
                    return ((String)obj).toLowerCase().contains(getFilterText().toLowerCase());
                }
            }
        });

        // Overall filter settings
        setPreFilterComparator((o1, o2) -> (o1.compareToIgnoreCase(o2)));
        setShowAllOnEmptyFilterText(true);
        setFilterBy(FilterBy.CONTAINS);

        // Ignore ItemChangeEvent for up/down arrow (or page) keys
        getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                ignoreItemChangeEvent = (e.getKeyCode() == KeyEvent.VK_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_PAGE_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_KP_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_PAGE_UP ||
                        e.getKeyCode() == KeyEvent.VK_KP_UP);
            }

            public void keyReleased(KeyEvent e) {
                ignoreItemChangeEvent = false;
            }
        });

        // To auto-complete the user editable text by drop-down item selection
        // In addition, take care of firing the ItemChange event for pressing the enter-key.
        // This special case should be considered because:
        //      Whenever user navigates from one item to other in the drop down list using arrow keys, ItemChange event occurs.
        //      Finally, user finalizes his/her selection by pressing the enter-key. As the item is not changed since the last ItemChange event, pressing the enter-key doesn't result the ItemChange event again.
        //      Fortunately, Action event fires for enter-key command.
        addActionListener(e -> {
            String selectedObject = getSelectedObject();
            String editableText = getEditableText();

            if (selectedObject != null) {
                setEditableText(selectedObject);
                fireItemSelectedEventIfRequired(e, selectedObject);
            } else {
                fireItemSelectedEventIfRequired(e, editableText);
            }

            SwingUtilities.invokeLater(()-> hidePopup());
        });

        // All the selected ItemChange events should be wired to ItemSelectedEvent.
        //  Note that, ItemChange events due to arrow (or page) keys should be ignored.
        addItemListener(e -> {
            if (!ignoreItemChangeEvent && e.getStateChange() == ItemEvent.SELECTED) {
                fireItemSelectedEvent(e);
            }
        });

        // Adjust the parent window if control is expanded outside of visible range
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Window parent = SimpleEditableSearchComboBox.this.getParentWindow();
                Component child = SimpleEditableSearchComboBox.this;
                if (parent.getSize().getWidth() <= child.getX() + child.getWidth()) {
                    parent.pack();
                }
            }
        });

        ((JTextField)getEditor().getEditorComponent()).setBorder(BorderFactory.createEmptyBorder());
    }

    private void fireItemSelectedEventIfRequired(ActionEvent e, String selectedObject) {
        if ("enter-key".equals(e.getActionCommand())) {
            fireItemSelectedEvent(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                    selectedObject, ItemEvent.SELECTED));
        }
    }

    private void fireItemSelectedEvent(ItemEvent e) {
        for (ItemListener listener : itemSelectedListeners) {
            listener.itemStateChanged(e);
        }
    }

    /**
     * Register the ItemSelectedEvent listener.
     * @param listener ItemListener instance
     */
    public void addItemSelectedListener(ItemListener listener) {
        itemSelectedListeners.add(listener);
    }

    /**
     * Set the filter-by mechanism
     * @param filterBy
     */
    public void setFilterBy(FilterBy filterBy) {
        ((FilterableComboBoxModel)this.getModel()).setSearchType(filterBy);
    }

    /**
     * Set to true if all items to be shown on empty filter text.
     * @param flag
     */
    public void setShowAllOnEmptyFilterText(boolean flag) {
        ((FilterableComboBoxModel)this.getModel()).setSearchShowAllOnEmptyFilterText(flag);
    }

    /**
     * Set the editable document size
     * @param size
     */
    public void setEditableDocumentSize(int size) {
        ((AbstractDocument)((JTextField)getEditor().getEditorComponent()).getDocument())
                .setDocumentFilter(new DocumentSizeFilter(size));
    }

    /**
     * Get the editable text
     * @return
     */
    public String getEditableText() {
        return ((JTextComponent)getEditor().getEditorComponent()).getText();
    }

    /**
     * Set the editable text.
     * @param text
     */
    public void setEditableText(String text) {
        ((JTextComponent)getEditor().getEditorComponent()).setText(text);
    }

    /**
     * Get the nearest window parent.
     * @return nearest window parent
     */
    public Window getParentWindow() {
        if (parentWindow == null) {
            Container container = this;

            while (container.getParent() != null) {
                container = container.getParent();
                if (container instanceof Window) {
                    break;
                }
            }

            parentWindow = (Window) container;
        }

        return parentWindow;
    }

}
