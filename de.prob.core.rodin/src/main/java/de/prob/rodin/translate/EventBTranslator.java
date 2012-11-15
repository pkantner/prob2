package de.prob.rodin.translate;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IConvergenceElement.Convergence;
import org.eventb.core.IEventBProject;
import org.eventb.core.IEventBRoot;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCCarrierSet;
import org.eventb.core.ISCConstant;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCExtendsContext;
import org.eventb.core.ISCGuard;
import org.eventb.core.ISCInternalContext;
import org.eventb.core.ISCInvariant;
import org.eventb.core.ISCMachineRoot;
import org.eventb.core.ISCParameter;
import org.eventb.core.ISCRefinesEvent;
import org.eventb.core.ISCRefinesMachine;
import org.eventb.core.ISCVariable;
import org.eventb.core.ISCVariant;
import org.eventb.core.ISCWitness;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import de.prob.model.eventb.newdom.Context;
import de.prob.model.eventb.newdom.Event;
import de.prob.model.eventb.newdom.Event.EventType;
import de.prob.model.eventb.newdom.EventBAction;
import de.prob.model.eventb.newdom.EventBAxiom;
import de.prob.model.eventb.newdom.EventBConstant;
import de.prob.model.eventb.newdom.EventBGuard;
import de.prob.model.eventb.newdom.EventBInvariant;
import de.prob.model.eventb.newdom.EventBMachine;
import de.prob.model.eventb.newdom.EventBVariable;
import de.prob.model.eventb.newdom.EventParameter;
import de.prob.model.eventb.newdom.Variant;
import de.prob.model.eventb.newdom.Witness;
import de.prob.model.representation.newdom.AbstractElement;
import de.prob.model.representation.newdom.BSet;

public class EventBTranslator {
	//
	// Map<String, ISCMachineRoot> machines = new HashMap<String,
	// ISCMachineRoot>();
	// Map<String, ISCContextRoot> contexts = new HashMap<String,
	// ISCContextRoot>();

	AbstractElement mainComponent;
	private final IEventBProject eventBProject;

	public EventBTranslator(final IEventBRoot root) {
		eventBProject = root.getEventBProject();
		IInternalElementType<? extends IInternalElement> elementType = root
				.getElementType();
		String name = elementType.getName();
		String id = elementType.getId();
		if (id.equals("org.eventb.core.machineFile")) {
			ISCMachineRoot scMachineRoot = eventBProject.getSCMachineRoot(root
					.getElementName());
			mainComponent = translateMachine(scMachineRoot);
		}
		if (root instanceof ISCContextRoot) {
			mainComponent = translateContext((ISCContextRoot) root);
		}
	}

