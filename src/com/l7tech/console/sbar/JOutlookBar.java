/**
 */
package com.l7tech.console.sbar;


import com.l7tech.console.action.BaseAction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.BevelBorder;

public class JOutlookBar extends JPanel implements ActionListener {
    protected Vector buttons = new Vector();
    protected Vector names = new Vector();
    protected Vector views = new Vector();

    public JOutlookBar() {
        setLayout(new ContextLayout());
        setBorder(new BevelBorder(BevelBorder.LOWERED));
        setPreferredSize(new Dimension(80, 80));
    }

    /**
     * outlook buttons and here is the code that is called when button is pressed.
     * param		context - its folder (some call it context) name
     * param		action - the Action called by button when pressed
     */
    public void addIcon(String context, Action action) {
        int index;

        JToolBar view;
        if ((index = names.indexOf(context)) > -1) {
            view = (JToolBar) views.get(index);
        } else {
            view = new JToolBar(JToolBar.HORIZONTAL);
            view.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            view.setFloatable(false);
            view.setLayout(new VerticalFlowLayout());
            names.add(context);
            views.add(view);
            addTab(context, new ScrollingPanel(view));
        }

        JButton button = new JButton(action);
        Icon largeIcon = (Icon)action.getValue(BaseAction.LARGE_ICON);
        if (largeIcon != null)
            button.setIcon(largeIcon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);

        view.add(button);
        doLayout();
    }

    public void setIndex(int index) {
        ((ContextLayout) getLayout()).setIndex(this, index);
    }

    public void addTab(String name, Component comp) {
        JButton button = new TabButton(name);
        add(button, comp);
        buttons.addElement(button);
        button.addActionListener(this);
    }

    public void removeTab(JButton button) {
        button.removeActionListener(this);
        buttons.removeElement(button);
        remove(button);
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        for (int i = 0; i < buttons.size(); i++) {
            if (source == buttons.elementAt(i)) {
                setIndex(i + 1);
                return;
            }
        }
    }
}