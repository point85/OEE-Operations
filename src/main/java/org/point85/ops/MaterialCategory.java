package org.point85.ops;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Material;

public class MaterialCategory {
	private Material material;
	private String category;

	public MaterialCategory(Material material) {
		this.material = material;
	}

	public MaterialCategory(String category) {
		this.category = category;
	}

	public String getName() {
		return (material != null) ? material.getName() : category;
	}

	public String getDescription() {
		return (material != null) ? material.getDescription() : "Category";
	}
	
	public Material getMaterial() {
		return material;
	}

	public List<MaterialCategory> getMaterialsInCategory() throws Exception {
		List<Material> materials = PersistenceService.instance().fetchMaterialsByCategory(category);

		List<MaterialCategory> materialCategories = new ArrayList<>();
		for (Material aMaterial : materials) {
			materialCategories.add(new MaterialCategory(aMaterial));
		}
		return materialCategories;
	}
}
