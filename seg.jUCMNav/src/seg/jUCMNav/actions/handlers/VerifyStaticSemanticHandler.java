package seg.jUCMNav.actions.handlers;

import org.eclipse.ui.IEditorActionDelegate;

import seg.jUCMNav.actions.staticSemantic.VerifyStaticSemanticDelegate;

public class VerifyStaticSemanticHandler extends LegacyDelegateHandler {
    @Override
    protected IEditorActionDelegate createDelegate() {
        return new VerifyStaticSemanticDelegate();
    }
}
