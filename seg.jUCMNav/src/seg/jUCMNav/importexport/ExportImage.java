package seg.jUCMNav.importexport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import seg.jUCMNav.Messages;
import seg.jUCMNav.extensionpoints.IUseCaseMapExport;
import seg.jUCMNav.importexport.reports.utils.ReportUtils;
import urncore.IURNDiagram;

/**
 * Abstract base class to export images using the save functions provided by SWT
 * 
 * @author jkealey
 * 
 */
public abstract class ExportImage implements IUseCaseMapExport {

    /**
     * Force GDI+ on Windows for an off-screen GC so antialiased Polygon/Polyline fills used by UCM
     * path-node figures (stubs, endpoints, forks/joins, direction arrows, actor stickman) render to
     * the bitmap. Without this, SWTGraphics's setAntialias(SWT.ON) on a non-advanced GC silently
     * skips the fill and the shapes vanish from exported / copied images.
     */
    public static void enableAdvancedRendering(GC gc) {
        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);
        gc.setTextAntialias(SWT.ON);
    }

    /**
     * Given the IFigure, save it to a file.
     *
     * @see seg.jUCMNav.extensionpoints.IUseCaseMapExport#export(org.eclipse.draw2d.IFigure, java.io.FileOutputStream)
     */
    public void export(IFigure unzoomedPane, FileOutputStream fos) {
        // generate image
        ScalableFreeformLayeredPane pane = (ScalableFreeformLayeredPane) unzoomedPane;
        pane.setScale(pane.getScale()+ 0.01);
        
        Image image = new Image(Display.getCurrent(), pane.getSize().width, pane.getSize().height);
        GC gc = new GC(image);
        enableAdvancedRendering(gc);
        SWTGraphics graphics = new SWTGraphics(gc);
        // if the bounds are in the negative x/y, we don't see them without a translation
        graphics.translate(-pane.getBounds().x, -pane.getBounds().y);
        pane.paint(graphics);
        pane.setScale(pane.getScale()- 0.01);

        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { ReportUtils.cropImage(image.getImageData()) };
        // loader.data = new ImageData[] { image.getImageData() };
        loader.save(fos, getType());

        gc.dispose();
        image.dispose();
    }

    public void export(IFigure pane, String path) {

    	
        FileOutputStream fos = null;
        
        boolean exists = false;
        
        final String IMAGES_LOCATION = "pages" + File.separator + "img" + File.separator; //$NON-NLS-1$ //$NON-NLS-2$
        
        if( !path.contains( IMAGES_LOCATION ) ) {
        	exists = (new File( path )).exists();

        	if( exists ){

        		final String title = Messages.getString("ExportImage.GraphicsFileExists"); //$NON-NLS-1$
        		final String message = Messages.getString("ExportImage.TheFileQuote") + path + Messages.getString("ExportImage.QuoteAlreadyExists"); //$NON-NLS-1$ //$NON-NLS-2$
        		final String[] labels = { Messages.getString("ExportImage.OverwriteFile"), Messages.getString("ExportImage.CreateUniqueFilename") }; //$NON-NLS-1$ //$NON-NLS-2$

        		// Some callers (HTMLReport.export -> ExportImageGIF.export(IFigure, String)) reach
        		// here from the ModalContext worker thread; MessageDialog construction and
        		// shell.getDisplay() must happen on the UI thread. The HTML path normally skips
        		// this branch because its imgPath contains 'pages/img/', but defensively wrap so
        		// any future caller with a different path layout does not crash. (Note: the dialog
        		// result is currently not used downstream -- the export proceeds regardless of
        		// 'Overwrite' vs 'Create Unique Filename'. Wiring the result through is a separate
        		// follow-up.)
        		Display.getDefault().syncExec(new Runnable() {
        			public void run() {
        				IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        				Shell shell = win != null ? win.getShell() : null;
        				if (shell == null) return;
        				MessageDialog md = new MessageDialog(shell, title,
        						shell.getDisplay().getSystemImage(SWT.ICON_QUESTION),
        						message, MessageDialog.QUESTION, labels, 1);
        				md.open();
        			}
        		});
        	}
        }
        
        try {
            fos = new FileOutputStream(path);
            export(pane, fos);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } finally {
            // close the stream
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    public void export(IURNDiagram diagram, FileOutputStream fos) {
        // not used.
    }

    public void export(IURNDiagram diagram, String path) {
        // not used.
    }

    /**
     * 
     * @return an SWT constant representing the image type.
     */
    public abstract int getType();

}
