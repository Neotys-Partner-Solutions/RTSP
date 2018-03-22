package com.neotys.rtsp.RSTPSetup;

import java.util.List;



import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;
import com.neotys.rtsp.common.RTSPControl;

public final class RTSPSetupActionEngine implements ActionEngine {
	RTSPControl player;
	
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		final SampleResult sampleResult = new SampleResult();
		final StringBuilder requestBuilder = new StringBuilder();
		final StringBuilder responseBuilder = new StringBuilder();

		try
		{
			player= (RTSPControl)context.getCurrentVirtualUser().get("RTSPStream");
		}
		catch(Exception e)
		{
			return getErrorResult(context, sampleResult, "You need to send an OPtion, decribe before the Setup of RSTP stream "
					, e);
			
		}	
		try
		{
			sampleResult.sampleStart();
			
	
			appendLineToStringBuilder(responseBuilder, player.RTSPSetup());
			// TODO perform execution.
	
			sampleResult.sampleEnd();
		}
		catch(Exception e)
		{
			return getErrorResult(context, sampleResult, "Technical Error "
					, e);
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
		result.setStatusCode("NL-RTSPSetup_ERROR");
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
