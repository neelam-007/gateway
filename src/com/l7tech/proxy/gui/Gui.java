package com.l7tech.proxy.gui;

import org.apache.log4j.Category;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.l7tech.proxy.RequestInterceptor;

/**
 * Encapsulates the Client Proxy's user interface.
 * User: mike
 * Date: May 22, 2003
 * Time: 1:47:04 PM
 * To change this template use Options | File Templates.
 */
public class Gui {
    private static final Category log = Category.getInstance(Gui.class.getName());

    private JFrame frame;
    private boolean shutdown = false;
    private Object waiter = new Object();

    private static final String MENU_FILE = "File";
    private static final String MENU_FILE_QUIT = "Quit";
    private static final String MENU_SHOW = "Show";
    private static final String MENU_SHOW_MESSAGES = "Show Messages";

    private MessageViewer messageViewer = new MessageViewer("Message Viewer");
    private JCheckBoxMenuItem showMessages;

    /** Shut down the GUI. */
    private void closeFrame() {
        messageViewer.dispose();
        frame.dispose();
        shutdown = true;
        synchronized(waiter) {
            waiter.notifyAll();
        }
    }

    /** Create the GUI frame. */
    private JFrame makeFrame() {
        frame = new JFrame("Client Proxy");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeFrame();
            }
        });

        JMenuBar menus = makeMenus();
        frame.setJMenuBar(menus);

        frame.getContentPane().setLayout(new BorderLayout());
        JPanel ssgListPanel = new JPanel();
        JList ssgList = new JList();
        ssgListPanel.add(ssgList);
        ssgListPanel.setMinimumSize(new Dimension(300, 500));
        JPanel ssgPanel = new JPanel();
        frame.getContentPane().add(ssgListPanel, BorderLayout.WEST);
        frame.getContentPane().add(ssgPanel, BorderLayout.EAST);
        DefaultListModel ssgListModel = new DefaultListModel();
        ssgList.setModel(ssgListModel);
        ssgListModel.addElement("Default SSG");

        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeFrame();
            }
        });
        ssgPanel.add(quitButton);

        frame.pack();
        return frame;
    }

    /** Build the menu bar. */
    private JMenuBar makeMenus() {
        ActionListener menuActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action(e);
            }
        };

        JMenuBar menus = new JMenuBar();
        JMenu fileMenu = new JMenu(MENU_FILE);

        JMenuItem fileQuit = new JMenuItem(MENU_FILE_QUIT);
        fileQuit.addActionListener(menuActionListener);
        fileMenu.add(fileQuit);

        menus.add(fileMenu);
        JMenu showMenu = new JMenu(MENU_SHOW);

        showMessages = new JCheckBoxMenuItem(MENU_SHOW_MESSAGES, false);
        showMessages.addActionListener(menuActionListener);
        showMenu.add(showMessages);

        menus.add(showMenu);
        return menus;
    }

    /** Respond to a menu command. */
    private void action(ActionEvent e) {
        if (MENU_FILE_QUIT.equals(e.getActionCommand())) {
            closeFrame();
        } else if (MENU_SHOW_MESSAGES.equals(e.getActionCommand())) {
            if (showMessages.isSelected()) {
                messageViewer.show();
            } else {
                messageViewer.hide();
            }
        }
    }

    /** Get the RequestInterceptor attached to the Message Viewer window. */
    public RequestInterceptor getRequestInterceptor() {
        return messageViewer.getMessageViewerModel();
    }

    /** Run the GUI until it is shut down. */
    public void run() {
        makeFrame().show();
        shutdown = false;

        while (!shutdown) {
            synchronized(waiter) {
                try {
                    waiter.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
