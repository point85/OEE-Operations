package org.point85.ops;

import org.point85.core.collector.CollectorServer;

public class WebCollector {
	private CollectorServer collector;

	public WebCollector() {
		// create the collector server
		collector = new CollectorServer();

		try {
			// start server
			collector.startup();
		} catch (Exception e) {
			collector.onException("Startup failed. ", e);
		} finally {
			try {
				collector.shutdown();
			} catch (Exception ex) {
				collector.onException("Shutdown failed. ", ex);
			}
		}
	}
}
