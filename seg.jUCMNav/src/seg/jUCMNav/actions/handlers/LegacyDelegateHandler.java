package seg.jUCMNav.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Bridge that adapts a legacy {@link IEditorActionDelegate} (the deprecated
 * {@code org.eclipse.ui.editorActions} extension point) into a {@code commands +
 * handlers} flow. Each concrete subclass returns a fresh delegate instance from
 * {@link #createDelegate()}. The bridge synthesises a no-op {@link IAction} carrying the
 * invoking command's id, walks the delegate's {@code setActiveEditor()} /
 * {@code run()} lifecycle, and surfaces any thrown {@link RuntimeException} as an
 * {@link ExecutionException}.
 *
 * <p>The seven jUCMNav delegates registered against this base ignore the
 * {@code IAction} parameter beyond reading {@code getId()}, so the synthetic action is
 * harmless. {@link AdvancedModeActionDelegate}'s {@code setChecked()} call is
 * absorbed by the synthetic action (no observable effect on the workbench); the
 * Advanced Mode toggle visual state is driven separately via the command's
 * {@code org.eclipse.ui.commands.toggleState} declared in plugin.xml.
 */
public abstract class LegacyDelegateHandler extends AbstractHandler {

    /** Return a freshly-constructed delegate. Called once per command invocation. */
    protected abstract IEditorActionDelegate createDelegate();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorActionDelegate delegate = createDelegate();
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        IAction synthetic = new Action() { /* no-op subclass */ };
        if (event.getCommand() != null && event.getCommand().getId() != null) {
            synthetic.setId(event.getCommand().getId());
        }
        try {
            delegate.setActiveEditor(synthetic, editor);
            delegate.run(synthetic);
        } catch (RuntimeException e) {
            throw new ExecutionException(
                    "Legacy action delegate failed: " + delegate.getClass().getName(), e); //$NON-NLS-1$
        }
        return null;
    }
}
