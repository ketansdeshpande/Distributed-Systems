import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.io.FileReader;

public class HttpProcessor extends Thread {
	Socket client;
	private static HashMap<String,Integer> resourceAccessCount = new HashMap<String,Integer>();

	public HttpProcessor(Socket clientIn) {
		client = clientIn;
		start();
	}

	public void run() {
		String httpResponse, cmdOut, line, filename, mimeFile, mimeType, extension;
		File file = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			while (true) {
				InputStreamReader inputStreamReader = new InputStreamReader(client.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				while((line = bufferedReader.readLine()) != null) {
					try {
						if(line.startsWith("GET")) {
							filename = line.split(" ")[1].substring(1);
							if(!new File(Server.ROOT).isDirectory()) throw new FileNotFoundException();
							file = new File(Server.ROOT + File.separator + filename);
							if (file.isFile()) {
								if(resourceAccessCount.get(filename) == null) {
									resourceAccessCount.put(filename, 1);
								} else {
									resourceAccessCount.put(filename, resourceAccessCount.get(filename) + 1);
								}
								cmdOut = "/" + file.getName() + "|";
								cmdOut += (((InetSocketAddress) client.getRemoteSocketAddress()).getAddress()).toString().replace("/","") + "|";
								cmdOut += client.getPort() + "|" + resourceAccessCount.get(filename);
								System.out.println(cmdOut);

								BufferedReader reader = new BufferedReader(new FileReader(new File("/etc/mime.types")));
								mimeFile = "";
								mimeType = "application/octet-stream";

								extension = ( filename.split("\\.").length > 1 ) ? filename.split("\\.")[1] : "";
								if(!extension.isBlank()) {
									while((mimeFile = reader.readLine()) != null ) {
										if(mimeFile.toLowerCase().contains(extension.toLowerCase())) {
											mimeType = mimeFile.split("[\\s\\t]+")[0];
											if(mimeType.equals("text/x-server-parsed-html")) mimeType = "text/html";
										}
									}
								}

								httpResponse = "HTTP/1.0 200 OK\n";
								httpResponse += "Date: " + simpleDateFormat.format(new Date()) + "\n";
								httpResponse += "Server: HTTP server/0.1\n";
								httpResponse += "Last-Modified: " + simpleDateFormat.format(file.lastModified()) + "\n";
								httpResponse += "Content-Type: " + mimeType + "\n";
								httpResponse += "Content-Length: " + (file.length()) + "\n\n";
								client.getOutputStream().write(httpResponse.getBytes());
								Files.copy(Paths.get(file.toString()), client.getOutputStream());
							} else {
								throw new FileNotFoundException();
							}
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						httpResponse = "HTTP/1.0 404 Not Found\n";
						httpResponse += "Date: " + simpleDateFormat.format(new Date()) + "\n";
						httpResponse += "Content-Type: text/html\r\n\r\n";
						httpResponse += "\n";
						client.getOutputStream().write(httpResponse.getBytes());
						File errorFile = new File("404.html");
						Files.copy(Paths.get(errorFile.toString()), client.getOutputStream());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				client.close();
				System.exit(0);
			} catch (IOException e1) {
				System.err.println("Error Closing socket Connection.");
				System.exit(0);
			}
			System.err.println("Connection closing...");
		}		
	}
}