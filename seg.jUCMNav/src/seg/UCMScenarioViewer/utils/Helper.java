
package seg.UCMScenarioViewer.utils;

import java.io.InputStream;
import java.io.ObjectInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import seg.UCMScenarioViewer.UCMScenarioViewer;
import seg.UCMScenarioViewer.UCMScenarioViewerPlugin;
import seg.UCMScenarioViewer.model.ScenarioGroup;
import seg.UCMScenarioViewer.wizards.ImportWizardFirstPage;

/**
 * Values for wizards and icon values on tree outline view.
 */
public class Helper {
	public static final String ICON_OUTLINE_DEFAULT = "icons/msc-default.gif";
	
	public static final String ICON_OUTLINE_SCENARIO = "icons/msc-scenario.gif";
	public static final String ICON_OUTLINE_LIFELINE = "icons/msc-lifeline.gif";
	public static final String ICON_OUTLINE_MESSAGE  = "icons/msc-message.gif";
	public static final String ICON_OUTLINE_SEQUENCE = "icons/msc-sequence.gif";
	public static final String ICON_OUTLINE_PAR_SEQUENCE = "icons/msc-parallel-seq.gif";

	public static final String ICON_OUTLINE_ACTION = "icons/msc-action.gif";
	public static final String ICON_OUTLINE_START_END_MSG = "icons/msc-start-stop.gif";
	public static final String ICON_OUTLINE_START_END_ACTION = ICON_OUTLINE_DEFAULT;
	public static final String ICON_OUTLINE_SET_TIMER = "icons/msc-timer-set.gif";
	public static final String ICON_OUTLINE_RESET_TIMER = "icons/msc-timer-reset.gif";
	public static final String ICON_OUTLINE_TIME_OUT = "icons/msc-timer-timeout.gif";
	public static final String ICON_OUTLINE_CONDITION = "icons/msc-condition.gif";
	
	public static final String ERR_MSG_PARSING = "Parsing error in Parse class!";
	
    public static final ImageDescriptor IMPORT_WIZARD_BANNER =
        ImageDescriptor.createFromFile(
            ImportWizardFirstPage.class,
            "banners/wizard-banner-import.jpg");
    
    public static final ImageDescriptor EXPORT_WIZARD_BANNER =
        ImageDescriptor.createFromFile(
        	ImportWizardFirstPage.class,
            "banners/wizard-banner-export.jpg");
    
    public static final ImageDescriptor FONT_CHANGE_BUTTON =
        ImageDescriptor.createFromFile(
        	UCMScenarioViewerPlugin.class,
            "model/icons/button-font-change.gif");
	
	public static final String SET_FONT_ACTION_TEXT = "Change Font";
	public static final String SET_FONT_ACTION_ID = "Change Font";
    
	public static final String WIZARD_IMPORT_WINDOW_TITLE = "Import UCM/MSC scenarios";
	public static final String WIZARD_EXPORT_WINDOW_TITLE = "Export UCM/MSC scenario to image";
	
	
	public static final String WIZARD_P1_TITLE = "Import UCM/MSC Scenarios";
    public static final String WIZARD_P1_DESC = "Import to view Message Sequence Chart diagrams for UCM scenarios";

    public static final String WIZARD_P1_LABEL = "Select .JUCMSCENARIOS, .XML, or .MSC file that contains UCM scenarios";
    public static final String WIZARD_P1_BROWSE_TEXT = "Browse";
    public static final String WIZARD_P1_CANT_PARSE_ERROR = "Can''t parse file: {0}";
    public static final String WIZARD_P1_WRONG_EXT_ERROR = "Wrong file extension: {0}";
    public static final String WIZARD_P1_NOT_A_FILE_ERROR = "Selected path is invalid.";

    public static final String WIZARD_P2_TITLE = "Select a Project";
    public static final String WIZARD_P2_DESCRIPTION = "Select a project for the imported Message Sequence Chart diagrams to reside in";
    public static final String WIZARD_P2_WRONG_EXT_ERROR="File name must end in .scenarios";
    
	public static final String EXTENSION_XML =".xml";
	public static final String EXTENSION_MSC =".msc";
	public static final String EXTENSION_JUCMSCENARIOS =".jucmscenarios";
	public static final String EXTENSION_SCENARIOS =".scenarios";
	
