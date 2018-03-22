package com.neotys.rtsp.common;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.awt.event.*;

import javax.media.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

import com.neotys.rest.dataexchange.client.DataExchangeAPIClient;
import com.neotys.rest.dataexchange.model.EntryBuilder;
import com.neotys.rest.error.NeotysAPIException;
import com.sun.media.rtp.RTPSessionMgr;
import com.sun.media.rtsp.RtspManager;

import javax.media.protocol.*;
import javax.media.format.*;
import javax.media.control.BufferControl;

public class RTPReceiver implements ReceiveStreamListener, SessionListener,
ControllerListener {

	    private String ipAddress;
	    private int portStr;
	    private int LocalPort;
	    private RTPManager mgr = null;
	    private boolean dataReceived = false;
	    private Object dataSync = new Object();
	    private String LocalIP=null;
	    private Player p;
	    private  MediaLocator src;
	    RTPSTATS stat;
		
	    final static int MAX_RETRY=15;
	    
		public RTPReceiver(String ipAddress, int rTPServerAudioPort,int rTPClientAudioPort,String Ip,RTPSTATS stats) {
			this.ipAddress = ipAddress;
			this.portStr = rTPServerAudioPort;
			this.LocalPort =rTPClientAudioPort;
			this.LocalIP=Ip;
			this.stat = stats;
	    }
		
	    public RTPReceiver(int rTPClientAudioPort) 
	    	{
	    		this.LocalPort =rTPClientAudioPort;
	    	}
	    
	    public void CreatePlayer(String URl)
	    {
	    	src =new MediaLocator(URl);
	    	 try {
	    	     p = Manager.createPlayer(src);
	    	     p.addControllerListener(this);
	    	     p.realize();
	    	 } catch(Exception e) {
	    	     e.printStackTrace();
	    	 
	    	 }
	    }
	    
	    	
	    	
	    protected void initialize() throws Exception{
	    	InetAddress ipAddr;
		    
	    		int retry=0;
			    int port = (new Integer(portStr)).intValue();
			    int locport=(new Integer(LocalPort)).intValue();
			    int ttl = 1;
			    SessionAddress localAddr = new SessionAddress();
			    SessionAddress destAddr;
	
			
				mgr = (RTPManager) RTPManager.newInstance();
				mgr.addSessionListener(this);
				mgr.addReceiveStreamListener(this);
			
				
				ipAddr = InetAddress.getByName(ipAddress);
				while((NetworkUtils.isPortInUse(locport,LocalIP)&&NetworkUtils.isPortInUse(locport+1,LocalIP))||retry<MAX_RETRY)
				{
					Thread.sleep(1000);
					retry++;
				}
				
				if(!NetworkUtils.isPortInUse(locport,LocalIP)&&!NetworkUtils.isPortInUse(locport+1,LocalIP))
				{
					if( ipAddr.isMulticastAddress()) {
					    // local and remote address pairs are identical:
					    localAddr= new SessionAddress( InetAddress.getByName(LocalIP), locport, ttl);
					    destAddr = new SessionAddress( ipAddr, port, ttl);
					} else {
						
					    localAddr= new SessionAddress( InetAddress.getByName(LocalIP), locport);
			            destAddr = new SessionAddress( ipAddr, port);
					}
				
					System.out.println("Opening RTP session for: addr: " + ipAddress + " port: " + port + " ttl: " + ttl);
				
				
					mgr.initialize(localAddr);
	
					// can try out some other buffer size to see if you can get better smoothness.
					BufferControl bc = (BufferControl)mgr.getControl("javax.media.control.BufferControl");
					if (bc != null)
					    bc.setBufferLength(850);
		
			    	mgr.addTarget(destAddr);  //can have more sessions than just this one if you want
				}
	

	    }


	     /**
	     * Close the players and the session managers.
	     */
	    protected void close() {

			// close the RTP session.
			if (mgr != null) {
				mgr.removeTargets( "Closing session from VideoReceive");
				mgr.dispose();
				mgr = null;
			}
	    }


	    /**
	     * SessionListener.
	     */
	    public synchronized void update(SessionEvent evt) {
			if (evt instanceof NewParticipantEvent) {
				Participant part = ((NewParticipantEvent)evt).getParticipant();
				System.out.println("Session Listener - A new participant has just joined: " + part.getCNAME());
			}
			stat.addStat(mgr.getGlobalReceptionStats());
	    }


	    /**
	     * ReceiveStreamListener
	     */
	    public synchronized void update( ReceiveStreamEvent evt) {

			RTPManager mgr = (RTPManager)evt.getSource();
			Participant participant = evt.getParticipant();	// could be null.
			ReceiveStream stream = evt.getReceiveStream();  // could be null.
		
			stat.addStat(mgr.getGlobalReceptionStats());
			
			if (evt instanceof RemotePayloadChangeEvent) {
				 if(p!=null){
	                    System.out.println("$$$::::$$$$$$$$ stoping and closing player....$$$::::$$$$$$$$ ");
	                    p.stop();
	                    p.removeControllerListener(this);
	                    p.close();                    
	               }

	               
	               DataSource ds=stream.getDataSource();
	                 try{
	               p=Manager.createPlayer(ds);          
	                    }
	                  catch(Exception e){
	               System.out.println("could not create player");
	               System.out.println(e);
	               System.exit(0);
	                  }     
	               p.addControllerListener(this);
	               p.realize();
			}

			else if (evt instanceof NewReceiveStreamEvent) {

				try {
					stream = ((NewReceiveStreamEvent)evt).getReceiveStream();
					DataSource ds = stream.getDataSource();
					
					// Find out the formats.
					RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
				
					if (ctl != null){
						System.out.println("ReceiveStream Listener - Received new RTP stream: " + ctl.getFormat());
					} else
						System.out.println("ReceiveStream Listener - Received new RTP stream");

					if (participant == null)
						System.out.println("      The sender of this stream had yet to be identified.");
					else {
						System.out.println("      The stream comes from: " + participant.getCNAME());
					}
				
					// create a player by passing datasource to the Media Manager
					System.out.println("Creating player...");
					 p = javax.media.Manager.createPlayer(ds);
					if (p == null)
						return;

					System.out.println("adding Listener...");
					p.addControllerListener(this);
					System.out.println("Realizing player...");
					p.realize();
							

					System.out.println("notifying initialize()...");
					// Notify initialize() that a new stream had arrived.
					synchronized (dataSync) {
						dataReceived = true;
						dataSync.notifyAll();
					}

				} catch (Exception e) {
					System.err.println("NewReceiveStreamEvent exception " + e.toString());
					return;
				}
			}

			else if (evt instanceof StreamMappedEvent) {
			
				
				 if (stream != null && stream.getDataSource() != null) {
					DataSource ds = stream.getDataSource();
					// Find out the formats.
					RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
					System.out.println("ReceiveStream Listener - The previously unidentified stream ");
					if (ctl != null)
						System.out.println("      " + ctl.getFormat());
					System.out.println("      had now been identified as sent by: " + participant.getCNAME());
				 }
				
			}
			
			else if (evt instanceof ByeEvent) {

				 System.out.println("ReceiveStream Listener - Got \"bye\" from: " + participant.getCNAME());
				 
			}

	    }


	    /**
	     * ControllerListener for the Player.
	     */
	    public synchronized void controllerUpdate(ControllerEvent ce) {

			Player p = (Player)ce.getSourceController();
			stat.addStat(mgr.getGlobalReceptionStats());
			if (p == null)
				return;
		
			// Get this when the internal players are realized.
			if (ce instanceof RealizeCompleteEvent) {
				
				p.prefetch();
			}
			if (ce instanceof PrefetchCompleteEvent) {
				
				p.start();
			}
			if (ce instanceof ControllerErrorEvent) {
				p.removeControllerListener(this);
				
			}

			
			
	    }

	 
	   



	    void Fatal (String s) {
		// Applications will make various choices about what
		// to do here. We print a message
			System.err.println("FATAL ERROR: " + s);
	    }

	  

	    static void prUsage() {
			System.err.println("Usage: VideoReceive <address> <port>");
			//System.exit(-1);
	    }
	}

