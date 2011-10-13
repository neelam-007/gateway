/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.widgets;

import org.jetbrains.annotations.Nullable;

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
        @Override
        public void repaint() {
            SquigglyTextField.this.repaint();
        }

        @Override
        public void setToolTipTextRaw(String text) {
            SquigglyTextField.super.setToolTipText(text);
        }

        @Override
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

    @Override
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

    @Override
    public void setToolTipText(String text) {
        support.setToolTipText(text);
    }

    @Override
    public String getToolTipText() {
        return support.getToolTipText();
    }

    @Override
    public String getModelessFeedback() {
        return support.getModelessFeedback();
    }

    @Override
    public void setModelessFeedback(@Nullable String feedback) {
        support.setModelessFeedback(feedback);
    }

    @Override
    public int getBegin() {
        return support.getBegin();
    }

    @Override
    public int getEnd() {
        return support.getEnd();
    }

    @Override
    public void setRange(int begin, int end) {
        support.setRange(begin, end);
    }

    @Override
    public void setAll() {
        support.setAll();
    }

    @Override
    public void setNone() {
        support.setNone();
    }

    @Override
    public Color getColor() {
        return support.getColor();
    }

    @Override
    public void setColor(Color color) {
        support.setColor(color);
    }

    @Override
    public void setSquiggly() {
        support.setSquiggly();
    }

    @Override
    public void setDotted() {
        support.setDotted();
    }

    @Override
    public void setStraight() {
        support.setStraight();
    }
}
