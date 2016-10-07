/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.ui.statistics.ui;

import static de.ovgu.featureide.fm.core.localization.StringTable.REFRESH_VIEW;
import static de.ovgu.featureide.fm.core.localization.StringTable.UPDATING_FEATURESTATISTICSVIEW;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.listeners.ICurrentBuildListener;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.ui.UIPlugin;
import de.ovgu.featureide.ui.statistics.core.ContentProvider;
import de.ovgu.featureide.ui.statistics.ui.helper.JobDoneListener;
import de.ovgu.featureide.ui.statistics.ui.helper.TreeClickListener;
import de.ovgu.featureide.ui.statistics.ui.helper.TreeLabelProvider;

/**
 * View to calculate and show the statistics of a feature project.
 * 
 * @author Dominik Hamann
 * @author Patrick Haese
 */
public class FeatureStatisticsView extends ViewPart implements GUIDefaults, ICurrentBuildListener{
	private TreeViewer viewer;
	private ContentProvider contentProvider;
	private IEditorPart currentEditor;
	private IResource currentInput;

	public static final String ID = UIPlugin.PLUGIN_ID + ".statistics.ui.FeatureStatisticsView";

	public static final Image EXPORT_IMG = FMUIPlugin.getImage("export_wiz.gif");
	public static final Image REFRESH_IMG = FMUIPlugin.getImage("refresh_tab.gif");

	@Override
	public void createPartControl(Composite parent) {

		viewer = new TreeViewer(parent);
		contentProvider = new ContentProvider(viewer);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new TreeLabelProvider());
		viewer.setInput(viewer);
		viewer.addDoubleClickListener(new TreeClickListener(viewer));
		ColumnViewerToolTipSupport.enableFor(viewer);

		CorePlugin.getDefault().addCurrentBuildListener(this); // BuildListener
		
		//TODO: Task Performance
		getSite().getPage().addPartListener(editorListener);
		setEditor(getSite().getPage().getActiveEditor());
		currentInput = (currentEditor == null) ? null : ResourceUtil.getResource((currentEditor.getEditorInput()));
		
		addButtons();
	}

	private void addButtons() {

		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

		Action checkBoxer = new Action() {
			public void run() {
				CheckBoxTreeViewDialog dial = new CheckBoxTreeViewDialog(viewer.getControl().getShell(), contentProvider.godfather, viewer);
				dial.open();
			}
		};

		Action refresher = new Action() {
			public void run() {
				FeatureStatisticsView.this.refresh(true);
			}
		};

		toolBarManager.add(refresher);
		refresher.setImageDescriptor(ImageDescriptor.createFromImage(REFRESH_IMG));
		refresher.setToolTipText(REFRESH_VIEW);

		toolBarManager.add(checkBoxer);
		checkBoxer.setImageDescriptor(ImageDescriptor.createFromImage(EXPORT_IMG));
		checkBoxer.setToolTipText("Export to *.csv");
	}

	private IPartListener editorListener = new IPartListener() {

		public void partOpened(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
			if (part == currentEditor) {
				setEditor(null);
			}
		}

		public void partBroughtToTop(IWorkbenchPart part) {
			boolean partOf = part instanceof ViewPart;
			if (partOf && part == getSite().getPart()) {
				refresh(true);
			}
		}

		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart) {
				ResourceUtil.getResource(((IEditorPart) part).getEditorInput());
				setEditor((IEditorPart) part);
			}
		}
	};

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private Job job = null;

	/**
	 * Refresh the view.
	 */
	private void refresh(final boolean force) {
		if (contentProvider.isCanceled()) {
			return;
		}

		/*
		 * This job waits for the calculation job to finish and starts
		 * immediately a new one
		 */
		if(getSite().getPage().isPartVisible(getSite().getPart())){ 
			Job waiter = new Job(UPDATING_FEATURESTATISTICSVIEW) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						if (job != null) {
							if (contentProvider.isCanceled()) {
								return Status.OK_STATUS;
							}
							contentProvider.setCanceled(true);
							job.join();
							contentProvider.setCanceled(false);
						}
					} catch (InterruptedException e) {
						FMUIPlugin.getDefault().logError(e);
					}

					job = new Job(UPDATING_FEATURESTATISTICSVIEW) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							if (currentEditor == null) {
								contentProvider.defaultContent();
							} else {
								IResource anyFile = ResourceUtil.getResource(((IEditorPart) currentEditor).getEditorInput());
								//TODO is refresh really necessary? -> true?

								if (force || currentInput == null || !anyFile.getProject().equals(currentInput.getProject())) {
									contentProvider.calculateContent(anyFile, true);
									currentInput = anyFile;
								} else {
									contentProvider.calculateContent(anyFile, false);
								}
							}

							return Status.OK_STATUS;
						}
					};
					job.setPriority(Job.DECORATE);
					job.schedule();
					return Status.OK_STATUS;
				}
			};
			waiter.setPriority(Job.DECORATE);
			waiter.schedule();
			cancelJobs();
		}
	}

	private void cancelJobs() {
		JobDoneListener jobListener = JobDoneListener.getInstance();
		if (jobListener != null) {
			jobListener.cancelAllRunningTreeJobs();
		}
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	/**
	 * Watches changes in the feature model if the selected editor is an
	 * instance of @{link FeatureModelEditor}
	 */
	private void setEditor(IEditorPart newEditor) {
		if (currentEditor != null) {
			if (currentEditor == newEditor) {
				return;
			}
		}
		boolean force = true;
		if (newEditor != null && currentEditor != null) {
			IEditorInput newInput = newEditor.getEditorInput();
			if (newInput instanceof FileEditorInput) {
				IEditorInput oldInput = currentEditor.getEditorInput();
				if (oldInput instanceof FileEditorInput) {
					IProject newProject = ((FileEditorInput) newInput).getFile().getProject();
					IProject oldProject = ((FileEditorInput) oldInput).getFile().getProject();
					if (newProject.equals(oldProject)) {
						force = false;
					}
				}
			}
		}
		currentEditor = newEditor;
		refresh(force);
	}

	/**
	 * Refreshes the Statistics View if model was saved
	 */
	@Override
	public void updateGuiAfterBuild(IFeatureProject project, IFile configurationFile) {
		boolean isVisible = getSite().getPage().isPartVisible(getSite().getPart());
		
		if (isVisible) {
			if (project != null && configurationFile != null) {
				refresh(true);
			}
		}
	}
}
