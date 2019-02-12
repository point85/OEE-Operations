package org.point85.ops;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.UnitOfMeasure;

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

public class OperationsView extends VerticalLayout {
	private static final long serialVersionUID = 6073934288316949481L;

	private static final String HOURS = "Hours";
	private static final String MINUTES = "Minutes";

	private static final String BY_EVENT = "By Event";
	private static final String SUMMARIZED = "Summarized";
	private static final String EVENT_TIME = "Event Time";
	private static final String FROM_TIME = "From Time";
	private static final String TO_TIME = "To Time";

	// good or reject production
	private static final String PROD_GOOD = "Good";
	private static final String PROD_REJECT = "Reject and Rework";
	private static final String PROD_STARTUP = "Startup and Yield";
	
	// manual source id
	private static final String OPS_SOURCE_ID = "OPERATIONS";

	// availability
	private RadioButtonGroup<String> groupAvailabilitySummary;
	private Button btnRecordAvailability;
	private TextField tfAvailabilityReason;
	private DateTimeField dtfAvailabilityStart;
	private DateTimeField dtfAvailabilityEnd;
	private TextField tfAvailabilityHours;
	private TextField tfAvailabilityMinutes;
	private Tree<EntityNode> treeEntity;
	private TreeGrid<Reason> treeGridReason;
	private TreeGrid<MaterialCategory> treeGridMaterial;

	// production
	private RadioButtonGroup<String> groupProductionSummary;
	private RadioButtonGroup<String> groupProductionType;
	private Button btnRecordProduction;
	private TextField tfAmount;
	private DateTimeField dtfProductionTime1;
	private DateTimeField dtfProductionTime2;
	private Label lbUOM;
	private Label lbMaterialId;
	private Label lbMaterialDescription;
	private Label lbJob;
	private TextField tfQualityReason;

	// setup/changeover
	private Button btnRecordSetup;
	private TextField tfMaterial;
	private TextField tfJob;
	private DateTimeField dtfSetupTime;

	// the presenter
	private final OperationsPresenter operationsPresenter;

	// the UI
	private final OperationsUI ui;

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
		operationsPresenter.populateTopEntityNodes(treeEntity);

		// query for the reasons
		operationsPresenter.populateReasonGrid(treeGridReason);

		// query for the materials
		operationsPresenter.populateMaterialGrid(treeGridMaterial);

		// summary availability
		groupAvailabilitySummary.setSelectedItem(SUMMARIZED);

