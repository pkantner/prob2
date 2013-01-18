/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.core.editor.figure;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import de.bmotionstudio.core.AttributeConstants;
import de.bmotionstudio.core.model.control.BControl;

public class MouseClickAdapter extends MouseAdapter {

	private BControl control;

	public MouseClickAdapter(BControl control) {
		this.control = control;
	}

	public void mousePressed(MouseEvent e) {
		// TODO: Reimplement me!!!
		// control.executeEvent(AttributeConstants.EVENT_MOUSECLICK);
	}

	// TODO: change mouse cursor!
	public void mouseEntered(MouseEvent e) {
		//TODO: Reimplement me!!!
//		if (control.getEvent(AttributeConstants.EVENT_MOUSECLICK) != null) {
//			if (control.getAttributeValue(AttributeConstants.ATTRIBUTE_ENABLED) != null) {
//				if (!Boolean.valueOf(control.getAttributeValue(
//						AttributeConstants.ATTRIBUTE_ENABLED).toString())) {
//					return;
//				}
//			}
//		}
	}

	// TODO: change mouse cursor!
	public void mouseExited(MouseEvent e) {
		//TODO: Reimplement me!!!
//		if (control.getEvent(AttributeConstants.EVENT_MOUSECLICK) != null) {
//		}
	}

}
