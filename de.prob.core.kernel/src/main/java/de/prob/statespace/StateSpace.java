package de.prob.statespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.IAnimator;
import de.prob.animator.command.EvaluateFormulasCommand;
import de.prob.animator.command.ExploreStateCommand;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.command.ICommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvaluationResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.OpInfo;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.Machine;
import de.prob.model.representation.Variable;

/**
 * 
 * The StateSpace is where the animation of a given model is carried out. The
 * methods in the StateSpace allow the user to:
 * 
 * 1) Find new states and operations
 * 
 * 2) Move between states within the StateSpace to inspect them
 * 
 * 3) Perform random animation steps
 * 
 * 4) Evaluate custom predicates and expressions -
 * 
 * 5) Register listeners that are notified of animation steps/new states and
 * operations.
 * 
 * 6) View information about the current state
 * 
 * @author joy
 * 
 */
/**
 * @author joy
 * 
 */
public class StateSpace extends StateSpaceGraph implements IAnimator {

	Logger logger = LoggerFactory.getLogger(StateSpace.class);

	private transient IAnimator animator;

	private ICommand loadcmd;

	private final HashSet<StateId> explored = new HashSet<StateId>();

	private final HashMap<IEvalElement, Set<Object>> formulaRegistry = new HashMap<IEvalElement, Set<Object>>();

	private final List<IStateSpaceChangeListener> stateSpaceListeners = new ArrayList<IStateSpaceChangeListener>();

	private final HashMap<String, StateId> states = new HashMap<String, StateId>();
	private final HashMap<String, OpInfo> ops = new HashMap<String, OpInfo>();
	private AbstractElement model;
	private final Map<StateId, Map<IEvalElement, EvaluationResult>> values = new HashMap<StateId, Map<IEvalElement, EvaluationResult>>();

	private final HashSet<StateId> invariantOk = new HashSet<StateId>();
	private final HashSet<StateId> timeoutOccured = new HashSet<StateId>();
	private final HashMap<StateId, Set<String>> operationsWithTimeout = new HashMap<StateId, Set<String>>();

	public final StateId __root;

	@Inject
	public StateSpace(final IAnimator animator,
			final DirectedMultigraphProvider graphProvider) {
		super(graphProvider.get());
		this.animator = animator;
		__root = new StateId("root", "1", this);
		addVertex(__root);
		states.put(__root.getId(), __root);
	}

	public StateId getRoot() {
		this.explore(__root);
		return __root;
	}

	// MAKE CHANGES TO THE STATESPACE GRAPH
	/**
	 * Takes a state id and calculates the successor states, the invariant,
	 * timeout, etc.
	 * 
	 * @param state
	 */
	public String explore(final StateId state) {
		if (!containsVertex(state)) {
			throw new IllegalArgumentException("state " + state
					+ " does not exist");
		}

		final ExploreStateCommand command = new ExploreStateCommand(
				state.getId());
		animator.execute(command);
		extractInformation(state, command);

		explored.add(state);
		final List<OpInfo> enabledOperations = command.getEnabledOperations();

		for (final OpInfo op : enabledOperations) {
			if (!containsEdge(op)) {
				op.setModel(model);
				ops.put(op.id, op);
				notifyStateSpaceChange(op.id,
						containsVertex(getVertex(op.dest)));
				final StateId newState = new StateId(op.dest, op.targetState,
						this);
				addVertex(newState);
				states.put(newState.getId(), newState);
				addEdge(states.get(op.src), states.get(op.dest), op);
			}
		}
		evaluateFormulas(state);
		return toString();
	}

	private void extractInformation(final StateId state,
			final ExploreStateCommand command) {
		operationsWithTimeout.put(state, command.getOperationsWithTimeout());
		if (command.isInvariantOk()) {
			invariantOk.add(state);
		}
		if (command.isTimeoutOccured()) {
			timeoutOccured.add(state);
		}
	}

	public String explore(final String state) {
		return explore(getVertex(state));
	}

	public String explore(final int i) {
		final String si = String.valueOf(i);
		return explore(si);
	}

	public StateId getVertex(final String key) {
		return states.get(key);
	}

