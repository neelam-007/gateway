package com.l7tech.console.sbar;

import java.awt.Insets;
import java.awt.Dimension;
import javax.swing.*;

public class TabButton extends JButton {
    public TabButton(String name) {
        super(name);
        setOpaque(true);
        setFocusPainted(false);
        setMargin(new Insets(2, 2, 2, 2));
        setMinimumSize(new Dimension(20, 20));
        setPreferredSize(new Dimension(20, 20));
    }

    public boolean isFocusTraversable() {
        return false;
    }

    public boolean isDefaultButton() {
        return false;
    }
}