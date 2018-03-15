package org.point85.ops;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;

import org.apache.log4j.PropertyConfigurator;
import org.point85.domain.persistence.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This UI is the application entry point. A UI may either represent a browser
 * window (or tab) or some part of an HTML page where a Vaadin application is
 * embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is
 * intended to be overridden to add component to the user interface and
 * initialize non-component functionality.
 */
// push UI changes from background threads
@Push
@Theme(ValoTheme.THEME_NAME)
public class OeeOpsUI extends UI {

	private static final long serialVersionUID = -4803764060046008577L;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeOpsUI.class);

	private EquipmentForm eventForm;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		// DESIGNER jdbc:sqlserver://localhost:1433;databaseName=OEE Point85 Point85
		// log4j.properties
		ServletContext context = OEEOperationsServlet.getCurrent().getServletContext();
		String path = context.getContextPath();
		String realPath = context.getRealPath("");

		ServletConfig config = OEEOperationsServlet.getCurrent().getServletConfig();

		String log4jProps = config.getInitParameter("log4jProps");
		String jdbcConn = config.getInitParameter("jdbcConn");
		String userName = config.getInitParameter("userName");
		String password = config.getInitParameter("password");

		// configure log4j
		log4jProps = realPath + "/log4j.properties";
		PropertyConfigurator.configure(log4jProps);

		// create the EMF
		if (logger.isInfoEnabled()) {
			logger.info("Initializing persistence service.");
		}
		PersistenceService.instance().initialize(jdbcConn, userName, password);

		eventForm = new EquipmentForm(this);

		try {
			eventForm.setSizeFull();
			eventForm.setMargin(true);
			setContent(eventForm);

			eventForm.startupCollector();

		} catch (Exception e) {
			e.printStackTrace();
			eventForm.getCollectorServer().onException("Startup failed. ", e);
			eventForm.getCollectorServer().shutdown();
		}
	}

	@WebServlet(urlPatterns = "/*", name = "OEEOperationsServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = OeeOpsUI.class, productionMode = false)
	public static class OEEOperationsServlet extends VaadinServlet {
		private static final long serialVersionUID = 3872491814140753200L;

	}
}
