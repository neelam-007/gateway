package com.l7tech.proxy.gui;

import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Ssg;

/**
 * Encapsulates the Client Proxy's user interface.
 * User: mike
 * Date: May 22, 2003
 * Time: 1:47:04 PM
 * To change this template use Options | File Templates.
 */
public class Gui {
    private static final Category log = Category.getInstance(Gui.class.getName());

    private static Gui instance;

    private JFrame frame;
    private boolean started = false;
    private MessageViewer messageViewer;

    private static final String MENU_FILE = "File";
    private static final String MENU_FILE_QUIT = "Quit";
    private static final String MENU_SHOW = "Show";
    private static final String MENU_SHOW_MESSAGES = "Show Messages";

    private JCheckBoxMenuItem showMessages;
    private SsgPanel ssgPanel;
    private DefaultListModel ssgListModel;
    private JList ssgList;

    /** Get the singleton Gui. */
    public static Gui getInstance() {
        if (instance == null)
            instance = new Gui();
        return instance;
    }

    private Gui() {
    }

    /**
     * Interface implemented by consumers who wish to be notified when the user shuts down the GUI.
     */
    public static interface ShutdownListener {
        public void guiShutdown();
    }

    private ShutdownListener ShutdownListener;

    /** Shut down the GUI. */
    private void closeFrame() {
        if (messageViewer != null) {
            messageViewer.dispose();
            messageViewer = null;
        }
        frame.dispose();
        frame = null;
        started = false;
        if (ShutdownListener != null)
            ShutdownListener.guiShutdown();
    }

    /**
     * Connect us to someone who wants to know when the GUI is exiting.
     * @param guiShutdownListener
     */
    public void setShutdownListener(ShutdownListener guiShutdownListener) {
        this.ShutdownListener = guiShutdownListener;
    }

    /** Create the Message Viewer. */
    private MessageViewer getMessageViewer() {
        if (messageViewer == null) {
            messageViewer = new MessageViewer("Message Window");
            messageViewer.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    showMessages.setSelected(false);
                }

                public void windowStateChanged(WindowEvent e) {
                    showMessages.setSelected(messageViewer.isShowing());
                }
            });
        }
        return messageViewer;
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
        ssgList = new JList();
        ssgList.setPreferredSize(new Dimension(180, 300));
        ssgListPanel.add(ssgList);
        JPanel buttonPanel = new JPanel();
        frame.getContentPane().add(ssgListPanel, BorderLayout.WEST);
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        ssgListModel = new DefaultListModel();
        ssgList.setModel(ssgListModel);
        ssgListModel.addElement(new Ssg("Main SSG", "SSG0", "http://localhost:9898/", "", ""));
        ssgListModel.addElement(new Ssg("Alternate SSG", "SSG1", "http://localhost:9898/", "", ""));
        ssgPanel = new SsgPanel((Ssg)ssgListModel.get(0));
        ssgList.setSelectedIndex(0);
        ssgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frame.getContentPane().add(ssgPanel, BorderLayout.EAST);
        ssgList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                selectSsg(ssgList.getSelectedIndex());
            }
        });

        JButton newSsgButton = new JButton("New SSG");
        buttonPanel.add(newSsgButton);
        JButton deleteSsgButton = new JButton("Delete SSG");
        buttonPanel.add(deleteSsgButton);
        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeFrame();
            }
        });
        buttonPanel.add(quitButton);

        frame.pack();

        return frame;
    }

    /** Change the currently selected SSG to the given index. */
    private void selectSsg(int index) {
        ssgPanel.setSsg((Ssg)ssgListModel.get(index));
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
                getMessageViewer().show();
            } else {
                getMessageViewer().hide();
            }
        }
    }

    /** Notification that the Message Viewer window has been shown or hidden. */
    public void updateMessageViewerStatus() {
        showMessages.setSelected(getMessageViewer().isVisible());
    }

    /** Get the RequestInterceptor attached to the Message Viewer window. */
    public RequestInterceptor getRequestInterceptor() {
        return getMessageViewer().getMessageViewerModel();
    }

    /** Start the GUI. */
    public void start() throws IllegalStateException {
        if (started)
            throw new IllegalStateException("Gui has already been started");

        makeFrame().show();
    }
}
