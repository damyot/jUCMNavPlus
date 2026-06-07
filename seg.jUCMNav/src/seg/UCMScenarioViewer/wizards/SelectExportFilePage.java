/*
 * Created on 31-Mar-2005
 */
package seg.UCMScenarioViewer.wizards;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import seg.UCMScenarioViewer.UCMScenarioViewer;
import seg.UCMScenarioViewer.model.Scenario;
import seg.UCMScenarioViewer.utils.Helper;


/**
 * @author mkova062
 */
public class SelectExportFilePage extends WizardPage implements SelectionListener{


    public static final String TITLE = "Save diagram(s) to file";
    public static final String DESCRIPTION = "Pick output (single file or directory for multi-export), file format, and zoom level.";

    private static final String LABEL_FILE   = "Select file to save diagram to:";
    private static final String LABEL_FOLDER = "Select directory to save diagrams to (one image per scenario):";
    private static final String BROWSE_TEXT = "Browse";
    private static final String INVALID_PATH_ERROR     = "Selected path is invalid";
    private static final String CANT_WRITE_ERROR       = "Can't overwrite selected file";
    private static final String DIR_NOT_FOUND_ERROR    = "Output directory does not exist";
    private static final String DIR_NOT_WRITABLE_ERROR = "Output directory is not writable";
    private static final String NOT_SELECTED_ERROR     = "Diagram is not selected";

    private static final int PNG_TYPE = 0;
    private static final int BMP_TYPE = 1;
    private static final int JPG_TYPE = 2;

    /** Preset zoom levels offered in the combo. 1.0 = native scale. */
    private static final double[] ZOOM_PRESETS = {0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0};
    private static final int DEFAULT_ZOOM_INDEX = 3; // 1.0

    private UCMScenarioViewer viewer;
    private int[] selectedScenarioIndices = new int[0];
    private Shell shell;
    private Label label;
    private Text fileField;
    private Button browseButton;
    private Group group;
    private Button png;
    private Button bmp;
    private Button jpeg;
    private Combo zoomCombo;
    private int fileType = PNG_TYPE; // .png by default

    public SelectExportFilePage(Shell shell) {
        super("selectDiagramTypeAndFilePage", TITLE, Helper.EXPORT_WIZARD_BANNER);
        this.shell = shell;
        setDescription(DESCRIPTION);
    }

    // Page data manipulation
    public void setViewer(UCMScenarioViewer viewer) {
        this.viewer = viewer;
        setDefaultFileName();
    }

    /** Called by the wizard before this page becomes visible. */
    public void setSelectedScenarioIndices(int[] indices) {
        this.selectedScenarioIndices = (indices != null) ? indices : new int[0];
        // Single vs multi selection changes the meaning of the path field
        // (file path vs output directory). Update the label and the default
        // entry whenever the selection changes.
        updatePathLabelAndDefault();
        validatePage(false);
    }

    public boolean export() {
        if (viewer == null) {
            return false;
        }

        GraphicalViewer gv = (GraphicalViewer)viewer.getAdapter(GraphicalViewer.class);
        if (gv == null) {
            return false;
        }

        // ZoomManager round-trip: temporarily set the live viewer's zoom to
        // the user's chosen factor so the captured figure renders at that
        // resolution (sharp text + edges), then restore in finally so the
        // editor returns to its prior zoom regardless of success. Same
        // pattern used by CopyAction's issue-#4 workaround.
        ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) gv.getRootEditPart();
        ZoomManager zm = root.getZoomManager();
        double originalZoom = (zm != null) ? zm.getZoom() : 1.0;
        double targetZoom = ZOOM_PRESETS[zoomComboIndex()];

        // Default to {single-selection mode using the currently active scenario}
        // when the wizard wasn't told otherwise. Defensive: should always have
        // at least one index after validation.
        int[] indices = (selectedScenarioIndices.length > 0)
                ? selectedScenarioIndices
                : new int[] { viewer.getMSCDiagram().getSelectedScenario().getNumber() };
        boolean multi = indices.length > 1;

        java.util.List scenarios = viewer.getMSCDiagram().getTreeChildren();

