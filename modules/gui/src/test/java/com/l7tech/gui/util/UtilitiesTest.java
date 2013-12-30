package com.l7tech.gui.util;

import org.junit.Test;

import javax.swing.*;

import static junit.framework.Assert.assertEquals;

/**
 *
 * @author Yuri
 */
public class UtilitiesTest {
    @Test
    public void testRemoveColonFromLabel() throws Exception {
        JLabel label = new JLabel("test label : *");
        assertEquals("test label", Utilities.removeColonFromLabel(label));
        label.setText("test label 2:");
        assertEquals("test label 2", Utilities.removeColonFromLabel(label));
    }
}
