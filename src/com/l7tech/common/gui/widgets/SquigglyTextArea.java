/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.gui.widgets;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

/**
 */
public class SquigglyTextArea extends JTextArea {
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
        return _begin;
    }

    public synchronized int getEnd() {
        return _end;
    }

    public synchronized void setRange(int begin, int end) {
        _begin = begin;
        _end = end;
        repaint();
    }

    public synchronized void setAll() {
        _begin = ALL;
        _end = ALL;
        repaint();
    }

    public synchronized void setNone() {
        _begin = NONE;
        _end = NONE;
        repaint();
    }

    public synchronized Color getColor() {
        return _color;
    }

    public synchronized void setColor(Color color) {
        _color = color;
        repaint();
    }

    public synchronized void setSquiggly() {
        _style = SQUIGGLY;
        repaint();
    }

    public synchronized void setStraight() {
        _style = STRAIGHT;
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int begin;
        int end;

        synchronized (this) {
            begin = _begin;
            end = _end;
        }

        if (begin == NONE || end == NONE) return;

        String text = getText();
        int len = text.length();

        if (begin >= len || len == 0) return;
        Color gColor = g.getColor();
        try {
            g.setColor(_color);

            int ya = getHeight() - 7;
            int xb = 0;
            int xe = getWidth();

            try {
                int beginLine = getLineOfOffset(begin == ALL ? 0 : begin);
                int endLine = getLineOfOffset(_end == ALL || _end > len ? len : _end);

                for(int i = beginLine; i <= endLine; i++) {
                    Rectangle firstChar = modelToView(begin == ALL ? 0 : begin);
                    ya = (int)(firstChar.getY() + firstChar.getHeight());
                    xb = (int)firstChar.getX();
                    Rectangle lastChar = modelToView(_end == ALL || _end > len ? len : _end);
                    xe = (int)(lastChar.getX() + lastChar.getWidth());
                    _style.draw(g, xb, xe, ya);
                }
            } catch (BadLocationException e) {
            }
        } finally {
            g.setColor(gColor);
        }
    }

    private static abstract class UnderlineStyle {
        public abstract void draw(Graphics g, int x1, int x2, int y);
    }

    private static final UnderlineStyle SQUIGGLY = new UnderlineStyle() {
        public void draw(Graphics g, int x1, int x2, int y) {
            int oldx = x1;
            int oldy = y;
            for (int x = x1; x < x2; x++) {
                int yy = (int)(0.5 + y + (Math.sin(x / 0.8) * 2));
                g.drawLine(oldx, oldy, x, yy);
                oldx = x;
                oldy = yy;
            }
        }
    };

    private static final UnderlineStyle STRAIGHT = new UnderlineStyle() {
        public void draw(Graphics g, int x1, int x2, int y) {
            g.drawLine(x1, y, x2, y);
        }
    };

    private int _begin = NONE;
    private int _end = NONE;
    private Color _color = Color.RED;
    private UnderlineStyle _style = SQUIGGLY;

    public static final int NONE = -2;
    public static final int ALL = -1;
}
