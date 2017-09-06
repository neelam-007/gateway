package com.l7tech.util;

import org.junit.Test;

import java.util.List;

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
        assertEquals("/folderA", result.left);
        assertEquals("folderB", result.right);
    }

    @Test
    public void parseEntityPathMissingForwardSlashPrefix() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("folderA/folderB");
        assertEquals("/folderA", result.left);
        assertEquals("folderB", result.right);
    }

    @Test
    public void parseEntityPathNoForwardSlashes() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("test");
        assertEquals("/", result.left);
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
        assertEquals("/fol\\/derA/fol\\/derB", result.left); //Is this the intended behaviour?
        assertEquals("test", result.right);
    }

    @Test
    public void parseEntityPathWithEscapedBackSlashes() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("/fol\\\\derA/fol\\\\derB/test");
        assertEquals("/fol\\\\derA/fol\\\\derB", result.left); //Is this the intended behaviour?
        assertEquals("test", result.right);
    }

    @Test
    public void parseEntityPathWithEscapedForwardSlashesAndMissingForwardSlashPrefix() {
        final Pair<String, String> result = PathUtils.parseEntityPathIntoFolderPathAndEntityName("fol\\/derA/fol\\/derB/test");
        assertEquals("/fol\\/derA/fol\\/derB", result.left); //Is this the intended behaviour?
        assertEquals("test", result.right);
    }

    @Test
    public void getPathsForRootFolder() {
        final List<String> paths = PathUtils.getPaths("/");
        assertNotNull(paths);
        assertTrue(paths.size() == 1);
        assertTrue(paths.get(0).equals("/"));
    }

    @Test
    public void getPathsForAbsolutePath() {
        final List<String> paths = PathUtils.getPaths("/a/b/c");
        assertNotNull(paths);
        assertTrue(paths.size() == 3);
        assertTrue(paths.get(0).equals("/a"));
        assertTrue(paths.get(1).equals("/a/b"));
        assertTrue(paths.get(2).equals("/a/b/c"));
    }

    @Test
    public void getPathsForRelativePath() {
        final List<String> paths = PathUtils.getPaths("a/b/c");
        assertNotNull(paths);
        assertTrue(paths.size() == 3);
        assertTrue(paths.get(0).equals("a"));
        assertTrue(paths.get(1).equals("a/b"));
        assertTrue(paths.get(2).equals("a/b/c"));
    }

    @Test
    public void getPathsForEmptyPath() {
        final List<String> paths = PathUtils.getPaths("");
        assertNotNull(paths);
        assertTrue(paths.size() == 0);
    }

    @Test
    public void testGetEscapedPathNormal() {
        final String result = PathUtils.getEscapedPathString(new String[]{"a","b","c"});
        assertEquals(result, "/a/b/c");
    }

    @Test
    public void testGetEscapedPathForwardSlashes() {
        final String result = PathUtils.getEscapedPathString(new String[]{"/a","//b","c"});
        assertEquals(result, "/\\/a/\\/\\/b/c");
    }

    @Test
    public void testGetEscapedPathBackSlashes() {
        final String result = PathUtils.getEscapedPathString(new String[]{"\\a","\\\\b","c"});
        assertEquals(result, "/\\\\a/\\\\\\\\b/c");
    }
}
