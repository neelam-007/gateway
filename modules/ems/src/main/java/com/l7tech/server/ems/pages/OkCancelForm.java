package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

/**
 * Form that includes a feedback panel and ok / cancel buttons.
 *
 * @author steve
 */
public abstract class OkCancelForm extends Form {

    //- PUBLIC

    /**
     * Create an OkCancelForm without a modal container.
     *
     * @param id The identifier for this component.
     * @param feedbackId The identifier for the nested feedback component.
     * @param okId The identifier for the nested OK button.
     * @param cancelId The identifier for the nested Cancel button.
     */
    public OkCancelForm( final String id,
                         final String feedbackId,
                         final String okId,
                         final String cancelId ) {
        this( id, feedbackId, okId, cancelId, null );
    }

    /**
     * Create an OkCancelForm with modal container.
     *
     * <p>The modal window is closed on successfull form processing.</p>
     *
     * @param id The identifier for this component.
     * @param feedbackId The identifier for the nested feedback component.
     * @param okId The identifier for the nested OK button.
     * @param cancelId The identifier for the nested Cancel button.
     * @param window The modal window that contains this form.
     */
    public OkCancelForm( final String id,
                         final String feedbackId,
                         final String okId,
                         final String cancelId,
                         final ModalWindow window ) {
        super( id );
        setOutputMarkupId(true);

        final FeedbackPanel feedback = new FeedbackPanel(feedbackId);
        feedback.setOutputMarkupId(true);

        AjaxFallbackButton okButton = new AjaxFallbackButton( okId, this ) {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                if ( ajaxRequestTarget != null ) {
                    ajaxRequestTarget.addComponent( feedback );
                }

                OkCancelForm.this.onSubmit( ajaxRequestTarget, form, window );
            }

            @Override
            protected void onError( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                if ( ajaxRequestTarget != null ) {
                    ajaxRequestTarget.addComponent( feedback );
                }
            }
	};

        AjaxFallbackButton cancelButton = new AjaxFallbackButton( cancelId, this ) {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                OkCancelForm.this.onCancel( ajaxRequestTarget, form, window );
            }
	};
        cancelButton.setDefaultFormProcessing(false);

        add(feedback);
        add(okButton);
        add(cancelButton);
    }

    //- PROTECTED

    /**
     * Action performed on cancel of the form.
     *
     * <p>The default action is to close the modal window.</p>
     *
     * @param ajaxRequestTarget The AJAX request target (may be null)
     * @param form the Form being cancelled
     * @param window The containing modal window (may be null)
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onCancel( final AjaxRequestTarget ajaxRequestTarget, final Form form, final ModalWindow window ) {
        if ( window != null && ajaxRequestTarget != null ) {
            window.close( ajaxRequestTarget );
        }
    }

    /**
     * Action performed on cancel of the form.
     *
     * <p>The default action is to close the modal window.</p>
     *
     * @param ajaxRequestTarget The AJAX request target (may be null)
     * @param form the Form being submitted
     * @param window The containing modal window (may be null)
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form, final ModalWindow window ) {
        if ( window != null && ajaxRequestTarget != null ) {
            window.close( ajaxRequestTarget );
        }
    }
}