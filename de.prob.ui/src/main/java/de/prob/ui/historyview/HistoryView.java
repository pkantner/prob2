package de.prob.ui.historyview;


import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.google.inject.Injector;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.History;
import de.prob.statespace.IHistoryChangeListener;
import de.prob.webconsole.ServletContextListener;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class HistoryView extends ViewPart implements IHistoryChangeListener{

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.prob.ui.operationview.OperationView";

	private TableViewer viewer;
	private History currentHistory; // FIXME not used?

	Injector injector = ServletContextListener.INJECTOR;
	 
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	/**
	 * The constructor.
	 */
	public HistoryView() {
		AnimationSelector selector = injector.getInstance(AnimationSelector.class);
		selector.registerHistoryChangeListener(this);
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new HistoryContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(null);
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "de.prob.ui.viewer");
		
		Table table = viewer.getTable();
		table.setLinesVisible(true);
	}


	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	@Override
	public void historyChange(final History history, AbstractModel model) {
		currentHistory = history;
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				viewer.setInput(history);
			}
		});
	}
}