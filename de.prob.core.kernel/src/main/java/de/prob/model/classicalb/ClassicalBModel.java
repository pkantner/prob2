package de.prob.model.classicalb;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.analysis.prolog.RecursiveMachineLoader;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.eventb.BStateSchema;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.Machine;
import de.prob.model.representation.ModelElementList;
import de.prob.model.representation.RefType;
import de.prob.model.representation.StateSchema;
import de.prob.statespace.StateSpace;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class ClassicalBModel extends AbstractModel {

	private ClassicalBMachine mainMachine = null;
	private final HashSet<String> done = new HashSet<String>();
	private final StateSchema schema = new BStateSchema();

	@Inject
	public ClassicalBModel(final StateSpace statespace) {
		this.statespace = statespace;
	}

	public DirectedSparseMultigraph<String, RefType> initialize(
			final Start mainast, final RecursiveMachineLoader rml,
			final File modelFile) {

		this.modelFile = modelFile;

		final DirectedSparseMultigraph<String, RefType> graph = new DirectedSparseMultigraph<String, RefType>();

		final DomBuilder d = new DomBuilder();
		mainMachine = d.build(mainast);

		graph.addVertex(mainMachine.getName());
		ModelElementList<ClassicalBMachine> machines = new ModelElementList<ClassicalBMachine>();
		machines.add(mainMachine);

		boolean fpReached;

		do {
			fpReached = true;
			final Set<String> vertices = new HashSet<String>(
					graph.getVertices());
			for (final String machineName : vertices) {
				final Start ast = rml.getParsedMachines().get(machineName);
				if (!done.contains(machineName)) {
					ast.apply(new DependencyWalker(machineName, machines,
							graph, rml.getParsedMachines()));
					done.add(machineName);
					fpReached = false;
				}
			}
		} while (!fpReached);
		this.graph = graph;

		put(Machine.class, machines);

		for (ClassicalBMachine classicalBMachine : machines) {
			components.put(classicalBMachine.getName(), classicalBMachine);
		}

		statespace.setModel(this);
		return graph;
	}

	public ClassicalBMachine getMainMachine() {
		return mainMachine;
	}

	@Override
	public StateSchema getStateSchema() {
		return schema;
	}

	@Override
	public AbstractElement getMainComponent() {
		return getMainMachine();
	}

	@Override
	public IEvalElement parseFormula(final String formula) {
		return new ClassicalB(formula);
	}
}
