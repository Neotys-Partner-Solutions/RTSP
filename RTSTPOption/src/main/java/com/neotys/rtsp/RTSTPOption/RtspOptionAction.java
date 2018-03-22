package com.neotys.rtsp.RTSTPOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Icon;

import com.google.common.base.Optional;
import com.neotys.extensions.action.Action;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;

public final class RtspOptionAction implements Action{
	static final String URL="URL";
	static final String NeoLoadAPIHost="NeoLoadAPIHost";
	static final String NeoLoadAPIport="NeoLoadAPIport";
	static final String NeoLoadKeyAPI="NeoLoadKeyAPI";
	static final String NeoLoadLocation="NeoLoadLocation";
	
	private static final String BUNDLE_NAME = "com.neotys.rtsp.RTSTPOption.bundle";
	private static final String DISPLAY_NAME = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayName");
	private static final String DISPLAY_PATH = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayPath");

	@Override
	public String getType() {
		return "RtspOption";
	}

	@Override
	public List<ActionParameter> getDefaultActionParameters() {
		final List<ActionParameter> parameters = new ArrayList<ActionParameter>();
		parameters.add(new ActionParameter("URL","rtsp://localhost/mediafile"));
		parameters.add(new ActionParameter("NeoLoadAPIHost","localhost"));
		parameters.add(new ActionParameter("NeoLoadAPIport","7400"));
		parameters.add(new ActionParameter("NeoLoadKeyAPI",""));
		parameters.add(new ActionParameter("NeoLoadLocation","Gemenos"));
		return parameters;
	}

	@Override
	public Class<? extends ActionEngine> getEngineClass() {
		return RtspOptionActionEngine.class;
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
		description.append("RtspOption action is Sendin the Option RTSP request to the server.\n")
		.append("The parameters are : \n")
		.append("URL : RTSP url of the media\n")
		.append("NeoLoadAPIHost : IP or Host of the NeoLaod controller\n")
		.append("NeoLoadAPIport : Port of the NeoLoad DataExchange API\n")
		.append("NeoLoadKeyAPI : Neoload DataExchange API key\n")
		.append("NeoLoadLocation : Location of the current user\n");
		
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
