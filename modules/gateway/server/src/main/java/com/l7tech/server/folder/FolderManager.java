package com.l7tech.server.folder;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;

/**
 * Manager interface for managing service/policy folders.
 */
public interface FolderManager extends EntityManager<Folder, FolderHeader> { }