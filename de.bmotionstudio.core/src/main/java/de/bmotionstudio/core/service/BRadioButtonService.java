/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.core.service;

import de.bmotionstudio.core.AbstractBControlService;
import de.bmotionstudio.core.IBControlService;
import de.bmotionstudio.core.editor.part.BMSAbstractEditPart;
import de.bmotionstudio.core.editor.part.RadioButtonPart;
import de.bmotionstudio.core.model.control.BControl;
import de.bmotionstudio.core.model.control.RadioButton;

/**
 * @author Lukas Ladenberger
 * 
 */
public class BRadioButtonService extends AbstractBControlService implements
		IBControlService {

	@Override
	public BControl createControl() {
		return new RadioButton();
	}

	@Override
	public BMSAbstractEditPart createEditPart() {
		return new RadioButtonPart();
	}

	@Override
	public Class<?> getControlClass() {
		return RadioButton.class;
	}
	
}
