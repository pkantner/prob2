package de.prob.ui.visualization;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.PartInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.visualization.AnimationNotLoadedException;

public class OpenTimeValueVizHandler extends AbstractHandler implements
		IHandler {

	Logger logger = LoggerFactory.getLogger(OpenTimeValueVizHandler.class);

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			VisualizationUtil
					.createVisualizationViewPart("sessions/ValueOverTime/");
		} catch (PartInitException e) {
			logger.error("Could not create value vs time visualization view: "
					+ e.getMessage());
		} catch (AnimationNotLoadedException e) {
			logger.error("Could not create value vs time visualization because an animation is not loaded: "
					+ e.getMessage());
		}
		return null;
	}

}
