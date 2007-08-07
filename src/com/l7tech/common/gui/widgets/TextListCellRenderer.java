package com.l7tech.common.gui.widgets;

import java.awt.Component;
import javax.swing.*;

import com.l7tech.common.util.Functions;

/**
 * General purpose ListCellRenderer for Objects with a textual representation.
 *
 * @author Steve Jones
 */
public class TextListCellRenderer extends JLabel implements ListCellRenderer {

    //- PUBLIC

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * <p>This will not call the given function for null values.</p>
     *
     * @param accessorFunction The function to use (must not be null)
     */
    public TextListCellRenderer(final Functions.Unary<String,Object> accessorFunction) {
        this(accessorFunction, false);
    }

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * @param accessorFunction The function to use (must not be null)
     * @param useAccessorForNull True to call the accessor function for null values.
     */
    public TextListCellRenderer(final Functions.Unary<String,Object> accessorFunction,
                                final boolean useAccessorForNull) {
        this.accessorFunction = accessorFunction;
        this.useAccessorForNull = useAccessorForNull;
    }

    /**
     * Return the component configured for rendering the given value.
     */
    public Component getListCellRendererComponent( JList list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus)
    {
        String text = "";

        if ( value != null || useAccessorForNull ) {
            text = accessorFunction.call(value);
        }

        setText( text );

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            setOpaque(true);
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setOpaque(false);
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());

        return this;
    }

    //- PRIVATE

    private final Functions.Unary<String,Object> accessorFunction;
    private final boolean useAccessorForNull;
}
