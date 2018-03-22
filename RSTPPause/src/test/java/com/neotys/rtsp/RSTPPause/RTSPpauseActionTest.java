package com.neotys.rtsp.RSTPPause;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RTSPpauseActionTest {
	@Test
	public void shouldReturnType() {
		final RTSPpauseAction action = new RTSPpauseAction();
		assertEquals("RTSPpause", action.getType());
	}

}
