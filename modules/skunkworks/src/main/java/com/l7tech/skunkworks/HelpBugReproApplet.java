package com.l7tech.skunkworks;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applet that does nothing but launch browser help.
 */
public class HelpBugReproApplet extends JApplet {
    private static final Logger logger = Logger.getLogger(HelpBugReproApplet.class.getName());

    public void init() {
        final String target = param("helpTarget", "managerAppletHelp");

        getContentPane().add(new JButton(new AbstractAction("Show Help") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    URL cb = getDocumentBase();
                    URL url = new URL(cb, param("helpRootUrl", "/ssg/webadmin/help/_start.htm"));
                    logger.log(Level.INFO, "Attempting to showDocument(" + url + " , " + target + ")");
                    getAppletContext().showDocument(url, target);
                } catch (MalformedURLException e) {
                    logger.log(Level.WARNING, "Unable to show help", e);
                }
            }
        }));
    }

    private String param(String name, String dflt) {
        String val = getParameter(name);
        return val == null || val.trim().length() < 1 ? dflt : val;
    }
}
