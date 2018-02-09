package org.point85.operations;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.IconGenerator;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.themes.ValoTheme;

public class EquipmentForm extends VerticalLayout {
	private static final long serialVersionUID = 6073934288316949481L;

	private TextField tfReason;
	private Tree<String> entityTree;
	private TreeGrid<Project> reasonTreeGrid;
	private Grid<Project> materialGrid;

	public EquipmentForm() {

		// root content
		setMargin(true);
		setSpacing(true);

		// header
		Label header = (Label) createHeader();
		addComponent(header);
		setComponentAlignment(header, Alignment.MIDDLE_CENTER);

		// main content
		addComponentsAndExpand(createMainPanel());

		// footer
		addComponent(createFooter());

		populateTreeGrid();
		populateTree();
	}

	private Component createMainPanel() {
		// entity tree on left and tabs on right
		HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();
		mainPanel.setSizeFull();
		mainPanel.setSplitPosition(33.3f);
		mainPanel.setStyleName(ValoTheme.SPLITPANEL_LARGE);

		// plant entity tree on left
		mainPanel.addComponent(createEntityTreePanel());

		// tabs on right
		mainPanel.addComponent(createTabSheet());
		return mainPanel;
	}

	private Component createTabSheet() {
		// job and material
		TabSheet tabSheet = new TabSheet();
		tabSheet.setSizeFull();
		tabSheet.setStyleName(ValoTheme.TABSHEET_FRAMED);
		Tab eventTab = tabSheet.addTab(createEventPanel());
		eventTab.setCaption("Event");
		eventTab.setIcon(VaadinIcons.EJECT);

		Tab productionTab = tabSheet.addTab(createProductionLayout());
		productionTab.setCaption("Production");
		productionTab.setIcon(VaadinIcons.PACKAGE);

		Tab jobTab = tabSheet.addTab(createJobLayout());
		jobTab.setCaption("Job/Material");
		jobTab.setIcon(VaadinIcons.MAILBOX);
		return tabSheet;
	}

	private Component createEventPanel() {
		VerticalSplitPanel eventPanel = new VerticalSplitPanel();
		eventPanel.setSizeFull();
		eventPanel.setSplitPosition(33.3f);
		eventPanel.setStyleName(ValoTheme.SPLITPANEL_LARGE);

		// event reason
		eventPanel.addComponent(createReasonPanel());

		// tree grid of reasons		
		eventPanel.addComponent(createReasonTreePanel());

		return eventPanel;
	}

	private Component createProductionLayout() {
		HorizontalLayout productionLayout = new HorizontalLayout();
		Button press = new Button("Click Me");
		productionLayout.addComponent(press);
		return productionLayout;
	}

	private Component createJobLayout() {
		HorizontalLayout jobLayout = new HorizontalLayout();
		Button press = new Button("Press Me");
		jobLayout.addComponent(press);
		return jobLayout;
	}

	private Component createEntityTreePanel() {
		//Panel entityTreeLayout = new Panel();
		//entityTreeLayout.setContent(createEntityTree());
		//entityTreeLayout.setMargin(false);
		
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.addComponentsAndExpand(createEntityTree());
		return layout;
	}

	private Component createHeader() {
		Label header = new Label("Point 85 OEE");
		header.addStyleName(ValoTheme.LABEL_COLORED);
		header.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		header.addStyleName(ValoTheme.LABEL_BOLD);
		header.addStyleName(ValoTheme.LABEL_HUGE);

		return header;
	}

	private Component createFooter() {
		Label footer = new Label("Point85 LLC");
		footer.addStyleName(ValoTheme.LABEL_COLORED);
		footer.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		footer.addStyleName(ValoTheme.LABEL_BOLD);
		footer.addStyleName(ValoTheme.LABEL_SMALL);
		return footer;
	}

	private Tree<String> createEntityTree() {
		entityTree = new Tree<String>();
		entityTree.setCaption("Plant Entities");
		entityTree.setItemIconGenerator(new IconGenerator<String>() {
			private static final long serialVersionUID = 5138538319672111077L;

			@Override
			public Resource apply(String item) {
				return VaadinIcons.AIRPLANE;
			}
		});
		return entityTree;
	}

