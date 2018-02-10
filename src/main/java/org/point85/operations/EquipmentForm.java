package org.point85.operations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.point85.core.persistence.PersistencyService;
import org.point85.core.plant.PlantEntity;
import org.point85.core.plant.Reason;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
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
	private TreeGrid<Reason> reasonTreeGrid;

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

		populateReasonGrid();
		populateTopEntityNodes();
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
		// Panel entityTreeLayout = new Panel();
		// entityTreeLayout.setContent(createEntityTree());
		// entityTreeLayout.setMargin(false);

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
		btnExecute.addClickListener(event -> Notification.show("Thanks!"));

		reasonLayout.addComponents(tfReason, dtfTime, btnExecute);

		// HorizontalLayout layout = new HorizontalLayout();
		// layout.setSizeFull();
		// layout.addComponent(reasonLayout);

		return reasonLayout;
	}

	private void populateTopEntityNodes() {

		// fetch the entities
		List<PlantEntity> entities = PersistencyService.getInstance().fetchTopPlantEntities();
		Collections.sort(entities);

		// An initial entity tree
		TreeData<String> treeData = new TreeData<>();

		// add the roots
		for (PlantEntity entity : entities) {
			treeData.addItem(null, entity.getName());
		}

		TreeDataProvider<String> inMemoryDataProvider = new TreeDataProvider<>(treeData);
		entityTree.setDataProvider(inMemoryDataProvider);
	}

	private void populateReasonGrid() {
		List<Reason> reasons = PersistencyService.getInstance().fetchTopReasons();

		// Initialize a TreeGrid and set in-memory data
		reasonTreeGrid.setItems(reasons, Reason::getChildren);

		// The first column gets the hierarchy indicator by default
		reasonTreeGrid.addColumn(Reason::getName).setCaption("Name");
		reasonTreeGrid.addColumn(Reason::getDescription).setCaption("Description");
		reasonTreeGrid.addColumn(Reason::getLossCategory).setCaption("Loss Category");
	}
}
