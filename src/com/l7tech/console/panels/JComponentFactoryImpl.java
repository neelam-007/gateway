package com.l7tech.console.panels;

import com.l7tech.console.factory.JComponentFactory;
import com.l7tech.console.MainWindow;
import com.l7tech.console.tree.ServicesFolderNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionNodeCellRenderer;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import java.net.URL;
import java.io.IOException;

/**
 * Class JComponentFactoryImpl is the
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class JComponentFactoryImpl implements JComponentFactory {
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    /**
     * Produce the <code>JComponent</code> for the given object.
     * @param o the object that the corresponding component
     * @return the component for the given object or <b>null</b> if
     *         th component cannot be
     */
    public JComponent getJComponent(Object o) {
        if (o instanceof ServicesFolderNode) {
            return getHomePageComponent();
        } else if (o instanceof ServiceNode) {
           return getServiceNodeComponent((ServiceNode)o);
        }

        return null;
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

     private JComponent getServiceNodeComponent(ServiceNode sn) {
         try {
             JTree tree = new JTree(PolicyTreeModel.make(sn.getPublishedService()));
             tree.setName(sn.getPublishedService().getName());
             tree.setCellRenderer(new AssertionNodeCellRenderer());
             return tree;
         } catch (FindException e) {
             throw new RuntimeException (e);
         }
     }
}
