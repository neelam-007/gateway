/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import java.awt.*;

/**
 * @author alex
 * @version $Revision$
 */
public class SquigglyTextField extends JTextField {
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

    public synchronized int getSquiggleBegin() {
        return _squiggleBegin;
    }

    public synchronized int getSquiggleEnd() {
        return _squiggleEnd;
    }

    public synchronized void setSquiggle( int begin, int end ) {
        _squiggleBegin = begin;
        _squiggleEnd = end;
    }

    public synchronized void setSquiggleAll() {
        _squiggleBegin = ALL;
        _squiggleEnd = ALL;
    }

    public synchronized void setSquiggleNone() {
        _squiggleBegin = NONE;
        _squiggleEnd = NONE;
    }

    public void paint( Graphics g ) {
        super.paint(g);

        int begin;
        int end;
        synchronized( this ) {
            begin = _squiggleBegin;
            end = _squiggleEnd;
        }

        if ( begin == NONE || end == NONE ) return;

        g.setColor( Color.RED );

        int ya = getHeight()-7;
        int xb = 0;
        int xe = getWidth();

        String text = getText();
        int len = text.length();

        if ( begin > len ) return;

        try {
            Rectangle firstChar = modelToView( begin == ALL ? 0 : begin );
            ya = (int)(firstChar.getY() + firstChar.getHeight());
            xb = (int)firstChar.getX();
            Rectangle lastChar = modelToView( _squiggleEnd == ALL || _squiggleEnd > len ? len : _squiggleEnd );
            xe = (int)(lastChar.getX()+lastChar.getWidth());
        } catch (BadLocationException e) {
        }

        int oldx = xb;
        int oldy = ya;
        for ( int x = xb; x < xe; x++ ) {
            int y = (int)(0.5 + ya + (Math.sin(x/0.8) * 2) );
            g.drawLine( oldx, oldy, x, y );
            oldx = x;
            oldy = y;
        }
    }

    private int _squiggleBegin = NONE;
    private int _squiggleEnd = NONE;

    public static final int NONE = -2;
    public static final int ALL = -1;
}
