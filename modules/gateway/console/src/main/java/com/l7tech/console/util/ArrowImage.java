package com.l7tech.console.util;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


/**
 * A BufferedImage of one of four types of arrow (up, down, left or right)
 * drawn to the size specified on the constructor.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ArrowImage extends BufferedImage {
    public static final int ARROW_UP = 0;
    public static final int ARROW_DOWN = 1;
    public static final int ARROW_LEFT = 2;
    public static final int ARROW_RIGHT = 3;

    private GeneralPath pathArrow = new GeneralPath();

    public ArrowImage(int nArrowDirection) {
        this(15, 9, nArrowDirection);
    }

    public ArrowImage(int nWidth, int nHeight, int nArrowDirection) {
        super(nWidth, nHeight, TYPE_INT_ARGB_PRE);

        Map map = new HashMap();
        map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        RenderingHints hints = new RenderingHints(map);

        Graphics2D g2 = this.createGraphics();	// Create a graphics context for this buffered image
        g2.setRenderingHints(hints);

        float h = getHeight();
        float w = getWidth();
        float w13 = w / 3;
        float w12 = w / 2;
        float w23 = w * 2 / 3;
        float h13 = h / 3;
        float h12 = h / 2;
        float h23 = h * 2 / 3;

        switch (nArrowDirection) {
            case ARROW_UP:
                pathArrow.moveTo(w12, h12);
                pathArrow.lineTo(w12, 0);
                pathArrow.lineTo(w, h - 1);
                pathArrow.lineTo(0, h - 1);
                pathArrow.closePath();
                g2.setPaint(
                  new GradientPaint(
                    w13, h13, SystemColor.controlLtHighlight,
                    w, h - 1, SystemColor.controlShadow
                  )
                );
                g2.fill(pathArrow);
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(new Line2D.Float(0, h - 1, w, h - 1));
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(w12, 0, w, h - 1));
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(new Line2D.Float(0, h - 1, w12, 0));
                break;
            case ARROW_DOWN:
                pathArrow.moveTo(w12, h12);
                pathArrow.lineTo(w, 0);
                pathArrow.lineTo(w12, h - 1);
                pathArrow.closePath();
                g2.setPaint(
                  new GradientPaint(
                    0, 0, SystemColor.controlLtHighlight,
                    w23, h23, SystemColor.controlShadow
                  )
                );
                g2.fill(pathArrow);
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(new Line2D.Float(w, 0, w12, h - 1));
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(w12, h - 1, 0, 0));
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(new Line2D.Float(0, 0, w, 0));
                break;
            case ARROW_LEFT:
                pathArrow.moveTo(w - 1, h13);
                pathArrow.lineTo(w13, h13);
                pathArrow.lineTo(w13, 0);
                pathArrow.lineTo(0, h12);
                pathArrow.lineTo(w13, h - 1);
                pathArrow.lineTo(w13, h23);
                pathArrow.lineTo(w - 1, h23);
                pathArrow.closePath();
                g2.setPaint(
                  new GradientPaint(
                    0, 0, Color.white, //SystemColor.controlLtHighlight,
                    0, h, SystemColor.controlShadow
                  )
                );
                g2.fill(pathArrow);
                pathArrow.reset();
                pathArrow.moveTo(w13, 0);
                pathArrow.lineTo(w13, h13);
                pathArrow.moveTo(w - 1, h13);
                pathArrow.lineTo(w - 1, h23);
                pathArrow.lineTo(w13, h23);
                pathArrow.lineTo(w13, h - 1);
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(pathArrow);
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(0, h12, w13, h - 1));
                pathArrow.reset();
                pathArrow.moveTo(0, h12);
                pathArrow.lineTo(w13, 0);
                pathArrow.moveTo(w13, h13);
                pathArrow.lineTo(w - 1, h13);
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(pathArrow);
                break;
            case ARROW_RIGHT:
            default:
                pathArrow.moveTo(0, h13);
                pathArrow.lineTo(w23, h13);
                pathArrow.lineTo(w23, 0);
                pathArrow.lineTo(w - 1, h12);
                pathArrow.lineTo(w23, h - 1);
                pathArrow.lineTo(w23, h23);
                pathArrow.lineTo(0, h23);
                pathArrow.closePath();
                g2.setPaint(
                  new GradientPaint(
                    0, 0, Color.white, //SystemColor.controlLtHighlight,
                    0, h, SystemColor.controlShadow
                  )
                );
                g2.fill(pathArrow);
                pathArrow.reset();
                pathArrow.moveTo(0, h23);
                pathArrow.lineTo(w23, h23);
                pathArrow.moveTo(w23, h - 1);
                pathArrow.lineTo(w - 1, h12);
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(pathArrow);
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(w - 1, h12, w23, 0));
                pathArrow.reset();
                pathArrow.moveTo(w23, 0);
                pathArrow.lineTo(w23, h13);
                pathArrow.lineTo(0, h13);
                pathArrow.lineTo(0, h23);
                pathArrow.moveTo(w23, h23);
                pathArrow.lineTo(w23, h - 1);
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(pathArrow);
                break;
        }
    }
}



		

