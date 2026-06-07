/*
 * Created on 29.03.2005
 */
package seg.UCMScenarioViewer.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.PlatformUI;

import seg.UCMScenarioViewer.UCMScenarioViewer;
import seg.UCMScenarioViewer.model.ScenarioGroup;
import seg.UCMScenarioViewer.utils.Helper;

/**
 * Implementation of 'font switching' command.
 */
public class ChangeFontCommand extends Command {

	private FontData newFont = UCMScenarioViewer.getApplicationFont().getFontData()[0];
	private FontData oldFont = UCMScenarioViewer.getApplicationFont().getFontData()[0];
	

	public ChangeFontCommand(FontData newFont) {
		this.newFont = newFont;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		return oldFont.getHeight() != newFont.getHeight()
			|| oldFont.getStyle() != newFont.getStyle()
			|| !oldFont.getName().equals(newFont.getName());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
        redo();
	}
    
    public void redo() {
        UCMScenarioViewer.setApplicationFont(newFont);
        refreshAllFigures();
    }

    public void undo() {
        UCMScenarioViewer.setApplicationFont(oldFont);
        refreshAllFigures();
    }

    /**
     * After swapping the applicationFont, propagate Properties.ID_REFRESH
     * through the model tree so every {@link AbstractModelElementEditPart}
     * re-calls fig.setFont(getModelElement().getFont()) and picks up the
     * new Font handle. setApplicationFont deliberately does NOT dispose
     * the previous Font (figures still reference it -- disposing crashes
     * the next paint), so any figure that doesn't get refreshed continues
     * to paint correctly with the now-leaked-but-valid old handle. The
     * refresh below is what makes the visual change take effect on the
     * already-laid-out figure tree without forcing the editor closed and
     * reopened.
     */
    private void refreshAllFigures() {
        UCMScenarioViewer viewer = Helper.getUCMScenarioViewer(PlatformUI.getWorkbench());
        if (viewer == null) return;
        ScenarioGroup group = viewer.getMSCDiagram();
        if (group == null) return;
        group.refreshAll();
    }
}
