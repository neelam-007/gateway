package com.l7tech.console.xmlviewer;

import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;

/**
 * Insert comments here.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class ViewerCmdLine {
    public static void main(String[] args) throws IOException, SAXParseException {
        ConfigurationProperties cp = new ConfigurationProperties();
        final File file = new File("GetLastTradePriceSoapRequest.xml");
        System.out.println(file.getAbsolutePath());
        ExchangerDocument ec = new ExchangerDocument(file.toURL(), false);
        ec.load();
        ViewerFrame viewer = new ViewerFrame(cp.getViewer(), ec);
        viewer.show();
    }
}
