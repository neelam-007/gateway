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

    public void paint( Graphics g ) {
        super.paint(g);
        g.setColor( Color.RED );

        int ya = getHeight()-7;
        int xb = 0;
        int xe = getWidth();
        try {
            Rectangle firstChar = modelToView(0);
            ya = (int)(firstChar.getY() + firstChar.getHeight());
            xb = (int)firstChar.getX();
            Rectangle lastChar = modelToView(getText().length());
            xe = (int)(lastChar.getX()+lastChar.getWidth());
        } catch (BadLocationException e) {
        }
        int oldx = 0;
        int oldy = ya;
        for ( int x = xb; x < xe; x++ ) {
            int y = (int)(Math.sin(x/0.8) * 2.2)+ya;
            g.drawLine( oldx, oldy, x, y );
            oldx = x;
            oldy = y;
        }
    }

}
