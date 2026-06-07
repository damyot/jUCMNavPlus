/*
 * Right-click -> "Export..." on the UCMScenarioViewer canvas.
 * Opens the existing UCMScenarioExportWizard programmatically so the user
 * doesn't have to navigate File -> Export -> MSC Diagram to Image.
 */
package seg.UCMScenarioViewer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ISharedImages;

import seg.UCMScenarioViewer.UCMScenarioViewer;
import seg.UCMScenarioViewer.wizards.UCMScenarioExportWizard;

/**
 * Opens the Export MSC Diagram to Image wizard, pre-seeded with the
 * currently active scenario viewer / file.
 */
public class MSCExportAction extends Action {

    public static final String ACTION_ID = "seg.UCMScenarioViewer.actions.MSCExport";

    private final UCMScenarioViewer editor;

    public MSCExportAction(UCMScenarioViewer editor) {
        this.editor = editor;
        setId(ACTION_ID);
        setText("&Export to Image...");
        setToolTipText("Export the MSC diagram(s) to image files (PNG / BMP / JPEG)");
        ISharedImages shared = PlatformUI.getWorkbench().getSharedImages();
        setImageDescriptor(shared.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));
    }

    @Override
    public boolean isEnabled() {
        return editor != null && editor.getMSCDiagram() != null
                && editor.getMSCDiagram().getSelectedScenario() != null
                && editor.getEditorInput() instanceof IFileEditorInput;
    }

    @Override
    public void run() {
        if (!isEnabled()) return;

        // Seed the wizard with the .jucmscenarios file backing this editor.
        // UCMScenarioExportWizard.getSelectedModel asks Helper.getUCMScenarioViewer
        // first (which finds the active editor in the workbench, i.e. us),
        // then falls back to the IStructuredSelection. Passing a real
        // selection containing our IFile keeps the fallback path correct too.
        IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
        StructuredSelection selection = new StructuredSelection(input.getFile());

        UCMScenarioExportWizard wizard = new UCMScenarioExportWizard();
        wizard.init(PlatformUI.getWorkbench(), selection);

        WizardDialog dialog = new WizardDialog(
                editor.getSite().getShell(), wizard);
        dialog.open();
    }
}
