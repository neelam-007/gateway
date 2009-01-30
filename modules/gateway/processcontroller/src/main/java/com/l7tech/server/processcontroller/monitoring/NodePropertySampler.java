package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.io.IOUtils;
import com.l7tech.server.management.config.monitoring.ComponentType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Superclass for property samplers that sample properties specific to the node managed by a single PC instance.
 */
public abstract class NodePropertySampler<V extends Serializable> extends PropertySampler<V> {
    public NodePropertySampler(String componentId, String propertyName) {
        super(ComponentType.NODE, componentId, propertyName);
    }

    /**
     * Reads the specified file (which is expected to be short enough to read quickly and fit entirely in
     * memory), applys the specified regex (which is expected to contain a single capture group), and returns
     * the captured value converted to a Long.
     *
     * @param path path of a file to open, ie "/proc/stat".  Required.
     * @param regex  regex to match, ie Pattern.compile("^btime\\s+(\\d+)$").  Required.
     * @return the matched value as a long.  Never null.
     * @throws PropertySamplingException if the file can't be opened or read, the regex can't be matched,
     *            the regex contains no capture groups, or the captured match value can't be converted to a long.
     */
    protected long matchNumberFromFile(String path, Pattern regex) throws PropertySamplingException {
        try {
            String fileContent = new String(IOUtils.slurpFile(new File(path)));
            return matchNumber(fileContent, path, regex);
        } catch (IOException e) {
            throw new PropertySamplingException(e);
        }
    }

    /**
     * Examines the specified file contents or program output, applys the specified regex (which is expected
     * to contain a single capture group), and returns the captured value converted to a long.
     *
     * @param contentToMatch content of file or program output to match.  Required.
     * @param location  the location from whence came the content, for constructing error messages;
     *                      ie "/proc/stat" or "/usr/bin/vmstat".  Required.
     * @param regex  regex to match, ie Pattern.compile("^btime\\s+(\\d+)$").  Required.
     * @return the matched value as a long.  Never null.
     * @throws PropertySamplingException if the file can't be opened or read, the regex can't be matched,
     *            the regex contains no capture groups, or the captured match value can't be converted to a long.
     */
    protected long matchNumber(String contentToMatch, String location, Pattern regex) throws PropertySamplingException {
        try {
            Matcher matcher = regex.matcher(contentToMatch);
            if (!matcher.matches())
                throw new PropertySamplingException("Unable to find " + propertyName + " in " + location);
            if (matcher.groupCount() < 1)
                throw new PropertySamplingException("Unable to find " + propertyName + " in " + location + ": regex contains no capture groups");
            String freeStr = matcher.group(1);
            return Long.parseLong(freeStr);
        } catch (NumberFormatException e) {
            // Regex only passes digits, so this can only happen if free swap exceeps 2^64
            throw new PropertySamplingException(e);
        }
    }
}
