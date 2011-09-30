package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasServiceOidImpl;
import com.l7tech.message.Message;
import com.l7tech.message.SshKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.SelectorUtils;
import org.apache.sshd.server.*;
import org.apache.sshd.server.session.ServerSession;

import java.io.*;
import java.net.PasswordAuthentication;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing SFTP subsystem (heavily borrowed from org.apache.sshd.server.sftp.SftpSubsystem).
 * Customized code is near the end of this file.
 */
public class MessageProcessingSftpSubsystem implements Command, Runnable, SessionAware, FileSystemAware {

    private static final Logger logger = Logger.getLogger(MessageProcessingSftpSubsystem.class.getName());

    /**
     * Properties key for the maximum of available open handles per session.
     */
    public static final String MAX_OPEN_HANDLES_PER_SESSION = "max-open-handles-per-session";

    public static final int LOWER_SFTP_IMPL = 3; // Working implementation from v3
    public static final int HIGHER_SFTP_IMPL = 3; //  .. up to
    public static final String ALL_SFTP_IMPL = "3";


    public static final int SSH_FXP_INIT = 1;
    public static final int SSH_FXP_VERSION = 2;
    public static final int SSH_FXP_OPEN = 3;
    public static final int SSH_FXP_CLOSE = 4;
    public static final int SSH_FXP_READ = 5;
    public static final int SSH_FXP_WRITE = 6;
    public static final int SSH_FXP_LSTAT = 7;
    public static final int SSH_FXP_REALPATH = 16;
    public static final int SSH_FXP_STAT = 17;
    public static final int SSH_FXP_RENAME = 18;
    public static final int SSH_FXP_READLINK = 19;
    public static final int SSH_FXP_LINK = 21;
    public static final int SSH_FXP_BLOCK = 22;
    public static final int SSH_FXP_UNBLOCK = 23;

    public static final int SSH_FXP_STATUS = 101;
    public static final int SSH_FXP_HANDLE = 102;
    public static final int SSH_FXP_DATA = 103;
    public static final int SSH_FXP_NAME = 104;
    public static final int SSH_FXP_ATTRS = 105;

    public static final int SSH_FXP_EXTENDED = 200;
    public static final int SSH_FXP_EXTENDED_REPLY = 201;

    public static final int SSH_FX_OK = 0;
    public static final int SSH_FX_EOF = 1;
    public static final int SSH_FX_NO_SUCH_FILE = 2;
    public static final int SSH_FX_PERMISSION_DENIED = 3;
    public static final int SSH_FX_FAILURE = 4;
    public static final int SSH_FX_BAD_MESSAGE = 5;
    public static final int SSH_FX_NO_CONNECTION = 6;
    public static final int SSH_FX_CONNECTION_LOST = 7;
    public static final int SSH_FX_OP_UNSUPPORTED = 8;
    public static final int SSH_FX_INVALID_HANDLE = 9;
    public static final int SSH_FX_NO_SUCH_PATH = 10;
    public static final int SSH_FX_FILE_ALREADY_EXISTS = 11;
    public static final int SSH_FX_WRITE_PROTECT = 12;
    public static final int SSH_FX_NO_MEDIA = 13;
    public static final int SSH_FX_NO_SPACE_ON_FILESYSTEM = 14;
    public static final int SSH_FX_QUOTA_EXCEEDED = 15;
    public static final int SSH_FX_UNKNOWN_PRINCIPAL = 16;
    public static final int SSH_FX_LOCK_CONFLICT = 17;
    public static final int SSH_FX_DIR_NOT_EMPTY = 18;
    public static final int SSH_FX_NOT_A_DIRECTORY = 19;
    public static final int SSH_FX_INVALID_FILENAME = 20;
    public static final int SSH_FX_LINK_LOOP = 21;
    public static final int SSH_FX_CANNOT_DELETE = 22;
    public static final int SSH_FX_INVALID_PARAMETER = 23;
    public static final int SSH_FX_FILE_IS_A_DIRECTORY = 24;
    public static final int SSH_FX_BYTE_RANGE_LOCK_CONFLICT = 25;
    public static final int SSH_FX_BYTE_RANGE_LOCK_REFUSED = 26;
    public static final int SSH_FX_DELETE_PENDING = 27;
    public static final int SSH_FX_FILE_CORRUPT = 28;
    public static final int SSH_FX_OWNER_INVALID = 29;
    public static final int SSH_FX_GROUP_INVALID = 30;
    public static final int SSH_FX_NO_MATCHING_BYTE_RANGE_LOCK = 31;

