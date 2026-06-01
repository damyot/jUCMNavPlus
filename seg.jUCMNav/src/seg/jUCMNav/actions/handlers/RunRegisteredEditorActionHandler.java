package seg.jUCMNav.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import seg.jUCMNav.editors.UCMNavMultiPageEditor;

/**
 * Looks up an action by id in the active jUCMNav editor's
 * {@link ActionRegistry} and runs it. Concrete subclasses hard-code the action id.
 *
 * <p>Replaces the legacy {@link seg.jUCMNav.actions.UCMActionDelegate} behaviour
 * (which dispatched on {@code IAction.getId()} via an {@code instanceof
 * EditorPluginAction} check). The dispatch now happens at the handler level, one
 * concrete subclass per command, so the legacy delegate's reliance on the internal
 * {@code EditorPluginAction} class is no longer needed.
 */
public abstract class RunRegisteredEditorActionHandler extends AbstractHandler {

    /** Action id registered in the editor's {@link ActionRegistry}. */
    protected abstract String getActionId();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (!(editor instanceof UCMNavMultiPageEditor)) {
            return null;
        }
        Object adapter = editor.getAdapter(ActionRegistry.class);
        if (!(adapter instanceof ActionRegistry)) {
            return null;
        }
        IAction toRun = ((ActionRegistry) adapter).getAction(getActionId());
        if (toRun != null) {
            toRun.run();
        }
        return null;
    }
}
