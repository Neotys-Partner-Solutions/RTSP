package com.neotys.rtsp.RTSTPOption;

import java.util.List;

import com.google.common.base.Strings;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;
import com.neotys.rtsp.common.RTSPControl;


public final class RtspOptionActionEngine implements ActionEngine {
	String URL;
	String NeoLoadAPIHost;
	String NeoLoadAPIport;
	String NeoLoadKeyAPI;
	String NeoLoadLocation;
	RTSPControl rtspConrol;
	
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		final SampleResult sampleResult = new SampleResult();
		final StringBuilder requestBuilder = new StringBuilder();
		final StringBuilder responseBuilder = new StringBuilder();

		
		for(ActionParameter parameter:parameters) {
			switch(parameter.getName()) {
			case RtspOptionAction.URL:
				URL = parameter.getValue();
				break;
			case RtspOptionAction.NeoLoadAPIHost:
				NeoLoadAPIHost = parameter.getValue();
				break;
			case RtspOptionAction.NeoLoadAPIport:
				NeoLoadAPIport = parameter.getValue();
				break;
			case RtspOptionAction.NeoLoadKeyAPI:
				NeoLoadKeyAPI = parameter.getValue();
				break;
			case RtspOptionAction.NeoLoadLocation:
				NeoLoadLocation = parameter.getValue();
				break;
				
			}
		}
		if (Strings.isNullOrEmpty(URL)) {
			return getErrorResult(context, sampleResult, "Invalid argument: URL cannot be null "
					+ RtspOptionAction.URL + ".", null);
		}
		if (Strings.isNullOrEmpty(NeoLoadAPIHost)) {
			return getErrorResult(context, sampleResult, "Invalid argument: NeoLoadAPIHost cannot be null "
					+ RtspOptionAction.NeoLoadAPIHost + ".", null);
		}
		if (Strings.isNullOrEmpty(NeoLoadAPIport)) {
			return getErrorResult(context, sampleResult, "Invalid argument: NeoLoadAPIport cannot be null "
					+ RtspOptionAction.NeoLoadAPIport + ".", null);
		}
		
		if (Strings.isNullOrEmpty(NeoLoadLocation)) {
			return getErrorResult(context, sampleResult, "Invalid argument: NeoLoadLocation cannot be null "
					+ RtspOptionAction.NeoLoadLocation + ".", null);
		}
		
		try
		{
			sampleResult.sampleStart();
	
			rtspConrol= new RTSPControl(URL,NeoLoadAPIHost,NeoLoadAPIport,NeoLoadLocation,NeoLoadKeyAPI,"NeoLoad"+context.getCurrentVirtualUser().getId());
		
			
			
			appendLineToStringBuilder(responseBuilder, rtspConrol.RTSPOptions());
			
			
			appendLineToStringBuilder(responseBuilder, rtspConrol.getVideoFilename());
			// TODO perform execution.
	
			sampleResult.sampleEnd();
			context.getCurrentVirtualUser().put("RTSPStream",rtspConrol);
		}
		catch(Exception e)
		{
			return getErrorResult(context, sampleResult, "RTSP Error  "
					+ RtspOptionAction.URL + ".", e);
		}
		sampleResult.setRequestContent(requestBuilder.toString());
		sampleResult.setResponseContent(responseBuilder.toString());
		return sampleResult;
	}

	private void appendLineToStringBuilder(final StringBuilder sb, final String line){
		sb.append(line).append("\n");
	}

	/**
	 * This method allows to easily create an error result and log exception.
	 */
	private static SampleResult getErrorResult(final Context context, final SampleResult result, final String errorMessage, final Exception exception) {
		result.setError(true);
		result.setStatusCode("NL-RtspOption_ERROR");
		result.setResponseContent(errorMessage);
		if(exception != null){
			context.getLogger().error(errorMessage, exception);
		} else{
			context.getLogger().error(errorMessage);
		}
		return result;
	}

	@Override
	public void stopExecute() {
		// TODO add code executed when the test have to stop.
	}

}