	private Context translateContext(final ISCContextRoot root) {
		Context c = new Context(root.getComponentName());
		try {
			List<Context> exts = new ArrayList<Context>();
			for (ISCExtendsContext iscExtendsContext : root
					.getSCExtendsClauses()) {
				String componentName = iscExtendsContext.getAbstractSCContext()
						.getRodinFile().getBareName();
				exts.add(translateContext(eventBProject
						.getSCContextRoot(componentName)));
			}
			c.addExtends(exts);

			List<BSet> sets = new ArrayList<BSet>();
			for (ISCCarrierSet iscCarrierSet : root.getSCCarrierSets()) {
				sets.add(new BSet(iscCarrierSet.getIdentifierString()));
			}
			c.addSets(sets);

			List<EventBAxiom> axioms = new ArrayList<EventBAxiom>();
			for (ISCAxiom iscAxiom : root.getSCAxioms()) {
				String elementName = iscAxiom.getRodinFile().getBareName();
				String predicateString = iscAxiom.getPredicateString();
				boolean theorem = iscAxiom.isTheorem();
				axioms.add(new EventBAxiom(elementName, predicateString,
						theorem));
			}
			c.addAxioms(axioms);

			List<EventBConstant> constants = new ArrayList<EventBConstant>();
			for (ISCConstant iscConstant : root.getSCConstants()) {
				constants.add(new EventBConstant(iscConstant.getElementName()));
			}
			c.addConstants(constants);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		return c;
	}

	private EventBMachine translateMachine(final ISCMachineRoot root) {
		EventBMachine machine = new EventBMachine(root.getComponentName());

		try {
			List<EventBMachine> refines = new ArrayList<EventBMachine>();
			ISCRefinesMachine[] scRefinesClauses = root.getSCRefinesClauses();
			for (ISCRefinesMachine iscRefinesMachine : scRefinesClauses) {
				IRodinFile abstractSCMachine = iscRefinesMachine
						.getAbstractSCMachine();
				String bareName = abstractSCMachine.getBareName();
				String elementName = abstractSCMachine.getElementName();
				refines.add(translateMachine(eventBProject
						.getSCMachineRoot(bareName)));
			}
			machine.addRefines(refines);

			List<Context> sees = new ArrayList<Context>();
			for (ISCInternalContext iscInternalContext : root
					.getSCSeenContexts()) {
				String componentName = iscInternalContext.getComponentName();
				sees.add(translateContext(eventBProject
						.getSCContextRoot(componentName)));
			}
			machine.addSees(sees);

			List<EventBVariable> variables = new ArrayList<EventBVariable>();
			for (ISCVariable iscVariable : root.getSCVariables()) {
				variables.add(new EventBVariable(iscVariable.getElementName()));
			}
			machine.addVariables(variables);

			List<EventBInvariant> invariants = new ArrayList<EventBInvariant>();
			for (ISCInvariant iscInvariant : root.getSCInvariants()) {
				String elementName = iscInvariant.getElementName();
				String predicateString = iscInvariant.getPredicateString();
				boolean theorem = iscInvariant.isTheorem();
				invariants.add(new EventBInvariant(elementName,
						predicateString, theorem));
			}
			machine.addInvariants(invariants);

			List<Variant> variant = new ArrayList<Variant>();
			for (ISCVariant iscVariant : root.getSCVariants()) {
				variant.add(new Variant(iscVariant.getExpressionString()));
			}
			machine.addVariant(variant);

			List<Event> events = new ArrayList<Event>();
			ISCEvent[] scEvents = root.getSCEvents();
			for (ISCEvent iscEvent : scEvents) {
				events.add(extractEvent(iscEvent));
			}
			machine.addEvents(events);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		return machine;
	}

	private Event extractEvent(final ISCEvent iscEvent) throws RodinDBException {
		String name = iscEvent.getElementName();
		int typeId = iscEvent.getConvergence().getCode();

		Event e = new Event(name, calculateEventType(typeId));

		List<Event> refines = new ArrayList<Event>();
		for (ISCRefinesEvent iscRefinesEvent : iscEvent.getSCRefinesClauses()) {
			refines.add(extractEvent(iscRefinesEvent.getAbstractSCEvent()));
		}
		e.addRefines(refines);

		List<EventBGuard> guards = new ArrayList<EventBGuard>();
		for (ISCGuard iscGuard : iscEvent.getSCGuards()) {
			String elementName = iscGuard.getElementName();
			String predicateString = iscGuard.getPredicateString();
			boolean theorem = iscGuard.isTheorem();
			guards.add(new EventBGuard(elementName, predicateString, theorem));
		}
		e.addGuards(guards);

		List<EventBAction> actions = new ArrayList<EventBAction>();
		for (ISCAction iscAction : iscEvent.getSCActions()) {
			String elementName = iscAction.getElementName();
			String assignmentString = iscAction.getAssignmentString();
			actions.add(new EventBAction(elementName, assignmentString));
		}
		e.addActions(actions);

		List<Witness> witnesses = new ArrayList<Witness>();
		for (ISCWitness iscWitness : iscEvent.getSCWitnesses()) {
			String elementName = iscWitness.getElementName();
			String predicateString = iscWitness.getPredicateString();
			witnesses.add(new Witness(elementName, predicateString));
		}
		e.addWitness(witnesses);

		List<EventParameter> parameters = new ArrayList<EventParameter>();
		for (ISCParameter iscParameter : iscEvent.getSCParameters()) {
			parameters.add(new EventParameter(iscParameter
					.getIdentifierString()));
		}
		e.addParameters(parameters);

		return e;
	}

	private EventType calculateEventType(final int typeId) {
		Convergence valueOf = Convergence.valueOf(typeId);
		if (valueOf.equals(Convergence.ORDINARY)) {
			return EventType.ORDINARY;
		}
		if (valueOf.equals(Convergence.CONVERGENT)) {
			return EventType.CONVERGENT;
		}
		if (valueOf.equals(Convergence.ANTICIPATED)) {
			return EventType.ANTICIPATED;
		}
		return null;
	}

	public AbstractElement getMainComponent() {
		return mainComponent;
	}
}