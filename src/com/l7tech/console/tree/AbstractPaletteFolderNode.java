package com.l7tech.console.tree;

import java.util.Comparator;

/**
 * User: megery
 * Date: Mar 9, 2006
 * Time: 3:05:19 PM
 */
public abstract class AbstractPaletteFolderNode extends AbstractAssertionPaletteNode{
    private final String name;

    protected AbstractPaletteFolderNode(String name) {
        this(name, null, null);
    }

    public AbstractPaletteFolderNode(String name, Object object) {
        this(name, object, null);

    }

    protected AbstractPaletteFolderNode(String name, Object object, Comparator c) {
        super(object, c);
        this.name = name;
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    protected String iconResource(boolean open) {
        if (open) return getOpenIconResource();
        return getClosedIconResource();
    }

    protected String getOpenIconResource() {
        return "com/l7tech/console/resources/folderOpen.gif";
    }
    protected String getClosedIconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }
}
