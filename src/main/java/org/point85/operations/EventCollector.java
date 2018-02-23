package org.point85.operations;

import java.time.OffsetDateTime;
import java.util.List;

import org.point85.core.collector.EventHistory;
import org.point85.core.collector.ProductionHistory;
import org.point85.core.collector.SetupHistory;
import org.point85.core.persistence.PersistencyService;
import org.point85.core.plant.Equipment;
import org.point85.core.plant.EquipmentEventResolver;
import org.point85.core.plant.EquipmentMaterial;
import org.point85.core.plant.Material;
import org.point85.core.script.OeeContext;
import org.point85.core.script.ResolvedEvent;
import org.point85.core.script.ScriptResolver;
import org.point85.core.script.ScriptResolverType;
import org.point85.core.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventCollector {
	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private OeeContext appContext = new OeeContext();

	public EventCollector() {

	}

	public void resolveEvent(Equipment equipment, ScriptResolverType type, Object sourceValue, OffsetDateTime timestamp)
			throws Exception {
		EquipmentEventResolver equipmentResolver = new EquipmentEventResolver();

		// find resolver by type
		List<ScriptResolver> resolvers = equipmentResolver.getResolvers(equipment);

		ScriptResolver configuredResolver = null;
		for (ScriptResolver resolver : resolvers) {
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
		ScriptResolverType type = resolvedEvent.getResolverType();

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
		case PROD_TOTAL:
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

		PersistencyService.instance().persist(history);
	}

	private void saveSetupRecord(ResolvedEvent resolvedItem) throws Exception {
		SetupHistory history = new SetupHistory(resolvedItem);

		PersistencyService.instance().persist(history);
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
				case PROD_TOTAL:
					uom = equipmentMaterial.getInputUOM();
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

		PersistencyService.instance().persist(history);
	}

	protected void onOtherResolution(ResolvedEvent resolvedItem) {
		if (logger.isInfoEnabled()) {
			logger.info("Other ");
		}
	}
}
