package seg.jUCMNav.actions.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import java.util.Map;

import seg.jUCMNav.actions.AdvancedModeActionDelegate;
import seg.jUCMNav.views.preferences.DisplayPreferences;

/**
 * Toggle handler for jUCMNav Advanced Mode. Reuses
 * {@link AdvancedModeActionDelegate} for the actual side effects (preference flip,
 * preference page open on enable), and refreshes the toggle check state from
 * {@link DisplayPreferences} via {@link IElementUpdater} so the menu / toolbar item
 * reflects the live preference value.
 */
public class AdvancedModeHandler extends LegacyDelegateHandler implements IElementUpdater {

    @Override
    protected IEditorActionDelegate createDelegate() {
        return new AdvancedModeActionDelegate();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Object result = super.execute(event);
        fireHandlerChanged(new HandlerEvent(this, true, false));
        return result;
    }

    @Override
    public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
        element.setChecked(DisplayPreferences.getInstance().isAdvancedControlEnabled());
    }
}
