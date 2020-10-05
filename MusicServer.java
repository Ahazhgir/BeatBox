import java.io.*;
import java.net.*;
import java.util.*;

public class MusicServer {
	ArrayList<ObjectOutputStream> clientOutputStream;

	public static void main(String[] args) {
		new MusicServer().go();

	}
	
	public class ClientHandler implements Runnable {
		ObjectInputStream in;
		Socket clientSocket;
		
		public ClientHandler(Socket socket) {
			try {
				clientSocket = socket;
				in = new ObjectInputStream(clientSocket.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			Object o2 = null;
			Object o1 = null;
			try {
				while((o1 = in.readObject()) != null) {
					o2 = in.readObject();
					
					System.out.println("Прочитать два объекта");
					tellEveryone(o1, o2);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	public void go() {
		clientOutputStream = new ArrayList<>();
		
		try {
			ServerSocket server = new ServerSocket(4243);
			
			while(true) {
				Socket clientSocket = server.accept();
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStream.add(out);
				
				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
				
				System.out.println("есть связь");
			}
		} catch(Exception e ) {
			e.printStackTrace();
		}
	}
	
	public void tellEveryone(Object one, Object two) {
		Iterator it = clientOutputStream.iterator();
		while(it.hasNext()) {
			try {
				ObjectOutputStream out = (ObjectOutputStream) it.next();
				out.writeObject(one);
				out.writeObject(two);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
