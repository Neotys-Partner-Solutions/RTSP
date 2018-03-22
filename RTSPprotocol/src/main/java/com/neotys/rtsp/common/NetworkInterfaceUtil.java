package com.neotys.rtsp.common;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;



public class NetworkInterfaceUtil{

	static class IsMacInBlackList implements Predicate<String> {
		static final Predicate<String> INSTANCE = new IsMacInBlackList();

		/**
		 * Blacklisted mac addresses (includes Virtual Host IPs). Use ':'
		 * separator.
		 */
		static final Iterable<String> BLACKLISTED_MAC_ADDRESSES = ImmutableSet.of("00:00:00:00:00:00",
				"00:00:00:00:00:01", "FF:FF:FF:FF:FF:FF", "00:50:56:C0:00:08", "00:50:56:C0:00:01", "00:53:45:00:00:00",
				"00:01:23:45:67:89", "00:00:00:00:00:e0", "00:00:00:00:00:00:00:E0", "02:00:54:55:4E:01");

		private IsMacInBlackList() {
			super();
		}

		public boolean apply(final String mac) {
			if (!IsMacAddress.INSTANCE.apply(mac)) {
				return false;
			}

			if (mac == null) {
				return false;
			}

			final String normalizedMac = mac.replace('-', ':');
			for (final String blacklisted : BLACKLISTED_MAC_ADDRESSES) {
				if (blacklisted.equalsIgnoreCase(normalizedMac)) {
					return true;
				}
			}

			return false;
		}
	}

	static class IsMacAddress implements Predicate<String> {
		static final Predicate<String> INSTANCE = new IsMacAddress();

		static final int MAC_LENGTH = 17;
		static final int MAC_LENGTH_MS = 23;

		// Example: 08-00-27-00-44-00
		static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile("((([0-9a-fA-F]){1,2}[-:]){5}([0-9a-fA-F]){1,2})");
		// Example: 00-00-00-00-00-00-00-E0
		static final Pattern MAC_ADDRESS_PATTERN_MS = Pattern
				.compile("((([0-9a-fA-F]){1,2}[-:]){7}([0-9a-fA-F]){1,2})");

		private IsMacAddress() {
			super();
		}

		public boolean apply(final String mac) {
			if (Strings.isNullOrEmpty(mac)) {
				return false;
			}

			if (mac.length() != MAC_LENGTH && mac.length() != MAC_LENGTH_MS) {
				return false;
			}

			if (MAC_ADDRESS_PATTERN.matcher(mac).matches()) {
				return true;
			}
			return MAC_ADDRESS_PATTERN_MS.matcher(mac).matches();
		}
	}

	static class IsMac4plusHexaPair implements Predicate<String> {

		private static final int MAX_MAC_BLACKLIST_COUNTER_VALUE = 4;

		static final Predicate<String> INSTANCE = new IsMac4plusHexaPair();

		private IsMac4plusHexaPair() {
			super();
		}

	
		public boolean apply(final String mac) {
			if (!IsMacAddress.INSTANCE.apply(mac)) {
				return false;
			}

			final String separator = String.valueOf(mac.charAt(2));
			final Iterable<String> splits = Splitter.on(separator).split(mac);
			for (final String split : splits) {
				if (StringUtils.countMatches(mac, split) >= MAX_MAC_BLACKLIST_COUNTER_VALUE) {
					return true;
				}
			}
			return false;
		}
	}

	static class NetworkInterfaceToMacAddress implements Function<NetworkInterface, String> {

		static final Function<NetworkInterface, String> INSTANCE = new NetworkInterfaceToMacAddress();
		private static final char SEPARATOR = SystemUtils.IS_OS_WINDOWS ? '-' : ':';

		private NetworkInterfaceToMacAddress() {
			super();
		}

		
		public String apply(final NetworkInterface input) {
			byte[] mac;
			try {
				mac = input.getHardwareAddress();
			} catch (final Exception e) {
				mac = null;
			}
			if (mac == null || mac.length == 0) {
				return null;
			}

			final StringBuilder sb = new StringBuilder(17);
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X", mac[i]));
				if (i < mac.length - 1) {
					sb.append(SEPARATOR);
				}
			}

