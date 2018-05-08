package com.l7tech.util;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class provides related path operations.
 */
public class PathUtils {

    /**
     * Note: this method is the same as the method in PathUtils from GMU Project, with minor modification.
     *
     * Reads an escaped path string and returns the elements of the path.
     * Elements of a path should be escaped as follows:
     *      "\" is escaped as "\\"
     *      "/" is escaped as "\/"
     * Elements are separated by a single "/"
     * eg: fold\/\\der/\/\\folder//\element  => [fol/\der],[/\folder],[\element]
     *
     * @param path a path containing path elements
     * @return an array of elements in the path
     */
    @NotNull
    public static String[] getPathElements(@NotNull final String path) {
        final List<String> pathElementsList = new ArrayList<>();
        StringBuffer element = new StringBuffer();

        for (int i =  0; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '/') {
                if (element.length() > 0) {
                    pathElementsList.add(element.toString());
                }
                element = new StringBuffer();
            } else if (c == '\\'){
                ++i;
                if (i >= path.length()) {
                    throw new IllegalArgumentException("Malformed path string: " + path);
                }
                element.append(path.charAt(i));
            } else{
                element.append(c);
            }
        }
        if (element.length() > 0) {
            pathElementsList.add(element.toString());
        }
        return pathElementsList.toArray(new String[pathElementsList.size()]);
    }

    /**
     * Parse a path and get a folder path and an entity name.
     *
     * If the given path is not prefixed with '/', it will be added as a prefix to the folder path.
     *
     * @param path: an entity path must not be null.
     * @return a pair of folder path and entity name.
     */
    @NotNull
    public static Pair<String, String> parseEntityPathIntoFolderPathAndEntityName(@NotNull final String path) {
        if (StringUtils.isBlank(path)) return new Pair<>(null, null);
        if (! path.contains("/")) return new Pair<>("/", path);
        if (path.equals("/")) return new Pair<>("/",null);
        // After the above two checks, a '/' is guaranteed to be in the path string.
        final String absFolderPath = path.startsWith("/")? path : ("/" + path);

        String fpath;
        String entityName;

        // Check if there is an escaping char, '\'.
        final String[] pathElements = getPathElements(absFolderPath);
        final int size = pathElements.length;
        assert size > 0;

        final String escapedString = getEscapedPathString(Arrays.copyOf(pathElements, size-1));
        fpath = StringUtils.isBlank(escapedString) ? "/" : escapedString;
        entityName = pathElements[size - 1];
        return new Pair<>(fpath, entityName);
    }

    /**
     * Converts a path array holding all the folders/folderable entities into a string with
     * escaped characters.
     * ie: [/a,\b,c] -> /\/a/\\b/c
     * @param path The path array containing folder names.
     * @return A string with escaped characters.
     */
    public static String getEscapedPathString(final String[] path){
        StringBuffer pathStr = new StringBuffer();
        StringBuffer element = new StringBuffer();
        for(String elementStr: path){
            for(int i =  0; i < elementStr.length(); ++i){
                char c  = elementStr.charAt(i);
                if(c == '\\' ){
                    element.append("\\\\");
                }else if (c == '/'){
                    element.append("\\/");
                }else{
                    element.append(c);
                }
            }
            pathStr.append('/');
            pathStr.append(element.toString());
            element = new StringBuffer();
        }
        return pathStr.toString();
    }

    /**
     * Generate all individual paths based on given a long path.
     * For example, /a/b/c/d will return /a, /a/b, /a/b/c, and /a/b/c/d.
     *
     * @param longPath a single path
     * @return a list of paths, which are sub-path of longPath, including itself of longPath.
     */
    @NotNull
    public static List<String> getPaths(@NotNull String longPath) {
        final String[] pathElements = PathUtils.getPathElements(longPath);
        final int size = pathElements.length;
        final List<String> paths = new ArrayList<>(size);

        // Handle the path is a single '/'.
        if (longPath.equals("/")) {
            paths.add("/");
            return paths;
        }

        String path = longPath.startsWith("/")? "/" : "";
        for (int i = 0; i < size; i++) {
            path += pathElements[i];
            paths.add(path);

            if (i < size - 1) {
                path += "/";
            }
        }

        return paths;
    }
}
