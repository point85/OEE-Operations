package org.point85.ops;

import java.time.OffsetDateTime;
import java.util.List;

import org.point85.domain.collector.EventHistory;
import org.point85.domain.collector.ProductionHistory;
import org.point85.domain.collector.SetupHistory;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentEventResolver;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventCollector {
	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private OeeContext appContext = new OeeContext();

	public EventCollector() {

	}

	public void resolveEvent(Equipment equipment, EventResolverType type, Object sourceValue, OffsetDateTime timestamp)
			throws Exception {
		EquipmentEventResolver equipmentResolver = new EquipmentEventResolver();

		// find resolver by type
		List<EventResolver> resolvers = equipmentResolver.getResolvers(equipment);

		EventResolver configuredResolver = null;
		for (EventResolver resolver : resolvers) {
			if (resolver.getType().equals(type)) {
				configuredResolver = resolver;
				break;
			}
		}

		if (configuredResolver == null) {
			throw new Exception("No script resolver found for equipment " + equipment.getName() + " with type " + type);
		}

		ResolvedEvent resolvedDataItem = equipmentResolver.invokeResolver(configuredResolver, appContext, sourceValue,
				timestamp);

		recordResolution(resolvedDataItem);
	}

	private synchronized void recordResolution(ResolvedEvent resolvedEvent) throws Exception {
		EventResolverType type = resolvedEvent.getResolverType();

		// first in database
		switch (type) {
		case AVAILABILITY:
			saveAvailabilityRecord(resolvedEvent);
			break;

		case JOB:
		case MATERIAL:
			saveSetupRecord(resolvedEvent);
			break;

		case OTHER:
			onOtherResolution(resolvedEvent);
			break;

		case PROD_GOOD:
		case PROD_REJECT:
			saveProductionRecord(resolvedEvent);
			break;

		default:
			break;
		}

		// send event message
		// sendResolutionMessage(resolvedEvent);
	}

	private void saveAvailabilityRecord(ResolvedEvent resolvedItem) throws Exception {
		EventHistory history = new EventHistory(resolvedItem);
		history.setReason(resolvedItem.getReason());

		PersistenceService.instance().persist(history);
	}

	private void saveSetupRecord(ResolvedEvent resolvedItem) throws Exception {
		SetupHistory history = new SetupHistory(resolvedItem);

		PersistenceService.instance().persist(history);
	}

	private void saveProductionRecord(ResolvedEvent resolvedItem) throws Exception {
		Equipment equipment = resolvedItem.getEquipment();
		Material material = resolvedItem.getMaterial();
		UnitOfMeasure uom = null;

		if (material != null) {
			EquipmentMaterial equipmentMaterial = equipment.getEquipmentMaterial(material);

			if (equipmentMaterial != null) {
				switch (resolvedItem.getResolverType()) {
				case PROD_GOOD:
					uom = equipmentMaterial.getRunRateUOM();
					break;
				case PROD_REJECT:
					uom = equipmentMaterial.getRejectUOM();
					break;
				default:
					break;
				}
			}
		}
		ProductionHistory history = new ProductionHistory(resolvedItem);
		history.setType(resolvedItem.getResolverType());
		history.setAmount(resolvedItem.getQuantity().getAmount());
		history.setUOM(uom);

		PersistenceService.instance().persist(history);
	}

	protected void onOtherResolution(ResolvedEvent resolvedItem) {
		if (logger.isInfoEnabled()) {
			logger.info("Other ");
		}
	}
}
