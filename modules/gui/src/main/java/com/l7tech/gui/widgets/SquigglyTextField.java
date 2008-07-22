/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.widgets;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

/**
 * A JTextField that can display modeless error feedback in the form of squiggly lines.
 *
 * @author alex
 * @version $Revision$
 */
public class SquigglyTextField extends JTextField implements SquigglyField {
    private final SquigglyFieldSupport support = new SquigglyFieldSupport(new SquigglyFieldSupport.Callbacks() {
        public void repaint() {
            SquigglyTextField.this.repaint();
        }

        public void setToolTipTextRaw(String text) {
            SquigglyTextField.super.setToolTipText(text);
        }

        public String getToolTipTextRaw() {
            return SquigglyTextField.super.getToolTipText();
        }
    });

    public SquigglyTextField() {
    }

    public SquigglyTextField(String text) {
        super(text);
    }

    public SquigglyTextField(int columns) {
        super(columns);
    }

    public SquigglyTextField(String text, int columns) {
        super(text, columns);
    }

    public SquigglyTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
    }

    public void paintComponent( Graphics g ) {
        super.paintComponent(g);

        int begin;
        int end;

        synchronized( this ) {
            begin = support.getBegin();
            end = support.getEnd();
        }

        if ( begin == NONE || end == NONE) return;

        g.setColor(support.getColor());

        int ya = getHeight()-7;
        int xb = 0;
        int xe = getWidth() - 4;

        String text = getText();
        int len = text.length();

        if ( begin > len ) return;

        try {
            Rectangle firstChar = modelToView( begin == ALL ? 0 : begin );
            ya = (int)(firstChar.getY() + firstChar.getHeight());
            xb = (int)firstChar.getX();

            if (end != ALL || len > 0) {
                Rectangle lastChar = modelToView( end == ALL || end > len ? len : end);
                xe = (int)(lastChar.getX()+lastChar.getWidth());
            }
        } catch (BadLocationException e) {
            // FALLTHROUGH and use default values
        }

        support.draw( g, xb, xe, ya );
    }

    public void setToolTipText(String text) {
        support.setToolTipText(text);
    }

    public String getToolTipText() {
        return support.getToolTipText();
    }

    public String getModelessFeedback() {
        return support.getModelessFeedback();
    }

    public void setModelessFeedback(String feedback) {
        support.setModelessFeedback(feedback);
    }

    public int getBegin() {
        return support.getBegin();
    }

    public int getEnd() {
        return support.getEnd();
    }

    public void setRange(int begin, int end) {
        support.setRange(begin, end);
    }

    public void setAll() {
        support.setAll();
    }

    public void setNone() {
        support.setNone();
    }

    public Color getColor() {
        return support.getColor();
    }

    public void setColor(Color color) {
        support.setColor(color);
    }

    public void setSquiggly() {
        support.setSquiggly();
    }

    public void setDotted() {
        support.setDotted();
    }

    public void setStraight() {
        support.setStraight();
    }
}