	public static ScenarioGroup readModel(IFile modelFile) {
		ScenarioGroup model = null;
		try {
			InputStream is = modelFile.getContents(false);
			ObjectInputStream ois = new ObjectInputStream(is);
			model = (ScenarioGroup) ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;

	}
	
    public static ScenarioGroup findModelInWorkbench(IWorkbench workbench, IFile modelFile) {
        if (workbench == null) {
            return null;
        }

        ScenarioGroup model = findModelInWindow(workbench.getActiveWorkbenchWindow(), modelFile);

        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        for (int i = 0; i < windows.length; i++) {
            IWorkbenchWindow window = windows[i];
            model = findModelInWindow(window, modelFile);
        }
        return model;
    }

    private static ScenarioGroup findModelInWindow(IWorkbenchWindow window, IFile modelFile) {
        if (window == null) {
            return null;
        }

        ScenarioGroup model = findModelInPage(window.getActivePage(), modelFile);

        if (model == null) {
            IWorkbenchPage[] pages = window.getPages();
            for (int j = 0; j < pages.length; j++) {
                IWorkbenchPage page = pages[j];
                model = findModelInPage(page, modelFile);
            }
        }
        return model;
    }

    private static ScenarioGroup findModelInPage(IWorkbenchPage page, IFile modelFile) {
        if (page == null) {
            return null;
        }

        ScenarioGroup model = findModelInEditor(page.getActiveEditor(), modelFile);

        if (model == null) {
            IEditorReference[] editors = page.getEditorReferences();
            for (int k = 0; k < editors.length; k++) {
                IEditorReference editorReference = editors[k];
                model = findModelInEditor(
                        editorReference.getEditor(false),
                        modelFile);
            }
        }
        return model;
    }

    private static ScenarioGroup findModelInEditor(IEditorPart editor, IFile modelFile) {
    	ScenarioGroup model = null;

        if (editor instanceof UCMScenarioViewer) {
        	UCMScenarioViewer scenViewer = (UCMScenarioViewer) editor;
            IFile editorInput = ((IFileEditorInput) scenViewer.getEditorInput()).getFile();
            if (scenViewer.getMSCDiagram() instanceof ScenarioGroup &&
                modelFile.equals(editorInput)) {
                model = (ScenarioGroup) scenViewer.getMSCDiagram();
            }
        }
        return model;
    }
    
    public static UCMScenarioViewer openViewer(IFile file) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
		if (window == null) {
			return null;
		}
		
		try {
		    return (UCMScenarioViewer)IDE.openEditor(window.getActivePage(), file);
		} catch (Exception e) {
		    return null;
		}
    }
    
    public static IFile getSelectedModel(IWorkbench workbench, IStructuredSelection selection) {
        UCMScenarioViewer editor = getUCMScenarioViewer(workbench);
        if (editor != null) {
            return ((IFileEditorInput) editor.getEditorInput()).getFile();
        }

        // If file is selected
        if (selection == null) {
            return null;
        }
        if (selection.getFirstElement() instanceof  IFile) {
            return (IFile) selection.getFirstElement();
        }

        // Nothing is selected
        return null;
    }

    public static UCMScenarioViewer getUCMScenarioViewer(IWorkbench workbench) {
        if (workbench == null) {
            return null;
        }

        org.eclipse.ui.IWorkbenchWindow win = workbench.getActiveWorkbenchWindow();
        if (win == null) return null;
        org.eclipse.ui.IWorkbenchPage page = win.getActivePage();
        if (page == null) return null;
        IEditorPart editor = page.getActiveEditor();
        return editor instanceof UCMScenarioViewer ? (UCMScenarioViewer) editor : null;
    }

    /**
     * Tight padding added on each side of the computed paint extent. Buys us
     * a few pixels for figures whose paint() draws slightly outside their
     * reported bounds (typical for arrow heads, italic text descenders, etc.).
     */
    private static final int PAINT_EXTENT_PAD = 4;

    /**
     * Compute the bounding rectangle that covers <code>root</code> and every
     * one of its descendant figures, then expand by {@link #PAINT_EXTENT_PAD}
     * pixels on each side.
     *
     * <p>Why this exists: the MSC viewer's printable layer reports a
     * {@code getBounds()} that reflects the layer's own laid-out box, not the
     * union of its children's extents. Condition shapes, loop messages on a
     * single lifeline (start/end on same lifeline), and other figures paint
     * outside the layer's reported bounds; using the layer's getBounds()
     * directly as the off-screen image size truncates them on all four sides.
     *
     * <p>Walking the figure tree and unioning every descendant's getBounds()
     * gives a faithful capture extent. The PAINT_EXTENT_PAD then catches the
     * remaining sub-pixel overflow (arrow heads etc.) that no figure bounds
     * captures exactly.
     */
    public static Rectangle computePaintExtent(IFigure root) {
        if (root == null) return new Rectangle();
        Rectangle extent = root.getBounds().getCopy();
        accumulateBounds(root, extent);
        extent.expand(PAINT_EXTENT_PAD, PAINT_EXTENT_PAD);
        return extent;
    }

    private static void accumulateBounds(IFigure figure, Rectangle extent) {
        java.util.List children = figure.getChildren();
        if (children == null) return;
        for (int i = 0; i < children.size(); i++) {
            IFigure child = (IFigure) children.get(i);
            extent.union(child.getBounds());
            accumulateBounds(child, extent);
        }
    }
}
