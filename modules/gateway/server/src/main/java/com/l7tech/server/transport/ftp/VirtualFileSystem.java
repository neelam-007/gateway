package com.l7tech.server.transport.ftp;

import java.util.List;
import java.util.ArrayList;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.jetbrains.annotations.Nullable;

/**
 * Per user virtual file system.
 *
 * <p>Tracks the current directory.</p>
 *
 * @author Steve Jones
 */
class VirtualFileSystem implements FileSystemView {

    /**
     * The current path, initially null. This will start with "/" but not end with it (null for "/").
     */
    private String currentPath;

    private String changedDirectory;

    @Override
    public boolean changeWorkingDirectory(String dir) throws FtpException {
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

    @Override
    public boolean isRandomAccessible() throws FtpException {
        return false;
    }

    public FtpFile getWorkingDirectory() throws FtpException {
        return buildDirectoryFileObject(buildCurrentPath());
    }

    public String getChangedDirectory() throws FtpException {
        if (this.changedDirectory == null || this.changedDirectory.isEmpty()) {
            return "/";
        } else if (!this.changedDirectory.startsWith("/")) {
            return  "/" +  this.changedDirectory;
        } else {
            return this.changedDirectory;
        }
    }

    public void setChangedDirectory(String changedDirectory) throws FtpException {
        if (changedDirectory.endsWith("/") && !changedDirectory.equals("/")){
            this.changedDirectory = changedDirectory.substring(0, changedDirectory.lastIndexOf("/"));
        } else {
            this.changedDirectory = changedDirectory;
        }
    }

    public String getParentDirectory() {
        if (this.changedDirectory != null )
            return this.changedDirectory.contains("/") ? this.changedDirectory.substring(0, this.changedDirectory.lastIndexOf("/")) : "/";
        else
            return "/";
    }

    public void setCombinedChangedDirectory (String changedDirectory) throws FtpException {
        if (this.changedDirectory == null){
            this.changedDirectory = changedDirectory;
        } else {
            if (this.changedDirectory.equals("/")) {
                this.changedDirectory = "/" + changedDirectory;
            } else if (changedDirectory.startsWith("/") || this.changedDirectory.endsWith("/")){
                this.changedDirectory = this.changedDirectory + changedDirectory;
            } else {
                this.changedDirectory = this.changedDirectory + "/" + changedDirectory;
            }
        }
    }

    @Override
    public FtpFile getFile(String file) throws FtpException {
        FtpFile fileObject;

        switch (file) {
            case ".":
            case "./":
                fileObject = buildDirectoryFileObject(buildCurrentPath());
                break;
            case "..":
            case "../":
                fileObject = buildDirectoryFileObject(combinePaths(buildCurrentPath(), ".."));
                break;
            default:
                fileObject = buildFileObject(buildCurrentPath(), file);
                break;
        }

        return fileObject;
    }

    @Override
    public FtpFile getHomeDirectory() throws FtpException {
        return buildDirectoryFileObject("/");
    }

    @Override
    public void dispose() {
    }

    private String buildCurrentPath() {
        return currentPath == null ? "/" : currentPath;
    }

    /**
     * Generated a path based on current and the new path
     *
     * @param current the current directory (could be null)
     * @param path the new path (not "/")
     */
    private String combinePaths(@Nullable String current, String path) {
        String outPath;
        String cdPath = normalize(path, false);

        if (cdPath.endsWith("/"))
            cdPath = cdPath.substring(0, cdPath.length()-1);        

        if (current == null) {
            if (cdPath.startsWith("/"))
                outPath = cdPath;
            else
                outPath = "/" + cdPath;
        } else {
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
            List<String> partList = new ArrayList<>(parts.length);

            for (String part1 : parts) {
                switch (part1) {
                    case ".":
                    case "":
                        break;
                    case "..":
                        if (!absolute && (partList.isEmpty() || "..".equals(partList.get(partList.size() - 1)))) {
                            partList.add(part1);
                        } else {
                            if (!partList.isEmpty())
                                partList.remove(partList.size() - 1);
                        }
                        break;
                    default:
                        partList.add(part1);
                        break;
                }
            }

            StringBuilder pathBuffer = new StringBuilder();

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

    private FtpFile buildFileObject(final String path, final String file) {
        return new VirtualFileObject(true, combinePaths(path, file));
    }

    private FtpFile buildDirectoryFileObject(final String path) {
        return new VirtualFileObject(false, path);
    }
}
