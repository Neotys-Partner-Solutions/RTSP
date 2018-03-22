package com.neotys.rtsp.RTSTPOption;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.Context;


public class RtspOptionActionTest {
	@Test
	public void shouldReturnType() {
		final RtspOptionAction action = new RtspOptionAction();
		assertEquals("RtspOption", action.getType());
	}
	/*@Test
	public void testStream()
	{	
		String myStreamURL="rtsp://media.smart-streaming.com/mytest/mp4:sample.mp4";
		RtspOptionActionEngine testrtsp = new RtspOptionActionEngine();
		
		List<ActionParameter> parameters = new ArrayList<>();
		parameters.add(new ActionParameter("URL", myStreamURL));
		parameters.add(new ActionParameter("NeoLoadAPIHost", "localhost"));
		parameters.add(new ActionParameter("NeoLoadAPIport", "7400"));
		parameters.add(new ActionParameter("NeoLoadLocation", "Gemenos"));
		testrtsp.execute(Mockito.mock(Context.class), parameters);
		
	}*/
}
