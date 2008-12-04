package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.DeleteException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Dec 4, 2008
 */
public class NonEmptyFolderDeletionException extends DeleteException {
    public NonEmptyFolderDeletionException(String message) {
        super(message);
    }
}
