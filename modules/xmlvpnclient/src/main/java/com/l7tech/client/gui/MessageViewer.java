package com.l7tech.client.gui;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.client.gui.util.IconManager;

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
 */
class MessageViewer extends JFrame {
    MessageViewerModel messageViewerModel = new MessageViewerModel();
    private JPanel messageView;
    private JList messageList;
    private JCheckBox formatXmlCheckbox;
    private JLabel warningMsg = new JLabel(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/proxy/resources/Warning16.png")));


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
            JScrollPane messageViewPane = new JScrollPane(
                    messageViewerModel.getComponentAt(idx, formatXmlCheckbox.isSelected()));
            messageView.add(messageViewPane,
                            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.CENTER,
                                                   GridBagConstraints.BOTH,
                                                   new Insets(0, 0, 0, 0), 0, 0));

            //check if message was truncated
            warningMsg.setVisible(messageViewerModel.isMessageTruncated(idx));
        }
        messageView.validate();
        messageView.updateUI();
    }

    /**
     * Create a new MessageViewer with the given title.
     * @param title  title for the window.  Required.
     */
    MessageViewer(final String title) {
        super(title);
        setIconImage(IconManager.getAppImage());
        Container cp = getContentPane();
        cp.setLayout(new GridBagLayout());

        messageViewerModel.addListDataListener(new MessageViewListener());
        messageList = new JList(messageViewerModel);
        messageList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                setSelectedIndex(messageList.getSelectedIndex());
            }
        });
        final JScrollPane messageListPane = new JScrollPane(messageList,
                                                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messageList.setMinimumSize(new Dimension(150, 380));
        messageList.setMaximumSize(new Dimension(150, 32768));
        //messageList.setPreferredSize(new Dimension(150, 380));
        messageListPane.setMinimumSize(new Dimension(170, 400));
        messageListPane.setMaximumSize(new Dimension(170, 32768));

        messageView = new JPanel(new GridBagLayout());
        messageView.add(new JLabel(""));
        final JScrollPane messageViewPane = new JScrollPane(messageView);
        messageViewPane.setMinimumSize(new Dimension(250, 400));

        formatXmlCheckbox = new JCheckBox("Reformat XML");
        formatXmlCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                setSelectedIndex(messageList.getSelectedIndex());
            }
        });

        final JButton clearButton = new JButton("Clear Recent Messages");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                messageViewerModel.clear();
                messageView.removeAll();
                messageView.validate();
            }
        });
        final JButton hideButton = new JButton("Close");
        final Action hideAction = new AbstractAction() {
                    public void actionPerformed(final ActionEvent e) {
                        MessageViewer.this.setVisible(false);
                    }
                };
        hideButton.addActionListener(hideAction);
        Utilities.runActionOnEscapeKey(getRootPane(), hideAction);
        getRootPane().setDefaultButton(hideButton);
        final JPanel buttonPanel = new JPanel();
        warningMsg.setForeground(new Color(255, 102, 0));
        warningMsg.setText("Message was truncated");
        warningMsg.setVisible(false);
        buttonPanel.add(warningMsg);
        buttonPanel.add(Box.createHorizontalStrut(15));
        buttonPanel.add(formatXmlCheckbox);
        buttonPanel.add(Box.createHorizontalStrut(15));
        buttonPanel.add(clearButton);
        buttonPanel.add(hideButton);

        cp.add(messageListPane,
               new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
                                      GridBagConstraints.EAST,
                                      GridBagConstraints.VERTICAL,
                                      new Insets(0, 0, 0, 0), 0, 0));
        cp.add(messageViewPane,
               new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                                      GridBagConstraints.WEST,
                                      GridBagConstraints.BOTH,
                                      new Insets(0, 0, 0, 0), 0, 0));
        cp.add(buttonPanel,
               new GridBagConstraints(0, 1, GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                      GridBagConstraints.SOUTH,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(0, 0, 0, 0), 0, 0));

        setSize(600, 400);
        validate();
        Utilities.centerOnScreen(this);
    }

    public void setVisible(boolean vis) {
        final boolean wasVisible = isVisible();
        super.setVisible(vis);
        if (wasVisible != vis) {
            Gui.getInstance().updateMessageViewerStatus(); // TODO: clean up this hack somehow

            final MessageViewerModel mod = getModel();
            if (vis) mod.addMessage("Recording Started",
                                    "Message viewer is visible: message traffic is being recorded.\n(Errors and policies are always recorded anyway.)");

            mod.setRecordFromClient(vis);
            mod.setRecordFromServer(vis);
            mod.setRecordToClient(vis);
            mod.setRecordToServer(vis);
            mod.setRecordErrors(true);
            mod.setRecordPolicies(true);

            if (!vis) mod.addMessage("Recording Stopped",
                                     "Message viewer is hidden: message traffic is not being recorded.\n(Errors and policies are always recorded anyway.)");
        }
    }

    /* Get the underlying MessageViewerModel. */
    MessageViewerModel getModel() {
        return messageViewerModel;
    }
}
