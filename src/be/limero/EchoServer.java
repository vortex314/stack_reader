package be.limero;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class EchoServer {
	private static final Logger log = Logger.getLogger(EchoServer.class
			.getName());

	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%1$tY-%1$tm-%1$td|%1$tH:%1$tM:%1$tS,%1$tL|%4$s|%2$s %5$s%6$s%n");
//		LogFormatter.Init();
		try {
			ServerSocket s = new ServerSocket(8008);
			while (true) {
				Socket incoming = s.accept();
				log.info("Connection accepted "+incoming.getRemoteSocketAddress());
				BufferedReader in = new BufferedReader(new InputStreamReader(
						incoming.getInputStream()));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(
						incoming.getOutputStream()));

				out.println("Hello! ....");
				out.println("Enter BYE to exit.");
				out.flush();
				while (true) {
					String str = in.readLine();
					if (str == null) {
						break; // client closed connection
					} else {
						log.info("<< "+str);
						out.println("Echo: " + str);
						out.flush();
						if (str.trim().equals("BYE")) {
							log.info("Disconnect");
							break;
						}
					}
				}
				incoming.close();
			}
		} catch (Exception e) {
		}
	}
}