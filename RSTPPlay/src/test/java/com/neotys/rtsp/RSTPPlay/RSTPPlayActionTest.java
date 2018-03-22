package com.neotys.rtsp.RSTPPlay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RSTPPlayActionTest {
	@Test
	public void shouldReturnType() {
		final RSTPPlayAction action = new RSTPPlayAction();
		assertEquals("RSTPPlay", action.getType());
	}

}
