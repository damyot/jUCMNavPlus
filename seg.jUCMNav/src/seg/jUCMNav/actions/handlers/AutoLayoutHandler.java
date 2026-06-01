package seg.jUCMNav.actions.handlers;

import org.eclipse.ui.IEditorActionDelegate;

import seg.jUCMNav.actions.AutoLayoutActionDelegate;

public class AutoLayoutHandler extends LegacyDelegateHandler {
    @Override
    protected IEditorActionDelegate createDelegate() {
        return new AutoLayoutActionDelegate();
    }
}
