package de.prob.statespace;

import java.util.List;

public interface IStatesCalculatedListener {
	public void newTransitions(List<? extends OpInfo> newOps);
}
