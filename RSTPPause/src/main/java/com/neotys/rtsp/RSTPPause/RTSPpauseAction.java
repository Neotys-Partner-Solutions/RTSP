package com.neotys.rtsp.RSTPPause;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Icon;

import com.google.common.base.Optional;
import com.neotys.extensions.action.Action;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;

public final class RTSPpauseAction implements Action{
	private static final String BUNDLE_NAME = "com.neotys.rtsp.RSTPPause.bundle";
	private static final String DISPLAY_NAME = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayName");
	private static final String DISPLAY_PATH = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayPath");

	@Override
	public String getType() {
		return "RTSPpause";
	}

	@Override
	public List<ActionParameter> getDefaultActionParameters() {
		final List<ActionParameter> parameters = new ArrayList<ActionParameter>();
		// TODO Add default parameters.
		return parameters;
	}

	@Override
	public Class<? extends ActionEngine> getEngineClass() {
		return RTSPpauseActionEngine.class;
	}

	@Override
	public Icon getIcon() {
		// TODO Add an icon
		return null;
	}

	@Override
	public String getDescription() {
		final StringBuilder description = new StringBuilder();
		// TODO Add description
		description.append("RTSPpause Action will Pause the current RTSP Stream.\n");
		description.append("RTSPpause requires an open stream ( RTSPPlay action needs to created before the RSPpause)\n");

		return description.toString();
	}

	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Override
	public String getDisplayPath() {
		return DISPLAY_PATH;
	}

	@Override
	public Optional<String> getMinimumNeoLoadVersion() {
		return Optional.of("5.1");
	}

	@Override
	public Optional<String> getMaximumNeoLoadVersion() {
		return Optional.absent();
	}
}
