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

	private static final String HOURS = "Hours";
	private static final String MINUTES = "Minutes";

	private static final String BY_EVENT = "By Event";
	private static final String SUMMARIZED = "Summarized";
	private static final String EVENT_TIME = "Event Time";
	private static final String FROM_TIME = "From Time";
	private static final String TO_TIME = "To Time";

	// good or reject production
	private static final String PROD_GOOD = "Good";
	private static final String PROD_REJECT = "Reject/Rework";

	// availability
	private RadioButtonGroup<String> groupAvailabilitySummary;
	private Button btnRecordAvailability;
	private TextField tfReason;
	private DateTimeField dtfAvailabilityTime1;
	private DateTimeField dtfAvailabilityTime2;
	private TextField tfAvailabilityHours;
	private TextField tfAvailabilityMinutes;
	private Tree<EntityNode> treeEntity;
	private TreeGrid<Reason> treeGridReason;
	private TreeGrid<MaterialCategory> treeGridMaterial;

	// production
	private RadioButtonGroup<String> groupProductionSummary;
	private Button btnRecordProduction;
	private RadioButtonGroup<String> groupProductionType;
	private TextField tfAmount;
	private DateTimeField dtfProductionTime1;
	private DateTimeField dtfProductionTime2;
	private Label lbUOM;
	private Label lbMaterialId;
	private Label lbMaterialDescription;
	private Label lbJob;

	// setup/changeover
	private Button btnRecordSetup;
	private TextField tfMaterial;
	private TextField tfJob;
	private DateTimeField dtfSetupTime;

	// the presenter
	private OperationsPresenter operationsPresenter;

	// the UI
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
		eventPanel.setSplitPosition(40.0f);
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
		groupProductionType.setItems(PROD_GOOD, PROD_REJECT);
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
		tfAmount.setIcon(VaadinIcons.PLAY_CIRCLE_O);
		tfAmount.setRequiredIndicatorVisible(true);
		tfAmount.setEnabled(false);

		lbUOM = new Label("Unit");
		lbUOM.setWidth("75px");

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
				String selectedItem = groupProductionSummary.getSelectedItem().get();
				if (selectedItem.equals(BY_EVENT)) {
					recordProductionEvent();
				} else {
					recordProductionSummary();
				}
				clearProduction();
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
		quantityLayout.addComponents(groupProductionType, tfAmount, lbUOM);

		HorizontalLayout timeLayout = new HorizontalLayout();
		timeLayout.addComponents(dtfProductionTime1, dtfProductionTime2);

		VerticalLayout productionLayout = new VerticalLayout();
		productionLayout.setMargin(true);

		productionLayout.addComponents(materialLayout, groupProductionSummary, quantityLayout, timeLayout,
				btnRecordProduction);

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
		tfReason.clear();
		tfAvailabilityHours.clear();
		tfAvailabilityMinutes.clear();
	}

	private void clearProduction() {
		groupProductionType.clear();
		tfAmount.clear();
		tfAmount.setEnabled(false);
	}

	private void clearSetup() {
		tfMaterial.clear();
		tfJob.clear();
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
		treeGridReason = new TreeGrid<>();
		treeGridReason.setCaption("Reasons");
		treeGridReason.setHeightByRows(6);

		treeGridReason.addColumn(Reason::getName).setCaption("Name");
		treeGridReason.addColumn(Reason::getDescription).setCaption("Description");
		treeGridReason.addColumn(Reason::getLossCategory).setCaption("Loss Category");

		treeGridReason.addItemClickListener(event -> {
			Reason reason = event.getItem();
			tfReason.setValue(reason.getName());
			tfReason.setData(reason);
		});

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponentsAndExpand(treeGridReason);
		layout.setMargin(true);

		return layout;
	}

	private Component createMaterialTreeLayout() {
		treeGridMaterial = new TreeGrid<>();
		treeGridMaterial.setCaption("Material");
		treeGridMaterial.setHeightByRows(6);

		treeGridMaterial.addColumn(MaterialCategory::getName).setCaption("Name");
		treeGridMaterial.addColumn(MaterialCategory::getDescription).setCaption("Description");

		treeGridMaterial.addItemClickListener(event -> {
			Material material = event.getItem().getMaterial();
			tfMaterial.setValue(material.getName());
			tfMaterial.setData(material);
		});

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponentsAndExpand(treeGridMaterial);
		layout.setMargin(true);

		return layout;
	}

	private Component createAvailabilityLayout() {
		VerticalLayout reasonLayout = new VerticalLayout();
		reasonLayout.setMargin(true);

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

		tfReason = new TextField("Reason");
		tfReason.setIcon(VaadinIcons.PENCIL);
		tfReason.setRequiredIndicatorVisible(true);

		dtfAvailabilityTime1 = new DateTimeField(EVENT_TIME);
		dtfAvailabilityTime1.setValue(LocalDateTime.now());
		dtfAvailabilityTime1.setIcon(VaadinIcons.TIME_FORWARD);
		dtfAvailabilityTime1.setRequiredIndicatorVisible(true);

		dtfAvailabilityTime2 = new DateTimeField(TO_TIME);
		dtfAvailabilityTime2.setValue(LocalDateTime.now());
		dtfAvailabilityTime2.setIcon(VaadinIcons.TIME_FORWARD);
		dtfAvailabilityTime2.setRequiredIndicatorVisible(true);

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
		btnRecordAvailability.setDescription("Button description");
		btnRecordAvailability.addClickListener(event -> {
			try {
				String selectedItem = groupAvailabilitySummary.getSelectedItem().get();
				if (selectedItem.equals(BY_EVENT)) {
					recordAvailabilityEvent();
				} else {
					recordAvailabilitySummary();
				}
				clearAvailability();
			} catch (Exception e) {
				showException(e);
			}
		});

		HorizontalLayout timeLayout = new HorizontalLayout();
		timeLayout.addComponents(dtfAvailabilityTime1, dtfAvailabilityTime2, tfAvailabilityHours,
				tfAvailabilityMinutes);

		reasonLayout.addComponents(groupAvailabilitySummary, tfReason, timeLayout, btnRecordAvailability);

		return reasonLayout;
	}

	private Component createSetupLayout() {
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
		Set<EntityNode> entityNodes = treeEntity.getSelectedItems();

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
		Material material = (Material) tfMaterial.getData();
		OffsetDateTime odt = DomainUtils.fromLocalDateTime(dtfProductionTime1.getValue());
		operationsPresenter.recordProductionEvent(equipment, amount, material, odt);
	}

	private void recordProductionSummary() throws Exception {
		Equipment equipment = getSelectedEquipment();
		Double amount = null;

		if (tfAmount.getValue() != null && tfAmount.getValue().trim().length() > 0) {
			amount = Double.valueOf(tfAmount.getValue());
		} else {
			throw new Exception("An amount must be specified.");
		}

		Material material = (Material) tfMaterial.getData();

		OffsetDateTime startTime = DomainUtils.fromLocalDateTime(dtfProductionTime1.getValue());
		OffsetDateTime endTime = DomainUtils.fromLocalDateTime(dtfProductionTime2.getValue());

		operationsPresenter.recordProductionSummary(equipment, amount, material, startTime, endTime);
	}

	private void recordChangeoverEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();
		OffsetDateTime odt = DomainUtils.fromLocalDateTime(dtfSetupTime.getValue());

		// job
		String job = tfJob.getValue();

		// material
		Material material = (Material) tfMaterial.getData();

		operationsPresenter.recordChangeoverEvent(equipment, job, material, odt);
	}

	private void recordAvailabilityEvent() throws Exception {
		Equipment equipment = getSelectedEquipment();

		String reason = tfReason.getValue();

		if (reason == null || reason.length() == 0 || tfReason.getData() == null) {
			throw new Exception("A reason must be selected.");
		}

		OffsetDateTime odt = DomainUtils.fromLocalDateTime(dtfAvailabilityTime1.getValue());

		operationsPresenter.recordAvailabilityEvent(equipment, (Reason) tfReason.getData(), odt);
	}

	private void recordAvailabilitySummary() throws Exception {
		Equipment equipment = getSelectedEquipment();

		String reason = tfReason.getValue();

		if (reason == null || reason.length() == 0 || tfReason.getData() == null) {
			throw new Exception("A reason must be selected.");
		}

		OffsetDateTime startTime = DomainUtils.fromLocalDateTime(dtfAvailabilityTime1.getValue());
		OffsetDateTime endTime = DomainUtils.fromLocalDateTime(dtfAvailabilityTime2.getValue());

		int seconds = 0;

		if (tfAvailabilityHours.getValue() != null && tfAvailabilityHours.getValue().trim().length() > 0) {
			seconds = Integer.valueOf(tfAvailabilityHours.getValue().trim()) * 3600;
		}

		if (tfAvailabilityMinutes.getValue() != null && tfAvailabilityMinutes.getValue().trim().length() > 0) {
			seconds += Integer.valueOf(tfAvailabilityMinutes.getValue().trim()) * 60;
		}

		Duration duration = Duration.ofSeconds(seconds);

		operationsPresenter.recordAvailabilitySummary(equipment, (Reason) tfReason.getData(), startTime, endTime,
				duration);
	}

	private void onSelectAvailabilityData(String type) {
		if (type.equals(BY_EVENT)) {
			dtfAvailabilityTime1.setCaption(EVENT_TIME);
			dtfAvailabilityTime2.setVisible(false);
			tfAvailabilityHours.setVisible(false);
			tfAvailabilityMinutes.setVisible(false);
		} else if (type.equals(SUMMARIZED)) {
			dtfAvailabilityTime1.setCaption(FROM_TIME);
			dtfAvailabilityTime2.setVisible(true);
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
		
		tfAmount.setEnabled(true);
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
