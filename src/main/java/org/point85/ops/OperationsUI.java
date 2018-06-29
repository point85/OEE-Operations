package org.point85.ops;

import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

// push UI changes from background threads
@Push
// preserve browswer state on refresh
@PreserveOnRefresh
@Theme(ValoTheme.THEME_NAME)
public class OperationsUI extends UI {
	private static final long serialVersionUID = -4803764060046008577L;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OperationsUI.class);

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		// initialize the persistence and data collector services
		AppServices.instance().initialize();

		// main UI form
		if (logger.isInfoEnabled()) {
			logger.info("Launching UI by request from " + vaadinRequest.getRemoteHost() + ":"
					+ vaadinRequest.getRemotePort());
		}

		// the view
		OperationsView operationsView = new OperationsView(this);
		operationsView.setSizeFull();
		operationsView.setMargin(true);
		setContent(operationsView);
	}

	@WebServlet(urlPatterns = { "/*" }, name = "OEEOperationsServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = OperationsUI.class, productionMode = false)
	public static class OEEOperationsServlet extends VaadinServlet {
		private static final long serialVersionUID = 3872491814140753200L;

	}
}
