package org.point85.ops;

import org.point85.domain.i18n.Localizer;

public class WebOperatorLocalizer extends Localizer {
	// name of resource bundle with translatable strings for text
	private static final String LANG_BUNDLE_NAME = "i18n.WebOperatorLang";

	// name of resource bundle with translatable strings for exception messages
	private static final String ERROR_BUNDLE_NAME = "i18n.WebOperatorError";

	// Singleton
	private static WebOperatorLocalizer localizer;

	private WebOperatorLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);
	}

	public static WebOperatorLocalizer instance() {
		if (localizer == null) {
			localizer = new WebOperatorLocalizer();
		}
		return localizer;
	}

	@Override
	public String getLangBundleName() {
		return LANG_BUNDLE_NAME;
	}

	@Override
	public String getErrorBundleName() {
		return ERROR_BUNDLE_NAME;
	}
}
