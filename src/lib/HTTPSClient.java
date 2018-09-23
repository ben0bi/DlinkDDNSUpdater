package lib; 

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HTTPSClient {
	private String mServer = null;

	private int mPort = 0;

	private String mUsername = null;

	private String mPassword = null;

	public void setCredentials(final String caUsername, final String caPassword) {
		this.mUsername = caUsername;
		this.mPassword = caPassword;
	}

	public void setConnection(final String caServer, final int caPort) {
		System.setProperty("jsse.enableSNIExtension", "false");
		this.mServer = caServer;
		this.mPort = caPort;
	}

	public String doHttpsGETRequest(final String caURL) {
		return this.doHttpsRequest(0, caURL);
	}

	private String doHttpsRequest(final int caMethod, String aURL) {
		if (aURL == null) {
			aURL = "";
		}

		String lMethod = null;

		switch (caMethod) {
		case 0:
			lMethod = "GET";
			break;
		case 1:
			lMethod = "POST";
			break;
		default:
			break;
		}

		Socket lSocket = null;
		BufferedReader lBufferedReader = null;
		StringWriter lStringWriter = null;
		Writer lWriter = null;

		try {
			final SSLContext clSSLContext = SSLContext.getInstance("SSL");
			clSSLContext.init(null, HTTPSClient.getWeakTrustManager(), new SecureRandom());
			final SSLSocketFactory clSSLSocketFactory = clSSLContext.getSocketFactory();
			lSocket = clSSLSocketFactory.createSocket(this.mServer, this.mPort);
			lSocket.setTcpNoDelay(true);

			final List<String> clLimited = new LinkedList<String>();
			for (final String clSuite : ((SSLSocket) lSocket).getEnabledCipherSuites()) {
				if (!clSuite.contains("TLS_DHE_RSA_")) {
					clLimited.add(clSuite);
				}
			}

			((SSLSocket) lSocket).setEnabledCipherSuites(clLimited.toArray(new String[clLimited.size()]));

			lWriter = new OutputStreamWriter(lSocket.getOutputStream());

			String lCredentials = "";
			byte[] lEncodedBytes = null;

			boolean lAuth = false;

			if (((this.mUsername != null) && (this.mUsername != "")) && ((this.mPassword != null) && (this.mPassword != ""))) {
				lCredentials = this.mUsername + ":" + this.mPassword;
				lEncodedBytes = Base64.getEncoder().encode(lCredentials.getBytes());
				lAuth = true;
			}

			final StringBuffer clStringBuffer = new StringBuffer();
			clStringBuffer.append(lMethod);
			clStringBuffer.append(" https://");
			clStringBuffer.append(this.mServer);
			clStringBuffer.append(":");
			clStringBuffer.append(this.mPort);
			clStringBuffer.append(aURL);
			clStringBuffer.append(" HTTP/1.0\r\n");
			clStringBuffer.append("Host: ");
			clStringBuffer.append(this.mServer + "\r\n");

			if (lAuth) {
				clStringBuffer.append("Authorization: Basic ");
				clStringBuffer.append(new String(lEncodedBytes));
				clStringBuffer.append("\r\n");
			}

			clStringBuffer.append("User-Agent: D-Link/DSR-250/1.09B32\r\n");
			clStringBuffer.append("Connection: close\r\n");
			clStringBuffer.append("Proxy-Connection: close\r\n");

			lWriter.write(clStringBuffer.toString());
			lWriter.write("\r\n");
			lWriter.flush();

			lBufferedReader = new BufferedReader(new InputStreamReader(lSocket.getInputStream()));

			int lChar = 0;
			lStringWriter = new StringWriter();

			while ((lChar = lBufferedReader.read()) != -1) {
				lStringWriter.append((char) lChar);
			}

			lStringWriter.flush();
		} catch (final ConnectException caConnectException) {
			System.out.println(caConnectException.getClass().getCanonicalName() + " " + caConnectException.getMessage());
			return null;
		} catch (final Exception caException) {
			caException.printStackTrace();
		} finally {
			try {
				if (lStringWriter != null) {
					lStringWriter.close();
				}

				if (lBufferedReader != null) {
					lBufferedReader.close();
				}

				if (lWriter != null) {
					lWriter.close();
				}

				if (lSocket != null) {
					lSocket.close();
				}
			} catch (final Exception caException) {
				caException.printStackTrace();
			}
		}

		return lStringWriter.toString();
	}

	private static final TrustManager[] getWeakTrustManager() {
		final TrustManager[] clWeakTrustManagerArr = new TrustManager[] { new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {

			}

			@Override
			public void checkServerTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {

			}
		} };

		return clWeakTrustManagerArr;
	}
}