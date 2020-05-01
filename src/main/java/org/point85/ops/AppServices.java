package org.point85.ops;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.log4j.PropertyConfigurator;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorService;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.ops.OperationsUI.OEEOperationsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppServices {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(AppServices.class);

	// collection flags
	private static final String ALL_COLLECTORS = "ALL";
	private static final String NO_COLLECTORS = "NONE";

	// event data collector
	private final CollectorService collectorService;

	// singleton
	private static AppServices services = new AppServices();

	private static boolean isInitialized = false;

	private AppServices() {
		// collector server
		collectorService = new CollectorService();
	}

	public static AppServices instance() {
		return services;
	}

	synchronized boolean initialize() {
		if (isInitialized) {
			return isInitialized;
		}

		// see web.xml for JDBC connection properties
		ServletConfig config = OEEOperationsServlet.getCurrent().getServletConfig();
		String jdbcConn = config.getInitParameter("jdbcConn");
		String userName = config.getInitParameter("userName");
		String password = config.getInitParameter("password");

		if (password != null && password.trim().length() == 0) {
			password = null;
		}

		// flag to run the collection service
		String collectorName = config.getInitParameter("collectorName");
		boolean collectData = collectorName != null && collectorName.trim().equalsIgnoreCase(NO_COLLECTORS) ? false
				: true;

		// configure log4j
		ServletContext context = OEEOperationsServlet.getCurrent().getServletContext();
		String realPath = context.getRealPath("");
		String log4jProps = realPath + "/log4j.properties";
		PropertyConfigurator.configure(log4jProps);

		if (logger.isInfoEnabled()) {
			logger.info("Initializing persistence service for connection " + jdbcConn + " and user " + userName);
		}

		// create the EMF on background thread
		PersistenceService.instance().initialize(jdbcConn, userName, password);

		// start the collector
		if (collectData) {
			if (logger.isInfoEnabled()) {
				logger.info("Starting collector for " + collectorName);
			}

			try {
				// start the data collection
				if (!collectorName.equalsIgnoreCase(ALL_COLLECTORS)) {
					collectorService.setCollectorName(collectorName);
				}

				startupCollector();
			} catch (Exception e) {
				logger.error(DomainUtils.formatException(e));
				try {
					shutdownCollector();
				} catch (Exception any) {
					logger.error(DomainUtils.formatException(any));
				}
			} finally {
				isInitialized = true;
			}
		} else {
			isInitialized = true;
		}
		return isInitialized;
	}

	private void startupCollector() throws Exception {
		// startup server
		collectorService.startup();
	}

	void shutdownCollector() throws Exception {
		if (collectorService != null) {
			collectorService.shutdown();
		}
	}

	synchronized void recordEvent(OeeEvent event) throws Exception {
		collectorService.recordResolution(event);
	}
	
	CollectorService getCollectorService() {
		return collectorService;
	}
}