		// summary production
		groupProductionSummary.setSelectedItem(SUMMARIZED);
	}

	private Component createMainPanel() {
		// entity tree on left and tabs on right
		HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();
		mainPanel.setSizeFull();
		mainPanel.setSplitPosition(33.3f);
		mainPanel.setStyleName(ValoTheme.SPLITPANEL_LARGE);

		// plant entity tree on left
		mainPanel.addComponent(createEntityTreeLayout());

		// tabs on right
		VerticalLayout rightLayout = new VerticalLayout();
		rightLayout.setMargin(true);
		rightLayout.setSizeFull();
		Component tabSheet = createTabSheet();
		rightLayout.addComponents(createMaterialJobLayout(), tabSheet);
		rightLayout.setExpandRatio(tabSheet, 1.0f);

		mainPanel.addComponent(rightLayout);
		return mainPanel;
	}

	private Component createMaterialJobLayout() {
		lbMaterialId = new Label("Identifier");
		lbMaterialDescription = new Label("Description");
		lbJob = new Label("Job Name");

		Label material = new Label("MATERIAL");
		material.addStyleName(ValoTheme.LABEL_BOLD);

		Label job = new Label("JOB");
		job.addStyleName(ValoTheme.LABEL_BOLD);

		HorizontalLayout materialLayout = new HorizontalLayout();
		materialLayout.setMargin(false);
		materialLayout.addComponents(material, lbMaterialId, lbMaterialDescription, job, lbJob);

		return materialLayout;
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

		Tab eventTab = tabSheet.addTab(createAvailabilityPanel());
		eventTab.setCaption("Availability/Rate");
		eventTab.setIcon(VaadinIcons.AUTOMATION);

		Tab productionTab = tabSheet.addTab(createProductionLayout());
		productionTab.setCaption("Production");
		productionTab.setIcon(VaadinIcons.STOCK);

		Tab jobTab = tabSheet.addTab(createJobMaterialPanel());
		jobTab.setCaption("Job/Material");
		jobTab.setIcon(VaadinIcons.PACKAGE);
		return tabSheet;
	}

	private Component createAvailabilityPanel() {
		VerticalSplitPanel eventPanel = new VerticalSplitPanel();
		eventPanel.setSizeFull();
		eventPanel.setSplitPosition(45.0f);
		eventPanel.setStyleName(ValoTheme.SPLITPANEL_LARGE);

		// event reason
		eventPanel.addComponent(createAvailabilityLayout());

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
		materialPanel.addComponent(createSetupLayout());

		// tree grid of materials
		materialPanel.addComponent(createMaterialTreeLayout());

		return materialPanel;
	}

	private Component createProductionLayout() {
		groupProductionSummary = new RadioButtonGroup<>("Production");
		groupProductionSummary.setItems(BY_EVENT, SUMMARIZED);
		groupProductionSummary.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
		groupProductionSummary.setRequiredIndicatorVisible(true);

		groupProductionSummary.addSelectionListener(event -> {
			try {
				Optional<String> item = event.getSelectedItem();

				onSelectProductionData(item.get());
			} catch (Exception e) {
				showException(e);
			}
		});

		groupProductionType = new RadioButtonGroup<>("Production Type");
		groupProductionType.setItems(PROD_GOOD, PROD_REJECT, PROD_STARTUP);
		groupProductionType.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
		groupProductionType.setRequiredIndicatorVisible(true);

		groupProductionType.addSelectionListener(event -> {
			try {
				Optional<String> item = event.getSelectedItem();

				onSelectProductionType(item.get());
			} catch (Exception e) {
				showException(e);
			}
		});

		tfAmount = new TextField("Quantity");
		tfAmount.setIcon(VaadinIcons.CUBES);
		tfAmount.setRequiredIndicatorVisible(true);
		tfAmount.setEnabled(false);

		lbUOM = new Label("Unit");
		lbUOM.addStyleName(ValoTheme.LABEL_BOLD);

		dtfProductionTime1 = new DateTimeField(EVENT_TIME);
		dtfProductionTime1.setValue(LocalDateTime.now());
		dtfProductionTime1.setIcon(VaadinIcons.TIME_FORWARD);
		dtfProductionTime1.setRequiredIndicatorVisible(true);

		dtfProductionTime2 = new DateTimeField(TO_TIME);
		dtfProductionTime2.setValue(LocalDateTime.now());
		dtfProductionTime2.setIcon(VaadinIcons.TIME_FORWARD);
		dtfProductionTime2.setRequiredIndicatorVisible(true);

		btnRecordProduction = new Button("Record");
		btnRecordProduction.setIcon(VaadinIcons.NOTEBOOK);
		btnRecordProduction.setEnabled(false);
		btnRecordProduction.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnRecordProduction.setDescription("Record production event");
		btnRecordProduction.addClickListener(event -> {
			try {
				recordProductionEvent();
				clearProduction();
			} catch (Exception e) {
				showException(e);
			}
		});
		
		tfQualityReason = new TextField("Reason");
		tfQualityReason.setIcon(VaadinIcons.PENCIL);
		tfQualityReason.setRequiredIndicatorVisible(false);

		HorizontalLayout quantityLayout = new HorizontalLayout();
		quantityLayout.addComponents(groupProductionType, tfAmount, lbUOM, tfQualityReason);
		quantityLayout.setMargin(false);

		HorizontalLayout timeLayout = new HorizontalLayout();
		timeLayout.addComponents(dtfProductionTime1, dtfProductionTime2);
		timeLayout.setMargin(false);

		VerticalLayout productionLayout = new VerticalLayout();
		productionLayout.setMargin(true);

		productionLayout.addComponents(groupProductionSummary, quantityLayout, timeLayout, btnRecordProduction);

		return productionLayout;
	}

	private Component createEntityTreeLayout() {
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
		treeEntity = new Tree<>();
		treeEntity.setSelectionMode(SelectionMode.SINGLE);
		treeEntity.setCaption("Plant Entities");
		treeEntity.setItemIconGenerator(new IconGenerator<EntityNode>() {
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

		treeEntity.addSelectionListener(event -> {
			try {
				EntityNode node = event.getFirstSelectedItem().get();
				onSelectEntity(node);
			} catch (Exception e) {
				showException(e);
			}
		});

		return treeEntity;
	}

	private void clearAvailability() {
		tfAvailabilityReason.clear();
		tfAvailabilityHours.clear();
		tfAvailabilityMinutes.clear();
	}

	private void clearProduction() {
		tfQualityReason.clear();
		tfAmount.clear();
		tfAmount.setEnabled(false);
	}

	private void clearSetup() {
		tfMaterial.clear();
		tfMaterial.setData(null);
		tfJob.clear();
	}

	private void updateMaterialJob(Material material, String job) {
		// current job
		if (job != null) {
			lbJob.setValue(job);
		} else {
			lbJob.setValue("");
		}

		if (material != null) {
			lbMaterialId.setValue(material.getName());
			lbMaterialId.setData(material);
			lbMaterialDescription.setValue(material.getDescription());
		} else {
			lbMaterialId.setValue("");
			lbMaterialId.setData(null);
			lbMaterialDescription.setValue("");
		}
	}

	private void onSelectEntity(EntityNode entityNode) throws Exception {
		// clear fields
		updateMaterialJob(null, null);
		clearAvailability();
		clearProduction();
		clearSetup();

		boolean enabled = false;

		Equipment equipment = getSelectedEquipment();

		if (equipment != null) {
			enabled = true;

			OeeEvent lastSetup = operationsPresenter.getLastSetup(equipment);

			if (lastSetup != null) {
				updateMaterialJob(lastSetup.getMaterial(), lastSetup.getJob());
			}
		} else {
			// higher level, get the children
			@SuppressWarnings("unchecked")
			TreeDataProvider<EntityNode> dataProvider = (TreeDataProvider<EntityNode>) treeEntity.getDataProvider();
			TreeData<EntityNode> treeData = dataProvider.getTreeData();

			Set<EntityNode> children = entityNode.getChildren();

			// add the node and its children
			treeData.addItems(entityNode, children);
		}

		btnRecordAvailability.setEnabled(enabled);
		btnRecordProduction.setEnabled(enabled);
		btnRecordSetup.setEnabled(enabled);

	}

	private Component createReasonTreeLayout() {
		// tree view of reasons
		treeGridReason = new TreeGrid<>();
		treeGridReason.setCaption("Reasons");
		treeGridReason.setHeightByRows(6);

		treeGridReason.addColumn(Reason::getName).setCaption("Name");
		treeGridReason.addColumn(Reason::getDescription).setCaption("Description");
		treeGridReason.addColumn(Reason::getLossCategory).setCaption("Loss Category");

		treeGridReason.addItemClickListener(event -> {
			Reason reason = event.getItem();
			tfAvailabilityReason.setValue(reason.getName());
			tfAvailabilityReason.setData(reason);
		});

		// refresh reasons
		Button btnRefreshReasons = new Button();
		btnRefreshReasons.setIcon(VaadinIcons.REFRESH);
		btnRefreshReasons.setEnabled(true);
		btnRefreshReasons.setStyleName(ValoTheme.BUTTON_ICON_ONLY);
		btnRefreshReasons.addClickListener(event -> {
			try {
				operationsPresenter.populateReasonGrid(treeGridReason);
			} catch (Exception e) {
				showException(e);
			}
		});

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponent(btnRefreshReasons);
		layout.addComponentsAndExpand(treeGridReason);

		layout.setMargin(true);

		return layout;
	}

	private Component createMaterialTreeLayout() {
		// materials by category
		treeGridMaterial = new TreeGrid<>();
		treeGridMaterial.setCaption("Material");
		treeGridMaterial.setHeightByRows(6);

		treeGridMaterial.addColumn(MaterialCategory::getName).setCaption("Name");
		treeGridMaterial.addColumn(MaterialCategory::getDescription).setCaption("Description");

		treeGridMaterial.addItemClickListener(event -> {
			MaterialCategory materialCategory = event.getItem();

			if (materialCategory.getMaterial() != null) {
				Material material = materialCategory.getMaterial();
				tfMaterial.setValue(material.getName());
				tfMaterial.setData(material);
			}
		});

		// refresh materials
		Button btnRefreshMaterials = new Button();
		btnRefreshMaterials.setIcon(VaadinIcons.REFRESH);
		btnRefreshMaterials.setEnabled(true);
		btnRefreshMaterials.setStyleName(ValoTheme.BUTTON_ICON_ONLY);
		btnRefreshMaterials.addClickListener(event -> {
			try {
				operationsPresenter.populateMaterialGrid(treeGridMaterial);
			} catch (Exception e) {
				showException(e);
			}
		});

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponent(btnRefreshMaterials);
		layout.addComponentsAndExpand(treeGridMaterial);
		layout.setMargin(true);

		return layout;
	}

	private Component createAvailabilityLayout() {
		groupAvailabilitySummary = new RadioButtonGroup<>("Availability");
		groupAvailabilitySummary.setItems(BY_EVENT, SUMMARIZED);
		groupAvailabilitySummary.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
		groupAvailabilitySummary.setRequiredIndicatorVisible(true);

		groupAvailabilitySummary.addSelectionListener(event -> {
			try {
				Optional<String> item = event.getSelectedItem();

				onSelectAvailabilityData(item.get());
			} catch (Exception e) {
				showException(e);
			}
		});

		tfAvailabilityReason = new TextField("Reason");
		tfAvailabilityReason.setIcon(VaadinIcons.PENCIL);
		tfAvailabilityReason.setRequiredIndicatorVisible(true);

		dtfAvailabilityStart = new DateTimeField(EVENT_TIME);
		dtfAvailabilityStart.setValue(LocalDateTime.now());
		dtfAvailabilityStart.setIcon(VaadinIcons.TIME_FORWARD);
		dtfAvailabilityStart.setRequiredIndicatorVisible(true);

		dtfAvailabilityEnd = new DateTimeField(TO_TIME);
		dtfAvailabilityEnd.setValue(null);
		dtfAvailabilityEnd.setIcon(VaadinIcons.TIME_FORWARD);
		dtfAvailabilityEnd.setRequiredIndicatorVisible(true);

		tfAvailabilityHours = new TextField(HOURS);
		tfAvailabilityHours.setIcon(VaadinIcons.CLOCK);
		tfAvailabilityHours.setRequiredIndicatorVisible(true);

		tfAvailabilityMinutes = new TextField(MINUTES);
		tfAvailabilityMinutes.setIcon(VaadinIcons.CLOCK);
		tfAvailabilityMinutes.setRequiredIndicatorVisible(true);

		btnRecordAvailability = new Button("Record");
		btnRecordAvailability.setIcon(VaadinIcons.NOTEBOOK);
		btnRecordAvailability.setEnabled(false);
		btnRecordAvailability.setStyleName(ValoTheme.BUTTON_PRIMARY);
		btnRecordAvailability.addClickListener(event -> {
			try {
				recordAvailabilityEvent();
				clearAvailability();
			} catch (Exception e) {
				showException(e);
			}
		});

		HorizontalLayout timeLayout = new HorizontalLayout();
		timeLayout.setMargin(false);
		timeLayout.addComponents(dtfAvailabilityStart, dtfAvailabilityEnd, tfAvailabilityHours, tfAvailabilityMinutes);

		VerticalLayout availabilityLayout = new VerticalLayout();
		availabilityLayout.setMargin(true);
		availabilityLayout.addComponents(groupAvailabilitySummary, tfAvailabilityReason, timeLayout, btnRecordAvailability);

		return availabilityLayout;
	}

	private Component createSetupLayout() {

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
				recordSetupEvent();
				clearSetup();
			} catch (Exception e) {
				showException(e);
			}
		});

		HorizontalLayout materialJobLayout = new HorizontalLayout();
		materialJobLayout.addComponents(tfMaterial, tfJob);

		VerticalLayout setupLayout = new VerticalLayout();
		setupLayout.setMargin(true);
		setupLayout.addComponents(materialJobLayout, dtfSetupTime, btnRecordSetup);

		return setupLayout;
	}

	Equipment getSelectedEquipment() throws Exception {
		Equipment equipment = null;

		Set<EntityNode> entityNodes = treeEntity.getSelectedItems();

		if (entityNodes.isEmpty()) {
			return equipment;
		}

		Iterator<EntityNode> iter = entityNodes.iterator();
		PlantEntity entity = iter.next().getEntity();

		if (entity.getLevel().equals(EntityLevel.EQUIPMENT)) {
			equipment = (Equipment) entity;
		}

		return equipment;
	}

	private OeeEventType getProductionType() {
		OeeEventType resolverType = null;

		if (groupProductionType.getSelectedItem().isPresent()) {
			String type = groupProductionType.getSelectedItem().get();

			if (type.equals(PROD_GOOD)) {
				resolverType = OeeEventType.PROD_GOOD;
			} else if (type.equals(PROD_REJECT)) {
				resolverType = OeeEventType.PROD_REJECT;
			} else if (type.equals(PROD_STARTUP)) {
				resolverType = OeeEventType.PROD_STARTUP;
			}
		}
		return resolverType;
	}

	private void recordProductionEvent() throws Exception {
		// quantity produced
		Double amount = null;

		if (tfAmount.getValue() != null && tfAmount.getValue().trim().length() > 0) {
			amount = Double.valueOf(tfAmount.getValue());
		} else {
			throw new Exception("An amount must be specified.");
		}

		UnitOfMeasure uom = (UnitOfMeasure) lbUOM.getData();

		LocalDateTime startTime = dtfProductionTime1.getValue();
		LocalDateTime endTime = dtfProductionTime2.getValue();

		String selectedItem = groupProductionSummary.getSelectedItem().get();

		Duration duration = null;
		if (selectedItem.equals(BY_EVENT)) {
			endTime = null;
		} else {
			duration = Duration.between(startTime, endTime);
		}
		
		// reason
		String reasonName = tfQualityReason.getValue();
		Reason reason = null;
		
		if (reasonName != null && reasonName.length() > 0) {
			reason = PersistenceService.instance().fetchReasonByName(reasonName);
			
			if (reason == null) {
				throw new Exception("The reason '" + reasonName + "' was not found in the database.");
			}
		}
		
		// the production event
		OeeEvent event = createEvent(getProductionType(), getSelectedEquipment(), startTime, endTime);

		event.setDuration(duration);
		event.setAmount(amount);
		event.setUOM(uom);
		event.setReason(reason);
		event.setInputValue(String.valueOf(amount));

		// material being produced
		OeeEvent setup = PersistenceService.instance().fetchLastEvent(getSelectedEquipment(), OeeEventType.MATL_CHANGE);

		if (setup != null) {
			event.setMaterial(setup.getMaterial());
		}

		AppServices.instance().recordEvent(event);
	}

	private OeeEvent createEvent(OeeEventType type, Equipment equipment, LocalDateTime startTime, LocalDateTime endTime)
			throws Exception {
		if (type == null) {
			throw new Exception("The event type must be specified.");
		}

		OeeEvent event = new OeeEvent(equipment);
		event.setEventType(type);
		event.setStartTime(DomainUtils.fromLocalDateTime(startTime));
		event.setEndTime(DomainUtils.fromLocalDateTime(endTime));
		event.setSourceId(OPS_SOURCE_ID);

		// get the shift from the work schedule
		WorkSchedule schedule = equipment.findWorkSchedule();

		if (schedule != null) {
			List<ShiftInstance> shifts = schedule.getShiftInstancesForTime(startTime);

			if (!shifts.isEmpty()) {
				event.setShift(shifts.get(0).getShift());
				event.setTeam(shifts.get(0).getTeam());
			}
		}
		return event;
	}

	private void recordSetupEvent() throws Exception {
		// job
		String job = tfJob.getValue();

		// material
		Material material = (Material) tfMaterial.getData();

		if (material == null) {
			throw new Exception("Material must be specified.");
		}

		OeeEvent event = createEvent(OeeEventType.MATL_CHANGE, getSelectedEquipment(), dtfSetupTime.getValue(), null);
		event.setJob(job);
		event.setMaterial(material);
		event.setInputValue(material.getName());

		AppServices.instance().recordEvent(event);

		updateMaterialJob(material, job);
	}

	private void recordAvailabilityEvent() throws Exception {
		// reason
		Reason reason = (Reason) tfAvailabilityReason.getData();

		if (reason == null) {
			throw new Exception("A reason must be selected.");
		}

		// duration
		LocalDateTime startTime = dtfAvailabilityStart.getValue();
		LocalDateTime endTime = dtfAvailabilityEnd.getValue();

		Duration duration = null;

		String selectedItem = groupAvailabilitySummary.getSelectedItem().get();
		if (selectedItem.equals(SUMMARIZED)) {
			// specified duration
			int seconds = 0;

			if (tfAvailabilityHours.getValue() != null && tfAvailabilityHours.getValue().trim().length() > 0) {
				seconds = Integer.valueOf(tfAvailabilityHours.getValue().trim()) * 3600;
			}

			if (tfAvailabilityMinutes.getValue() != null && tfAvailabilityMinutes.getValue().trim().length() > 0) {
				seconds += Integer.valueOf(tfAvailabilityMinutes.getValue().trim()) * 60;
			}

			duration = Duration.ofSeconds(seconds);
		} else {
			// by event (no end time)
			endTime = null;
		}

		// create availability event
		OeeEvent event = createEvent(OeeEventType.AVAILABILITY, getSelectedEquipment(), startTime, endTime);
		event.setReason(reason);
		event.setDuration(duration);
		event.setInputValue(reason.getName());

		// material being produced
		OeeEvent setup = PersistenceService.instance().fetchLastEvent(getSelectedEquipment(), OeeEventType.MATL_CHANGE);

		if (setup != null) {
			event.setMaterial(setup.getMaterial());
		}

		AppServices.instance().recordEvent(event);
	}

	private void onSelectAvailabilityData(String type) {
		if (type.equals(BY_EVENT)) {
			dtfAvailabilityStart.setCaption(EVENT_TIME);
			dtfAvailabilityEnd.setVisible(false);
			tfAvailabilityHours.setVisible(false);
			tfAvailabilityMinutes.setVisible(false);
		} else if (type.equals(SUMMARIZED)) {
			dtfAvailabilityStart.setCaption(FROM_TIME);
			dtfAvailabilityEnd.setVisible(true);
			tfAvailabilityHours.setVisible(true);
			tfAvailabilityMinutes.setVisible(true);
		}
	}

	private void onSelectProductionData(String type) {
		if (type.equals(BY_EVENT)) {
			dtfProductionTime1.setCaption(EVENT_TIME);
			dtfProductionTime2.setVisible(false);
		} else if (type.equals(SUMMARIZED)) {
			dtfProductionTime1.setCaption(FROM_TIME);
			dtfProductionTime2.setVisible(true);
		}
	}

	private void onSelectProductionType(String type) throws Exception {
		// update current material/job
		Equipment equipment = getSelectedEquipment();

		Material material = (Material) lbMaterialId.getData();

		if (material == null) {
			throw new Exception("The material being processed must be defined.");
		}

		// update UOMs
		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);

		if (eqm == null) {
			throw new Exception("The equipment settings for material " + material.getName() + " have not been defined");
		}

		OeeEventType resolverType = null;
		if (type.equals(PROD_GOOD)) {
			resolverType = OeeEventType.PROD_GOOD;
		} else if (type.equals(PROD_REJECT)) {
			resolverType = OeeEventType.PROD_REJECT;
		} else if (type.equals(PROD_STARTUP)) {
			resolverType = OeeEventType.PROD_STARTUP;
		}

		UnitOfMeasure uom = equipment.getUOM(material, resolverType);
		if (uom == null) {
			throw new Exception("The unit of measure has not been defined for material " + material.getName()
					+ " for this type of production.");
		}
		lbUOM.setValue(uom.getSymbol());
		lbUOM.setData(uom);

		tfAmount.setEnabled(true);
	}

	// callback
	void onException(Exception e) {
		// put on UI thread
		Runnable exceptionTask = () -> {
			showException(e);
		};
		ui.access(exceptionTask);
	}

	private void showException(Exception e) {
		Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
	}
}
