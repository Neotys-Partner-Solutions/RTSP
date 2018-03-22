package com.neotys.rtsp.RSTPSetup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RTSPSetupActionTest {
	@Test
	public void shouldReturnType() {
		final RTSPSetupAction action = new RTSPSetupAction();
		assertEquals("RTSPSetup", action.getType());
	}

}
