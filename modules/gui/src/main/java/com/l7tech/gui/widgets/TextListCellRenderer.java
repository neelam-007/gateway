package com.l7tech.gui.widgets;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

/**
 * General purpose ListCellRenderer for Objects with are displayable with a JLabel (text + icon)
 *
 * @author Steve Jones
 */
public class TextListCellRenderer<SO> extends JLabel implements ListCellRenderer<SO> {

    //- PUBLIC

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * <p>This will not call the given function for null values.</p>
     *
     * @param accessorFunction The function to use (must not be null)
     */
    public TextListCellRenderer(final Functions.Unary<String,? super SO> accessorFunction) {
        this(accessorFunction, null, false);
    }

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * @param accessorFunction The function to use (must not be null)
     * @param tooltipAccessorFunction The function to use (ignored if null)
     * @param useAccessorForNull True to call the accessor function for null values.
     */
    public TextListCellRenderer(final Functions.Unary<String,? super SO> accessorFunction,
                                @Nullable final Functions.Unary<String,? super SO> tooltipAccessorFunction,
                                final boolean useAccessorForNull) {
        this(accessorFunction, tooltipAccessorFunction, null, useAccessorForNull);
    }

    /**
     * Create a ListCellRenderer that uses the given accessor to get the displayed text.
     *
     * @param accessorFunction The function to use (must not be null)
     * @param tooltipAccessorFunction The function to use (ignored if null)
     * @param iconAccessorFunction The function to use (ignored if null)
     * @param useAccessorForNull True to call the accessor function for null values.
     */
    public TextListCellRenderer(final Functions.Unary<String,? super SO> accessorFunction,
                                @Nullable final Functions.Unary<String,? super SO> tooltipAccessorFunction,
                                @Nullable final Functions.Unary<Icon,? super SO> iconAccessorFunction,
                                final boolean useAccessorForNull) {
        if(accessorFunction == null) throw new NullPointerException("accessorFunction cannot be null");

        this.accessorFunction = accessorFunction;
        this.tooltipAccessorFunction = tooltipAccessorFunction;
        this.useAccessorForNull = useAccessorForNull;
        this.iconAccessorFunction = iconAccessorFunction;
    }

    @Override
    public Dimension getPreferredSize() {
        //Get the text and add 3 spaces, then recalculate the size, then set the text back to what it originally was.
        //The result is that the preferred size for this label will have been calculated at the length of the text plus
        //3 characters. The end result is that when the combo box that uses this renderer is calculating it's preferred
        //size, it will do so based on the largest JLabel it contains. This solves an apparent outstanding issue with
        //the windows look and feel where its bounds are off by a couple of pixels.
        //See http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=b03d208eca8872e536f9f5084a20?bug_id=6477341

        //Do this for all look and feel's as it gets around any issue trying to determine system properties if a
        //security manager is installed.
        final String text = getText();
        setText(text + "   ");
        final Dimension size = super.getPreferredSize();
        setText(text);
        return size;
    }

    /**
     * @return true if the background is completely opaque and differs from the JList's background; false otherwise
     */
    @Override
    public boolean isOpaque() {
        final Color back = getBackground();
        Component p = getParent();
        if (p != null) {
            p = p.getParent();
        }
        // p should now be the JList.
        boolean colorMatch = (back != null) && (p != null) &&
            back.equals(p.getBackground()) &&
                        p.isOpaque();
        return !colorMatch && super.isOpaque();
    }

    public TableCellRenderer asTableCellRenderer(){
        return new DefaultTableCellRenderer(){
            @SuppressWarnings({ "unchecked" })
            @Override
            public Component getTableCellRendererComponent( final JTable table,
                                                            final Object value,
                                                            final boolean isSelected,
                                                            final boolean hasFocus,
                                                            final int row,
                                                            final int column ) {
                final Component component = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );

                if( component instanceof JLabel ) {
                    final JLabel cell = (JLabel) component;
                    String text = "";
                    String tooltipText = null;

                    if ( value != null || useAccessorForNull ) {
                        text = accessorFunction.call((SO)value);
                        tooltipText = tooltipAccessorFunction != null ?
                                tooltipAccessorFunction.call((SO)value) :
                                null;
                    }

                    cell.setText( text );
                    cell.setToolTipText( tooltipText );
                }

                return component;
            }
        };
    }

    /**
     * Get a renderer for use in a JComboBox.
     *
     * <p>The renderer will render truncated strings in JComboBox menus.</p>
     *
     * @return The renderer
     */
    public static <T> TextListCellRenderer<T> basicComboBoxRenderer() {
        final Functions.Unary<String, T> toStringAccessor = TextListCellRenderer.toStringAccessor();
        TextListCellRenderer<T> renderer = new TextListCellRenderer<T>( toStringAccessor, toStringAccessor, false );
        renderer.setRenderClipped(true);
        renderer.setSmartTooltips(true);
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
        Icon icon = null;

        if ( value != null || useAccessorForNull ) {
            text = accessorFunction.call((SO)value);
            tooltipText = tooltipAccessorFunction != null ?
                    tooltipAccessorFunction.call((SO)value) :
                    null;
            icon =  iconAccessorFunction != null ?
                    iconAccessorFunction.call((SO) value) :
                    null;
        }

        if ( smartTooltips && text != null && tooltipText != null && list.getParent() != null ) {
            final int width = Math.min(list.getWidth(), list.getParent().getWidth());
            if ( width > Utilities.computeStringWidth( list.getFontMetrics(list.getFont()), text ) ) {
                tooltipText = null; // suppress tooltip if full text is visible
            }
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

        if(icon != null) {
            setIcon(icon);
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setMinimumSize(new Dimension(-1,getFontMetrics(getFont()).getHeight()));

        return this;
    }

    public boolean isRenderClipped() {
        return renderClipped;
    }

    public void setRenderClipped( final boolean renderClipped ) {
        this.renderClipped = renderClipped;
    }

    /**
     * Smart tooltips will only display if the text for an entry is not fully visible.
     *
     * <p>You still need to configure the accessor for the tooltip.</p>
     *
     * @return True if smart tooltips are in use.
     */
    public boolean isSmartTooltips() {
        return smartTooltips;
    }

    public void setSmartTooltips( final boolean smartTooltips ) {
        this.smartTooltips = smartTooltips;
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

    private final Functions.Unary<String,? super SO> accessorFunction;
    private final Functions.Unary<String,? super SO> tooltipAccessorFunction;
    private final Functions.Unary<Icon, ? super SO> iconAccessorFunction;
    private final boolean useAccessorForNull;
    private boolean renderClipped;
    private boolean smartTooltips;
}
