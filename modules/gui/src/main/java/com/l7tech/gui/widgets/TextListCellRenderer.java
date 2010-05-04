package com.l7tech.gui.widgets;

import java.awt.*;
import javax.swing.*;

import com.l7tech.util.Functions;

/**
 * General purpose ListCellRenderer for Objects with a textual representation.
 *
 * @author Steve Jones
 */
public class TextListCellRenderer<SO> extends JLabel implements ListCellRenderer {

    //- PUBLIC

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * <p>This will not call the given function for null values.</p>
     *
     * @param accessorFunction The function to use (must not be null)
     */
    public TextListCellRenderer(final Functions.Unary<String,SO> accessorFunction) {
        this(accessorFunction, null, false);
    }

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * @param accessorFunction The function to use (must not be null)
     * @param tooltipAccessorFunction The function to use (must not be null)
     * @param useAccessorForNull True to call the accessor function for null values.
     */
    public TextListCellRenderer(final Functions.Unary<String,SO> accessorFunction,
                                final Functions.Unary<String,SO> tooltipAccessorFunction,
                                final boolean useAccessorForNull) {
        this.accessorFunction = accessorFunction;
        this.tooltipAccessorFunction = tooltipAccessorFunction;
        this.useAccessorForNull = useAccessorForNull;
    }

    /**
     * Get a renderer for use in a JComboBox.
     *
     * <p>The renderer will render truncated strings in JComboBox menus.</p>
     *
     * @return The renderer
     */
    public static <T> TextListCellRenderer<T> basicComboBoxRenderer() {
        TextListCellRenderer<T> renderer = new TextListCellRenderer<T>( TextListCellRenderer.<T>toStringAccessor() );
        renderer.setRenderClipped(true);
        return renderer;
    }

    /**
     * Get an accessor that calls toString on an object.
     *
     * @return The accessor
     */
    public static <T> Functions.Unary<String,T> toStringAccessor() {
        return new Functions.Unary<String,T>(){
            @Override
            public String call( final T t ) {
                return t == null ? "" : t.toString();
            }
        };
    }

    /**
     * Return the component configured for rendering the given value.
     */
    @Override
    public Component getListCellRendererComponent( JList list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus)
    {
        String text = "";
        String tooltipText = null;

        if ( value != null || useAccessorForNull ) {
            text = accessorFunction.call((SO)value);
            tooltipText = tooltipAccessorFunction != null ?
                    tooltipAccessorFunction.call((SO)value) :
                    null;
        }

        setText( text );
        setToolTipText(tooltipText);

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

    public boolean isRenderClipped() {
        return renderClipped;
    }

    public void setRenderClipped( final boolean renderClipped ) {
        this.renderClipped = renderClipped;
    }

    //- PROTECTED

    @Override
    protected void paintComponent( final Graphics g ) {
        if ( renderClipped ) {
            // Ensure the bounds is set correctly for drawing the truncated string in a JComboBox menu.
            final int width = Math.min(getWidth(), (int)g.getClip().getBounds2D().getWidth());
            Rectangle bounds = getBounds();
            bounds.width = width;
            setBounds( bounds );
        }

        super.paintComponent( g );
    }

    //- PRIVATE

    private final Functions.Unary<String,SO> accessorFunction;
    private final Functions.Unary<String,SO> tooltipAccessorFunction;
    private final boolean useAccessorForNull;
    private boolean renderClipped;
}
