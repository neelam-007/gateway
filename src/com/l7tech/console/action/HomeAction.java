package com.l7tech.console.action;

import com.l7tech.console.panels.NewUserDialog;
import com.l7tech.console.panels.WorkBenchPanel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.MainWindow;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import java.net.URL;
import java.io.IOException;

/**
 * The <code>HomeAction</code> displays the dds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class HomeAction extends BaseAction {
   private WorkBenchPanel wpanel;
   private ClassLoader cl = getClass().getClassLoader();

    public HomeAction(WorkBenchPanel panel) {
        wpanel = panel;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Home page";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Goto policy editor home page";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/server16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {
                wpanel.setComponent(getHomePageComponent());
            }
        });
    }


    private JComponent getHomePageComponent() {
        final String home = MainWindow.RESOURCE_PATH + "/home.html";
        HTMLDocument doc = new HTMLDocument();
        JTextPane htmlPane = new JTextPane(doc);

        URL url = cl.getResource(home);
        htmlPane.setName("Home");
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    System.out.println("activated " + e.getURL());
                }
            }
        });
        try {
            htmlPane.setPage(url);
            return htmlPane;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