	/**
	 * Explore state if not explored and return it.
	 * 
	 * @param state
	 * @return
	 */
	public StateId getState(final StateId state) {
		if (!isExplored(state)) {
			explore(state);
		}
		return state;
	}

	/**
	 * Get the target State for given operation, explore it, and return it.
	 * 
	 * @param op
	 * @return
	 */
	public StateId getState(final OpInfo op) {
		final StateId edgeTarget = getEdgeTarget(op);
		if (!isExplored(edgeTarget)) {
			explore(edgeTarget);
		}
		return edgeTarget;
	}

	/**
	 * Takes the name of an operation and a predicate and finds Operations that
	 * satisfy the name and predicate at the given stateId. New Operations are
	 * added to the graph.
	 * 
	 * @param stateId
	 * @param name
	 * @param predicate
	 * @param nrOfSolutions
	 * @return list of operations
	 * @throws BException
	 */
	public List<OpInfo> opFromPredicate(final StateId stateId,
			final String name, final String predicate, final int nrOfSolutions)
			throws BException {
		final ClassicalB pred = new ClassicalB(predicate);
		final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(
				stateId.getId(), name, pred, nrOfSolutions);
		animator.execute(command);
		final List<OpInfo> newOps = command.getOperations();

		// (id,name,src,dest,args)
		for (final OpInfo op : newOps) {
			op.setModel(model);
			if (!containsEdge(op)) {
				ops.put(op.id, op);
				notifyStateSpaceChange(op.id,
						containsVertex(getVertex(op.dest)));
				addEdge(getVertex(op.src), getVertex(op.dest), op);
			}
		}
		return newOps;
	}

	/**
	 * Checks if the state with stateId is a deadlock
	 * 
	 * @param state
	 * @return returns if a specific state is deadlocked
	 */
	public boolean isDeadlock(final StateId state) {
		if (!isExplored(state)) {
			explore(state);
		}
		return outDegreeOf(state) == 0;
	}

	public boolean hasInvariantViolation(final StateId state) {
		if (!isExplored(state)) {
			explore(state);
		}
		return !invariantOk.contains(state);
	}

	/**
	 * Checks if the state with stateId has been explored yet
	 * 
	 * @param state
	 * @return returns if a specific state is explored
	 */
	public boolean isExplored(final StateId state) {
		if (!containsVertex(state)) {
			throw new IllegalArgumentException("Unknown State id");
		}
		return explored.contains(state);
	}

	// EVALUATE FORMULAS

	/**
	 * The method eval takes a stateId and a list of formulas and returns a list
	 * of EvaluationResults for the given formulas. It first checks to see if
	 * any of the formulas have cached values for the given state and then, if
	 * there are formulas that have not yet been calculated, it contacts Prolog
	 * to get the remaining values.
	 * 
	 * @param stateId
	 * @param code
	 * @return
	 */
	public List<EvaluationResult> eval(final StateId stateId,
			final List<IEvalElement> code) {
		if (!containsVertex(stateId)) {
			throw new IllegalArgumentException("state does not exist");
		}
		if (code.isEmpty()) {
			return new ArrayList<EvaluationResult>();
		}

		// Check to see if there are any cached results for the given StateId
		Map<IEvalElement, EvaluationResult> map = values.get(stateId);
		if (map == null) {
			map = new HashMap<IEvalElement, EvaluationResult>();
		}

		// Filter out any EvalElements that have already been calculated
		Set<IEvalElement> calculated = map.keySet();
		List<IEvalElement> toEval = new ArrayList<IEvalElement>();
		for (IEvalElement iEvalElement : code) {
			if (!calculated.contains(iEvalElement)) {
				toEval.add(iEvalElement);
			}
		}

		// If there are formulas for which no value has been calculated, send
		// them to prolog to get the results
		List<EvaluationResult> fromProlog;
		if (!toEval.isEmpty()) {
			final EvaluateFormulasCommand command = new EvaluateFormulasCommand(
					toEval, stateId.getId(), model);
			execute(command);

			fromProlog = command.getValues();
		} else {
			fromProlog = new ArrayList<EvaluationResult>();
		}

		// Merge the calculated results from Prolog with the cached results for
		// the desired list
		final List<EvaluationResult> values = new ArrayList<EvaluationResult>();
		for (IEvalElement iEvalElement : code) {
			if (calculated.contains(iEvalElement)) {
				values.add(map.get(iEvalElement));
			} else {
				values.add(fromProlog.get(toEval.indexOf(iEvalElement)));
			}
		}

		return values;

	}

