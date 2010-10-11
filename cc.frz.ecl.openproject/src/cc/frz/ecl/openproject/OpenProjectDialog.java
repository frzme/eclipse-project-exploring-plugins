/*******************************************************************************
 * Copyright (c) 2008 Patrick "Frz" Huy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package cc.frz.ecl.openproject;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.navigator.CommonViewer;

// TextFieldNavigationHandler
@SuppressWarnings("restriction")
public class OpenProjectDialog extends FilteredItemsSelectionDialog {
	// it's dangerous to go alone, take this
	private static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";
	private static boolean loggedTextFieldNavigationNotavailable;

	public OpenProjectDialog(Shell shell) {
		super(shell);
		setTitle("Go to Project");
		Shell temp = new Shell();
		CommonViewer commonViewer = new CommonViewer(PROJECT_EXPLORER_ID, temp, 0);
		ILabelProvider labelProvider = commonViewer.getNavigatorContentService().createCommonLabelProvider();
		setListLabelProvider(labelProvider);
		setDetailsLabelProvider(labelProvider);
		temp.close();
		commonViewer.dispose();
		temp.dispose();
	}

	private String getProjectName(IProject pj) {
		if (pj.isOpen()) {
			try {
				return pj.getDescription().getName();
			} catch (CoreException e) {
				Activator.log(e);
			}
		}
		return pj.getName();
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
			@Override
			public boolean isConsistentItem(Object item) {
				// jaja passt scho
				return true;
			}

			@Override
			public boolean matchItem(Object item) {
				return matches(getProjectName((IProject) item));
			}

			@Override
			public String getPattern() {
				String sup = super.getPattern();
				if (sup.equals("")) {
					return "*";
				}
				return sup;
			}
		};
	}

	/*
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#create()
	 */
	public void create() {
		super.create();
		Control patternControl = getPatternControl();
		if (patternControl instanceof Text) {
			try {
				TextFieldNavigationHandler.install((Text) patternControl);
			} catch (NoClassDefFoundError t) {
				// we are running without jdt or they moved their internal stuff log it once
				if (!loggedTextFieldNavigationNotavailable) {
					Activator.log(t);
					loggedTextFieldNavigationNotavailable = true;
				}
			}
		}
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter,
			IProgressMonitor progressMonitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		ProjectColletor projectColletor = new ProjectColletor();
		root.accept(projectColletor, 0);
		for (IProject project : projectColletor.projects) {
			contentProvider.add(project, itemsFilter);
		}
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		return Activator.getDefault().getDialogSettings();
	}

	@Override
	public String getElementName(Object item) {
		return getProjectName((IProject) item);
	}

	@Override
	protected Comparator<IProject> getItemsComparator() {
		return new Comparator<IProject>() {
			public int compare(IProject o1, IProject o2) {
				return getProjectName(o1).compareTo(getProjectName(o2));
			}
		};
	}

	@Override
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, Activator.PLUGIN_ID, "");
	}

	private static class ProjectColletor implements IResourceProxyVisitor {
		public List<IProject> projects = new LinkedList<IProject>();

		public boolean visit(IResourceProxy proxy) throws CoreException {
			if (proxy.getType() == IResource.PROJECT) {
				projects.add((IProject) proxy.requestResource());
				return false;
			}
			return proxy.isAccessible();
		}
	}

}
