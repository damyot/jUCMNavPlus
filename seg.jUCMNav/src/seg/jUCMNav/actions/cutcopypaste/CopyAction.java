package seg.jUCMNav.actions.cutcopypaste;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import seg.jUCMNav.actions.SelectionHelper;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.editors.UrnEditor;
import seg.jUCMNav.editparts.URNRootEditPart;
import seg.jUCMNav.importexport.reports.utils.ReportUtils;
import seg.jUCMNav.model.commands.cutcopypaste.CopyCommand;

public class CopyAction extends SelectionAction {

    public CopyAction(IWorkbenchPart part) {
        super(part);
        setId(ActionFactory.COPY.getId());
        setText(GEFMessages.CopyAction_Label);
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
    }

    protected boolean calculateEnabled() {
        return true;
    }

    public void run() {
        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        // Don't need to put this on the stack.
        if (sel.getUrnspec() != null) {
            ImageData screenshot = buildScreenshot(getWorkbenchPart());

            CopyCommand cmd = new CopyCommand(sel.getUrnspec(), getSelectedObjects(), screenshot);
            cmd.execute();
        }
    }

    public static ImageData buildScreenshot(IWorkbenchPart part) {
        ImageData screenshot = null;

        if (part instanceof UCMNavMultiPageEditor) {
            UCMNavMultiPageEditor multi = (UCMNavMultiPageEditor) part;
            if (multi.getCurrentPage() != null) {
                UrnEditor editor = (UrnEditor) multi.getCurrentPage();
                LayeredPane layeredPane = ((URNRootEditPart) (editor.getGraphicalViewer().getRootEditPart())).getScaledLayers();
                ScalableFreeformLayeredPane pane = (ScalableFreeformLayeredPane) layeredPane;

                // Workaround for issue #4: at zoom EXACTLY 1.0, the live
                // ScalableFreeformLayeredPane.paintClientArea takes its
                // `if (scale == 1.0)` fast path and skips the internal
                // ScaledGraphics wrap; children then paint straight through
                // SWTGraphics.fillPolygon, which on an off-screen GC silently
                // drops every UCM antialiased polygon fill (stubs, endpoints,
                // AND/OR forks/joins, direction arrows). Earlier attempts to
                // nudge by 1e-5 directly via pane.setScale did not visibly
                // change the rendered image -- either modern draw2d compares
                // scale to 1.0 with an epsilon larger than 1e-5, or
                // setScale() didn't take effect before the immediate paint.
                // Go through ZoomManager.setZoom instead (the same public
                // API the zoom toolbar uses), which fires the full zoom
                // pipeline (invalidate, revalidate, ZoomListener events) and
                // uses a clearly-out-of-epsilon nudge of 1e-3 -- still
                // sub-pixel on any diagram up to ~1000 px wide and not
                // visible to the user because the restore happens in the
                // same UI tick (the two repaint requests coalesce into one
                // on-screen paint at the original zoom).
                final ZoomManager zoomManager = (ZoomManager) editor.getGraphicalViewer()
                        .getProperty(ZoomManager.class.toString());
                final double originalZoom = (zoomManager != null) ? zoomManager.getZoom() : 1.0;
                final boolean needsNudge = (zoomManager != null) && Math.abs(originalZoom - 1.0) < 1e-6;
                try {
                    if (needsNudge) {
                        zoomManager.setZoom(originalZoom + 0.001);
                    }

                    IFigure figure = pane;
                    int w = figure.getSize().width;
                    int h = figure.getSize().height;
                    Image image = new Image(Display.getDefault(), w, h);
                    GC gc = null;
                    SWTGraphics graphics = null;
                    try {
                        gc = new GC(image);
                        // Force GDI+ on Windows so antialiased Polygon/Polyline fills (used by every
                        // UCM PathNode -- stubs, endpoints, forks/joins, direction arrows, the actor
                        // stickman) actually render off-screen. Without this, SWTGraphics calls
                        // gc.setAntialias(SWT.ON) on a non-advanced GC and the subsequent
                        // fillPolygon/drawPolygon silently no-op, so those shapes vanish from the
                        // bitmap while GrlNodeFigure's plain rectangle/ellipse Shape paths still work.
                        gc.setAdvanced(true);
                        gc.setAntialias(SWT.ON);
                        gc.setTextAntialias(SWT.ON);
                        graphics = new SWTGraphics(gc);
                        graphics.translate(-pane.getBounds().x, -pane.getBounds().y);
                        figure.paint(graphics);

                        // TODO: Improve crop to make use of current selection.
                        screenshot = ReportUtils.cropImage(image.getImageData());
                    } finally {
                        if (graphics != null) graphics.dispose();
                        if (gc != null) gc.dispose();
                        image.dispose();
                    }
                } finally {
                    if (needsNudge) {
                        zoomManager.setZoom(originalZoom);
                    }
                }
            }
        }
        return screenshot;
    }

}
