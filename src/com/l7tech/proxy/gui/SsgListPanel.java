package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
    private DefaultListModel ssgListModel;
    private JList ssgList;

    SsgListPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        JPanel ssgListPanel = new JPanel();

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        add(toolBar, BorderLayout.NORTH);
        add(ssgListPanel, BorderLayout.CENTER);

        ssgListModel = new DefaultListModel();
        ssgListModel.addElement(new Ssg("Main SSG", "SSG0", "http://localhost:9898/", "", ""));
        ssgListModel.addElement(new Ssg("Alternate SSG", "SSG1", "http://localhost:9898/", "", ""));
        ssgList = new JList(ssgListModel);
        ssgList.setSelectedIndex(0);
        ssgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ssgList.setPreferredSize(new Dimension(350, 300));
        JScrollPane ssgListPane = new JScrollPane(ssgList);
        ssgListPanel.add(ssgListPane);

        toolBar.add(new AbstractAction("New", IconManager.getAdd()) {
            public void actionPerformed(ActionEvent e) {
            }
        });

        toolBar.add(new AbstractAction("Edit", IconManager.getEdit()) {
            public void actionPerformed(ActionEvent e) {
                try {
                    PropertyDialog.getPropertyDialogForObject(ssgList.getSelectedValue()).show();
                } catch (ClassNotFoundException e1) {
                    log.error(e1);
                }
            }
        });

        toolBar.add(new AbstractAction("Delete", IconManager.getRemove()) {
            public void actionPerformed(ActionEvent e) {
            }
        });
    }
}
