/*
 * Created on 13-Feb-2005
 */
package seg.UCMScenarioViewer.actions;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;

/**
 * Right-click context menu for the MSC scenario viewer canvas. Pulls the
 * Copy and Export actions from the editor's ActionRegistry (registered in
 * UCMScenarioViewer.createActions) and slots them into the standard EDIT
 * and ADDITIONS groups created by GEFActionConstants.addStandardActionGroups.
 *
 * Historically this provider was a stub. The two actions exposed here also
 * have other invocations: Copy is bound to Ctrl+C via the workbench's
 * org.eclipse.ui.edit.copy command, and Export is also reachable via the
 * top-level File -> Export -> MSC Diagram to Image wizard.
 */
public class UCMScenarioViewerContextMenuProvider extends ContextMenuProvider {

    private ActionRegistry actionRegistry;

    public UCMScenarioViewerContextMenuProvider(EditPartViewer viewer) {
        super(viewer);
    }

    public UCMScenarioViewerContextMenuProvider(EditPartViewer viewer, ActionRegistry registry) {
        super(viewer);
        this.actionRegistry = registry;
    }

    /* (non-Javadoc)
     * @see org.eclipse.gef.ContextMenuProvider#buildContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    public void buildContextMenu(IMenuManager menu) {
        GEFActionConstants.addStandardActionGroups(menu);
        if (actionRegistry == null) return;

        IAction copy = actionRegistry.getAction(MSCCopyAction.ACTION_ID);
        if (copy != null) {
            menu.appendToGroup(GEFActionConstants.GROUP_EDIT, copy);
        }

        IAction export = actionRegistry.getAction(MSCExportAction.ACTION_ID);
        if (export != null) {
            // GROUP_REST is the catch-all "additions" group; Export is a
            // higher-level action than Copy/Cut/Paste, so it sits separately.
            menu.appendToGroup(GEFActionConstants.GROUP_REST, export);
        }
    }
}
