package org.point85.ops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.point85.domain.collector.CollectorExceptionListener;
import org.point85.domain.collector.CollectorServer;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventType;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;

public class OperationsPresenter implements CollectorExceptionListener {
	// type of event resolver
	private EventType resolverType;

	// event data collector
	private CollectorServer collectorServer;

	// view
	private OperationsView operationsView;

	OperationsPresenter(OperationsView view) {
		this.operationsView = view;

		// collector server
		collectorServer = new CollectorServer();
	}

	void setResolverType(EventType resolverType) {
		this.resolverType = resolverType;
	}

	EventType getResolverType() {
		return this.resolverType;
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
		// entityNodes.forEach(entityNode -> treeData.addItems(entityNode,
		// entityNode.getChildren()));

		TreeDataProvider<EntityNode> dataProvider = new TreeDataProvider<>(treeData);
		entityTree.setDataProvider(dataProvider);
	}

	void populateReasonGrid(TreeGrid<Reason> reasonTreeGrid) {
		List<Reason> reasons = PersistenceService.instance().fetchTopReasons();

		// Initialize a TreeGrid and set in-memory data
		reasonTreeGrid.setItems(reasons, Reason::getChildren);
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
	}

	// callback
	@Override
	public void onException(Exception e) {
		operationsView.onException(e);
	}

	public void recordEvent(OeeEvent event) throws Exception {
		this.collectorServer.saveOeeEvent(event);
	}

	public OeeEvent getLastSetup(Equipment equipment) {
		return PersistenceService.instance().fetchLastSetup(equipment);
	}
}
