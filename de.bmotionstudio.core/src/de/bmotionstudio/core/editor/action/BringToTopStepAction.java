/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.core.editor.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.bmotionstudio.core.BMotionEditorPlugin;
import de.bmotionstudio.core.editor.command.BringToTopStepCommand;
import de.bmotionstudio.core.editor.part.BMSAbstractEditPart;
import de.bmotionstudio.core.editor.part.VisualizationPart;
import de.bmotionstudio.core.model.control.BControl;

public class BringToTopStepAction extends SelectionAction {

	public final static String ID = "de.bmotionstudio.core.action.bringToTopStep";

	public BringToTopStepAction(final IWorkbenchPart part) {
		super(part);
		setLazyEnablementCalculation(false);
	}

	protected void init() {
		setText("Bring to top (Step)");
		setToolTipText("Bring to top (Step)");
		setId(ID);
		ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(
				BMotionEditorPlugin.PLUGIN_ID,
				"icons/eclipse16/uncaught_ovr.gif");
		if (icon != null) {
			setImageDescriptor(icon);
		}
		setEnabled(false);
	}

	@Override
	protected boolean calculateEnabled() {
		List<?> selectedObjects = getSelectedObjects();
		if (selectedObjects.size() == 1) {
			if (selectedObjects.get(0) instanceof VisualizationPart) {
				return false;
			}
		}
		return true;
	}

	public BringToTopStepCommand createBringToTopStepCommand(
			List<BControl> modelList) {
		BringToTopStepCommand command = new BringToTopStepCommand();
		command.setControlList(modelList);
		return command;
	}

	public void run() {

		List<BControl> modelList = new ArrayList<BControl>();

		List<?> selectedObjects = getSelectedObjects();

		for (Object obj : selectedObjects) {
			if (obj instanceof BMSAbstractEditPart) {
				modelList
						.add((BControl) ((BMSAbstractEditPart) obj).getModel());
			}
		}

		execute(createBringToTopStepCommand(modelList));

	}

}
