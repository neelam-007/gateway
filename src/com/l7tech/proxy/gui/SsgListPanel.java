package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

import org.apache.log4j.Category;

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

    SsgListPanel() {
        init();
    }

    private void init() {
        setLayout(new GridBagLayout());

        JPanel ssgListPanel = new JPanel(new BorderLayout());
        ssgListPanel.setMinimumSize(new Dimension(400, 300));

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
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
        JScrollPane ssgListPane = new JScrollPane(ssgList);
        ssgListPanel.add(ssgListPane, BorderLayout.CENTER);

        toolBar.add(new AbstractAction("New", IconManager.getAdd()) {
            public void actionPerformed(ActionEvent e) {
                Ssg newSsg = new Ssg();
                newSsg.setName("New SSG");
                try {
                    if (PropertyDialog.getPropertyDialogForObject(newSsg).runDialog())
                        ssgListModel.addSsg(newSsg);
                } catch (ClassNotFoundException e1) {
                    // No property editor for Ssg objects.  this can't happen
                    log.error(e1);
                }
            }
        });

        toolBar.add(new AbstractAction("Edit", IconManager.getEdit()) {
            public void actionPerformed(ActionEvent e) {
                try {
                    Ssg ssg = (Ssg)ssgList.getSelectedValue();
                    if (ssg != null) {
                        if (PropertyDialog.getPropertyDialogForObject(ssgList.getSelectedValue()).runDialog())
                            ssgListModel.editedSsg(ssg);
                    }
                } catch (ClassNotFoundException e1) {
                    // can't happen
                    log.error(e1);
                }
            }
        });

        toolBar.add(new AbstractAction("Delete", IconManager.getRemove()) {
            public void actionPerformed(ActionEvent e) {
                Ssg ssg = (Ssg)ssgList.getSelectedValue();
                if (ssg != null) {
                    ssgListModel.removeSsg(ssg);
                }
            }
        });
    }


}
