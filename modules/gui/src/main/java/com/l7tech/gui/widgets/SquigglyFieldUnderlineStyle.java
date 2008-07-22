package com.l7tech.gui.widgets;

import java.awt.*;

/**
 * Provides underline styles for {@link SquigglyField}s.
 */
abstract class SquigglyFieldUnderlineStyle {
    /**
     * Draw underline in current style with the specified Graphics along y, starting from x1 and going to x2
     * (already translated into g's coordinate system).
     *
     * @param g   Graphics instance.  Required.
     * @param x1  left edge
     * @param x2  right edge
     * @param y   average y coordinate
     */
    public abstract void draw( Graphics g, int x1, int x2, int y );

    public static final SquigglyFieldUnderlineStyle SQUIGGLY = new SquigglyFieldUnderlineStyle() {
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

    public static final SquigglyFieldUnderlineStyle STRAIGHT = new SquigglyFieldUnderlineStyle() {
        public void draw(Graphics g, int x1, int x2, int y) {
            g.drawLine( x1, y, x2, y );
        }
    };

    public static final SquigglyFieldUnderlineStyle DOTTED = new SquigglyFieldUnderlineStyle() {
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
}
