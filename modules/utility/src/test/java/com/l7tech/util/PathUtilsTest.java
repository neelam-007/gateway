package com.l7tech.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PathUtilsTest {
    @Test
    public void getPathElements() {
        assertArrayEquals(new String[]{"folder","folder","folder"}, PathUtils.getPathElements("folder/folder/folder"));
    }

    @Test
    public void getPathElementsWithBackslashes() {
        assertArrayEquals(new String[]{"\\folder","\\folder","\\folder"}, PathUtils.getPathElements("\\\\folder/\\\\folder/\\\\folder"));//  \\folder/\\folder/\\folder
        assertArrayEquals(new String[]{"folder/","folder/","/folder"}, PathUtils.getPathElements("folder\\//folder\\//\\/folder"));// folder\//folder\//\/folder
        assertArrayEquals(new String[]{"folder","/folder//","folder"}, PathUtils.getPathElements("folder/\\/folder\\/\\//folder")); // folder/\/folder\/\//folder
    }

    @Test
    public void getPathElementsWithBackAndForwardSlashes() {
        assertArrayEquals(new String[]{"\\/folder/\\","\\folder","\\folder"}, PathUtils.getPathElements("\\\\\\/folder\\/\\\\/\\\\folder/\\\\folder")); // \\\/folder\/\\/\\folder/\\folder
    }

    @Test
    public void getPathElementsWithForwardSlash() {
        assertArrayEquals(new String[]{"pa/th","folder","folder"}, PathUtils.getPathElements("/pa\\/th/folder/folder"));
    }

    @Test
    public void getPathElementsWithForwardSlashPrefixInFirstElement() {
        assertArrayEquals(new String[]{"/folder","folder","folder"}, PathUtils.getPathElements("/\\/folder/folder/folder"));
        assertArrayEquals(new String[]{"/folder","folder","folder"}, PathUtils.getPathElements("\\/folder/folder/folder"));
    }

    @Test
    public void getPathElementsWithForwardSlashPrefixInAllElements() {
        assertArrayEquals(new String[]{"/folder","/folder","/folder"}, PathUtils.getPathElements("\\/folder/\\/folder/\\/folder"));
    }

    @Test
    public void getPathElementsWithForwardSlashSuffixInAllElements() {
        assertArrayEquals(new String[]{"folder/","folder/","/folder/"}, PathUtils.getPathElements("folder\\//folder\\//\\/folder\\/")); //  folder\//folder\//\/folder\/
    }

    @Test
    public void getPathElementsWithForwardSlashSuffixAfterLastElement() {
        assertArrayEquals(new String[]{"folder","folder","folder","/"}, PathUtils.getPathElements("folder/folder/folder/\\/")); // folder/folder/folder/\/
    }

    @Test
    public void getPathElementsWithForwardSlashInMiddleOfElement() {
        assertArrayEquals(new String[]{"folder","badna/../me","folder"}, PathUtils.getPathElements("folder/badna\\/..\\/me/folder"));
    }

    @Test
    public void getPathElementsRootFolder() {
        assertArrayEquals(new String[0], PathUtils.getPathElements("/"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void getPathElementsOnlyEscapeCharacter(){
        try {
            PathUtils.getPathElements("\\");
            fail("Should throw error");
        } catch(final IllegalArgumentException e){
            assertTrue(e.getMessage().startsWith("Malformed path string:"));
            throw e;
        }
    }

    @Test
    public void parseEntityPath() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/folderA/folderB/testService");
        assertEquals("/folderA/folderB", result.left);
        assertEquals("testService", result.right);
    }

    @Test
    public void parseEntityPathRootFolder() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/");
        assertEquals("/", result.left);
        assertNull(result.right);
    }

    @Test
    public void parseEntityPathEndsWithForwardSlash() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/folderA/folderB/");
        assertEquals("/folderA/folderB", result.left);
        assertNull(result.right);
    }

    @Test
    public void parseEntityPathNoForwardSlashes() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("test");
        assertNull(result.left);
        assertEquals("test", result.right);
    }

    @Test
    public void parseEntityPathForChildOfRootFolder() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/test");
        assertEquals("/", result.left);
        assertEquals("test", result.right);
    }

    @Test
    public void parseEntityPathEmpty() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("");
        assertNull(result.left);
        assertNull(result.right);
    }

    @Test
    public void parseEntityPathWhitespace() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("     ");
        assertNull(result.left);
        assertNull(result.right);
    }

    @Test
    public void parseEntityPathWithEscapedForwardSlashes() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/fol\\/derA/fol\\/derB/test");
        assertEquals("/fol/derA/fol/derB", result.left); //Is this the intended behaviour?
        assertEquals("test", result.right);
    }

    @Test
    public void parseEntityPathWithEscapedBackSlashes() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/fol\\\\derA/fol\\\\derB/test");
        assertEquals("/fol\\derA/fol\\derB", result.left); //Is this the intended behaviour?
        assertEquals("test", result.right);
    }
}
