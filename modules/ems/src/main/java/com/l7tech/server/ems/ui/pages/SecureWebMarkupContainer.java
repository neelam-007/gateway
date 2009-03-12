package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import com.l7tech.server.ems.ui.SecureComponent;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;

/**
 * Secure version of WebMarkupContainer.
 */
public class SecureWebMarkupContainer extends WebMarkupContainer implements SecureComponent {

    //- PUBLIC

    public SecureWebMarkupContainer( final String id ) {
        super(id);
        this.attemptedOperation = null;
    }

    public SecureWebMarkupContainer( final String id, final IModel model ) {
        super(id, model);
        this.attemptedOperation = null;
    }

    public SecureWebMarkupContainer( final String id, final AttemptedOperation attemptedOperation ) {
        super(id);
        this.attemptedOperation = attemptedOperation;
    }

    public SecureWebMarkupContainer( final String id, final AttemptedOperation attemptedOperation, final IModel model ) {
        super(id, model);
        this.attemptedOperation = attemptedOperation;
    }

    @Override
    public final AttemptedOperation getAttemptedOperation() {
         return attemptedOperation;
    }

    //- PRIVATE

    private final AttemptedOperation attemptedOperation;

}
