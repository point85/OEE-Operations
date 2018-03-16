package org.point85.ops;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.point85.domain.DomainUtils;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.uom.UnitOfMeasure;

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

public class OperationsView extends VerticalLayout {
	private static final long serialVersionUID = 6073934288316949481L;

	private static final String PROD_GOOD = "Good";
	private static final String PROD_REJECT = "Reject/Rework";

	// availability
	private Button btnRecordAvailability;
	private TextField tfReason;
	private DateTimeField dtfAvailabilityTime;
	private Tree<EntityNode> entityTree;
	private TreeGrid<Reason> reasonTreeGrid;
	private TreeGrid<MaterialCategory> materialTreeGrid;

	// production
	private Button btnRecordProduction;
	private RadioButtonGroup<String> productionGroup;
	private TextField tfAmount;
	private DateTimeField dtfProductionTime;
	private Label lbUOM;
	private Label lbMaterialId;
	private Label lbMaterialDescription;
	private Label lbJob;

	// setup/changeover
	private Button btnRecordSetup;
	private TextField tfMaterial;
	private TextField tfJob;
	private DateTimeField dtfSetupTime;

	private OperationsPresenter operationsPresenter;

	// reference to UI
	private OperationsUI ui;

	public OperationsView(OperationsUI ui) {
		// the UI
		this.ui = ui;

		// presenter
		operationsPresenter = new OperationsPresenter(this);

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

		// query for the root plant entities
		operationsPresenter.populateTopEntityNodes(entityTree);

		// query for the reasons
		operationsPresenter.populateReasonGrid(reasonTreeGrid);

		// query for the materials
		operationsPresenter.populateMaterialGrid(materialTreeGrid);
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
			} catch (Exception e) {
				showException(e);
			}
		});

		Tab eventTab = tabSheet.addTab(createEventPanel());
		eventTab.setCaption("Availability");
		eventTab.setIcon(VaadinIcons.AUTOMATION);

		Tab productionTab = tabSheet.addTab(createProductionLayout());
		productionTab.setCaption("Production");
		productionTab.setIcon(VaadinIcons.STOCK);

		Tab jobTab = tabSheet.addTab(createJobMaterialPanel());
		jobTab.setCaption("Job/Material");
		jobTab.setIcon(VaadinIcons.PACKAGE);
		return tabSheet;
	}

	private Component createEventPanel() {
		VerticalSplitPanel eventPanel = new VerticalSplitPanel();
		eventPanel.setSizeFull();
		eventPanel.setSplitPosition(33.3f);
		eventPanel.setStyleName(ValoTheme.SPLITPANEL_LARGE);

		// event reason
		eventPanel.addComponent(createAvailabilityPanel());

		// tree grid of reasons
		eventPanel.addComponent(createReasonTreeLayout());

		return eventPanel;
	}

	private Component createJobMaterialPanel() {
		VerticalSplitPanel materialPanel = new VerticalSplitPanel();
		materialPanel.setSizeFull();
		materialPanel.setSplitPosition(40.0f);
		materialPanel.setStyleName(ValoTheme.SPLITPANEL_LARGE);

		// material and job
		materialPanel.addComponent(createSetupPanel());

		// tree grid of materials
		materialPanel.addComponent(createMaterialTreeLayout());

		return materialPanel;
	}

	private Component createProductionLayout() {

		productionGroup = new RadioButtonGroup<>("Production");
		productionGroup.setItems(PROD_GOOD, PROD_REJECT);
		productionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
		productionGroup.setRequiredIndicatorVisible(true);

		productionGroup.addSelectionListener(event -> {
			try {
				Optional<String> item = event.getSelectedItem();

				onSelectResolverType(item.get());
			} catch (Exception e) {
				showException(e);
			}
		});

		tfAmount = new TextField("Quantity");
		tfAmount.setIcon(VaadinIcons.PLAY_CIRCLE_O);
		tfAmount.setRequiredIndicatorVisible(true);

		lbUOM = new Label("Unit");
		lbUOM.setWidth("75px");

		dtfProductionTime = new DateTimeField("Production Time");
		dtfProductionTime.setValue(LocalDateTime.now());
		dtfProductionTime.setIcon(VaadinIcons.TIME_FORWARD);
		dtfProductionTime.setRequiredIndicatorVisible(true);

		btnRecordProduction = new Button("Record");
		btnRecordProduction.setIcon(VaadinIcons.NOTEBOOK);
		btnRecordProduction.setEnabled(false);
		btnRecordProduction.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnRecordProduction.setDescription("Record production event");
		btnRecordProduction.addClickListener(event -> {
			try {
				recordProductionEvent();
			} catch (Exception e) {
				showException(e);
			}
		});

		lbMaterialId = new Label("Material");
		lbMaterialDescription = new Label("Description");
		lbJob = new Label("Job");

		HorizontalLayout materialLayout = new HorizontalLayout();
		materialLayout.addComponents(lbMaterialId, lbMaterialDescription, lbJob);

		HorizontalLayout quantityLayout = new HorizontalLayout();
		quantityLayout.addComponents(tfAmount, lbUOM);

		VerticalLayout productionLayout = new VerticalLayout();
		productionLayout.setMargin(true);

		productionLayout.addComponents(materialLayout, productionGroup, quantityLayout, dtfProductionTime,
				btnRecordProduction);

		return productionLayout;
	}

	private Component createEntityTreePanel() {
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.addComponentsAndExpand(createEntityTree());
		return layout;
	}

	private Component createHeader() {
		Label header = new Label("Point 85 Operations");
		header.addStyleName(ValoTheme.LABEL_COLORED);
		header.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		header.addStyleName(ValoTheme.LABEL_BOLD);
		header.addStyleName(ValoTheme.LABEL_HUGE);

		return header;
	}

	private Component createFooter() {
		Label footer = new Label("Point85 OEE");
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
					icon = VaadinIcons.OFFICE;
					break;
				case ENTERPRISE:
					icon = VaadinIcons.GLOBE_WIRE;
					break;
				case EQUIPMENT:
					icon = VaadinIcons.COG;
					break;
				case PRODUCTION_LINE:
					icon = VaadinIcons.CUBES;
					break;
				case SITE:
					icon = VaadinIcons.FACTORY;
					break;
				case WORK_CELL:
					icon = VaadinIcons.COGS;
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
				showException(e);
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

	private void clearSetup() {
		tfMaterial.clear();
		tfJob.clear();
		dtfSetupTime.setValue(LocalDateTime.now());
	}

	private void onSelectEntity(EntityNode node) throws Exception {
		// clear fields
		clearAvailability();
		clearProduction();
		clearSetup();

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

		btnRecordAvailability.setEnabled(true);
		btnRecordProduction.setEnabled(true);
		btnRecordSetup.setEnabled(true);

	}

	private Component createReasonTreeLayout() {
		reasonTreeGrid = new TreeGrid<>();
		reasonTreeGrid.setCaption("Reasons");
		reasonTreeGrid.setHeightByRows(6);

		reasonTreeGrid.addColumn(Reason::getName).setCaption("Name");
		reasonTreeGrid.addColumn(Reason::getDescription).setCaption("Description");
		reasonTreeGrid.addColumn(Reason::getLossCategory).setCaption("Loss Category");

		/*
		reasonTreeGrid.addExpandListener(event -> System.out.println("Reason expanded: " + event.getExpandedItem()));
		reasonTreeGrid
				.addCollapseListener(event -> System.out.println("Reason collapsed: " + event.getCollapsedItem()));
				*/

		reasonTreeGrid.addItemClickListener(event -> {
			Reason reason = event.getItem();
			tfReason.setValue(reason.getName());
			tfReason.setData(reason);
		});

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponentsAndExpand(reasonTreeGrid);
		layout.setMargin(true);

		return layout;
	}

	private Component createMaterialTreeLayout() {
		materialTreeGrid = new TreeGrid<>();
		materialTreeGrid.setCaption("Material");
		materialTreeGrid.setHeightByRows(6);

		materialTreeGrid.addColumn(MaterialCategory::getName).setCaption("Name");
		materialTreeGrid.addColumn(MaterialCategory::getDescription).setCaption("Description");

		/*
		materialTreeGrid
				.addExpandListener(event -> System.out.println("Material expanded: " + event.getExpandedItem()));
		materialTreeGrid
				.addCollapseListener(event -> System.out.println("Material collapsed: " + event.getCollapsedItem()));
				*/
		materialTreeGrid.addItemClickListener(event -> {
			Material material = event.getItem().getMaterial();
			tfMaterial.setValue(material.getName());
			tfMaterial.setData(material);
		});

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponentsAndExpand(materialTreeGrid);
		layout.setMargin(true);

		return layout;
	}

	private Component createAvailabilityPanel() {
		VerticalLayout reasonLayout = new VerticalLayout();
		reasonLayout.setMargin(true);

		tfReason = new TextField("Reason");
		tfReason.setIcon(VaadinIcons.PENCIL);
		tfReason.setRequiredIndicatorVisible(true);

		dtfAvailabilityTime = new DateTimeField("Event Time");
		dtfAvailabilityTime.setValue(LocalDateTime.now());
		dtfAvailabilityTime.setIcon(VaadinIcons.TIME_FORWARD);
		dtfAvailabilityTime.setRequiredIndicatorVisible(true);

		btnRecordAvailability = new Button("Record");
		btnRecordAvailability.setIcon(VaadinIcons.NOTEBOOK);
		btnRecordAvailability.setEnabled(false);
		btnRecordAvailability.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnRecordAvailability.setDescription("Button description");
		btnRecordAvailability.addClickListener(event -> {
			try {
				recordAvailabilityEvent();
			} catch (Exception e) {
				showException(e);
			}
		});

		reasonLayout.addComponents(tfReason, dtfAvailabilityTime, btnRecordAvailability);

		return reasonLayout;
	}

	private Component createSetupPanel() {
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(true);

		tfMaterial = new TextField("Material");
		tfMaterial.setIcon(VaadinIcons.STOCK);
		tfMaterial.setRequiredIndicatorVisible(true);

		tfJob = new TextField("Job");
		tfJob.setIcon(VaadinIcons.TAG);

		dtfSetupTime = new DateTimeField("Changeover Time");
		dtfSetupTime.setValue(LocalDateTime.now());
		dtfSetupTime.setIcon(VaadinIcons.TIME_FORWARD);
		dtfSetupTime.setRequiredIndicatorVisible(true);

		btnRecordSetup = new Button("Record");
		btnRecordSetup.setIcon(VaadinIcons.NOTEBOOK);
		btnRecordSetup.setEnabled(false);
		btnRecordSetup.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnRecordSetup.setDescription("Button description");
		btnRecordSetup.addClickListener(event -> {
			try {
				recordChangeoverEvent();
			} catch (Exception e) {
				showException(e);
			}
		});

		layout.addComponents(tfMaterial, tfJob, dtfSetupTime, btnRecordSetup);

		return layout;
	}

	Equipment getSelectedEquipment() throws Exception {
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

	private void recordProductionEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();
		Double amount = Double.valueOf(tfAmount.getValue());
		Material material = (Material)tfMaterial.getData();
		OffsetDateTime odt = DomainUtils.fromLocalDateTime(dtfProductionTime.getValue());
		operationsPresenter.recordProductionEvent(equipment, amount, material, odt);
	}

	private void recordChangeoverEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();
		OffsetDateTime odt = DomainUtils.fromLocalDateTime(dtfSetupTime.getValue());

		// job
		String job = tfJob.getValue();

		// material
		Material material = (Material)tfMaterial.getData();

		operationsPresenter.recordChangeoverEvent(equipment, job, material, odt);
	}

	private void recordAvailabilityEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();

		String reason = tfReason.getValue();

		if (reason == null || reason.length() == 0 || tfReason.getData() == null) {
			throw new Exception("A reason must be selected.");
		}

		OffsetDateTime odt = DomainUtils.fromLocalDateTime(dtfAvailabilityTime.getValue());

		operationsPresenter.recordAvailabilityEvent(equipment, (Reason) tfReason.getData(), odt);
	}

	private void recordAvailabilitySummary() throws Exception {
		Equipment equipment = getSelectedEquipment();

		String reason = tfReason.getValue();

		if (reason == null || reason.length() == 0 || tfReason.getData() == null) {
			throw new Exception("A reason must be selected.");
		}

		OffsetDateTime startTime = DomainUtils.fromLocalDateTime(dtfAvailabilityTime.getValue());
		OffsetDateTime endTime = DomainUtils.fromLocalDateTime(dtfAvailabilityTime.getValue());
		Duration duration = Duration.ofSeconds(600);

		operationsPresenter.recordAvailabilitySummary(equipment, (Reason) tfReason.getData(), startTime, endTime,
				duration);
	}

	private void onSelectResolverType(String type) throws Exception {

		// update current material/job
		Equipment equipment = getSelectedEquipment();

		Material material = equipment.getCurrentMaterial();

		if (material == null) {
			throw new Exception("The material being processed must be defined.");
		}

		// update UOMs
		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);

		if (eqm == null) {
			throw new Exception("The equipment settings for material " + material.getName() + " have not been defined");
		}

		UnitOfMeasure uom = null;

		EventResolverType resolverType = null;
		if (type.equals(PROD_GOOD)) {
			resolverType = EventResolverType.PROD_GOOD;
			uom = eqm.getRunRateUOM();
		} else if (type.equals(PROD_REJECT)) {
			resolverType = EventResolverType.PROD_REJECT;
			uom = eqm.getRejectUOM();
		}
		operationsPresenter.setResolverType(resolverType);

		if (uom == null) {
			throw new Exception("The unit of measure has not been defined for material " + material.getName());
		}
		lbUOM.setValue(uom.getSymbol());
		lbUOM.setData(uom);
	}

	// callback
	void onException(Exception e) {
		// put on UI thread
		ui.access(new Runnable() {
			@Override
			public void run() {
				showException(e);
			}
		});

	}

	private void showException(Exception e) {
		Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
	}

	void startupCollector() throws Exception {
		operationsPresenter.startupCollector();
	}

	void shutdownCollector() {
		try {
			operationsPresenter.shutdownCollector();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
