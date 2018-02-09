package org.point85.operations;

import javax.servlet.annotation.WebServlet;

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
@Theme(ValoTheme.THEME_NAME)
public class OEEOperations extends UI {

	private static final long serialVersionUID = -4803764060046008577L;

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		EquipmentForm eventForm = new EquipmentForm();
		eventForm.setSizeFull();
		eventForm.setMargin(true);
		setContent(eventForm);
	}

	@WebServlet(urlPatterns = "/*", name = "OEEOperationsServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = OEEOperations.class, productionMode = false)
	public static class OEEOperationsServlet extends VaadinServlet {
		private static final long serialVersionUID = 3872491814140753200L;
	}
}
