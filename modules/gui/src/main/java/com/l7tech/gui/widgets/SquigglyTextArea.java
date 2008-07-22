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
 * A JTextArea that can display modeless error feedback in the form of squiggly lines.
 */
public class SquigglyTextArea extends JTextArea implements SquigglyField {
    private final SquigglyFieldSupport support = new SquigglyFieldSupport(new SquigglyFieldSupport.Callbacks() {
        public void repaint() {
            SquigglyTextArea.this.repaint();
        }

        public void setToolTipTextRaw(String text) {
            SquigglyTextArea.super.setToolTipText(text);
        }

        public String getToolTipTextRaw() {
            return SquigglyTextArea.super.getToolTipText();
        }
    });

    public SquigglyTextArea() {
    }

    public SquigglyTextArea(String text) {
        this(null, text, 0, 0);
    }

    public SquigglyTextArea(String text, int rows, int columns) {
        this(null, text, rows, columns);
    }

    public SquigglyTextArea(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
    }

    public synchronized int getBegin() {
        return support.getBegin();
    }

    public synchronized int getEnd() {
        return support.getEnd();
    }

    public synchronized void setRange(int begin, int end) {
        support.setRange(begin, end);
    }

    public synchronized void setAll() {
        support.setAll();
    }

    public synchronized void setNone() {
        support.setNone();
    }

    public synchronized Color getColor() {
        return support.getColor();
    }

    public synchronized void setColor(Color color) {
        support.setColor(color);
    }

    public synchronized void setSquiggly() {
        support.setSquiggly();
    }

    public synchronized void setDotted() {
        support.setDotted();
    }

    public synchronized void setStraight() {
        support.setStraight();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int begin;
        int end;

        synchronized (this) {
            begin = getBegin();
            end = getEnd();
        }

        if (begin == NONE || end == NONE) return;

        String text = getText();
        int len = text.length();

        if (begin >= len || len == 0) return;
        Color gColor = g.getColor();
        try {
            g.setColor(getColor());

            int ya;
            int xb;
            int xe;

            try {
                int beginLine = getLineOfOffset(begin == ALL ? 0 : begin);
                int endLine = getLineOfOffset(end == ALL || end > len ? len : end);

                for(int i = beginLine; i <= endLine; i++) {
                    Rectangle firstChar = modelToView(begin == ALL ? 0 : begin);
                    ya = (int)(firstChar.getY() + firstChar.getHeight());
                    xb = (int)firstChar.getX();
                    Rectangle lastChar = modelToView(end == ALL || end > len ? len : end);
                    xe = (int)(lastChar.getX() + lastChar.getWidth());
                    support.draw(g, xb, xe, ya);
                }
            } catch (BadLocationException e) {
                // FALLTHROUGH and omit drawing it
            }
        } finally {
            g.setColor(gColor);
        }
    }

    public String getModelessFeedback() {
        return support.getModelessFeedback();
    }

    public void setModelessFeedback(String feedback) {
        support.setModelessFeedback(feedback);
    }

    public void setToolTipText(String text) {
        support.setToolTipText(text);
    }

    public String getToolTipText() {
        return support.getToolTipText();
    }
}
