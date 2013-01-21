package com.l7tech.console.util;

import com.l7tech.console.MainWindow;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A singleton class that contains icon resources.
 * todo: rework this with weak cache and icons lazy loading
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IconManager {
    /* this class classloader */
    private final ClassLoader cl = IconManager.class.getClassLoader();
    private static IconManager instance = new IconManager();
    private static final Logger logger = Logger.getLogger(IconManager.class.getName());

    public static IconManager getInstance() {
        return instance;
    }

    /**
     * default constructor
     */
    protected IconManager() {
        loadimages();
    }

    /**
     * load icon images using this instance ClassLoader.
     *
     * @see java.lang.ClassLoader
     */
    private void loadimages() {

        // icons for adding (all) and removing (all)
        iconAdd
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Add16.gif"));
        iconAddAll
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/AddAll16.gif"));
        iconRemove
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Remove16.gif"));
        iconRemoveAll
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/RemoveAll16.gif"));

        defaultEdit
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Properties16.gif"));
        defaultDelete
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/Delete16.gif"));
        defaultNew
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/New16.gif"));

        upOneLevel
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/upOneLevel.gif"));

        openFolder
          = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/FolderOpen16.gif"));

        imageNames = retrieveImageNames();
 }


    public ImageIcon getIconAdd() {
        return iconAdd;
    }

    public ImageIcon getIconAddAll() {
        return iconAddAll;
    }

    public ImageIcon getIconRemove() {
        return iconRemove;
    }

    public ImageIcon getIconRemoveAll() {
        return iconRemoveAll;
    }

    /** @return the default Edit icon */
    public ImageIcon getDefaultEditIcon() {
        return defaultEdit;
    }

    /** @return the default Delete icon */
    public ImageIcon getDefaultDeleteIcon() {
        return defaultDelete;
    }

    /** @return the default New icon */
    public ImageIcon getDefaultNewIcon() {
        return defaultNew;
    }

    /** @return the 'up one level' icon */
    public ImageIcon getUpOneLevelIcon() {
        return upOneLevel;
    }

    /** @return the 'open folder' icon */
    public ImageIcon getOpenFolderIcon() {
        return openFolder;
    }

    /**
     * @return a list of available console image names. May be empty but never null.
     */
    @NotNull
    public List<String> getImageNames() {
        return imageNames;
    }

    /**
     * Scans com/l7tech/console/resources for names of images resources.
     *
     * @return a set of image resource names found in com/l7tech/console/resources.
     */
    private List<String> retrieveImageNames() {
        final List<String> names = new ArrayList<String>();
        final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(cl);
        try {
            for (final String extension : FileUtils.getImageFileFilter().getExtensions()) {
                names.addAll(retrieveImageNamesByExtension(resourceResolver, extension));
            }
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error retrieving icon names: " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        return names;
    }

    private List<String> retrieveImageNamesByExtension(@NotNull final PathMatchingResourcePatternResolver resolver, @NotNull final String imageExtension) throws IOException {
        final List<String> names = new ArrayList<String>();
        final Resource[] resources = resolver.getResources(MainWindow.RESOURCE_PATH + "/*." + imageExtension);
        if (resources != null) {
            for (final Resource resource : resources) {
                final String path = resource.getURL().getPath();
                final int slashIndex = path.lastIndexOf("/");
                if (slashIndex > -1) {
                    final String name = path.substring(slashIndex + 1);
                    names.add(name);
                }
            }
        }
        return names;
    }

    private ImageIcon iconAdd;
    private ImageIcon iconAddAll;
    private ImageIcon iconRemove;
    private ImageIcon iconRemoveAll;

    /** the default Edit icon */
    private ImageIcon defaultEdit;
    /** the default Delete icon */
    private ImageIcon defaultDelete;
    /** the default New icon */
    private ImageIcon defaultNew;
    /** the 'up one level' icon */
    private ImageIcon upOneLevel;
    /** the 'action open folder' icon */
    private ImageIcon openFolder;
    private List<String> imageNames;
}
