package com.l7tech.console.xmlviewer;

import com.l7tech.console.xmlviewer.properties.ViewerProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Insert comments here.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class ViewerFrame extends JFrame {
    Viewer viewer = null;
    ViewerProperties viewerProperties = null;
    ExchangerDocument document = null;

    /**
     * Constructs an explorer view with the ExplorerProperties supplied.
     *
     * @param props the explorer properties.
     */
    public ViewerFrame(ViewerProperties props, ExchangerDocument document) {
        setTitle(document.getName());
        viewer = new Viewer(props, document);
        document.addListener(viewer);

        viewerProperties = props;
        this.document = document;

        // this goes
        ViewerMenuBar menu = new ViewerMenuBar(this, props);
        JPanel status = new StatusPanel(new BorderLayout(2, 0), menu, viewer);

        ViewerToolBar toolbar = new ViewerToolBar(props, viewer);
        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(viewer, BorderLayout.CENTER);
        contentPane.add(status, BorderLayout.SOUTH);
        setJMenuBar(menu);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });


        setSize(viewerProperties.getDimension());
        setLocation(viewerProperties.getPosition());

    }

    /**
     * @return the viewer hosted in this frame
     */
    public Viewer getViewer() {
        return viewer;
    }

    /**
     * Closes the Viewer frame.
     */
    public void close() {
        document.removeListener(viewer);
        viewerProperties.setDimension(getSize());
        viewerProperties.setPosition(getLocation());

        dispose();
    }


}
