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
    private static final Category log = Category.getInstance(MessageViewer.class);

    MessageViewerModel messageViewerModel = new MessageViewerModel();
    private JPanel messageView;
    private JList messageList;

    private class MessageViewListener implements ListDataListener {
        public void intervalAdded(final ListDataEvent e) {
            contentsChanged(e);
        }

        public void intervalRemoved(final ListDataEvent e) {
            contentsChanged(e);
        }

        public void contentsChanged(final ListDataEvent e) {
            final int sel = messageList.getSelectedIndex();
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
    private void setSelectedIndex(final int idx) {
        if (messageList.getSelectedIndex() != idx)
            messageList.setSelectedIndex(idx);
        messageView.removeAll();
        if (idx >= 0 && idx <= messageViewerModel.getSize()) {
            messageView.add(messageViewerModel.getComponentAt(idx),
                            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.CENTER,
                                                   GridBagConstraints.BOTH,
                                                   new Insets(0, 0, 0, 0), 0, 0));
        }
        messageView.invalidate();
        messageView.validate();
    }

    /**
     * Create a new MessageViewer with the given title.
     * @param title
     */
    public MessageViewer(final String title) {
        super(title);
        getContentPane().setLayout(new BorderLayout());

        messageViewerModel.addListDataListener(new MessageViewListener());
        messageList = new JList(messageViewerModel);
        messageList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                setSelectedIndex(messageList.getSelectedIndex());
            }
        });
        final JScrollPane messageListPane = new JScrollPane(messageList);
        messageListPane.setPreferredSize(new Dimension(210, 350));

        messageView = new JPanel(new GridBagLayout());
        //messageView.setPreferredSize(new Dimension(500, 400));
        final JScrollPane messageViewPane = new JScrollPane(messageView);
        messageViewPane.setPreferredSize(new Dimension(610, 350));

        final JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                messageViewerModel.clear();
                messageView.removeAll();
                messageView.invalidate();
                messageView.validate();
            }
        });
        final JButton hideButton = new JButton("Hide");
        hideButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                MessageViewer.this.setVisible(false);
                Gui.getInstance().updateMessageViewerStatus(); // TODO: clean up this hack somehow
            }
        });
        final JPanel buttonPanel = new JPanel();
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
