package org.point85.ops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.OeeEventType;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;

public class OperationsPresenter  {
	// view
	private final OperationsView operationsView;

	OperationsPresenter(OperationsView view) {
		this.operationsView = view;
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
				onException(e);
			}
			return materials;
		});
	}

	// callback
	void onException(Exception e) {
		operationsView.onException(e);
	}

	public OeeEvent getLastSetup(Equipment equipment) {
		return PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);
	}
}
