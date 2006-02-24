package com.l7tech.console.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;

/**
 * Accept a file drop and pass File objects to a listener.
 *
 * @author $Author$
 * @version $Revision$
 */
public class FileDropTransferHandler extends TransferHandler {

    //- PUBLIC

    /**
     * Create a file drop handler with the given listener.
     *
     * @param fileDropListener the listener (must not be null)
     * @param filter optional file name filter (may be null)
     */
    public FileDropTransferHandler(FileDropListener fileDropListener, FilenameFilter filter) {
        if(fileDropListener==null) throw new NullPointerException("fileDropListener must not be null");
        this.listener = fileDropListener;
        this.filter = filter;
    }

    /**
     * Accepts drops with mime types of text/uri-list.
     *
     * @param comp ignored
     * @param transferFlavors DataFlavors to check.
     */
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return listener.isDropEnabled() &&
                hasAcceptableDataFlavour(transferFlavors);
    }

    /**
     * Import data from the given transferable.
     *
     * @param comp ignored
     * @param transferable the transferred data
     */
    public boolean importData(JComponent comp, Transferable transferable) {
        boolean imported = false;

        if(listener.isDropEnabled()) {
            String[] fileUris = getFileUris(transferable);

            if(fileUris==null || fileUris.length==0) {
                logger.warning("No files found in dropped data.");
            }
            else {
                try {
                    File[] files = new File[fileUris.length];
                    for (int i = 0; i < files.length; i++) {
                        files[i] = new File(new URI(fileUris[i]));
                    }

                    if(filter!=null) {
                        List filtered = new ArrayList();
                        for (int i = 0; i < files.length; i++) {
                            File file = files[i];
                            if(filter.accept(file.getParentFile(), file.getName())) {
                                filtered.add(file);
                            }
                        }
                        files = (File[]) filtered.toArray(new File[filtered.size()]);
                    }

                    if(files.length>0) {
                        imported = listener.acceptFiles(files);
                    }
                }
                catch(URISyntaxException urise) {
                    logger.log(Level.WARNING, "Error parsing file URI.", urise);
                }
            }
        }

        return imported;
    }

    /**
     * Disallows export of data.
     */
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
    }

    /**
     * Iterface implemented by the file drop owner to accept files.
     */
    public interface FileDropListener {
        boolean isDropEnabled();
        boolean acceptFiles(File[] files);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FileDropTransferHandler.class.getName());

    private final FileDropListener listener;
    private final FilenameFilter filter;

    /**
     * Flavour, with a U.
     *
     * @return true if (any of) the flavour is acceptable.
     */
    private boolean hasAcceptableDataFlavour(DataFlavor[] df) {
        boolean acceptable = false;

        for (int i = 0; i < df.length; i++) {
            DataFlavor dataFlavor = df[i];
            String mimeType = dataFlavor.getMimeType();

            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Checking mime type for dropped data '"+mimeType+"'.");
            }

            if(mimeType.toLowerCase().equals("text/uri-list")
            || mimeType.toLowerCase().startsWith("text/uri-list;")) {
                acceptable = true;
            }
        }

        return acceptable;
    }

    /**
     *
     */
    private DataFlavor[] getAcceptableDataFlavours(DataFlavor[] df) {
        List acceptable = new ArrayList();

        for (int i = 0; i < df.length; i++) {
            DataFlavor dataFlavour = df[i];
            if(hasAcceptableDataFlavour(new DataFlavor[]{dataFlavour})) {
                acceptable.add(dataFlavour);
            }
        }

        return (DataFlavor[]) acceptable.toArray(new DataFlavor[acceptable.size()]);
    }

    /**
     *
     */
    private String[] getFileUris(Transferable transferable) {
        String[] fileUris = null;
        DataFlavor[] potentialFlavours = getAcceptableDataFlavours(transferable.getTransferDataFlavors());
        for (int i = 0; i < potentialFlavours.length; i++) {
            DataFlavor potentialFlavour = potentialFlavours[i];
            try {
                Object data = transferable.getTransferData(potentialFlavour);
                if(data instanceof String) {
                    String fileUriList = (String) data;
                    StringTokenizer str = new StringTokenizer(fileUriList);
                    int tokenCount = str.countTokens();
                    String[] workingUris = new String[tokenCount];
                    for(int t=0; t<tokenCount; t++) {
                        workingUris[t] = str.nextToken();
                    }
                    fileUris = workingUris;
                    break;
                }
                else {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest("Ignoring dropped data of unsupported type '"+
                                (data==null ? "null" : data.getClass().toString())+"'.");
                    }
                }
            }
            catch(IOException ioe) {
                logger.log(Level.WARNING, "Error getting transfer data.", ioe);
            }
            catch(UnsupportedFlavorException ufe) {
                logger.log(Level.WARNING, "Error getting transfer data.", ufe);
            }
        }

        return fileUris;
    }
}