			return sb.toString().toUpperCase(Locale.getDefault());
		}
	}

	static class IsStringNullOrEmptyPredicate implements Predicate<String> {
		static final Predicate<String> INSTANCE = new IsStringNullOrEmptyPredicate();

		private IsStringNullOrEmptyPredicate() {
		}

		public boolean apply(String input) {
			return input == null || input.length() == 0;
		}
		
		public static String getlocaladress()
	    {
	    	String StrResult=null;
	    	
	    	try
	    	{
	    		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	    		while (interfaces.hasMoreElements())
	    		{
	    			NetworkInterface current = interfaces.nextElement();
	    			
	    			if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
	    			Enumeration<InetAddress> addresses = current.getInetAddresses();
	    			while (addresses.hasMoreElements())
	    			{
	    				InetAddress current_addr = addresses.nextElement();
	    				if (current_addr.isLoopbackAddress()) continue;
	    				if (current_addr instanceof Inet6Address) continue;
	    				StrResult=current_addr.getHostAddress();
	    				return StrResult;
	    			}	
	    		}
	    		return StrResult;
	    	}
	    	catch(SocketException e)
	    	{
	    		e.printStackTrace();
				return null;
	    	}
	    }
		static class StartsWithPredicate implements Predicate<String> {
			private final Iterable<String> prefixes;

			StartsWithPredicate(ImmutableCollection<String> prefixes) {
				this.prefixes = (Iterable) Preconditions.checkNotNull(prefixes);
			}

			public boolean apply(String input) {
				Iterator var2 = this.prefixes.iterator();

				String prefix;
				do {
					if (!var2.hasNext()) {
						return false;
					}

					prefix = (String) var2.next();
				} while (!input.startsWith(prefix));

				return true;
			}
		}

		static final class MoreStrings {
			private MoreStrings() {
				throw new IllegalAccessError();
			}

			public static Predicate<String> startsWith(String... prefixes) {
				return new StartsWithPredicate(ImmutableList.copyOf(prefixes));
			}

			public static Predicate<String> isNullOrEmpty() {
				return IsStringNullOrEmptyPredicate.INSTANCE;
			}
		}

		final static class IsVirtualHostNetworkInterface implements Predicate<NetworkInterface> {

			static final IsVirtualHostNetworkInterface INSTANCE = new IsVirtualHostNetworkInterface();

			/**
			 * Vhost network interfaces.
			 */
			static final String[] VIRTUALISATION_EXCLUDED_INTERFACES = new String[] {
					"VMware Virtual Ethernet Adapter for VMnet", "VirtualBox Host-Only Ethernet Adapter" };

			private IsVirtualHostNetworkInterface() {
				super();
			}

			
			public boolean apply(NetworkInterface iface) {
				if (iface == null) {
					return false;
				}

				final String name = Objects.firstNonNull(iface.getDisplayName(),
						Objects.firstNonNull(iface.getName(), ""));
				return isVirtualInterface(name);
			}

			static boolean isVirtualInterface(final String name) {
				return MoreStrings.startsWith(VIRTUALISATION_EXCLUDED_INTERFACES).apply(name);
			}
		}

		private static List<NetworkInterface> networkInterfacesEnumerationAsList(
				Enumeration<NetworkInterface> networkInterfaces) {
			List<NetworkInterface> ifaces = new ArrayList<NetworkInterface>();
			while (networkInterfaces.hasMoreElements()) {
				ifaces.add(networkInterfaces.nextElement());
			}
			return ifaces;
		}

		public static boolean isMacBlackListed(final String mac) {
			return Strings.isNullOrEmpty(mac) || IsMac4plusHexaPair.INSTANCE.apply(mac)
					|| IsMacInBlackList.INSTANCE.apply(mac);
		}

		public static boolean isMacBlackListed(final NetworkInterface networkInterface) throws SocketException {
			if (networkInterface.isLoopback()) {
				return false;
			}
			return isMacBlackListed(NetworkInterfaceToMacAddress.INSTANCE.apply(networkInterface));
		}

		private static List<NetworkInterface> getAllCurrentNetworkInterfaceFiltred() throws SocketException {
			List<NetworkInterface> ret = new ArrayList<NetworkInterface>(3);
			List<NetworkInterface> networkInterfaces = networkInterfacesEnumerationAsList(
					NetworkInterface.getNetworkInterfaces());

			for (NetworkInterface networkInterface : networkInterfaces) {

				// blacklisted iface are removed
				if (isMacBlackListed(networkInterface)) {
					continue;
				}
				// virtualization black list ?
				if (IsVirtualHostNetworkInterface.INSTANCE.apply(networkInterface)) {
					continue;
				}

				// Mac address is not globally unique.
				if (networkInterface.getHardwareAddress() != null && networkInterface.getHardwareAddress().length > 0
						&& (networkInterface.getHardwareAddress()[0] & 0x02) != 0) {
					continue;
				}
				ret.add(networkInterface);
			}
			return ret;
		}
		
		public static List<String> getAllCurrentNoLoopbackPhysicalHostIPS() throws SocketException {
			List<String> ips = new ArrayList<String>(3);

			for (NetworkInterface networkInterface : getAllCurrentNetworkInterfaceFiltred()) {
				if (networkInterface.isLoopback())
					continue;
				Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (!address.isLinkLocalAddress())
						ips.add(address.getHostAddress());
				}
			}

			return ips;
		}
	}
}
