/*
 * Created on 31-Mar-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package seg.UCMScenarioViewer.wizards;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import seg.UCMScenarioViewer.UCMScenarioViewer;
import seg.UCMScenarioViewer.model.Scenario;
import seg.UCMScenarioViewer.utils.Helper;


/**
 * @author mkova062
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SelectScenarioPage extends WizardPage {

    public static final String TITLE = "Select Scenarios to export";
    public static final String DESCRIPTION = "Select one or more scenarios from the scenario group; each will be exported to its own image file.";

    private static final String LABEL = "Select scenarios (Ctrl- or Shift-click for multi-select):";

    private Label label;
    private List scenarioList;
    private Button selectAllButton;
    private Button deselectAllButton;
    private UCMScenarioViewer viewer;

    public SelectScenarioPage() {
        super("selectScenarioFromModelPage", TITLE, Helper.EXPORT_WIZARD_BANNER);
        setDescription(DESCRIPTION);
    }

    // Page data manipulation
    public void setModelFile(IFile modelFile) {
        viewer = Helper.openViewer(modelFile);
        updateScenarioList();
    }

    public UCMScenarioViewer getSelectedDiagram() {
    	return viewer;
    }

    /**
     * Indices into the scenario group's tree-children list that the user has
     * checked for export. Returned in the order they appear in the list (not
     * the order the user clicked). Empty array means nothing selected.
     */
    public int[] getSelectedScenarioIndices() {
        if (scenarioList == null) return new int[0];
        int[] sel = scenarioList.getSelectionIndices();
        java.util.Arrays.sort(sel);
        return sel;
    }

    // Page validation
    protected void validatePage(boolean showErrorMessage) {
        String errorMessage = null;

        int[] sel = scenarioList.getSelectionIndices();
        errorMessage = (sel == null || sel.length == 0) ? "At least one scenario must be selected" : null;
        // Keep the viewer's "current scenario" pointing at the first selection
        // so existing single-scenario code paths (e.g. default filename based
        // on the active scenario name) still have something sensible to read.
        if (sel != null && sel.length > 0) {
            viewer.getMSCDiagram().setSelectedScenario(sel[0]);
        }
        setErrorMessage(showErrorMessage ? errorMessage : null);
        setPageComplete(errorMessage == null);
    }

    // Page controls
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());
        setControl(composite);

        createLabel(composite);
        createSelectionButtons(composite);
        createDiagramsList(composite);

        hookDiagramSelection();
        hookSelectionButtons();

        updateScenarioList();
        validatePage(false);
    }

    private void createLabel(Composite parent) {
        label = new Label(parent, SWT.NONE);
        label.setText(LABEL);

        FormData formData = new FormData();
        formData.top = new FormAttachment(0, 10);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        label.setLayoutData(formData);
    }

    private void createSelectionButtons(Composite parent) {
        selectAllButton = new Button(parent, SWT.PUSH);
        selectAllButton.setText("Select All");
        FormData formData = new FormData();
        formData.top = new FormAttachment(label, 5);
        formData.right = new FormAttachment(100, 0);
        selectAllButton.setLayoutData(formData);

        deselectAllButton = new Button(parent, SWT.PUSH);
        deselectAllButton.setText("Deselect All");
        formData = new FormData();
        formData.top = new FormAttachment(label, 5);
        formData.right = new FormAttachment(selectAllButton, -5);
        deselectAllButton.setLayoutData(formData);
    }

    private void createDiagramsList(Composite parent) {
        // Multi-selection (Ctrl/Shift-click) so the user can export several
        // scenarios in one pass. The export page below ships each selected
        // scenario as its own image file in a chosen output directory.
        scenarioList = new List(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);

        FormData formData = new FormData();
        formData.top = new FormAttachment(selectAllButton, 5);
        formData.bottom = new FormAttachment(100, 0);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        scenarioList.setLayoutData(formData);
    }

    private void hookSelectionButtons() {
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (scenarioList.getItemCount() == 0) return;
                scenarioList.selectAll();
                validatePage(true);
            }
        });
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                scenarioList.deselectAll();
                validatePage(true);
            }
        });
    }
    
    private void updateScenarioList() {
        if (scenarioList == null) {
            return;
        }
        
        scenarioList.removeAll();
        
        if (viewer == null) {
            return;
        }
        
        java.util.List scenarios = viewer.getMSCDiagram().getTreeChildren();
        Iterator i = scenarios.iterator();
        while (i.hasNext()) {
            scenarioList.add( ((Scenario)i.next()).getName());
        }
        
        // Pre-select the scenario currently active in the viewer; the user can
        // extend the selection with Ctrl/Shift-click, or use Select All above.
        scenarioList.setSelection(new int[]{viewer.getMSCDiagram().getSelectedScenario().getNumber()});
        
        validatePage(true);
    }

    private void hookDiagramSelection() {
        scenarioList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                validatePage(true);
            }
        });
    }


}
