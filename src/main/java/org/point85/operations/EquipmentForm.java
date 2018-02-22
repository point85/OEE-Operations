package org.point85.operations;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.core.persistence.PersistencyService;
import org.point85.core.plant.EntityLevel;
import org.point85.core.plant.Equipment;
import org.point85.core.plant.EquipmentMaterial;
import org.point85.core.plant.Material;
import org.point85.core.plant.PlantEntity;
import org.point85.core.plant.Reason;
import org.point85.core.script.ScriptResolverType;
import org.point85.core.uom.UnitOfMeasure;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.IconGenerator;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.RadioButtonGroup;
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

	private static final String PROD_GOOD = "Good";
	private static final String PROD_REJECT = "Reject/Rework";
	private static final String PROD_TOTAL = "Total";

	private TextField tfReason;
	private DateTimeField dtfAvailabilityTime;
	private Tree<EntityNode> entityTree;
	private TreeGrid<Reason> reasonTreeGrid;

	private RadioButtonGroup<String> productionGroup;

	private TextField tfAmount;
	private DateTimeField dtfProductionTime;

	private Label lbUOM;

	private Label lbMaterialId;
	private Label lbMaterialDescription;
	private Label lbJob;

	private EventCollector eventCollector = new EventCollector();

	private ScriptResolverType resolverType;

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

		tabSheet.addSelectedTabChangeListener(event -> {
			try {
				// Object source = event.getSource();

				// onTabSelection(source);
			} catch (Exception e) {
				Notification.show(e.getMessage());
			}
		});

		Tab eventTab = tabSheet.addTab(createEventPanel());
		eventTab.setCaption("Availability");
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

		productionGroup = new RadioButtonGroup<>("Production");
		productionGroup.setItems(PROD_GOOD, PROD_REJECT, PROD_TOTAL);
		productionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
		productionGroup.setRequiredIndicatorVisible(true);

		productionGroup.addSelectionListener(event -> {
			try {
				Optional<String> item = event.getSelectedItem();

				onSelectResolverType(item.get());
			} catch (Exception e) {
				Notification.show(e.getMessage());
			}
		});

		tfAmount = new TextField("Quantity");
		tfAmount.setIcon(VaadinIcons.REPLY);
		tfAmount.setRequiredIndicatorVisible(true);

		/*
		 * TextField tfUOM = new TextField("Unit"); tfUOM.setWidth("75px");
		 * tfUOM.setEnabled(false); tfUOM.setValue("cans");
		 */
		lbUOM = new Label("Unit");
		lbUOM.setWidth("75px");

		dtfProductionTime = new DateTimeField("Production Time");
		dtfProductionTime.setValue(LocalDateTime.now());
		dtfProductionTime.setIcon(VaadinIcons.TIME_FORWARD);
		dtfProductionTime.setRequiredIndicatorVisible(true);

		Button btnExecute = new Button("Record");
		btnExecute.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnExecute.setDescription("Record production event");
		btnExecute.addClickListener(event -> {
			try {
				recordProductionEvent();
			} catch (Exception e) {
				Notification.show(e.getMessage());
			}
		});

		lbMaterialId = new Label("Material");
		lbMaterialDescription = new Label("Description");
		lbJob = new Label("Job");

		HorizontalLayout materialLayout = new HorizontalLayout();
		// materialLayout.setHeight("50px");
		materialLayout.addComponents(lbMaterialId, lbMaterialDescription, lbJob);

		HorizontalLayout quantityLayout = new HorizontalLayout();
		quantityLayout.addComponents(tfAmount, lbUOM);

		VerticalLayout productionLayout = new VerticalLayout();
		productionLayout.setMargin(true);
		// productionLayout.setSizeFull();

		// FormLayout formLayout = new FormLayout();
		// formLayout.setSizeFull();
		// formLayout.addComponents(productionGroup, quantityLayout, dtfProductionTime,
		// btnExecute);

		productionLayout.addComponents(materialLayout, productionGroup, quantityLayout, dtfProductionTime, btnExecute);

		return productionLayout;
	}

	private Component createJobLayout() {
		HorizontalLayout jobLayout = new HorizontalLayout();
		Button press = new Button("Press Me");
		jobLayout.addComponent(press);
		return jobLayout;
	}

	private Component createEntityTreePanel() {
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

	private Tree<EntityNode> createEntityTree() {
		entityTree = new Tree<>();
		entityTree.setSelectionMode(SelectionMode.SINGLE);
		entityTree.setCaption("Plant Entities");
		entityTree.setItemIconGenerator(new IconGenerator<EntityNode>() {
			private static final long serialVersionUID = 5138538319672111077L;

			@Override
			public Resource apply(EntityNode entityNode) {
				Resource icon = null;
				PlantEntity entity = entityNode.getEntity();
				switch (entity.getLevel()) {
				case AREA:
					icon = VaadinIcons.AIRPLANE;
					break;
				case ENTERPRISE:
					icon = VaadinIcons.HAMMER;
					break;
				case EQUIPMENT:
					icon = VaadinIcons.BOOK;
					break;
				case PRODUCTION_LINE:
					icon = VaadinIcons.WALLET;
					break;
				case SITE:
					icon = VaadinIcons.CAMERA;
					break;
				case WORK_CELL:
					icon = VaadinIcons.DIAMOND;
					break;
				default:
					break;

				}
				return icon;
			}
		});

		entityTree.addSelectionListener(event -> {
			try {
				EntityNode node = event.getFirstSelectedItem().get();
				onSelectEntity(node);
			} catch (Exception e) {
				Notification.show(e.getMessage());
			}
		});

		return entityTree;
	}

	private void clearAvailability() {
		tfReason.clear();
		dtfAvailabilityTime.setValue(LocalDateTime.now());
	}

	private void clearProduction() {
		tfAmount.clear();
		dtfProductionTime.setValue(LocalDateTime.now());
		productionGroup.clear();
	}

	private void clearJobMaterial() {

	}

	private void onSelectEntity(EntityNode node) throws Exception {
		// clear fields
		clearAvailability();
		clearProduction();
		clearJobMaterial();

		Equipment equipment = getSelectedEquipment();
		String job = equipment.getCurrentJob();

		// current job
		if (job != null) {
			lbJob.setValue(job);
		} else {
			lbJob.setValue("");
		}

		// current material
		Material material = equipment.getCurrentMaterial();

		if (material != null) {
			lbMaterialId.setValue(material.getName());
			lbMaterialDescription.setValue(material.getDescription());
		} else {
			lbMaterialId.setValue("");
			lbMaterialDescription.setValue("");
		}

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
		VerticalLayout reasonLayout = new VerticalLayout();
		reasonLayout.setMargin(true);
		// reasonLayout.setSizeFull();

		tfReason = new TextField("Reason");
		tfReason.setIcon(VaadinIcons.REPLY);
		tfReason.setRequiredIndicatorVisible(true);

		dtfAvailabilityTime = new DateTimeField("Event Time");
		dtfAvailabilityTime.setValue(LocalDateTime.now());
		dtfAvailabilityTime.setIcon(VaadinIcons.TIME_FORWARD);
		dtfAvailabilityTime.setRequiredIndicatorVisible(true);

		Button btnExecute = new Button("Record");
		btnExecute.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnExecute.setDescription("Button description");
		btnExecute.addClickListener(event -> {
			try {
				recordAvailabilityEvent();
			} catch (Exception e) {
				Notification.show(e.getMessage());
			}
		});

		reasonLayout.addComponents(tfReason, dtfAvailabilityTime, btnExecute);

		return reasonLayout;
	}

	private Equipment getSelectedEquipment() throws Exception {
		Set<EntityNode> entityNodes = entityTree.getSelectedItems();

		if (entityNodes.size() == 0) {
			throw new Exception("Equipment must be selected in order to record the event.");
		}

		Iterator<EntityNode> iter = entityNodes.iterator();
		PlantEntity entity = iter.next().getEntity();

		if (!entity.getLevel().equals(EntityLevel.EQUIPMENT)) {
			throw new Exception("Equipment must be selected in order to record the event.");
		}

		return (Equipment) entity;
	}

	private void recordAvailabilityEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();

		String reason = tfReason.getValue();

		if (reason == null || reason.length() == 0) {
			throw new Exception("A reason must be selected.");
		}

		OffsetDateTime odt = AppUtils.fromLocalDateTime(dtfAvailabilityTime.getValue());

		eventCollector.resolveEvent(equipment, ScriptResolverType.AVAILABILITY, reason, odt);
	}

	private void onSelectResolverType(String type) throws Exception {

		// update current material/job
		Equipment equipment = getSelectedEquipment();

		Material material = equipment.getCurrentMaterial();

		// update UOMs
		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);

		UnitOfMeasure uom = null;

		if (type.equals(PROD_GOOD)) {
			resolverType = ScriptResolverType.PROD_GOOD;
			uom = (eqm != null) ? eqm.getRunRateUOM() : null;
		} else if (type.equals(PROD_REJECT)) {
			resolverType = ScriptResolverType.PROD_REJECT;
			uom = (eqm != null) ? eqm.getRejectUOM() : null;
		} else if (type.equals(PROD_TOTAL)) {
			resolverType = ScriptResolverType.PROD_TOTAL;
			uom = (eqm != null) ? eqm.getInputUOM() : null;
		}

		if (uom != null) {
			lbUOM.setValue(uom.getSymbol());
		}
	}

	private void recordProductionEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();

		if (resolverType == null) {
			throw new Exception("A production type must be selected.");
		}

		Double amount = Double.valueOf(tfAmount.getValue());

		OffsetDateTime odt = AppUtils.fromLocalDateTime(dtfProductionTime.getValue());

		eventCollector.resolveEvent(equipment, resolverType, amount, odt);
	}

	private void populateTopEntityNodes() {

		// fetch the entities
		List<PlantEntity> entities = PersistencyService.instance().fetchTopPlantEntities();
		Collections.sort(entities);

		List<EntityNode> entityNodes = new ArrayList<>();

		for (PlantEntity entity : entities) {
			entityNodes.add(new EntityNode(entity));
		}

		// An initial entity tree
		TreeData<EntityNode> treeData = new TreeData<>();

		// add the roots
		treeData.addItems(null, entityNodes);

		entityNodes.forEach(entityNode -> treeData.addItems(entityNode, entityNode.getChildren()));

		TreeDataProvider<EntityNode> dataProvider = new TreeDataProvider<>(treeData);
		entityTree.setDataProvider(dataProvider);
	}

	private void populateReasonGrid() {
		List<Reason> reasons = PersistencyService.instance().fetchTopReasons();

		// Initialize a TreeGrid and set in-memory data
		reasonTreeGrid.setItems(reasons, Reason::getChildren);

		// The first column gets the hierarchy indicator by default
		reasonTreeGrid.addColumn(Reason::getName).setCaption("Name");
		reasonTreeGrid.addColumn(Reason::getDescription).setCaption("Description");
		reasonTreeGrid.addColumn(Reason::getLossCategory).setCaption("Loss Category");
	}

	private class EntityNode {
		private PlantEntity entity;

		private EntityNode(PlantEntity entity) {
			this.entity = entity;
		}

		private PlantEntity getEntity() {
			return entity;
		}

		private Set<EntityNode> getChildren() {
			Set<EntityNode> children = new HashSet<>();

			for (PlantEntity childEntity : entity.getChildren()) {
				children.add(new EntityNode(childEntity));
			}
			return children;
		}

		@Override
		public String toString() {
			String text = "";

			if (entity != null) {
				text = entity.getName() + " (" + entity.getDescription() + ")";
			}

			return text;
		}
	}
}