    public static final int SSH_FILEXFER_ATTR_SIZE = 0x00000001;
    public static final int SSH_FILEXFER_ATTR_PERMISSIONS = 0x00000004;
    public static final int SSH_FILEXFER_ATTR_ACMODTIME = 0x00000008; //v3 naming convention
    public static final int SSH_FILEXFER_ATTR_ACCESSTIME = 0x00000008;
    public static final int SSH_FILEXFER_ATTR_CREATETIME = 0x00000010;
    public static final int SSH_FILEXFER_ATTR_MODIFYTIME = 0x00000020;
    public static final int SSH_FILEXFER_ATTR_ACL = 0x00000040;
    public static final int SSH_FILEXFER_ATTR_OWNERGROUP = 0x00000080;
    public static final int SSH_FILEXFER_ATTR_SUBSECOND_TIMES = 0x00000100;
    public static final int SSH_FILEXFER_ATTR_BITS = 0x00000200;
    public static final int SSH_FILEXFER_ATTR_ALLOCATION_SIZE = 0x00000400;
    public static final int SSH_FILEXFER_ATTR_TEXT_HINT = 0x00000800;
    public static final int SSH_FILEXFER_ATTR_MIME_TYPE = 0x00001000;
    public static final int SSH_FILEXFER_ATTR_LINK_COUNT = 0x00002000;
    public static final int SSH_FILEXFER_ATTR_UNTRANSLATED_NAME = 0x00004000;
    public static final int SSH_FILEXFER_ATTR_CTIME = 0x00008000;
    public static final int SSH_FILEXFER_ATTR_EXTENDED = 0x80000000;

    public static final int SSH_FILEXFER_TYPE_REGULAR = 1;
    public static final int SSH_FILEXFER_TYPE_DIRECTORY = 2;
    public static final int SSH_FILEXFER_TYPE_SYMLINK = 3;
    public static final int SSH_FILEXFER_TYPE_SPECIAL = 4;
    public static final int SSH_FILEXFER_TYPE_UNKNOWN = 5;
    public static final int SSH_FILEXFER_TYPE_SOCKET = 6;
    public static final int SSH_FILEXFER_TYPE_CHAR_DEVICE = 7;
    public static final int SSH_FILEXFER_TYPE_BLOCK_DEVICE = 8;
    public static final int SSH_FILEXFER_TYPE_FIFO = 9;


    public static final int SSH_FXF_ACCESS_DISPOSITION = 0x00000007;
    public static final int SSH_FXF_CREATE_NEW = 0x00000000;
    public static final int SSH_FXF_CREATE_TRUNCATE = 0x00000001;
    public static final int SSH_FXF_OPEN_EXISTING = 0x00000002;
    public static final int SSH_FXF_OPEN_OR_CREATE = 0x00000003;
    public static final int SSH_FXF_TRUNCATE_EXISTING = 0x00000004;
    public static final int SSH_FXF_APPEND_DATA = 0x00000008;
    public static final int SSH_FXF_APPEND_DATA_ATOMIC = 0x00000010;
    public static final int SSH_FXF_TEXT_MODE = 0x00000020;
    public static final int SSH_FXF_BLOCK_READ = 0x00000040;
    public static final int SSH_FXF_BLOCK_WRITE = 0x00000080;
    public static final int SSH_FXF_BLOCK_DELETE = 0x00000100;
    public static final int SSH_FXF_BLOCK_ADVISORY = 0x00000200;
    public static final int SSH_FXF_NOFOLLOW = 0x00000400;
    public static final int SSH_FXF_DELETE_ON_CLOSE = 0x00000800;
    public static final int SSH_FXF_ACCESS_AUDIT_ALARM_INFO = 0x00001000;
    public static final int SSH_FXF_ACCESS_BACKUP = 0x00002000;
    public static final int SSH_FXF_BACKUP_STREAM = 0x00004000;
    public static final int SSH_FXF_OVERRIDE_OWNER = 0x00008000;

