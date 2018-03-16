package org.point85.ops;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.point85.domain.collector.AvailabilityHistory;
import org.point85.domain.collector.AvailabilitySummary;
import org.point85.domain.collector.CollectorExceptionListener;
import org.point85.domain.collector.CollectorServer;
import org.point85.domain.collector.LossSummary;
import org.point85.domain.collector.ProductionSummary;
import org.point85.domain.collector.SetupHistory;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;

public class OperationsPresenter implements CollectorExceptionListener {
	// type of event resolver
	private EventResolverType resolverType;

	// event data collector
	private CollectorServer collectorServer;

	// view
	private OperationsView operationsView;

	OperationsPresenter(OperationsView view) {
		this.operationsView = view;

		// collector server
		collectorServer = new CollectorServer();
	}

	void setResolverType(EventResolverType resolverType) {
		this.resolverType = resolverType;
	}

	CollectorServer getCollectorServer() {
		return collectorServer;
	}

	void startupCollector() throws Exception {
		// register for exceptions
		collectorServer.registerExceptionLisener(this);

		// startup server
		collectorServer.startup();
	}

	void shutdownCollector() throws Exception {
		if (collectorServer != null) {
			collectorServer.shutdown();
		}
	}

	void populateTopEntityNodes(Tree<EntityNode> entityTree) {

		// fetch the entities
		List<PlantEntity> entities = PersistenceService.instance().fetchTopPlantEntities();
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

	void populateReasonGrid(TreeGrid<Reason> reasonTreeGrid) {
		List<Reason> reasons = PersistenceService.instance().fetchTopReasons();

		// Initialize a TreeGrid and set in-memory data
		reasonTreeGrid.setItems(reasons, Reason::getChildren);

		// The first column gets the hierarchy indicator by default
		/*
		 * reasonTreeGrid.addColumn(Reason::getName).setCaption("Name");
		 * reasonTreeGrid.addColumn(Reason::getDescription).setCaption("Description");
		 * reasonTreeGrid.addColumn(Reason::getLossCategory).setCaption("Loss Category"
		 * );
		 */
	}

	void populateMaterialGrid(TreeGrid<MaterialCategory> materialTreeGrid) {
		List<String> categories = PersistenceService.instance().fetchMaterialCategories();

		List<MaterialCategory> materialCategories = new ArrayList<>();
		for (String category : categories) {
			materialCategories.add(new MaterialCategory(category));
		}

		// Initialize a TreeGrid and set in-memory data
		materialTreeGrid.setItems(materialCategories, source -> {
			List<MaterialCategory> materials = new ArrayList<>();
			try {
				materials = source.getMaterialsInCategory();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return materials;
		});

		// The first column gets the hierarchy indicator by default
		/*
		 * materialTreeGrid.addColumn(MaterialCategory::getName).setCaption("Name");
		 * materialTreeGrid.addColumn(MaterialCategory::getDescription).setCaption(
		 * "Description");
		 */
	}

	// callback
	@Override
	public void onException(Exception e) {
		operationsView.onException(e);
	}

	void recordProductionEvent(Equipment equipment, Double amount, Material material, OffsetDateTime odt)
			throws Exception {
		if (resolverType == null) {
			throw new Exception("A production type must be selected.");
		}

		ResolvedEvent event = new ResolvedEvent();
		event.setEquipment(equipment);
		event.setMaterial(material);
		event.setTimestamp(odt);
		event.setResolverType(resolverType);

		collectorServer.saveProductionHistory(event);
	}

	void recordProductionSummary(Equipment equipment, Double amount, Material material, OffsetDateTime startTime,
			OffsetDateTime endTime, Duration duration) throws Exception {
		if (resolverType == null) {
			throw new Exception("A production type must be selected.");
		}

		LossSummary lossSummary = new LossSummary();
		lossSummary.setEquipment(equipment);
		lossSummary.setMaterial(material);
		lossSummary.setStartTime(startTime);
		lossSummary.setEndTime(endTime);

		UnitOfMeasure uom = null;
		Quantity quantity = new Quantity(amount, uom);
		lossSummary.setQuantity(quantity);

		ProductionSummary summary = new ProductionSummary(lossSummary);

		PersistenceService.instance().persist(summary);
	}

	void recordChangeoverEvent(Equipment equipment, String job, Material material, OffsetDateTime odt)
			throws Exception {
		ResolvedEvent event = new ResolvedEvent();
		event.setEquipment(equipment);
		event.setTimestamp(odt);
		event.setJob(job);
		event.setMaterial(material);

		// job
		if (job != null && job.trim().length() > 0) {
			event.setResolverType(EventResolverType.JOB);
		}

		if (material != null) {
			event.setResolverType(EventResolverType.MATERIAL);
		}
		SetupHistory history = new SetupHistory(event);

		PersistenceService.instance().persist(history);
	}

	void recordAvailabilityEvent(Equipment equipment, Reason reason, OffsetDateTime odt) throws Exception {
		ResolvedEvent event = new ResolvedEvent();
		event.setEquipment(equipment);
		event.setTimestamp(odt);

		AvailabilityHistory history = new AvailabilityHistory(event);
		history.setReason(reason);

		PersistenceService.instance().persist(history);
	}

	void recordAvailabilitySummary(Equipment equipment, Reason reason, OffsetDateTime startTime, OffsetDateTime endTime,
			Duration duration) throws Exception {

		LossSummary lossSummary = new LossSummary();
		lossSummary.setEquipment(equipment);
		lossSummary.setStartTime(startTime);
		lossSummary.setEndTime(endTime);
		lossSummary.setReason(reason);

		AvailabilitySummary summary = new AvailabilitySummary(lossSummary);

		PersistenceService.instance().persist(summary);
	}
}