	/**
	 * The method evaluateFormulas calculates all of the subscribed formulas for
	 * the given state and caches them.
	 * 
	 * @param state
	 */
	public void evaluateFormulas(final StateId state) {
		if (!canBeEvaluated(state)) {
			return;
		}
		final Set<IEvalElement> formulas = formulaRegistry.keySet();
		final List<IEvalElement> toEvaluate = new ArrayList<IEvalElement>();
		Map<IEvalElement, EvaluationResult> valueMap = new HashMap<IEvalElement, EvaluationResult>();

		// Check to see which formulas have subscribers. These are the ones that
		// will be calculated
		for (final IEvalElement iEvalElement : formulas) {
			if (!formulaRegistry.get(iEvalElement).isEmpty()) {
				toEvaluate.add(iEvalElement);
			}
		}
		final List<EvaluationResult> results = eval(state, toEvaluate);

		assert results.size() == toEvaluate.size();
		if (results != null) {
			for (int i = 0; i < results.size(); i++) {
				valueMap.put(toEvaluate.get(i), results.get(i));
			}
		}
		values.put(state, valueMap);
	}

	/**
	 * Calculated the registered formulas at the given state and returns the
	 * cached values
	 * 
	 * @param stateId
	 * @return
	 */
	public Map<IEvalElement, EvaluationResult> valuesAt(final StateId stateId) {
		if (canBeEvaluated(stateId)) {
			evaluateFormulas(stateId);
		}
		if (values.containsKey(stateId)) {
			return values.get(stateId);
		}
		return new HashMap<IEvalElement, EvaluationResult>();
	}

