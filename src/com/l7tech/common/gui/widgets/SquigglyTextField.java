/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.gui.util.ModelessFeedback;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author alex
 * @version $Revision$
 */
public class SquigglyTextField extends JTextField implements ModelessFeedback {
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

    public synchronized int getBegin() {
        return _begin;
    }

    public synchronized int getEnd() {
        return _end;
    }

    public synchronized void setRange( int begin, int end ) {
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

    public synchronized void setDotted() {
        _style = DOTTED;
        repaint();
    }

    public synchronized void setStraight() {
        _style = STRAIGHT;
        repaint();
    }

    public void paintComponent( Graphics g ) {
        super.paintComponent(g);

        int begin;
        int end;

        synchronized( this ) {
            begin = _begin;
            end = _end;
        }

        if ( begin == NONE || end == NONE ) return;

        g.setColor( _color );

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

            if (_end != ALL || len > 0) {
                Rectangle lastChar = modelToView( _end == ALL || _end > len ? len : _end );
                xe = (int)(lastChar.getX()+lastChar.getWidth());
            }
        } catch (BadLocationException e) {
        }

        _style.draw( g, xb, xe, ya );
    }

    private static abstract class UnderlineStyle {
        public abstract void draw( Graphics g, int x1, int x2, int y );
    }

    private static final UnderlineStyle SQUIGGLY = new UnderlineStyle() {
        public void draw(Graphics g, int x1, int x2, int y) {
            int oldx = x1;
            int oldy = y;
            for ( int x = x1; x < x2; x++ ) {
                int yy = (int)(0.5 + y + (Math.sin(x/0.8) * 2) );
                g.drawLine( oldx, oldy, x, yy );
                oldx = x;
                oldy = yy;
            }
        }
    };

    private static final UnderlineStyle STRAIGHT = new UnderlineStyle() {
        public void draw(Graphics g, int x1, int x2, int y) {
            g.drawLine( x1, y, x2, y );
        }
    };

    private static final UnderlineStyle DOTTED = new UnderlineStyle() {
        public void draw(Graphics g, int x1, int x2, int y) {
            boolean on = true;
            for (int i = x1; i < x2; i += 3) {
                if (on) {
                    g.drawLine(i, y, i+2, y);
                    g.drawLine(i, y+1, i+2, y+1);
                }
                on = !on;
            }
        }
    };

    public String getModelessFeedback() {
        if (!isShowingModlessFeedback())
            return null;

        return modelessFeedback;
    }

    private boolean isShowingModlessFeedback() {
        return !(getBegin() == NONE || getEnd() == NONE);
    }

    public void setModelessFeedback(String feedback) {
        String prev = modelessFeedback;
        modelessFeedback = feedback;
        if (feedback == null || feedback.length() < 1) {
            setNone();
            super.setToolTipText(toolTipText);
        } else {
            setAll();
            super.setToolTipText(modelessFeedback);
        }
    }

    public void setToolTipText(String text) {
        toolTipText = text;
        if (isShowingModlessFeedback())
            super.setToolTipText(modelessFeedback);
        else
            super.setToolTipText(text);
    }

    public String getToolTipText(MouseEvent event) {
        return isShowingModlessFeedback() ? modelessFeedback : toolTipText;
    }

    private int _begin = NONE;
    private int _end = NONE;
    private Color _color = Color.RED;
    private UnderlineStyle _style = SQUIGGLY;
    private String modelessFeedback = null;
    private String toolTipText = null;

    public static final int NONE = -2;
    public static final int ALL = -1;
}
