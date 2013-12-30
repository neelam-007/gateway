package com.l7tech.common.ftp;

/**
 * Supported FTP Commands
 * 
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public enum FtpCommand {

    /* Upload commands */
    APPE("Append"),
    STOR("Store"),
    STOU("Store unique"),

    /* Download commands */
    RETR("Retrieve"),

    /* List commands */
    LIST("List"),
    MLSD("Machine list directory"),
    NLST("Name list"),

    /* Simple commands */
    CDUP("Change to parent directory"),
    CWD("Change working directory"),
    DELE("Delete"),
    MDTM("Modification time"),
    MLST("Machine list"),
    MKD("Make directory"),
    NOOP("No operation"),
    PWD("Print working directory"),
    RMD("Remove directory"),
    SIZE("Size of file");

    private final String description;
    
    FtpCommand(String description) {
         this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
