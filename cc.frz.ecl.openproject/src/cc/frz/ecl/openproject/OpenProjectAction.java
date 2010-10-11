/*******************************************************************************
 * Copyright (c) 2008 Patrick "Frz" Huy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package cc.frz.ecl.openproject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.views.navigator.ResourceNavigator;

@SuppressWarnings("restriction")
public class OpenProjectAction implements IWorkbenchWindowActionDelegate {
	private final static List<Class<?>> knownViews = new ArrayList<Class<?>>();

	static {
		knownViews.add(CommonNavigator.class);
		knownViews.add(ResourceNavigator.class);
		try {
			knownViews.add(PackageExplorerPart.class);
		} catch (NoClassDefFoundError e) {
			Activator.log(e);
		}
	}

	public void dispose() {
		//
	}

	public void init(IWorkbenchWindow window) {
		//
	}

	public void selectionChanged(IAction action, ISelection selection) {
		//
	}

	public void run(IAction action) {
		OpenProjectDialog openProjectDialog = new OpenProjectDialog(Display.getCurrent().getActiveShell());
		if (openProjectDialog.open() != IDialogConstants.OK_ID)
			return;

		Object[] res = openProjectDialog.getResult();
		if (res.length == 0) {
			return;
		}

		// find the explorers (!)
		// HACK THE PLANET (!)

		List<ISetSelectionTarget> nav = new ArrayList<ISetSelectionTarget>();
		int openViews = 0;
		IWorkbench workbench = Activator.getDefault().getWorkbench();

		for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IViewReference reference : page.getViewReferences()) {
					IWorkbenchPart part = reference.getPart(false);

					if (part != null) {
						for (Class<?> view : knownViews) {
							if (view.isAssignableFrom(part.getClass())) {
								nav.add((ISetSelectionTarget) part);
								if (page.isPartVisible(part)) {
									openViews++;
								}
							}
						}
					}
				}
			}
		}

		if (openViews == 0) { // try to open the package explorer
			try {
				nav.add((ISetSelectionTarget) workbench.getActiveWorkbenchWindow().getActivePage().showView(
						"org.eclipse.jdt.ui.PackageExplorer"));
			} catch (PartInitException e) {
				// didn't work, try the project explorer
				try {
					nav.add((ISetSelectionTarget) workbench.getActiveWorkbenchWindow().getActivePage().showView(
							"org.eclipse.ui.navigator.ProjectExplorer"));
				} catch (PartInitException e1) {
					// give up
				}
			}
		}
		for (ISetSelectionTarget setSelectionTarget : nav) {
			setSelectionTarget.selectReveal(new StructuredSelection(res[0]));
		}
	}
}