	private boolean canBeEvaluated(final StateId stateId) {
		for (OpInfo opInfo : outgoingEdgesOf(stateId)) {
			if (opInfo.getName().equals("$setup_constants")
					|| opInfo.getName().equals("$initialise_machine")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * If a class is interested in having a particular formula calculated and
	 * cached whenever a new state is explored, then they "subscribe" to that
	 * formula with a reference to themselves.
	 * 
	 * @param subscriber
	 * @param formulaOfInterest
	 */
	public void subscribe(final Object subscriber,
			final IEvalElement formulaOfInterest) {
		if (formulaRegistry.containsKey(formulaOfInterest)) {
			formulaRegistry.get(formulaOfInterest).add(subscriber);
		} else {
			HashSet<Object> subscribers = new HashSet<Object>();
			subscribers.add(subscriber);
			formulaRegistry.put(formulaOfInterest, subscribers);
		}
	}

	/**
	 * If a subscribed class is no longer interested in the value of a
	 * particular formula, then they can unsubscribe to that formula
	 * 
	 * @param subscriber
	 * @param formulaOfInterest
	 */
	public void unsubscribe(final Object subscriber,
			final IEvalElement formulaOfInterest) {
		if (formulaRegistry.containsKey(formulaOfInterest)) {
			final Set<Object> subscribers = formulaRegistry
					.get(formulaOfInterest);
			subscribers.remove(subscriber);
		}
	}

	// ANIMATOR

	@Override
	public void execute(final ICommand command) {
		animator.execute(command);
	}

	@Override
	public void execute(final ICommand... commands) {
		animator.execute(commands);
	}

	@Override
	public void sendInterrupt() {
		animator.sendInterrupt();
	}

	// NOTIFICATION SYSTEM

	/**
	 * Adds an IStateSpaceChangeListener to the list of StateSpaceListeners.
	 * This listener will be notified whenever a new operation or a new state is
	 * added to the graph.
	 * 
	 * @param l
	 */
	public void registerStateSpaceListener(final IStateSpaceChangeListener l) {
		stateSpaceListeners.add(l);
	}

	private void notifyStateSpaceChange(final String opName,
			final boolean isDestStateNew) {
		for (final IStateSpaceChangeListener listener : stateSpaceListeners) {
			listener.newTransition(opName, isDestStateNew);
		}
	}

	// METHODS TO MAKE THE INTERACTION WITH THE GROOVY SHELL EASIER
	@Override
	public String toString() {
		String result = "";
		result += super.toString();
		return result;
	}

	public String printInfo() {
		String result = "";
		result += "Formulas: \n" + values.toString() + "\n";
		result += "Invariants Ok: \n  " + invariantOk.toString() + "\n";
		result += "Timeout Occured: \n  " + timeoutOccured.toString() + "\n";
		result += "Operations With Timeout: \n  "
				+ operationsWithTimeout.toString() + "\n";
		return result;
	}

	public HashMap<String, StateId> getStates() {
		return states;
	}

	public HashMap<String, OpInfo> getOps() {
		return ops;
	}

	public String printOps(final StateId state) {
		final StringBuilder sb = new StringBuilder();
		final Collection<OpInfo> opIds = outgoingEdgesOf(state);
		Set<String> withTO = operationsWithTimeout.get(state);

		sb.append("Operations: \n");
		for (final OpInfo opId : opIds) {
			sb.append("  " + opId.id + ": " + opId.toString());
			if (withTO.contains(opId.id)) {
				sb.append(" (WITH TIMEOUT)");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String printState(final StateId state) {
		final StringBuilder sb = new StringBuilder();

		explore(state);

		sb.append("STATE: " + state + "\n\n");
		sb.append("VALUES:\n");
		Map<IEvalElement, EvaluationResult> currentState = values.get(state);
		if (currentState != null) {
			final Set<Entry<IEvalElement, EvaluationResult>> entrySet = currentState
					.entrySet();
			for (final Entry<IEvalElement, EvaluationResult> entry : entrySet) {
				sb.append("  " + entry.getKey().getCode() + " -> "
						+ entry.getValue().toString() + "\n");
			}
		}
		sb.append("\nINVARIANT: ");
		if (invariantOk.contains(state)) {
			sb.append(" OK\n");
		} else {
			sb.append(" KO\n");
		}
		if (timeoutOccured.contains(state)) {
			sb.append("\nTIMEOUT OCCURED\n");
		}
		return sb.toString();
	}

	public History getTrace(final int state) {
		final StateId id = states.get(String.valueOf(state));
		final List<OpInfo> path = new DijkstraShortestPath<StateId, OpInfo>(
				this.getGraph(), this.getRoot(), id).getPathEdgeList();
		History h = new History(this);
		for (final OpInfo opInfo : path) {
			h = h.add(opInfo.getId());
		}
		return h;
	}

	public void setAnimator(final IAnimator animator) {
		this.animator = animator;
	}

	public ICommand getLoadcmd() {
		return loadcmd;
	}

	public void setLoadcmd(final ICommand loadcmd) {
		this.loadcmd = loadcmd;
	}

	public void setModel(final AbstractElement model) {
		this.model = model;

		Set<Machine> machines = model.getChildrenOfType(Machine.class);
		for (Machine machine : machines) {
			for (Variable variable : machine.getChildrenOfType(Variable.class)) {
				subscribe(this, variable.getExpression());
			}
		}
	}

	public AbstractElement getModel() {
		return model;
	}

	public Object asType(final Class<?> className) {
		if (className.getSimpleName().equals("AbstractModel")) {
			if (model instanceof AbstractModel) {
				return model;
			}
		}
		if (className.getSimpleName().equals("EventBModel")) {
			if (model instanceof EventBModel) {
				return model;
			}
		}
		if (className.getSimpleName().equals("ClassicalBModel")) {
			if (model instanceof ClassicalBModel) {
				return model;
			}
		}
		if (className.getSimpleName().equals("History")) {
			return new History(this);
		}
		throw new ClassCastException("An element of class " + className
				+ " was not found");
	}

	public Object getAt(final Object that) {
		StateId id = null;
		if (that instanceof String) {
			id = getVertex((String) that);
		}
		if (that instanceof Integer) {
			id = getVertex(String.valueOf(that));
		}
		if (id != null) {
			return id;
		}
		throw new IllegalArgumentException(
				"StateSpace does not contain vertex " + that);
	}
}
