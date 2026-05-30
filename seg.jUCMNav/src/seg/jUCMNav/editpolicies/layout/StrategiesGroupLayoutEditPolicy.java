package seg.jUCMNav.editpolicies.layout;

import grl.ContributionContext;
import grl.ContributionContextGroup;
import grl.EvaluationStrategy;
import grl.StrategiesGroup;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

import seg.jUCMNav.model.commands.transformations.MoveContributionContextCommand;
import seg.jUCMNav.model.commands.transformations.MoveDynamicContextCommand;
import seg.jUCMNav.model.commands.transformations.MoveScenarioCommand;
import seg.jUCMNav.model.commands.transformations.MoveStrategyCommand;
import seg.jUCMNav.model.commands.transformations.MoveTimepointCommand;
import ucm.scenario.ScenarioDef;
import ucm.scenario.ScenarioGroup;
import urn.dyncontext.DynamicContext;
import urn.dyncontext.DynamicContextGroup;
import urn.dyncontext.Timepoint;
import urn.dyncontext.TimepointGroup;

/**
 * Routes palette create-requests for scenarios / strategies / contribution
 * contexts / dynamic contexts / timepoints onto their respective tree groups.
 *
 * Originally extended LayoutEditPolicy, but that base class is graphical-only:
 * every getHost() call casts to GraphicalEditPart, and several internal hooks
 * (activate/deactivate/setListener/getTargetEditPart) call getHost(). Tree
 * edit parts crash on every one of them under modern GEF (3.25.x). The fix is
 * to extend AbstractEditPolicy and reimplement just the two routing methods
 * we need; AbstractEditPolicy.getHost() returns EditPart without a cast.
 *
 * @author etremblay, jkealey
 */
public class StrategiesGroupLayoutEditPolicy extends AbstractEditPolicy {

    @Override
    public EditPart getTargetEditPart(Request request) {
        Object type = request.getType();
        if (RequestConstants.REQ_CREATE.equals(type)
                || RequestConstants.REQ_ADD.equals(type)
                || RequestConstants.REQ_MOVE.equals(type)
                || RequestConstants.REQ_CLONE.equals(type)) {
            return getHost();
        }
        return null;
    }

    @Override
    public Command getCommand(Request request) {
        if (RequestConstants.REQ_CREATE.equals(request.getType())) {
            return getCreateCommand((CreateRequest) request);
        }
        return null;
    }

    protected Command getCreateCommand(CreateRequest request) {
        Object newObjectType = null;
        if (request.getNewObject() != null)
            newObjectType = request.getNewObjectType();

        EditPart host = getHost();
        if (host == null) return null;
        EditPart target = host.getTargetEditPart(request);
        if (target == null) return null;
        Object targetModel = target.getModel();

        if (newObjectType == ScenarioDef.class && targetModel instanceof ScenarioGroup) {
            Object obj = request.getNewObject();
            if (obj instanceof ScenarioDef) {
                return new MoveScenarioCommand((ScenarioGroup) targetModel, (ScenarioDef) obj);
            }
        } else if (newObjectType == EvaluationStrategy.class && targetModel instanceof StrategiesGroup) {
            Object obj = request.getNewObject();
            if (obj instanceof EvaluationStrategy) {
                return new MoveStrategyCommand((StrategiesGroup) targetModel, (EvaluationStrategy) obj);
            }
        } else if (newObjectType == ContributionContext.class && targetModel instanceof ContributionContextGroup) {
            Object obj = request.getNewObject();
            if (obj instanceof ContributionContext) {
                return new MoveContributionContextCommand((ContributionContextGroup) targetModel, (ContributionContext) obj);
            }
        } else if (newObjectType == DynamicContext.class && targetModel instanceof DynamicContextGroup) {
            Object obj = request.getNewObject();
            if (obj instanceof DynamicContext) {
                return new MoveDynamicContextCommand((DynamicContextGroup) targetModel, (DynamicContext) obj);
            }
        } else if (newObjectType == Timepoint.class && targetModel instanceof TimepointGroup) {
            Object obj = request.getNewObject();
            if (obj instanceof Timepoint) {
                return new MoveTimepointCommand((TimepointGroup) targetModel, (Timepoint) obj);
            }
        }

        return null;
    }
}
