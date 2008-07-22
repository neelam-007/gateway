package com.l7tech.gui.widgets;

import static com.l7tech.gui.widgets.SquigglyField.*;
import static com.l7tech.gui.widgets.SquigglyFieldUnderlineStyle.*;

import java.awt.*;

/**
 * Mixin delegate that provides most of an implementation of SquigglyField and also allows one to offer
 * ModelessFeedback support using its tooltip.
 * <p/>
 * When there is modeless feedback, the tooltip changes to it; otherwise the original tooltip is restored.
 * To use this class, delegate get/setModelessFeedback and get/setTooltipText to it.
 */
class SquigglyFieldSupport {
    private final Callbacks callbacks;
    private int begin = SquigglyField.NONE;
    private int end = SquigglyField.NONE;
    private Color color = Color.RED;
    private SquigglyFieldUnderlineStyle style = SQUIGGLY;
    private String modelessFeedback = null;
    private String toolTipText = null;

    public interface Callbacks {
        /** Cause the squiggly field to be repainted. */
        void repaint();

        /**
         * Cause the squiggly line to display the specified tooltip or modeless feedback.  Must bypass delegation back to here.
         * @param text  the text that should currently appear in the box when the mouse stops over the component.
         */
        void setToolTipTextRaw(String text);

        String getToolTipTextRaw();
    }

    /**
     * Create SquigglyFieldSupport mixin.  It will manage tooltips on the specified JComponent,
     * and manage the squiggly field presence on the specified SquigglyField.  Typically these will both
     * be the exact same object.
     *
     * @param callbacks      callbacks to repaint the component and change its low-level tooltip display.  Required.
     */
    public SquigglyFieldSupport(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public synchronized int getBegin() {
        return begin;
    }

    public synchronized int getEnd() {
        return end;
    }

    public void draw( Graphics g, int x1, int x2, int y ) {
        style.draw(g, x1, x2, y);
    }

    public synchronized void setRange( int begin, int end ) {
        this.begin = begin;
        this.end = end;
        callbacks.repaint();
    }

    public synchronized void setAll() {
        begin = ALL;
        end = ALL;
        callbacks.repaint();
    }

    public synchronized void setNone() {
        begin = NONE;
        end = NONE;
        callbacks.repaint();
    }

    public synchronized Color getColor() {
        return color;
    }

    public synchronized void setColor(Color color) {
        this.color = color;
        callbacks.repaint();
    }

    public synchronized void setSquiggly() {
        style = SQUIGGLY;
        callbacks.repaint();
    }

    public synchronized void setDotted() {
        style = DOTTED;
        callbacks.repaint();
    }

    public synchronized void setStraight() {
        style = STRAIGHT;
        callbacks.repaint();
    }

    public boolean isShowingModlessFeedback() {
        return !(getBegin() == SquigglyField.NONE || getEnd() == SquigglyField.NONE);
    }

    public String getModelessFeedback() {
        if (!isShowingModlessFeedback())
            return null;

        return modelessFeedback;
    }

    public void setModelessFeedback(String feedback) {
        modelessFeedback = feedback;
        if (feedback == null || feedback.length() < 1) {
            setNone();
            callbacks.setToolTipTextRaw(toolTipText);
        } else {
            setAll();
            callbacks.setToolTipTextRaw(modelessFeedback);
        }
    }

    public void setToolTipText(String text) {
        toolTipText = text;
        if (isShowingModlessFeedback())
            callbacks.setToolTipTextRaw(modelessFeedback);
        else
            callbacks.setToolTipTextRaw(toolTipText);
    }

    public String getToolTipText() {
        return callbacks.getToolTipTextRaw();
    }
}
