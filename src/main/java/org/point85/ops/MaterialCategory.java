package org.point85.ops;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Material;

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
	
	public Material getMaterial() {
		return material;
	}

	public List<MaterialCategory> getMaterialsInCategory() throws Exception {
		List<Material> materials = PersistenceService.instance().fetchMaterialsByCategory(materialCategory);

		List<MaterialCategory> materialCategories = new ArrayList<>();
		for (Material material : materials) {
			materialCategories.add(new MaterialCategory(material));
		}
		return materialCategories;
	}
}
