package app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import lib.HTTPSClient;

public class App {
	private final boolean cmLog;
	private final String cmServer;
	private final String cmUsername;
	private final String cmPassword;
	private final String cmHostname;
	private final int cmPort;
	private final int cmRefreshTime;

	private final HTTPSClient cmhttpsClientDLinkDDNS;
	private final HTTPSClient cmhttpsClientExternalIP;

	private String mIPAddress;

	public static void main(final String[] caArgsArr) throws Exception {
		new App();
	}

	private App() throws Exception {
		// Datei 'config.properties' mit den Konfigurationsdaten laden
		final File clFile = new File("config.properties");
		final FileInputStream clFileInputStream = new FileInputStream(clFile);
		final Properties clProperties = new Properties();
		clProperties.load(clFileInputStream);
		this.cmServer = clProperties.getProperty("server");
		this.cmPort = Integer.parseInt(clProperties.getProperty("port"));
		this.cmUsername = clProperties.getProperty("username");
		this.cmPassword = clProperties.getProperty("password");
		this.cmHostname = clProperties.getProperty("hostName");
		this.cmLog = Boolean.parseBoolean(clProperties.getProperty("log"));
		this.cmRefreshTime = Integer.parseInt(clProperties.getProperty("refresh"));
		clFileInputStream.close();

		// HTTPS-Client für dlinkddns.com laden
		this.cmhttpsClientDLinkDDNS = new HTTPSClient();
		this.cmhttpsClientDLinkDDNS.setConnection(this.cmServer, this.cmPort);
		this.cmhttpsClientDLinkDDNS.setCredentials(this.cmUsername, this.cmPassword);

		// HTTPS-Client für externe IP-Adressabfrage laden
		this.cmhttpsClientExternalIP = new HTTPSClient();
		this.cmhttpsClientExternalIP.setConnection("ident.me", 443);

		while (true) {
			final String clExternalIP = this.getMyExternalIP();
			if (this.mIPAddress == null) {
				this.mIPAddress = clExternalIP;
				if (App.isIPAddressValid(this.mIPAddress)) {
					this.refresh();
					System.out.println("IP-Adresse wurde aktualisiert: " + this.mIPAddress);
				} else {
					System.out.println("Ungültige IP-Adresse wurde nicht aktualisiert: " + this.mIPAddress);
				}
			} else {
				if (App.isIPAddressValid(clExternalIP)) {
					if (this.mIPAddress.intern() == clExternalIP.intern()) {
						System.out.println("Die IP-Adresse hat sich nicht geändert: " + this.mIPAddress);
					} else {
						this.mIPAddress = clExternalIP;
						this.refresh();
						System.out.println("Die IP-Adresse hat sich geändert: " + this.mIPAddress + " (Wurde aktualisiert.)");
					}
				} else {
					System.out.println("Ungültige IP-Adresse wurde nicht aktualisiert: " + clExternalIP);
				}

			}
			Thread.sleep(this.cmRefreshTime);
		}
	}

	private String refresh() {
		String lResponse = this.cmhttpsClientDLinkDDNS.doHttpsGETRequest("/nic/update?system=dyndns&hostname=" + this.cmHostname + "&myip=" + this.mIPAddress + "&wildcard=OFF");

		if (lResponse != null) {
			lResponse = lResponse.trim();
			lResponse = lResponse.substring(lResponse.lastIndexOf("\n"));
			lResponse = lResponse.replace("\n", "").replace("\r", "");

			if (this.cmLog) {
				System.out.println(lResponse);
			}
		}

		return lResponse;
	}

	private String getMyExternalIP() {
		String lIPAddress = this.cmhttpsClientExternalIP.doHttpsGETRequest(null);

		if (lIPAddress != null) {
			lIPAddress = lIPAddress.trim();
			lIPAddress = lIPAddress.substring(lIPAddress.lastIndexOf("\n"));
			lIPAddress = lIPAddress.replace("\n", "").replace("\r", "");
		}

		return lIPAddress;
	}

	private static boolean isIPAddressValid(final String ip) {
		if ((ip == null) || (ip.length() < 7) || (ip.length() > 15)) {
			return false;
		}

		try {
			int x = 0;
			int y = ip.indexOf('.');

			if ((y == -1) || (ip.charAt(x) == '-') || (Integer.parseInt(ip.substring(x, y)) > 255)) {
				return false;
			}

			x = ip.indexOf('.', ++y);
			if ((x == -1) || (ip.charAt(y) == '-') || (Integer.parseInt(ip.substring(y, x)) > 255)) {
				return false;
			}

			y = ip.indexOf('.', ++x);
			return !((y == -1) || (ip.charAt(x) == '-') || (Integer.parseInt(ip.substring(x, y)) > 255) || (ip.charAt(++y) == '-') || (Integer.parseInt(ip.substring(y, ip.length())) > 255) || (ip.charAt(ip.length() - 1) == '.'));

		} catch (final NumberFormatException e) {
			return false;
		}
	}
}