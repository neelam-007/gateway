package com.l7tech.console.panels;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 26, 2003
 * Time: 12:18:13 PM
 * To change this template use Options | File Templates.
 */
public class MiscPanel extends JTabbedPane {

    public MiscPanel(){
        setTabPlacement(JTabbedPane.TOP);
    }

    public void updateDisplay(){
        boolean visibleFlag = false;
        int numOfRemovedItems = 0;

        // if one of items is visible, make this tabbed pane visible
        for(int i=0; i < getTabCount(); i++){
            if(getComponentAt(i).isVisible()){
                visibleFlag = true;
            }
        }

        setVisible(visibleFlag);
        validate();
        repaint();
    }

    public void removeComponent(JComponent component){

        // select the first component in the tabbed pane
        if(getTabCount() > 1){
            getComponentAt(0).setVisible(true);
            setSelectedIndex(0);
        }

        this.remove(component);

    }
}
