package com.neotys.rtsp.RTSPstop;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StopStreamActionTest {
	@Test
	public void shouldReturnType() {
		final StopStreamAction action = new StopStreamAction();
		assertEquals("StopStream", action.getType());
	}

}
