package com.l7tech.console.util;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventListener;

/**
 * The <code>HyperlinkLabel</code> extends the JLabel with hyperlink
 * support.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HyperlinkLabel extends JLabel {
    URL url;

    public HyperlinkLabel(String text, Icon icon, String url)
      throws MalformedURLException {
        this(text, icon, url, LEADING);
    }

    public HyperlinkLabel(String text, Icon icon, String url, int horizontalAlignment)
         throws MalformedURLException {
           super(makeHyperlink(text, url), horizontalAlignment);
           this.setIcon(icon);
           this.url = new URL(url);
           addListeners();
       }


    public void addHyperlinkListener(HyperlinkListener l) {
        this.listenerList.add(HyperlinkListener.class, l);
    }

    public void removeHyperlinkListener(HyperlinkListener l) {
        this.listenerList.remove(HyperlinkListener.class, l);
    }

    private void addListeners() {
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                fireHyperlinkUpdated();
            }

            public void mouseEntered(MouseEvent e) {
                HyperlinkLabel.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                HyperlinkLabel.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

        });
    }

    private void fireHyperlinkUpdated() {
        HyperlinkEvent event =
          new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, url);
        EventListener[] listeners = listenerList.getListeners(HyperlinkListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((HyperlinkListener)listeners[i]).hyperlinkUpdate(event);
        }
    }

    private static String makeHyperlink(String text, String url) {
        return "<html><a href=\"+"+url+"\">"+  text  +"</a></html>";
    }
}
