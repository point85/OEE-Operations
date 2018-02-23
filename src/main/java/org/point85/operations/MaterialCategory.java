package org.point85.operations;

import java.util.ArrayList;
import java.util.List;

import org.point85.core.persistence.PersistencyService;
import org.point85.core.plant.Material;

public class MaterialCategory {
	private Material material;
	private String materialCategory;

	public MaterialCategory(Material material) {
		this.material = material;
	}

	public MaterialCategory(String category) {
		this.materialCategory = category;
	}

	public String getName() {
		return (material != null) ? material.getName() : materialCategory;
	}

	public String getDescription() {
		return (material != null) ? material.getDescription() : "Category";
	}

	public List<MaterialCategory> getMaterialsInCategory() throws Exception {
		List<Material> materials = PersistencyService.instance().fetchMaterialsByCategory(materialCategory);

		List<MaterialCategory> materialCategories = new ArrayList<>();
		for (Material material : materials) {
			materialCategories.add(new MaterialCategory(material));
		}
		return materialCategories;
	}
}
