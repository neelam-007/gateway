package com.l7tech.util;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    public static String[] getPathElements(final String path) {
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
     * Parse a path and get a folder path and an entity name
     *
     * @param path: an entity path must not be null.
     * @return a pair of folder path and entity name.
     */
    @NotNull
    public static Pair<String, String> parseEntityPathIntoFolderPathAndEntityName(@Nullable final String path) {
        if (StringUtils.isBlank(path)) return new Pair<>(null, null);
        if (! path.contains("/")) return new Pair<>(null, path);
        if (path.endsWith("/")) return new Pair<>(path.substring(0, path.length() == 1? 1: path.length() - 1), null);
        // After the above three checks, a '/' is guaranteed to be in the middle of the path string.

        // Check if there is an escaping char, '\'.
        if (! path.contains("\\")) {
            final int idxOfLastSlash = path.lastIndexOf('/');
            return new Pair<>(path.substring(0, idxOfLastSlash == 0? 1 : idxOfLastSlash), path.substring(idxOfLastSlash + 1));
        } else {
            final String[] pathElements = getPathElements(path);
            final int size = pathElements.length;
            assert size > 1;

            final StringBuffer folderPath = path.startsWith("/")? new StringBuffer("/") : new StringBuffer();
            folderPath.append(pathElements[0]);

            for (int i = 1; i < size - 1; i++) {
                folderPath.append("/").append(pathElements[i]);
            }

            return new Pair<>(folderPath.toString(), pathElements[size - 1]);
        }
    }
}
