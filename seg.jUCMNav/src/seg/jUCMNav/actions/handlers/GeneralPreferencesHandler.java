package seg.jUCMNav.actions.handlers;

import org.eclipse.ui.IEditorActionDelegate;

import seg.jUCMNav.actions.GeneralPreferencesActionDelegate;

public class GeneralPreferencesHandler extends LegacyDelegateHandler {
    @Override
    protected IEditorActionDelegate createDelegate() {
        return new GeneralPreferencesActionDelegate();
    }
}
