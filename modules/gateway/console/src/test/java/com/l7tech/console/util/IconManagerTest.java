package com.l7tech.console.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class IconManagerTest {
    @Test
    public void getIconNames() {
        final List<String> iconNames = IconManager.getInstance().getImageNames();
        assertEquals(3, iconNames.size());
        assertTrue(iconNames.contains("testGIF.gif"));
        assertTrue(iconNames.contains("testJPG.jpg"));
        assertTrue(iconNames.contains("testPNG.png"));
    }
}
