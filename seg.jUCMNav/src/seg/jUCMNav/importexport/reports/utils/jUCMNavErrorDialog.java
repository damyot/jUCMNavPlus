package seg.jUCMNav.importexport.reports.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog showing an error message caught during report generation.
 *
 * <p>Called from worker threads (typically the {@code ModalContext} thread that
 * runs report generation behind a wizard's progress dialog), so the actual
 * MessageBox construction is dispatched to the UI thread via
 * {@link Display#syncExec(Runnable)}. The parent shell is also resolved on the
 * UI thread; the previous implementation captured {@code new Display()} and
 * {@code new Shell(display)} as instance fields, which executed on whatever
 * thread the caller was on (usually NOT the UI thread). That triggered both a
 * second {@code Display} allocation -- illegal -- and an "Invalid thread access"
 * SWTException when the syncExec'd runnable tried to use the mis-bound shell.
 *
 * @author dessure
 */
public class jUCMNavErrorDialog {

    /**
     * display the message box
     *
     * @param errorMessage
     *            the error message coming from the exception
     */
    public jUCMNavErrorDialog(final String errorMessage) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                Shell shell = Display.getDefault().getActiveShell();
                if (shell == null) {
                    IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (win != null) {
                        shell = win.getShell();
                    }
                }
                MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                messageBox.setText("Warning"); //$NON-NLS-1$
                messageBox.setMessage(errorMessage);
                messageBox.open();
            }
        });
    }
}
