/*
 * Copy the currently displayed MSC diagram to the system clipboard as an image.
 * Wired to:
 *   - Ctrl+C (via IHandlerService binding to org.eclipse.ui.edit.copy)
 *   - Right-click -> Copy on the UCMScenarioViewer canvas
 */
package seg.UCMScenarioViewer.actions;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.IScalablePane;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import seg.UCMScenarioViewer.UCMScenarioViewer;

/**
 * Copy the currently displayed MSC scenario diagram to the system clipboard
 * as an Image (PNG/BMP-compatible payload via SWT ImageTransfer).
 *
 * Mirrors the off-screen-capture approach used by seg.jUCMNav.actions.cutcopypaste.CopyAction
 * for UCM diagrams, including the issue-#4 scale-nudge workaround so
 * polygon-based shapes don't drop at exactly 100 % zoom.
 */
public class MSCCopyAction extends Action {

    public static final String ACTION_ID = ActionFactory.COPY.getId();

    private final UCMScenarioViewer editor;

    public MSCCopyAction(UCMScenarioViewer editor) {
        this.editor = editor;
        setId(ACTION_ID);
        // Bind to the workbench's standard Copy command so Ctrl+C (and the
        // Edit -> Copy menu item) route to us when this editor is active.
        setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
        setText("&Copy");
        setToolTipText("Copy the current MSC diagram to the clipboard");
        // Use the workbench's shared Copy icon (don't allocate our own).
        ISharedImages shared = PlatformUI.getWorkbench().getSharedImages();
        setImageDescriptor(shared.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        setDisabledImageDescriptor(shared.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
    }

    @Override
    public boolean isEnabled() {
        return editor != null && editor.getMSCDiagram() != null
                && editor.getMSCDiagram().getSelectedScenario() != null;
    }

    @Override
    public void run() {
        if (!isEnabled()) return;

        GraphicalViewer gv = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
        if (gv == null) return;

        ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) gv.getRootEditPart();
        IFigure pane = root.getLayer(LayerConstants.PRINTABLE_LAYERS);
        if (pane == null) return;

        // Issue #4 workaround: at zoom exactly 1.0 the ScalableFreeformLayeredPane
        // skips its ScaledGraphics wrap and antialiased Polygon/Polyline fills
        // silently drop on the off-screen GC. Nudge the scale just for this
        // paint; restore in finally. See CopyAction.buildScreenshot for the
        // UCM-editor equivalent.
        final IScalablePane scalable = (pane instanceof IScalablePane) ? (IScalablePane) pane : null;
        final double originalScale = (scalable != null) ? scalable.getScale() : 1.0;
        final boolean needsNudge = (scalable != null) && Math.abs(originalScale - 1.0) < 1e-6;

        Image image = null;
        GC gc = null;
        Graphics graphics = null;
        Clipboard clipboard = null;
        try {
            if (needsNudge) scalable.setScale(originalScale + 0.001);

            image = new Image(Display.getCurrent(),
                    pane.getSize().width, pane.getSize().height);
            gc = new GC(image);
            // Force GDI+ on Windows so antialiased fills render off-screen.
            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.ON);
            graphics = new SWTGraphics(gc);
            graphics.translate(-pane.getBounds().x, -pane.getBounds().y);
            pane.paint(graphics);

            ImageData data = image.getImageData();
            clipboard = new Clipboard(Display.getCurrent());
            clipboard.setContents(new Object[]{data},
                    new Transfer[]{ImageTransfer.getInstance()});
        } finally {
            if (needsNudge) scalable.setScale(originalScale);
            if (clipboard != null) clipboard.dispose();
            if (graphics != null) graphics.dispose();
            if (gc != null) gc.dispose();
            if (image != null && !image.isDisposed()) image.dispose();
        }
    }
}
