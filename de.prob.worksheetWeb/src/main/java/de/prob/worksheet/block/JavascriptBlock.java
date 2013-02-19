package de.prob.worksheet.block;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlType;

import com.google.inject.Inject;

import de.prob.worksheet.ServletContextListener;
import de.prob.worksheet.WorksheetMenuNode;
import de.prob.worksheet.WorksheetObjectMapper;

@XmlType(name = "JavascriptBlock")
public class JavascriptBlock extends DefaultBlock {

	@Inject
	public JavascriptBlock() {
		this.setEvaluatorType("state");
		this.initBlockMenu("Javascript", new String[] { "Standard" });
	}

	private void initBlockMenu() {
		final ArrayList<WorksheetMenuNode> menu = new ArrayList<WorksheetMenuNode>();

		String[] blockTypes = this.getInputBlockTypes();

		final WorksheetMenuNode typeMenu = new WorksheetMenuNode("Javascript",
				"", "");
		typeMenu.setTitle(true);
		for (final String typeName : blockTypes) {
			if (typeName.equals("Javascript") || typeName.equals("Standard"))
				continue;
			final WorksheetMenuNode type = new WorksheetMenuNode(typeName, "",
					"");
			type.setClick("function(){$(this).closest('.ui-block').block('switchBlock','"
					+ typeName + "');}");
			// TODO add better keyCode Selection
			typeMenu.addChild(type);
		}
		if (typeMenu.getChildren().length != 0) {
			menu.add(typeMenu);
			this.setMenu(menu.toArray(new WorksheetMenuNode[menu.size()]));
		} else {
			this.setHasMenu(false);
		}
	}

	private String[] getInputBlockTypes() {
		WorksheetObjectMapper mapper = ServletContextListener.INJECTOR
				.getInstance(WorksheetObjectMapper.class);
		return mapper.getInputBlockNames();
	}

	@Override
	public boolean equals(final Object obj) {
		// TODO Do we need an overriden default equals method ????
		return super.equals(obj);
	}

}
