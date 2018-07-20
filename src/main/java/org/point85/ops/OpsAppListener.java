package org.point85.ops;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class OpsAppListener implements ServletContextListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(OpsAppListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		if (logger.isInfoEnabled()) {
			logger.info("Operations app is starting. ");
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (logger.isInfoEnabled()) {
			logger.info("Operations app is shutting down.");
		}

		try {
			AppServices.instance().shutdownCollector();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}
