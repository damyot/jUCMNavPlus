package seg.jUCMNav.actions.cutcopypaste;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.ScalableLayeredPane;
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
                LayeredPane pane = ((URNRootEditPart) (editor.getGraphicalViewer().getRootEditPart())).getScaledLayers();

                IFigure figure = pane;
                int w = figure.getSize().width;
                int h = figure.getSize().height;
                Image image = new Image(Display.getDefault(), w, h);
                GC gc = null;
                SWTGraphics graphics = null;
                // At zoom exactly 1.0, ScalableFreeformLayeredPane.paintClientArea takes the
                // `if (scale == 1.0)` fast path and skips its internal ScaledGraphics wrap.
                // Children then paint straight through SWTGraphics.fillPolygon, which on an
                // off-screen GC drops every UCM antialiased polygon fill (stubs, endpoints,
                // forks/joins, direction arrows). At any other zoom the pane allocates its own
                // ScaledGraphics, which routes polygon draws via gc.fillPath and renders fine.
                // Nudge the pane's scale by 1e-4 -- imperceptible on a multi-hundred-pixel
                // diagram -- so the working branch is taken, then restore the original scale.
                // Only applied when scale is exactly 1.0; non-identity zooms already work.
                final ScalableLayeredPane scalablePane = (pane instanceof ScalableLayeredPane)
                        ? (ScalableLayeredPane) pane : null;
                final double originalScale = scalablePane != null ? scalablePane.getScale() : 1.0;
                final boolean needsNudge = scalablePane != null && originalScale == 1.0;
                try {
                    gc = new GC(image);
                    gc.setAdvanced(true);
                    gc.setAntialias(SWT.ON);
                    gc.setTextAntialias(SWT.ON);
                    graphics = new SWTGraphics(gc);
                    graphics.translate(-pane.getBounds().x, -pane.getBounds().y);
                    if (needsNudge) scalablePane.setScale(1.0001);
                    figure.paint(graphics);

                    // TODO: Improve crop to make use of current selection.
                    screenshot = ReportUtils.cropImage(image.getImageData());
                } finally {
                    if (needsNudge && scalablePane != null) scalablePane.setScale(originalScale);
                    if (graphics != null) graphics.dispose();
                    if (gc != null) gc.dispose();
                    image.dispose();
                }
            }
        }
        return screenshot;
    }

}
