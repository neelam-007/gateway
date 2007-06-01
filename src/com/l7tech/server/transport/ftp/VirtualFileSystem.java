package com.l7tech.server.transport.ftp;

import java.util.List;
import java.util.ArrayList;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FileObject;

/**
 * Per user virtual file system.
 *
 * <p>Tracks the current directory.</p>
 *
 * @author Steve Jones
 */
class VirtualFileSystem implements FileSystemView {

    //- PUBLIC

    public boolean changeDirectory(String dir) throws FtpException {
        boolean changed = false;                                      

        if (dir != null) {
            if ("/".equals(dir)) {
                changed = true;
                currentPath = null;
            }
            else if (dir.startsWith("/")) {
                changed = true;
                currentPath = combinePaths(null, dir);
            }
            else {
                changed = true;
                currentPath = combinePaths(currentPath, dir);
            }
        }

        return changed;
    }

    public boolean isRandomAccessible() throws FtpException {
        return false;
    }

    public FileObject getCurrentDirectory() throws FtpException {
        return buildDirectoryFileObject(buildCurrentPath());
    }

    public FileObject getFileObject(String file) throws FtpException {
        FileObject fileObject;
        if (file.equals(".") || file.equals("./")) {
            fileObject = buildDirectoryFileObject(buildCurrentPath());
        }
        else if (file.equals("..") || file.equals("../")) {
            fileObject = buildDirectoryFileObject(combinePaths(buildCurrentPath(), ".."));
        }
        else {
            fileObject = buildFileObject(buildCurrentPath(), file);
        }
        return fileObject;
    }

    public FileObject getHomeDirectory() throws FtpException {
        return buildDirectoryFileObject("/");
    }

    public void dispose() {
    }

    //- PRIVATE

    /**
     * The current path, initially null. This will start with "/" but not end with it (null for "/").
     */
    private String currentPath;

    private String buildCurrentPath() {
        return currentPath == null ? "/" : currentPath;
    }

    /**
     * Generated a path based on current and the new path
     *
     * @param current the current directory (could be null)
     * @param path the new path (not "/")
     */
    private String combinePaths(String current, String path) {
        String outPath = null;
        String cdPath = normalize(path, false);

        if (cdPath.endsWith("/"))
            cdPath = cdPath.substring(0, cdPath.length()-1);        

        if (current == null) {
            if (cdPath.startsWith("/"))
                outPath = cdPath;
            else
                outPath = "/" + cdPath;
        }
        else {
            if (cdPath.startsWith("/"))
                outPath = cdPath;
            else
                outPath = current + "/" + cdPath;
        }

        return normalize(outPath, true);
    }

    /**
     * Remove any "." or ".." from a path
     *
     * @param path The path to normalize
     * @param absolute True if this is an absolute path (as opposed to relative)
     * @return The normalized path (if relative it may still have .. at the start)
     */
    private String normalize(String path, boolean absolute) {
        String normalized = path;

        if (normalized != null) {
            String[] parts = normalized.split("/");
            List<String> partList = new ArrayList(parts.length);

            for (int p=0; p<parts.length; p++) {
                if (".".equals(parts[p]) || "".equals(parts[p])) {
                    continue;
                }
                else if ("..".equals(parts[p])) {
                    if (!absolute && (partList.isEmpty() || "..".equals(partList.get(partList.size()-1)))) {
                        partList.add(parts[p]);
                    }
                    else {
                        if (!partList.isEmpty())
                            partList.remove(partList.size()-1);
                    }
                }
                else {
                    partList.add(parts[p]);
                }
            }

            StringBuffer pathBuffer = new StringBuffer();
            if (absolute) pathBuffer.append('/');
            for (String part : partList) {
                pathBuffer.append(part);    
                pathBuffer.append('/');
            }

            normalized = pathBuffer.toString();
            if (normalized.length() > 1) {
                normalized = normalized.substring(0, normalized.length()-1);
            }
        }

        return normalized;
    }

    private FileObject buildFileObject(final String path, final String file) {
        return new VirtualFileObject(true, combinePaths(path, file));
    }

    private FileObject buildDirectoryFileObject(final String path) {
        return new VirtualFileObject(false, path);
    }
}
