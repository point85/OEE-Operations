package org.point85.ops;

import java.util.HashSet;
import java.util.Set;

import org.point85.domain.plant.PlantEntity;

public class EntityNode {
	private PlantEntity entity;

	EntityNode(PlantEntity entity) {
		this.entity = entity;
	}

	PlantEntity getEntity() {
		return entity;
	}

	Set<EntityNode> getChildren() {
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
