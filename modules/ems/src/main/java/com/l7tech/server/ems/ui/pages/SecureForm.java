package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.server.ems.ui.SecureComponent;

/**
 * Secure version of Form.
 */
public class SecureForm<T> extends Form<T> implements SecureComponent {

    //- PUBLIC

    public SecureForm( final String id ) {
        super(id);
    }

    public SecureForm( final String id, final IModel<T> model ) {
        super(id, model);
    }


    public SecureForm( final String id, final AttemptedOperation attemptedOperation ) {
        super(id);
        this.attemptedOperation = attemptedOperation;
    }

    public SecureForm( final String id, final AttemptedOperation attemptedOperation, final IModel<T> model ) {
        super(id, model);
        this.attemptedOperation = attemptedOperation;
    }

    @Override
     public final AttemptedOperation getAttemptedOperation() {
         return attemptedOperation;
     }

     public SecureForm add( final AttemptedOperation attemptedOperation ) {
         this.attemptedOperation = attemptedOperation;
         return this;
     }

     //- PRIVATE

     private AttemptedOperation attemptedOperation;

}
