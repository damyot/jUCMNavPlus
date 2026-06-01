package seg.jUCMNav.actions.handlers;

import org.eclipse.ui.IEditorActionDelegate;

import seg.jUCMNav.actions.metrics.CalculateMetricsActionDelegate;

public class CalculateMetricsHandler extends LegacyDelegateHandler {
    @Override
    protected IEditorActionDelegate createDelegate() {
        return new CalculateMetricsActionDelegate();
    }
}
