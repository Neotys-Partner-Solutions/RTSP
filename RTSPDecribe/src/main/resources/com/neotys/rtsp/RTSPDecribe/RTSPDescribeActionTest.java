package com.neotys.rtsp.RTSPDecribe;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RTSPDescribeActionTest {
	@Test
	public void shouldReturnType() {
		final RTSPDescribeAction action = new RTSPDescribeAction();
		assertEquals("RTSPDescribe", action.getType());
	}

}
