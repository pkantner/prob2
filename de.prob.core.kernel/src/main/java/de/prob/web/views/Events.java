package de.prob.web.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.Event;
import de.prob.model.eventb.EventParameter;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Machine;
import de.prob.model.representation.ModelElementList;
import de.prob.scripting.ScriptEngineProvider;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.OpInfo;
import de.prob.statespace.Trace;
import de.prob.web.AbstractSession;
import de.prob.web.WebUtils;

@Singleton
public class Events extends AbstractSession implements IAnimationChangeListener {

	Logger logger = LoggerFactory.getLogger(Events.class);
	Trace currentTrace;
	private final AnimationSelector selector;
	AbstractModel currentModel;
	List<String> opNames = new ArrayList<String>();
	Map<String, List<String>> opToParams = new HashMap<String, List<String>>();
	Comparator<Operation> sorter = new ModelOrder(new ArrayList<String>());
	List<Operation> events = new ArrayList<Operation>();
	private String filter = "";
	boolean hide = false;
	private final ScriptEngine groovy;

	@Inject
	public Events(final AnimationSelector selector,
			final ScriptEngineProvider sep) {
		this.selector = selector;
		groovy = sep.get();
		selector.registerAnimationChangeListener(this);
	}

	// used in JS
	@SuppressWarnings("unused")
	private static class Operation {
		public final String name;
		public final List<String> params;
		public final String id;
		public final String enablement;

		public Operation(final String id, final String name,
				final List<String> params, final boolean isEnabled) {
			this.id = id;
			this.name = name;
			this.params = params;
			enablement = isEnabled ? "enabled" : "notEnabled";
		}
	}

	@Override
	public void traceChange(final Trace trace) {
		if (trace == null) {
			currentTrace = null;
			currentModel = null;
			opNames = new ArrayList<String>();
			if (sorter instanceof ModelOrder) {
				sorter = new ModelOrder(opNames);
			}
			Map<String, String> wrap = WebUtils.wrap("cmd", "Events.newTrace",
					"ops", WebUtils.toJson(opNames), "canGoBack", false,
					"canGoForward", false);
			submit(wrap);
			return;
		}

		if (trace.getModel() != currentModel) {
			updateModel(trace);
		}
		currentTrace = trace;
		Set<OpInfo> ops = trace.getNextTransitions();
		events = new ArrayList<Operation>(ops.size());
		Set<String> notEnabled = new HashSet<String>(opNames);
		for (OpInfo opInfo : ops) {
			String name = opInfo.name;
			notEnabled.remove(name);
			Operation o = new Operation(opInfo.id, name, opInfo.params, true);
			events.add(o);
		}
		for (String s : notEnabled) {
			if (!s.equals("INITIALISATION")) {
				events.add(new Operation(s, s, opToParams.get(s), false));
			}
		}
		Collections.sort(events, sorter);
		String json = WebUtils.toJson(applyFilter(filter));
		Map<String, String> wrap = WebUtils.wrap("cmd", "Events.newTrace",
				"ops", json, "canGoBack", currentTrace.canGoBack(),
				"canGoForward", currentTrace.canGoForward());
		submit(wrap);
	}

	private void updateModel(final Trace trace) {
		currentModel = trace.getModel();
		AbstractElement mainComponent = currentModel.getMainComponent();
		opNames = new ArrayList<String>();
		opToParams = new HashMap<String, List<String>>();
		if (mainComponent instanceof Machine) {
			ModelElementList<BEvent> events = mainComponent
					.getChildrenOfType(BEvent.class);
			for (BEvent e : events) {
				opNames.add(e.getName());

				List<String> pList = new ArrayList<String>();
				if (e instanceof Event) {
					for (EventParameter eP : ((Event) e).getParameters()) {
						pList.add(eP.getName());
					}
				} else if (e instanceof de.prob.model.classicalb.Operation) {
					pList.addAll(((de.prob.model.classicalb.Operation) e)
							.getParameters());
				}
				opToParams.put(e.getName(), pList);
			}
		}
		if (sorter instanceof ModelOrder) {
			sorter = new ModelOrder(opNames);
		}
	}

	@Override
	public String html(final String clientid,
			final Map<String, String[]> parameterMap) {
		return simpleRender(clientid, "ui/eventview/index.html");
	}

	public Object execute(final Map<String, String[]> params) {
		String id = params.get("id")[0];
		final Trace newTrace = currentTrace.add(id);
		selector.replaceTrace(currentTrace, newTrace);
		return null;
	}

