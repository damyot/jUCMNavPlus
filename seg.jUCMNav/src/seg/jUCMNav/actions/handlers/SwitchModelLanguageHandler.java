package seg.jUCMNav.actions.handlers;

import org.eclipse.ui.IEditorActionDelegate;

import seg.jUCMNav.actions.SwitchModelLanguageDelegate;

public class SwitchModelLanguageHandler extends LegacyDelegateHandler {
    @Override
    protected IEditorActionDelegate createDelegate() {
        return new SwitchModelLanguageDelegate();
    }
}