    public static final int SSH_FXF_READ = 0x00000001;
    public static final int SSH_FXF_WRITE = 0x00000002;
    public static final int SSH_FXF_APPEND = 0x00000004;
    public static final int SSH_FXF_CREAT = 0x00000008;
    public static final int SSH_FXF_TRUNC = 0x00000010;
    public static final int SSH_FXF_EXCL = 0x00000020;
    public static final int SSH_FXF_TEXT = 0x00000040;

    public static final int ACE4_READ_DATA = 0x00000001;
    public static final int ACE4_LIST_DIRECTORY = 0x00000001;
    public static final int ACE4_WRITE_DATA = 0x00000002;
    public static final int ACE4_ADD_FILE = 0x00000002;
    public static final int ACE4_APPEND_DATA = 0x00000004;
    public static final int ACE4_ADD_SUBDIRECTORY = 0x00000004;
    public static final int ACE4_READ_NAMED_ATTRS = 0x00000008;
    public static final int ACE4_WRITE_NAMED_ATTRS = 0x00000010;
    public static final int ACE4_EXECUTE = 0x00000020;
    public static final int ACE4_DELETE_CHILD = 0x00000040;
    public static final int ACE4_READ_ATTRIBUTES = 0x00000080;
    public static final int ACE4_WRITE_ATTRIBUTES = 0x00000100;
    public static final int ACE4_DELETE = 0x00010000;
    public static final int ACE4_READ_ACL = 0x00020000;
    public static final int ACE4_WRITE_ACL = 0x00040000;
    public static final int ACE4_WRITE_OWNER = 0x00080000;

    public static final int S_IRUSR = 0000400;
    public static final int S_IWUSR = 0000200;
    public static final int S_IXUSR = 0000100;
    public static final int S_IRGRP = 0000040;
    public static final int S_IWGRP = 0000020;
    public static final int S_IXGRP = 0000010;
    public static final int S_IROTH = 0000004;
    public static final int S_IWOTH = 0000002;
    public static final int S_IXOTH = 0000001;
    public static final int S_ISUID = 0004000;
    public static final int S_ISGID = 0002000;
    public static final int S_ISVTX = 0001000;


    private ExitCallback callback;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private Environment env;
    private ServerSession session;
    private boolean closed = false;

    private FileSystemView root;

    private int version;
    private Map<String, Handle> handles = new HashMap<String, Handle>();


    protected static abstract class Handle {
        SshFile file;

        public Handle(SshFile file) {
            this.file = file;
        }

        public SshFile getFile() {
            return file;
        }

        public void close() throws IOException {
            file.handleClose();
        }

    }

    protected static class DirectoryHandle extends Handle {
        boolean done;

        public DirectoryHandle(SshFile file) {
            super(file);
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    protected static class FileHandle extends Handle {
        int flags;

        public FileHandle(SshFile sshFile, int flags) {
            super(sshFile);
            this.flags = flags;
        }

        public int getFlags() {
            return flags;
        }
    }


    public void setSession(ServerSession session) {
        this.session = session;
    }

    public void setFileSystemView(FileSystemView view) {
        this.root = view;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void start(Environment env) throws IOException {
        this.env = env;
        new Thread(this).start();
    }

    protected void sendHandle(int id, String handle) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_HANDLE);
        buffer.putInt(id);
        buffer.putString(handle);
        send(buffer);
    }