	public Object executeEvent(final Map<String, String[]> params) {
		String event = params.get("event")[0];
		String code = "t = animations.getCurrentTrace();"
				+ "t1 = execTrace(t) { " + event + "};"
				+ "animations.replaceTrace(t,t1)";
		try {
			groovy.eval(code);
		} catch (ScriptException e) {
			logger.error("Not able to execute event " + event
					+ " for current trace. " + e.getMessage());
		}
		return null;
	}

	@Override
	public void reload(final String client, final int lastinfo,
			final AsyncContext context) {
		super.reload(client, lastinfo, context);
		Map<String, String> wrap = WebUtils.wrap("cmd", "Events.setView",
				"ops", WebUtils.toJson(events), "canGoBack",
				currentTrace == null ? false : currentTrace.canGoBack(),
				"canGoForward",
				currentTrace == null ? false : currentTrace.canGoForward(),
				"sortMode", getSortMode(), "hide", hide);
		submit(wrap);
	}

	public Object random(final Map<String, String[]> params) {
		int num = Integer.parseInt(params.get("num")[0]);
		Trace newTrace = currentTrace.randomAnimation(num);
		selector.replaceTrace(currentTrace, newTrace);
		return null;
	}

	public Object back(final Map<String, String[]> params) {
		Trace back = currentTrace.back();
		selector.replaceTrace(currentTrace, back);
		return null;
	}

	public Object forward(final Map<String, String[]> params) {
		Trace forward = currentTrace.forward();
		selector.replaceTrace(currentTrace, forward);
		return null;
	}

	public Object sort(final Map<String, String[]> params) {
		String mode = params.get("sortMode")[0];
		if ("normal".equals(mode)) {
			sorter = new ModelOrder(opNames);
		} else if ("aToZ".equals(mode)) {
			sorter = new AtoZ();
		} else if ("zToA".equals(mode)) {
			sorter = new ZtoA();
		}
		Collections.sort(events, sorter);
		return WebUtils.wrap("cmd", "Events.setContent", "ops",
				WebUtils.toJson(applyFilter(filter)));
	}

	public String getSortMode() {
		if (sorter instanceof ModelOrder) {
			return "normal";
		}
		if (sorter instanceof AtoZ) {
			return "aToZ";
		}
		if (sorter instanceof ZtoA) {
			return "zToA";
		}
		return "other";
	}

	public Object filter(final Map<String, String[]> params) {
		filter = params.get("filter")[0];
		List<Operation> filteredEvents = applyFilter(filter);
		return WebUtils.wrap("cmd", "Events.setContent", "ops",
				WebUtils.toJson(filteredEvents));
	}

	public Object hide(final Map<String, String[]> params) {
		hide = Boolean.valueOf(params.get("hidden")[0]);
		return null;
	}

	private List<Operation> applyFilter(final String filter) {
		List<Operation> newOps = new ArrayList<Operation>();
		for (Operation op : events) {
			if (op.name.startsWith(filter)) {
				newOps.add(op);
			}
		}
		return newOps;
	}

	private class EventComparator {

		private String stripString(final String param) {
			return param.replaceAll("\\{", "").replaceAll("\\}", "");
		}

		public int compareParams(final List<String> params1,
				final List<String> params2) {
			for (int i = 0; i < params1.size(); i++) {
				String p1 = stripString(params1.get(i));
				String p2 = stripString(params2.get(i));
				if (p1.compareTo(p2) != 0) {
					return p1.compareTo(p2);
				}

			}
			return 0;
		}
	}

	private class ModelOrder extends EventComparator implements
			Comparator<Operation> {

		private final List<String> ops;

		public ModelOrder(final List<String> ops) {
			this.ops = ops;
		}

		@Override
		public int compare(final Operation o1, final Operation o2) {
			if (ops.indexOf(o1.name) == ops.indexOf(o2.name)) {
				return compareParams(o1.params, o2.params);
			}
			return ops.indexOf(o1.name) - ops.indexOf(o2.name);
		}
	}

	private class AtoZ extends EventComparator implements Comparator<Operation> {

		@Override
		public int compare(final Operation o1, final Operation o2) {
			if (o1.name.compareTo(o2.name) == 0) {
				return compareParams(o1.params, o2.params);
			}
			return o1.name.compareTo(o2.name);
		}

	}

	private class ZtoA extends EventComparator implements Comparator<Operation> {

		@Override
		public int compare(final Operation o1, final Operation o2) {
			if (o1.name.compareTo(o2.name) == 0) {
				return compareParams(o1.params, o2.params);
			}
			return -1 * o1.name.compareTo(o2.name);
		}

	}
}
