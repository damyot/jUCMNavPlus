package seg.jUCMNav.actions.handlers;

import org.eclipse.ui.IEditorActionDelegate;

import seg.jUCMNav.actions.scenarios.VerifySyntaxActionDelegate;

public class VerifySyntaxHandler extends LegacyDelegateHandler {
    @Override
    protected IEditorActionDelegate createDelegate() {
        return new VerifySyntaxActionDelegate();
    }
}