    protected void sendAttrs(int id, SshFile file) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_ATTRS);
        buffer.putInt(id);
        writeAttrs(buffer, file);
        send(buffer);
    }

    protected void sendAttrs(int id, SshFile file, int flags) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_ATTRS);
        buffer.putInt(id);
        writeAttrs(buffer, file, flags);
        send(buffer);
    }


    protected void sendPath(int id, SshFile f) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_NAME);
        buffer.putInt(id);
        buffer.putInt(1);
        //normalize the given path, use *nix style separator
        String normalizedPath = SelectorUtils.normalizePath(f.getAbsolutePath(), "/");
        if (normalizedPath.length() == 0) {
            normalizedPath = "/";
        }
        buffer.putString(normalizedPath);
        f = resolveFile(normalizedPath);
        if (f.getName().length() == 0) {
            f = resolveFile(".");
        }
        if (version <= 3) {
            buffer.putString(getLongName(f)); // Format specified in the specs
            buffer.putInt(0);
        } else {
            buffer.putString(f.getName()); // Supposed to be UTF-8
            writeAttrs(buffer, f);
        }
        send(buffer);
    }

    protected void sendName(int id, Collection<SshFile> files) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_NAME);
        buffer.putInt(id);
        buffer.putInt(files.size());
        for (SshFile f : files) {
            buffer.putString(f.getName());
            if (version <= 3) {
                buffer.putString(getLongName(f)); // Format specified in the specs
            } else {
                buffer.putString(f.getName()); // Supposed to be UTF-8
            }
            writeAttrs(buffer, f);
        }
        send(buffer);
    }

    private String getLongName(SshFile f) {
        String username = session.getUsername();
        if (username.length() > 8) {
            username = username.substring(0, 8);
        } else {
            for (int i = username.length(); i < 8; i++) {
                username = username + " ";
            }
        }

        long length = f.getSize();
        String lengthString = String.format("%1$#8s", length);

        StringBuilder sb = new StringBuilder();
        sb.append((f.isDirectory() ? "d" : "-"));
        sb.append((f.isReadable() ? "r" : "-"));
        sb.append((f.isWritable() ? "w" : "-"));
        sb.append((/*f.canExecute() ? "x" :*/ "-"));
        sb.append((f.isReadable() ? "r" : "-"));
        sb.append((f.isWritable() ? "w" : "-"));
        sb.append((/*f.canExecute() ? "x" :*/ "-"));
        sb.append((f.isReadable() ? "r" : "-"));
        sb.append((f.isWritable() ? "w" : "-"));
        sb.append((/*f.canExecute() ? "x" :*/ "-"));
        sb.append(" ");
        sb.append("  1");
        sb.append(" ");
        sb.append(username);
        sb.append(" ");
        sb.append(username);
        sb.append(" ");
        sb.append(lengthString);
        sb.append(" ");
        sb.append(getUnixDate(f.getLastModified()));
        sb.append(" ");
        sb.append(f.getName());

        return sb.toString();
    }

    protected void writeAttrs(Buffer buffer, SshFile file) throws IOException {
        writeAttrs(buffer, file, 0);
    }


    protected void writeAttrs(Buffer buffer, SshFile file, int flags) throws IOException {
        if (!file.doesExist()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        if (version >= 4) {
            long size = file.getSize();
            String username = session.getUsername();
            long lastModif = file.getLastModified();
            int p = 0;
            if (file.isReadable()) {
                p |= S_IRUSR;
            }
            if (file.isWritable()) {
                p |= S_IWUSR;
            }
            /*
            if (file.canExecute()) {
                p |= S_IXUSR;
            }
            */
            if (file.isFile()) {
                buffer.putInt(SSH_FILEXFER_ATTR_PERMISSIONS);
                buffer.putByte((byte) SSH_FILEXFER_TYPE_REGULAR);
                buffer.putInt(p);
            } else if (file.isDirectory()) {
                buffer.putInt(SSH_FILEXFER_ATTR_PERMISSIONS);
                buffer.putByte((byte) SSH_FILEXFER_TYPE_DIRECTORY);
                buffer.putInt(p);
            } else {
                buffer.putInt(0);
                buffer.putByte((byte) SSH_FILEXFER_TYPE_UNKNOWN);
            }
        } else {
            int p = 0;
            if (file.isFile()) {
                p |= 0100000;
            }
            if (file.isDirectory()) {
                p |= 0040000;
            }
            if (file.isReadable()) {
                p |= 0000400;
            }
            if (file.isWritable()) {
                p |= 0000200;
            }
            /*
            if (file.canExecute()) {
                p |= 0000100;
            }
            */
            if (file.isFile()) {
                buffer.putInt(SSH_FILEXFER_ATTR_SIZE| SSH_FILEXFER_ATTR_PERMISSIONS | SSH_FILEXFER_ATTR_ACMODTIME);
                buffer.putLong(file.getSize());
                buffer.putInt(p);
                buffer.putInt(file.getLastModified()/1000);
                buffer.putInt(file.getLastModified()/1000);
            } else if (file.isDirectory()) {
                buffer.putInt(SSH_FILEXFER_ATTR_PERMISSIONS | SSH_FILEXFER_ATTR_ACMODTIME);
                buffer.putInt(p);
                buffer.putInt(file.getLastModified()/1000);
                buffer.putInt(file.getLastModified()/1000);
            } else {
                buffer.putInt(0);
            }
        }
    }

    protected void sendStatus(int id, int substatus, String msg) throws IOException {
        sendStatus(id, substatus, msg, "");
    }

    protected void sendStatus(int id, int substatus, String msg, String lang) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_STATUS);
        buffer.putInt(id);
        buffer.putInt(substatus);
        buffer.putString(msg);
        buffer.putString(lang);
        send(buffer);
    }

    protected void send(Buffer buffer) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(buffer.available());
        dos.write(buffer.array(), buffer.rpos(), buffer.available());
        dos.flush();
    }

    public void destroy() {
        closed = true;
    }

    private SshFile resolveFile(String path) {
    	return this.root.getFile(path);
    }

    private final static String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May",
            "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * Get unix style date string.
     */
    private final static String getUnixDate(long millis) {
        if (millis < 0) {
            return "------------";
        }

        StringBuffer sb = new StringBuffer(16);
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);

        // month
        sb.append(MONTHS[cal.get(Calendar.MONTH)]);
        sb.append(' ');

        // day
        int day = cal.get(Calendar.DATE);
        if (day < 10) {
            sb.append(' ');
        }
        sb.append(day);
        sb.append(' ');

        long sixMonth = 15811200000L; // 183L * 24L * 60L * 60L * 1000L;
        long nowTime = System.currentTimeMillis();
        if (Math.abs(nowTime - millis) > sixMonth) {

            // year
            int year = cal.get(Calendar.YEAR);
            sb.append(' ');
            sb.append(year);
        } else {

            // hour
            int hh = cal.get(Calendar.HOUR_OF_DAY);
            if (hh < 10) {
                sb.append('0');
            }
            sb.append(hh);
            sb.append(':');

            // minute
            int mm = cal.get(Calendar.MINUTE);
            if (mm < 10) {
                sb.append('0');
            }
            sb.append(mm);
        }
        return sb.toString();
    }

    // *** CUSTOMIZED CODE STARTS BELOW ***

    private SsgConnector connector;
    private MessageProcessor messageProcessor;
    private EventChannel messageProcessingEventChannel;
    private SoapFaultManager soapFaultManager;
    private StashManagerFactory stashManagerFactory;

    public MessageProcessingSftpSubsystem(SsgConnector c, MessageProcessor mp, StashManagerFactory smf,
                                          SoapFaultManager sfm, EventChannel mpec) {
        connector = c;
        messageProcessor = mp;
        messageProcessingEventChannel = mpec;
        soapFaultManager = sfm;
        stashManagerFactory = smf;
    }

    public void run() {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(in);
            while (true) {
                int length = dis.readInt();
                if (length < 5) {
                    throw new IllegalArgumentException();
                }
                Buffer buffer = new Buffer(length + 4);
                buffer.putInt(length);
                int nb = length;
                while (nb > 0) {
                    int l = dis.read(buffer.array(), buffer.wpos(), nb);
                    if (l < 0) {
                        throw new IllegalArgumentException();
                    }
                    buffer.wpos(buffer.wpos() + l);
                    nb -= l;
                }
                process(buffer);
            }
        } catch (Throwable t) {
            if (!closed && !(t instanceof EOFException)) { // Ignore han
                logger.log(Level.SEVERE, "Exception caught in SFTP subsystem", t);
            }
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, "Could not close DataInputStream", ioe);
                }
            }
            dis = null;

            // flush and close output stream for all outstanding handles
            if (handles != null) {
                for (Handle handle : handles.values()) {
                    if (handle instanceof FileHandle) {
                        SshFile file = ((FileHandle) handle).getFile();
                        flushAndCloseQuietly(file);
                    }
                }
            }

            callback.onExit(0);
        }
    }

    protected void process(Buffer buffer) throws IOException {
        int length = buffer.getInt();
        int type = buffer.getByte();
        int id = buffer.getInt();

        // customize support for selected SFTP functions on the Gateway
        switch (type) {
            case SSH_FXP_INIT: {
                if (length != 5) {
                    throw new IllegalArgumentException();
                }
                version = id;
                if (version >= LOWER_SFTP_IMPL) {
                    version = Math.min(version, HIGHER_SFTP_IMPL);
                    buffer.clear();
                    buffer.putByte((byte) SSH_FXP_VERSION);
                    buffer.putInt(version);
                    send(buffer);
                } else {
                    // We only support version 3 (Version 1 and 2 are not common)
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "SFTP server only support versions " + ALL_SFTP_IMPL);
                }
                break;
            }
            case SSH_FXP_OPEN: {
                sshFxpOpen(buffer, id);
                break;
            }
            case SSH_FXP_CLOSE: {
                sshFxpClose(buffer, id);
                break;
            }
            case SSH_FXP_WRITE: {
                sshFxpWrite(buffer, id);
                break;
            }
            case SSH_FXP_LSTAT:
            case SSH_FXP_STAT: {
                String path = buffer.getString();
                try {
                    SshFile p = resolveFile(path);
                    sendAttrs(id, p);
                } catch (FileNotFoundException e) {
                    sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } catch (IOException e) {
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                break;
            }
            case SSH_FXP_REALPATH: {
                String path = buffer.getString();
                if (path.trim().length() == 0) {
                    path = ".";
                }
                try {
                    SshFile p = resolveFile(path);
                    sendPath(id, p);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                break;
            }

            default:
                logger.log(Level.WARNING, "Received unsupported type: {" + type + "}");
                sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Command " + type + " is unsupported on the Gateway");
        }
    }

    protected void sshFxpOpen(Buffer buffer, int id) throws IOException {
                if (session.getFactoryManager().getProperties() != null) {
                    String maxHandlesString = session.getFactoryManager().getProperties().get(MAX_OPEN_HANDLES_PER_SESSION);
                    if (maxHandlesString != null) {
                        int maxHandleCount = Integer.parseInt(maxHandlesString);
                        if (handles.size() > maxHandleCount) {
                            sendStatus(id, SSH_FX_FAILURE, "Too many open handles");
                            return;
                        }
                    }
                }

                if (version <= 4) {
                    String path = buffer.getString();
                    int pflags = buffer.getInt();
                    SshFile file = null;
                    // attrs
                    try {
                        file = resolveFile(path);
                        if (file.doesExist()) {
                            if (((pflags & SSH_FXF_CREAT) != 0) && ((pflags & SSH_FXF_EXCL) != 0)) {
                                sendStatus(id, SSH_FX_FILE_ALREADY_EXISTS, path);
                                return;
                            }
                        } else {
                            if (((pflags & SSH_FXF_CREAT) != 0)) {
                                if (!file.isWritable()) {
                                    sendStatus(id, SSH_FX_FAILURE, "Can not create " + path);
                                }
                            }
                        }
                        String acc = ((pflags & (SSH_FXF_READ | SSH_FXF_WRITE)) != 0 ? "r" : "") +
                                ((pflags & SSH_FXF_WRITE) != 0 ? "w" : "");
                        if ((pflags & SSH_FXF_TRUNC) != 0) {
                            file.truncate();
                        }
                        String handle = UUID.randomUUID().toString();

                        // start thread to process Gateway request message
                        startGatewayMessageProcessThread(connector, file);

                        handles.put(handle, new FileHandle(file, pflags)); // handle flags conversion
                        sendHandle(id, handle);
                    } catch (IOException e) {
                        flushAndCloseQuietly(file);
                        sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                    }
                } else {
                    String path = buffer.getString();
                    int acc = buffer.getInt();
                    int flags = buffer.getInt();
                    SshFile file = null;
                    // attrs
                    try {
                        file = resolveFile(path);
                        switch (flags & SSH_FXF_ACCESS_DISPOSITION) {
                            case SSH_FXF_CREATE_NEW: {
                                if (file.doesExist()) {
                                    sendStatus(id, SSH_FX_FILE_ALREADY_EXISTS, path);
                                    return;
                                } else if (!file.isWritable()) {
                                    sendStatus(id, SSH_FX_FAILURE, "Can not create " + path);
                                }
                                break;
                            }
                            case SSH_FXF_CREATE_TRUNCATE: {
                                if (file.doesExist()) {
                                    sendStatus(id, SSH_FX_FILE_ALREADY_EXISTS, path);
                                    return;
                                } else if (!file.isWritable()) {
                                    sendStatus(id, SSH_FX_FAILURE, "Can not create " + path);
                                }
                                file.truncate();
                                break;
                            }
                            case SSH_FXF_OPEN_EXISTING: {
                                if (!file.doesExist()) {
                                    if (!file.getParentFile().doesExist()) {
                                        sendStatus(id, SSH_FX_NO_SUCH_PATH, path);
                                    } else {
                                        sendStatus(id, SSH_FX_NO_SUCH_FILE, path);
                                    }
                                    return;
                                }
                                break;
                            }
                            case SSH_FXF_OPEN_OR_CREATE: {
                                break;
                            }
                            case SSH_FXF_TRUNCATE_EXISTING: {
                                if (!file.doesExist()) {
                                    if (!file.getParentFile().doesExist()) {
                                        sendStatus(id, SSH_FX_NO_SUCH_PATH, path);
                                    } else {
                                        sendStatus(id, SSH_FX_NO_SUCH_FILE, path);
                                    }
                                    return;
                                }
                                file.truncate();
                                break;
                            }
                            default:
                                throw new IllegalArgumentException("Unsupported open mode: " + flags);
                        }
                        String handle = UUID.randomUUID().toString();

                        // start thread to process Gateway request message
                        startGatewayMessageProcessThread(connector, file);

                        handles.put(handle, new FileHandle(file, flags));
                        sendHandle(id, handle);
                    } catch (IOException e) {
                        flushAndCloseQuietly(file);
                        sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                    }
                }
    }

    protected void sshFxpWrite(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long offset = buffer.getLong();
        byte[] data = buffer.getBytes();
        SshFile sshFile = null;
        try {
            Handle p = handles.get(handle);
            if (!(p instanceof FileHandle)) {
                sendStatus(id, SSH_FX_INVALID_HANDLE, handle);
            } else {
                sshFile = ((FileHandle) p).getFile();

                // write data
                pipeDataToGatewayMessageProcessor(sshFile, data, 0);

                sshFile.setLastModified(new Date().getTime());
                sendStatus(id, SSH_FX_OK, "");
            }
        } catch (IOException e) {
            flushAndCloseQuietly(sshFile);
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
        }
    }

    protected void sshFxpClose(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        try {
            Handle h = handles.get(handle);
            if (h == null) {
                sendStatus(id, SSH_FX_INVALID_HANDLE, handle, "");
            } else {
                handles.remove(handle);
                h.close();

                SshFile sshFile = ((FileHandle) h).getFile();
                flushAndCloseQuietly(sshFile);

                AssertionStatus status = getStatusFromGatewayMessageProcess(sshFile,
                        connector.getIntProperty(SshServerModule.LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS, 3));
                if (status == null || status == AssertionStatus.UNDEFINED) {
                    sendStatus(id, SSH_FX_FAILURE, "No status returned from Gateway message processing.");
                } else if (status == AssertionStatus.NONE) {
                    sendStatus(id, SSH_FX_OK, "", "");
                } else if (status == AssertionStatus.AUTH_FAILED) {
                    sendStatus(id, SSH_FX_PERMISSION_DENIED, status.toString());
                } else if (status == AssertionStatus.FAILED) {
                    sendStatus(id, SSH_FX_FAILURE, status.toString());
                } else {
                    sendStatus(id, SSH_FX_BAD_MESSAGE, status.toString());
                }
            }
        } catch (IOException e) {
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
        } catch (InterruptedException e) {
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
        } catch (ExecutionException e) {
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
        } catch (TimeoutException e) {
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
        }
    }

    /*
     * Start Gateway Message Process thread.  Thread will finish when there nothing left in the InputStream (e.g. when it has been closed).
     */
    private void startGatewayMessageProcessThread(SsgConnector connector, SshFile file) throws IOException {
        if (file instanceof VirtualSshFile) {
            final VirtualSshFile virtualSshFile = (VirtualSshFile) file;

            final PipedInputStream pis = new PipedInputStream();
            PipedOutputStream pos = new PipedOutputStream(pis);
            virtualSshFile.setPipedOutputStream(pos);

            String path = virtualSshFile.getAbsolutePath();
            if (path.indexOf('/') > -1) {
                path = path.substring(0, path.lastIndexOf('/'));
            }

            Message request = new Message();
            final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, true);
            final long requestSizeLimit = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, Message.getMaxBytes());
            String ctypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
            ContentTypeHeader ctype = ctypeStr == null ? ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.create(ctypeStr);

            request.initialize(stashManagerFactory.createStashManager(), ctype, pis, requestSizeLimit);

            // attach ssh knob
            String userName = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_USERNAME);
            String userPublicKey = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_PUBLIC_KEY);
            if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPublicKey)) {
                request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file.getName(), path, new SshKnob.PublicKeyAuthentication(userName, userPublicKey)));
            } else {
                userName = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_USERNAME);
                String userPassword = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_PASSWORD);
                if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPassword)) {
                    request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file.getName(), path, new PasswordAuthentication(userName, userPassword.toCharArray())));
                } else {
                    request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file.getName(), path, null, null));
                }
            }

            long hardwiredServiceOid = connector.getLongProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, -1);
            if (hardwiredServiceOid != -1) {
                request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
            }

            final CountDownLatch startedSignal = new CountDownLatch(1);
            final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "GatewayMessageProcessThread-" + System.currentTimeMillis());
                    thread.setDaemon(true);
                    return thread;
                }
            });

            Future<AssertionStatus> future = executorService.submit(new Callable<AssertionStatus>()
            {
                public AssertionStatus call() throws Exception {
                    AssertionStatus status = AssertionStatus.UNDEFINED;
                    String faultXml = null;
                    try {
                        try {
                            startedSignal.countDown();
                            status = messageProcessor.processMessage(context);

                            if (logger.isLoggable(Level.FINER)) {
                                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
                            }
                        } catch ( PolicyVersionException pve ) {
                            logger.log( Level.INFO, "Request referred to an outdated version of policy" );
                            faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
                        } catch ( Throwable t ) {
                            logger.log( Level.WARNING, "Exception while processing SFTP message", t );
                            faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                        }

                        if ( status != AssertionStatus.NONE ) {
                            faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                        }
                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } finally {
                        startedSignal.countDown();
                        ResourceUtils.closeQuietly(context);
                        ResourceUtils.closeQuietly(pis);
                    }
                    return status;
                }
            });
            virtualSshFile.setMessageProcessStatus(future);

            try {
                startedSignal.await();
            }
            catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new CausedIOException("Interrupted waiting for data.", ie);
            }
        }
    }

    /*
     * Pipe data to Message Processor.
     */
    private void pipeDataToGatewayMessageProcessor(final SshFile sshFile, final byte[] data, final int offset) throws IOException {
        if (sshFile instanceof VirtualSshFile) {
            final PipedOutputStream pos = ((VirtualSshFile) sshFile).getPipedOutputStream();
            pos.write(data, offset, data.length);
        }
    }

    /*
     * Flush and close VirtualSshFile's PipedOutputStream
     */
    private void flushAndCloseQuietly(SshFile sshFile) {
        if (sshFile != null && sshFile instanceof VirtualSshFile) {
            VirtualSshFile virtualSshFile = (VirtualSshFile) sshFile;
            OutputStream os = virtualSshFile.getPipedOutputStream();
            ResourceUtils.flushQuietly(os);
            ResourceUtils.closeQuietly(os);
        }
    }

    /*
     * Get status set by Gateway Message Processing
     */
    private AssertionStatus getStatusFromGatewayMessageProcess(SshFile file, long waitSeconds)
            throws InterruptedException, ExecutionException, TimeoutException {
        AssertionStatus status = null;
        if (file instanceof VirtualSshFile) {
            VirtualSshFile virtualSshFile = (VirtualSshFile) file;
            Future<AssertionStatus> future = virtualSshFile.getMessageProcessStatus();
            if (future != null) {
                status = future.get(waitSeconds, TimeUnit.SECONDS);
            }
        }
        return status;
    }
}
