package de.bmotionstudio.core.model.observer;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.bmotionstudio.core.editor.wizard.observer.BOperationObserverWizard;
import de.bmotionstudio.core.editor.wizard.observer.ObserverWizard;
import de.bmotionstudio.core.model.control.BControl;
import de.prob.statespace.History;
import de.prob.statespace.OpInfo;

public class BOperationObserver extends Observer {

	private String operation, attribute, predicate;

	private Object value;
	
	@Override
	public void check(History history, BControl control) {

		if (operation == null || attribute == null || value == null)
			return;

		if (predicate == null || (predicate != null && predicate.length() < 1))
			predicate = "TRUE";

		try {
			List<OpInfo> opFromPredicate = history.getStatespace()
					.opFromPredicate(history.getCurrentState(), operation,
							predicate, 1);
			if (opFromPredicate != null && !opFromPredicate.isEmpty())
				control.setAttributeValue(attribute, value, true, false);
		} catch (BException e) {
			e.printStackTrace();
		}

	}

	@Override
	public ObserverWizard getWizard(Shell shell, BControl control) {
		return new BOperationObserverWizard(shell, control, this);
	}


	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		String oldVal = this.predicate;
		this.predicate = predicate;
		firePropertyChange("predicate", oldVal, predicate);
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		String oldVal = this.attribute;
		this.attribute = attribute;
		firePropertyChange("attribute", oldVal, attribute);
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		String oldVal = this.operation;
		this.operation = operation;
		firePropertyChange("operation", oldVal, operation);
	}
	
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		Object oldVal = this.value;
		this.value = value;
		firePropertyChange("value", oldVal, value);
	}
	
	@Override
	public String getType() {
		return "B Operation Observer";
	}

	@Override
	public String getDescription() {
		return "";
	}

}