        try {
            if (zm != null && Math.abs(targetZoom - originalZoom) > 1e-9) {
                zm.setZoom(targetZoom);
            }

            String pathFieldText = fileField.getText();
            for (int i = 0; i < indices.length; i++) {
                int scenarioIdx = indices[i];

                // Switch to the scenario we want to capture. The MSC viewer
                // rebuilds its figure tree on this call; drain pending UI
                // events so the new figures actually paint before we walk
                // the layer for the screenshot.
                viewer.getMSCDiagram().setSelectedScenario(scenarioIdx);
                pumpEvents();

                Scenario scenario = (Scenario) scenarios.get(scenarioIdx);
                String outPath = multi
                        ? new File(pathFieldText, sanitize(scenario.getName()) + getFileExtension()).getPath()
                        : pathFieldText;

                if (!captureScenarioToFile(gv, outPath)) {
                    return false;
                }
            }
        } catch (Exception e) {
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            messageBox.setText("Error");
            messageBox.setMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            messageBox.open();
            return false;
        } finally {
            if (zm != null && Math.abs(targetZoom - originalZoom) > 1e-9) {
                zm.setZoom(originalZoom);
            }
        }

        return true;
    }

    /** Walk the printable-layers figure of the live viewer and save it as one image. */
    private boolean captureScenarioToFile(GraphicalViewer gv, String outPath) {
        Image img = null;
        GC gc = null;
        Graphics graphics = null;
        try {
            IFigure f = ((ScalableFreeformRootEditPart) gv.getRootEditPart()).getLayer(LayerConstants.PRINTABLE_LAYERS);

            img = new Image(null, f.getSize().width, f.getSize().height);
            gc = new GC(img);
            // See CopyAction.buildScreenshot -- without GDI+ mode, antialiased
            // Polygon/Polyline fills used by UCM path-node figures silently
            // no-op off-screen.
            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.ON);
            graphics = new SWTGraphics(gc);
            graphics.translate(f.getBounds().getLocation());
            f.paint(graphics);

            ImageLoader il = new ImageLoader();
            il.data = new ImageData[]{img.getImageData()};
            if (fileType == PNG_TYPE)
                il.save(outPath, SWT.IMAGE_PNG);
            else if (fileType == BMP_TYPE)
                il.save(outPath, SWT.IMAGE_BMP_RLE);
            else if (fileType == JPG_TYPE)
                il.save(outPath, SWT.IMAGE_JPEG);
            return true;
        } finally {
            if (graphics != null) graphics.dispose();
            if (gc != null) gc.dispose();
            if (img != null && !img.isDisposed()) img.dispose();
        }
    }

    /** Drain pending UI events so the viewer renders the new scenario before capture. */
    private void pumpEvents() {
        Display d = Display.getCurrent();
        if (d == null) return;
        // Bounded loop: prevent runaway re-entrant event delivery from
        // dragging the export out indefinitely. 32 cycles is plenty for the
        // MSC viewer's layout/paint pipeline to settle.
        //
        // Defensive catch on SWTException(ERROR_WIDGET_DISPOSED): readAndDispatch
        // also runs the popup-menu queue (Display.runPopups), where a known
        // Win32 SWT race can fire Menu.wmTimer on a Menu that has already been
        // disposed (e.g. the user dismissed a context menu just before the
        // wizard's Finish click). That's an upstream Eclipse bug -- not ours,
        // and not specific to this code path -- but if it surfaces while we're
        // draining the queue between scenario captures, we don't want one stray
        // popup-timer exception to abort an entire multi-scenario export. Log
        // it and keep going; the next loop iteration's events are still ours
        // to drive.
        for (int i = 0; i < 32; i++) {
            try {
                if (!d.readAndDispatch()) break;
            } catch (org.eclipse.swt.SWTException ex) {
                if (ex.code == SWT.ERROR_WIDGET_DISPOSED) {
                    ex.printStackTrace();
                } else {
                    throw ex;
                }
            }
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isEmpty()) return "scenario";
        // Conservative: replace anything that isn't ASCII alphanumeric, dash,
        // dot, underscore or space with '_'. Matches Windows + POSIX rules.
        return name.replaceAll("[^A-Za-z0-9._\\-\\s]", "_").trim();
    }

    private boolean isMultiMode() {
        return selectedScenarioIndices != null && selectedScenarioIndices.length > 1;
    }

    // Page validation
    protected void validatePage(boolean showErrorMessage) {
        String errorMessage = null;

        if (viewer == null) {
            errorMessage = NOT_SELECTED_ERROR;
        } else if (isMultiMode()) {
            errorMessage = testDirectoryValid();
        } else {
            errorMessage = testPathValid();
            if (errorMessage == null) errorMessage = testCanWrite();
        }

        setErrorMessage(showErrorMessage ? errorMessage : null);
        setPageComplete(errorMessage == null);
    }

    private String testPathValid() {
        return "".equals(fileField.getText()) || !Path.EMPTY.isValidPath(fileField.getText()) ?
                INVALID_PATH_ERROR :
                null;
    }

    private String testCanWrite() {
        File file = new File(fileField.getText());
        return file.exists() && !file.canWrite() ? CANT_WRITE_ERROR : null;
    }

    private String testDirectoryValid() {
        String t = fileField.getText();
        if (t == null || t.isEmpty() || !Path.EMPTY.isValidPath(t)) return INVALID_PATH_ERROR;
        File f = new File(t);
        if (!f.exists() || !f.isDirectory()) return DIR_NOT_FOUND_ERROR;
        if (!f.canWrite()) return DIR_NOT_WRITABLE_ERROR;
        return null;
    }

    // Page controls
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());
        setControl(composite);

        createGroup(composite);
        createZoomControl(composite);
        createLabel(composite);
        createFileField(composite);
        createBrowseButton(composite);

        hookBrowseSelected();
        hookFileFieldChanged();

        validatePage(false);
    }

    private void createLabel(Composite parent) {
        label = new Label(parent, SWT.NONE);
        label.setText(LABEL_FILE);

        FormData formData = new FormData();
        formData.top = new FormAttachment(zoomCombo, 10, SWT.BOTTOM);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        label.setLayoutData(formData);
    }

    private void createGroup(Composite parent) {
        group = new Group(parent, SWT.NONE);
    	group.setLayout(new GridLayout());
    	group.setText("Image file formats:");
    	group.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    	png = new Button(group,SWT.RADIO);
    	png.setText(".PNG format file (best results for Web and printed documents)");
    	png.addSelectionListener(this);
    	png.setSelection(true);

    	bmp = new Button(group,SWT.RADIO);
    	bmp.setText(".BMP format file");
    	bmp.addSelectionListener(this);

    	jpeg = new Button(group, SWT.RADIO);
    	jpeg.setText(".JPEG format file");
    	jpeg.addSelectionListener(this);

        FormData formData = new FormData();
        formData.top = new FormAttachment(0, 10);
        formData.left = new FormAttachment(0, 0);
        formData.right = new FormAttachment(100, 0);
        group.setLayoutData(formData);
    }

    private void createZoomControl(Composite parent) {
        Label zoomLabel = new Label(parent, SWT.NONE);
        zoomLabel.setText("Zoom for export:");
        FormData lf = new FormData();
        lf.top = new FormAttachment(group, 10, SWT.BOTTOM);
        lf.left = new FormAttachment(0, 0);
        zoomLabel.setLayoutData(lf);

        zoomCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        for (double z : ZOOM_PRESETS) {
            zoomCombo.add(formatZoom(z));
        }
        zoomCombo.select(DEFAULT_ZOOM_INDEX);
        FormData cf = new FormData();
        cf.top = new FormAttachment(group, 5, SWT.BOTTOM);
        cf.left = new FormAttachment(zoomLabel, 10);
        zoomCombo.setLayoutData(cf);
    }

    private int zoomComboIndex() {
        if (zoomCombo == null) return DEFAULT_ZOOM_INDEX;
        int idx = zoomCombo.getSelectionIndex();
        return (idx >= 0 && idx < ZOOM_PRESETS.length) ? idx : DEFAULT_ZOOM_INDEX;
    }

    private static String formatZoom(double z) {
        int pct = (int) Math.round(z * 100);
        return pct + " %";
    }

    private void createFileField(Composite parent) {
        fileField = new Text(parent, SWT.LEFT | SWT.SINGLE | SWT.BORDER);

        setDefaultFileName();

        FormData data = new FormData();
        data.top = new FormAttachment(label, 5);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, -80);
        fileField.setLayoutData(data);
    }

    private void createBrowseButton(Composite parent) {
        browseButton = new Button(parent, SWT.NONE);
        browseButton.setText(BROWSE_TEXT);

        FormData formData = new FormData();
        formData.top = new FormAttachment(label, 5);
        formData.bottom = new FormAttachment(fileField, 0, SWT.BOTTOM);
        formData.left = new FormAttachment(fileField, 5);
        formData.right = new FormAttachment(100, 0);
        browseButton.setLayoutData(formData);
    }

    private void hookFileFieldChanged() {
        fileField.addModifyListener(
                new ModifyListener() {
                    public void modifyText(ModifyEvent e) {
                        validatePage(true);
                    }
                });
    }

    private void hookBrowseSelected() {
        browseButton.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        if (isMultiMode()) {
                            DirectoryDialog dd = new DirectoryDialog(shell);
                            String current = fileField.getText();
                            File seed = new File(current);
                            if (seed.isDirectory()) {
                                dd.setFilterPath(current);
                            } else if (seed.getParentFile() != null) {
                                dd.setFilterPath(seed.getParentFile().getPath());
                            }
                            String dir = dd.open();
                            if (dir != null) {
                                fileField.setText(dir);
                                validatePage(true);
                            }
                        } else {
                            FileDialog fileDialog = new FileDialog(shell);
                            fileDialog.setFileName(fileField.getText());
                            String file = fileDialog.open();
                            if (file != null) {
                                fileField.setText(file);
                                validatePage(true);
                            }
                        }
                    }
                });
    }

    // Convenience
    private void setDefaultFileName() {
        if (fileField == null || viewer == null) return;
        if (isMultiMode()) {
            // Multi mode: default the path to the directory of the current
            // entry (or the user's home if empty). Filenames are generated
            // per-scenario at export time.
            String current = fileField.getText();
            File f = new File(current);
            String dir;
            if (f.isDirectory()) {
                dir = f.getPath();
            } else if (f.getParentFile() != null && f.getParentFile().isDirectory()) {
                dir = f.getParentFile().getPath();
            } else {
                dir = System.getProperty("user.home", "");
            }
            fileField.setText(dir);
        } else {
            String fileName = viewer.getMSCDiagram().getSelectedScenario().getName() + getFileExtension();
            File fileFolder = new File(fileField.getText()).getParentFile();
            String filePath = new File(fileFolder, fileName).getPath();
            fileField.setText(filePath);
        }
    }

    private void updatePathLabelAndDefault() {
        if (label != null) {
            label.setText(isMultiMode() ? LABEL_FOLDER : LABEL_FILE);
            label.getParent().layout();
        }
        setDefaultFileName();
    }

    public IWizardPage getPreviousPage() {
        return super.getPreviousPage();
    }

    /**
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(SelectionEvent)
     */
    public void widgetSelected(SelectionEvent e) {
    	if( e.getSource() == png ){
    		fileType = PNG_TYPE;
    		setFileExtension();
    	} else if (e.getSource() == bmp){
    		fileType = BMP_TYPE;
    		setFileExtension();
    	}
    	else {
    		fileType = JPG_TYPE;
    		setFileExtension();
    	}
    }

    private void setFileExtension() {
        // In multi-mode the file field is a directory path; nothing to
        // re-extension. Per-scenario file names are built at export time
        // with the current getFileExtension().
        if (isMultiMode()) return;
    	String fileName = fileField.getText();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return; // no extension yet
    	String result = fileName.substring(0, dot).concat(getFileExtension());
    	fileField.setText(result);
    }

    private String getFileExtension() {
    	if (fileType == PNG_TYPE)
    		return ".png";
    	if (fileType == BMP_TYPE)
    		return ".bmp";
    	return ".jpg";
    }

    /**
     * Empty method
     */
    public void widgetDefaultSelected(SelectionEvent e) {

    }
}
