import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Server {
	private static ServerSocket server;
	final static int PORT = 8080;
	final static String ROOT = "www";

	public static void main(String[] args) {
		try {
			System.out.println("Server started at: " + InetAddress.getLocalHost().getHostName() + ":" + PORT);
			server = new ServerSocket(PORT);
			while(true) {
				Socket client = server.accept();
				new HttpProcessor(client);
			}
		} catch (UnknownHostException ue) {
			ue.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}