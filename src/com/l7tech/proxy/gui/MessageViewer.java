package com.l7tech.proxy.gui;

import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays a list of messages.
 * User: mike
 * Date: May 22, 2003
 * Time: 5:01:29 PM
 * To change this template use Options | File Templates.
 */
public class MessageViewer extends JFrame {
    private static Category log = Category.getInstance(MessageViewer.class);

    MessageViewerModel messageViewerModel = new MessageViewerModel();
    private JTextArea messageView;
    private JList messageList;

    private class MessageViewListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            contentsChanged(e);
        }

        public void intervalRemoved(ListDataEvent e) {
            contentsChanged(e);
        }

        public void contentsChanged(ListDataEvent e) {
            int sel = messageList.getSelectedIndex();
            if (sel <= 0 || sel >= messageViewerModel.getSize()) {
                setSelectedIndex(-1);
            } else if (sel >= 0 && sel < messageViewerModel.getSize()) {
                setSelectedIndex(sel);
            }
        }
    }

    /**
     * Update the messageView with data for message #idx.
     * Will clear the messageView if idx is out of range.
     * @param idx The message number to focus on. Set to -1 to focus on no message
     */
    private void setSelectedIndex(int idx) {
        if (messageList.getSelectedIndex() != idx)
            messageList.setSelectedIndex(idx);
        if (idx >= 0 && idx <= messageViewerModel.getSize()) {
            messageView.setText(messageViewerModel.getMessageTextAt(idx));
        } else {
            messageView.setText("");
        }
    }

    /**
     * Create a new MessageViewer with the given title.
     * @param title
     */
    public MessageViewer(String title) {
        super(title);
        getContentPane().setLayout(new BorderLayout());

        messageViewerModel.addListDataListener(new MessageViewListener());
        messageList = new JList(messageViewerModel);
        messageList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                setSelectedIndex(messageList.getSelectedIndex());
            }
        });
        JScrollPane messageListPane = new JScrollPane(messageList);
        messageListPane.setPreferredSize(new Dimension(210, 350));

        messageView = new JTextArea(15, 30);
        messageView.setWrapStyleWord(true);
        JScrollPane messageViewPane = new JScrollPane(messageView);
        messageViewPane.setPreferredSize(new Dimension(610, 350));

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                messageViewerModel.clear();
            }
        });
        JButton hideButton = new JButton("Hide");
        hideButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MessageViewer.this.setVisible(false);
                Gui.getInstance().updateMessageViewerStatus(); // TODO: clean up this hack somehow
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(clearButton);
        buttonPanel.add(hideButton);

        getContentPane().add(messageListPane, BorderLayout.WEST);
        getContentPane().add(messageViewPane, BorderLayout.EAST);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    /** Get the underlying MessageViewerModel. */
    public MessageViewerModel getMessageViewerModel() {
        return messageViewerModel;
    }
}
