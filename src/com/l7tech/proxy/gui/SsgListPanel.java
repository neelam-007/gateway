package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.gui.util.IconManager;
import org.apache.log4j.Category;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel listing known SSGs and allowing create/edit/delete.
 * User: mike
 * Date: May 29, 2003
 * Time: 3:22:24 PM
 * To change this template use Options | File Templates.
 */
public class SsgListPanel extends JPanel {
    private final Category log = Category.getInstance(SsgListPanel.class);
    private SsgListModel ssgListModel;
    private JList ssgList;
    private Action actionNewSsg;
    private Action actionEditSsg;
    private Action actionDeleteSsg;

    SsgListPanel() {
        init();
    }

    private void init() {
        setLayout(new GridBagLayout());

        final JPanel ssgListPanel = new JPanel(new BorderLayout());
        ssgListPanel.setMinimumSize(new Dimension(400, 300));

        final JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        add(toolBar,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.NORTH,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 0, 0),
                                   0, 0));
        add(ssgListPanel,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                   GridBagConstraints.SOUTH,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 0, 0),
                                   0, 0));

        ssgListModel = new SsgListModel();
        ssgList = new JList(ssgListModel);
        ssgList.setSelectedIndex(0);
        ssgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ssgList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    getActionEditSsg().actionPerformed(new ActionEvent(this, 1, "properties"));
            }
        });
        final JScrollPane ssgListPane = new JScrollPane(ssgList);
        ssgListPanel.add(ssgListPane, BorderLayout.CENTER);

        toolBar.add(new JButton(getActionNewSsg()));
        toolBar.add(new JButton(getActionEditSsg()));
        toolBar.add(new JButton(getActionDeleteSsg()));
    }

    public Action getActionDeleteSsg() {
        if (actionDeleteSsg == null) {
            actionDeleteSsg = new AbstractAction("Delete", IconManager.getRemove()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = (Ssg)ssgList.getSelectedValue();
                    if (ssg == null)
                        return;

                    Object[] options = { "Delete", "Cancel" };
                    int result = JOptionPane.showOptionDialog(null,
                                                              "Are you sure you want to remove the " +
                                                              "registration for the SSG " + ssg + "?\n" +
                                                              "This action cannot be undone.",
                                                              "Delete SSG?",
                                                              JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                                                              null, options, options[0]);
                    if (result == 0)
                        ssgListModel.removeSsg(ssg);
                }
            };
            actionDeleteSsg.putValue(Action.SHORT_DESCRIPTION, "Remove this SSG registration");
        }
        return actionDeleteSsg;
    }

    public Action getActionEditSsg() {
        if (actionEditSsg == null) {
            actionEditSsg = new AbstractAction("Properties", IconManager.getEdit()) {
                public void actionPerformed(final ActionEvent e) {
                    try {
                        final Ssg ssg = (Ssg)ssgList.getSelectedValue();
                        if (ssg != null) {
                            if (PropertyDialog.getPropertyDialogForObject(ssg).runDialog()) {
                                if (ssg.isDefaultSsg())
                                    ssgListModel.setDefaultSsg(ssg);
                                ssgListModel.editedSsg();
                            }
                        }
                    } catch (ClassNotFoundException e1) {
                        // No property editor for Ssg objects.  this can't happen
                        log.error(e1);
                    }
                }
            };
            actionEditSsg.putValue(Action.SHORT_DESCRIPTION, "View or change properties associated with this SSG");
        }
        return actionEditSsg;
    }

    public Action getActionNewSsg() {
        if (actionNewSsg == null) {
            actionNewSsg = new AbstractAction("New", IconManager.getAdd()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg newSsg = ssgListModel.createSsg();
                    newSsg.setName("New SSG");
                    if (ssgListModel.getSize() < 1)
                        newSsg.setDefaultSsg(true);
                    try {
                        if (PropertyDialog.getPropertyDialogForObject(newSsg).runDialog())
                            ssgListModel.addSsg(newSsg);
                    } catch (ClassNotFoundException e1) {
                        // No property editor for Ssg objects.  this can't happen
                        log.error(e1);
                    }
                }
            };
            actionNewSsg.putValue(Action.SHORT_DESCRIPTION, "Register a new SSG with this Client Proxy");
        }
        return actionNewSsg;
    }

    /**
     * Get the number of registered SSGs.
     * @return the number of registered SSGs.
     */
    public int getNumSsgs() {
        return ssgList.getModel().getSize();

    }
}
