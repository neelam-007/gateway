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
        }

        public void intervalRemoved(ListDataEvent e) {
            int sel = messageList.getSelectedIndex();
            if (sel <= 0 || sel >= messageViewerModel.getSize()) {
                setSelectedIndex(-1);
            } else if (sel >= e.getIndex0() && sel <= e.getIndex1()) {
                setSelectedIndex(sel);
            }
        }

        public void contentsChanged(ListDataEvent e) {
            int sel = messageList.getSelectedIndex();
            if (sel <= 0 || sel >= messageViewerModel.getSize()) {
                setSelectedIndex(-1);
            } else if (sel >= e.getIndex0() && sel <= e.getIndex1()) {
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
            messageView.setText(messageViewerModel.getXmlAt(idx));
        } else {
            messageView.setText("");
        }

        log.info("selected msg #" + idx);
    }

    /**
     * Create a new MessageViewer with the given title.
     * @param title
     */
    MessageViewer(String title) {
        super(title);
        getContentPane().setLayout(new BorderLayout());
        messageViewerModel.addListDataListener(new MessageViewListener());
        JPanel buttonPanel = new JPanel();
        messageList = new JList(messageViewerModel);
        messageList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                setSelectedIndex(messageList.getSelectedIndex());
            }
        });
        messageView = new JTextArea(15, 30);
        messageView.setWrapStyleWord(true);
        JScrollPane messageListPanel = new JScrollPane(messageList);
        JScrollPane messageViewPanel = new JScrollPane(messageView);
        messageListPanel.setPreferredSize(new Dimension(450, 110));
        messageViewPanel.setPreferredSize(new Dimension(450, 110));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                messageViewerModel.clear();
            }
        });
        buttonPanel.add(clearButton);
        getContentPane().add(messageListPanel, BorderLayout.WEST);
        getContentPane().add(messageViewPanel, BorderLayout.EAST);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    /** Get the underlying MessageViewerModel. */
    public MessageViewerModel getMessageViewerModel() {
        return messageViewerModel;
    }
}