	private Component createReasonTreePanel() {
		reasonTreeGrid = new TreeGrid<>();
		reasonTreeGrid.setCaption("Reasons");
		reasonTreeGrid.setHeightByRows(6);

		reasonTreeGrid.addExpandListener(event -> System.out.println("Item expanded: " + event.getExpandedItem()));
		reasonTreeGrid.addCollapseListener(event -> System.out.println("Item collapsed: " + event.getCollapsedItem()));
		reasonTreeGrid.addItemClickListener(event -> tfReason.setValue(event.getItem().getName()));
		
		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponentsAndExpand(reasonTreeGrid);
		layout.setMargin(true);	

		return layout;
	}
	

	private Component createReasonPanel() {
		FormLayout reasonLayout = new FormLayout();
		reasonLayout.setMargin(true);
		reasonLayout.setSizeFull();

		tfReason = new TextField("Reason");
		tfReason.setIcon(VaadinIcons.REPLY);
		tfReason.setRequiredIndicatorVisible(true);

		DateTimeField dtfTime = new DateTimeField("Event Time");
		dtfTime.setValue(LocalDateTime.now());
		dtfTime.setIcon(VaadinIcons.TIME_FORWARD);
		dtfTime.setRequiredIndicatorVisible(true);

		Button btnExecute = new Button("Do It!");
		btnExecute.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnExecute.setDescription("Button description");
		btnExecute.addClickListener(event -> {updateTree(); updateTreeGrid();});

		reasonLayout.addComponents(tfReason, dtfTime, btnExecute);
		
		//HorizontalLayout layout = new HorizontalLayout();
		//layout.setSizeFull();
		//layout.addComponent(reasonLayout);

		return reasonLayout;
	}

	private void populateTree() {
		// An initial planet tree
		TreeData<String> treeData = new TreeData<>();

		// Couple of childless root items
		treeData.addItem(null, "Mercury");
		treeData.addItem(null, "Venus");

		// Items with hierarchy
		treeData.addItem(null, "Earth");
		treeData.addItem("Earth", "The Moon");

		TreeDataProvider<String> inMemoryDataProvider = new TreeDataProvider<>(treeData);
		entityTree.setDataProvider(inMemoryDataProvider);
		// tree.expand("Earth");
	}

	private Collection<Project> getRootProjects() {
		Collection<Project> projects = new HashSet<>();
		Project p1 = new Project("Project1", 100);

		p1.getSubProjects().add(new Project("Project11", 1));
		p1.getSubProjects().add(new Project("Project12", 2));

		projects.add(p1);

		return projects;
	}

	private void populateTreeGrid() {
		// Initialize a TreeGrid and set in-memory data
		reasonTreeGrid.setItems(getRootProjects(), Project::getSubProjects);

		// The first column gets the hierarchy indicator by default
		reasonTreeGrid.addColumn(Project::getName).setCaption("Project Name");
		reasonTreeGrid.addColumn(Project::getHoursDone).setCaption("Hours Done");
		reasonTreeGrid.addColumn(Project::getLastModified).setCaption("Last Modified");
	}
	
	private void addRecursively(TreeData<Project> treeData, Project parent) {
		treeData.addItems(parent, parent.getSubProjects());
		for (Project child : parent.getSubProjects()) {
			this.addRecursively(treeData, child);
		}
	}

	private void updateTreeGrid() {
		TreeDataProvider<Project> dataProvider = (TreeDataProvider<Project>) reasonTreeGrid.getDataProvider();
		TreeData<Project> treeData = dataProvider.getTreeData();

		// add new items
		Project newRoot = new Project("Project200", 200);

		Project p21 = new Project("Project21", 3);
		p21.getSubProjects().add(new Project("Project211", 211));
		p21.getSubProjects().add(new Project("Project212", 212));
		p21.getSubProjects().add(new Project("Project213", 213));
		
		newRoot.getSubProjects().add(p21);
		newRoot.getSubProjects().add(new Project("Project22", 4));

		treeData.addItem(null, newRoot);
		//treeData.addItems(newRoot, newRoot.getSubProjects());
		this.addRecursively(treeData, newRoot);

		// after adding / removing data, data provider needs to be refreshed
		dataProvider.refreshAll();
	}

	private void updateTree() {
		TreeDataProvider<String> dataProvider = (TreeDataProvider<String>) entityTree.getDataProvider();
		TreeData<String> treeData = dataProvider.getTreeData();

		// Add Mars with satellites
		treeData.addItem(null, "Mars");
		treeData.addItem("Mars", "Phobos");
		treeData.addItem("Mars", "Deimos");

		dataProvider.refreshAll();

		Notification.show("Tree Updated!");
	}

}
