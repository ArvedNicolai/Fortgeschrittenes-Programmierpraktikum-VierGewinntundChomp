import java.net.*;
import java.io.*;

public class ChatServer implements Runnable
{
    private ChatServerThread clients[] = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread thread = null;
    private int clientCount = 0;

    public ChatServer(int port)
    {
        try
        {
            System.out.println("Binding to port " + port + ", please wait  ...");
            server = new ServerSocket(port);
            System.out.println("Server started: " + server);
            start();
        }
        catch(IOException ioe)
        {
            System.out.println("Can not bind to port " + port + ": " + ioe.getMessage()); }
        }
    public void run()
    {
        while (thread != null)
        {
            try
            {
                System.out.println("Waiting for a client ...");
                addThread(server.accept());
            }
            catch(IOException ioe)
            {
                System.out.println("Server accept error: " + ioe); stop();
            }
        }
    }
    public void start()
    {
        if (thread == null)
        {
            thread = new Thread(this);
            thread.start();
        }
    }
    public void stop()
    {
        if (thread != null)
        {
            thread.stop(); //deprecated
            thread = null;
        }
    }
    private int findClient(int ID)
    {
        for (int i = 0; i < clientCount; i++)
        if (clients[i].getID() == ID)
            return i;
        return -1;
    }
    public void sendJoined(int ID)
    {
        ChatServerThread client = clients[findClient(ID)];
        for (int i = 0; i < clientCount; i++)
        {
            clients[i].send(client.name + " joined");
        }
        client.send("Zurzeit im Raum: " +getList());
    }
    public synchronized void handle(int ID, String input)
    {
        ChatServerThread client = clients[findClient(ID)];
        if (input.equals("getList"))
        {
            client.send(getList());
        }
        else
        {
            String message = client.name + ": " + input;
            for (int i = 0; i < clientCount; i++)
            {
                clients[i].send(message);
                if (input.equals(".bye"))
                {
                    clients[i].send(client.name + " disconnected");
                }
            }
            if (input.equals(".bye"))
            {
                remove(ID);
            }
        }
    }
    public synchronized void remove(int ID)
    {
        int pos = findClient(ID);
        if (pos >= 0)
        {
            ChatServerThread toTerminate = clients[pos];
            System.out.println("Removing client thread " + ID + " at " + pos);
            if (pos < clientCount-1)
                for (int i = pos+1; i < clientCount; i++)
                    clients[i-1] = clients[i];
            clientCount--;
            try
            {
                toTerminate.close();
            }
            catch(IOException ioe)
            {
                System.out.println("Error closing thread: " + ioe);
            }
            toTerminate.stop(); //deprecated
        }
    }
    private void addThread(Socket socket)
    {
        if (clientCount < clients.length)
        {
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try
            {
                clients[clientCount].open();
                clients[clientCount].start();
                clientCount++;
            }
            catch(IOException ioe)
            {
                System.out.println("Error opening thread: " + ioe);
            }
        }
        else
            System.out.println("Client refused: maximum " + clients.length + " reached.");
    }
    public String getList()
    {
	String namen = "[";
	for (int i = 0; i < clientCount; i++)
	    {
		    if (clients[i].name != null)
		    {
		        if (i < clientCount - 1)
                {
                    namen = namen + clients[i].name + ", ";
                }
		        else
                {
                    namen = namen + clients[i].name;
                }
		    }
	    }
	namen = namen + "]";
	return namen;
    }
    public static void main(String args[])
    {
        ChatServer server = new ChatServer(5555);
    }
}

class ChatServerThread extends Thread
{
    private ChatServer server = null;
    private Socket socket = null;
    private int ID = -1;
    DataInputStream streamIn = null;
    DataOutputStream streamOut = null;
    boolean accepted = false;
    String name;

    public ChatServerThread(ChatServer server, Socket socket)
    {
        super();
        this.server = server;
        this.socket = socket;
        ID = socket.getPort();
    }
    public void send(String msg)
    {
        try
        {
            streamOut.writeUTF(msg);
            streamOut.flush();
        }
        catch(IOException ioe)
        {
            System.out.println(ID + " ERROR sending: " + ioe.getMessage());
            server.remove(ID);
            stop(); //deprecated
        }
    }
    public int getID()
    {
        return ID;
    }
    public void run()
    {
        System.out.println("Server Thread " + ID + " running.");
        while (true)
        {
            try
            {
                while (!accepted)
                {
                    send("Name: ");
                    String tempName = streamIn.readUTF();
                    send("Passwort: ");
                    String tempPasswort = streamIn.readUTF();
                    if (CheckAccount(tempName, tempPasswort))
                    {
                        name = tempName;
                        server.sendJoined(ID);
                        accepted = true;

                    }
                    else
                    {
                        send("Access denied");
                        server.remove(ID);
                        accepted = false;
                    }
                }
                server.handle(ID, streamIn.readUTF());
            }
            catch(IOException ioe)
            {
                System.out.println(ID + " ERROR reading: " + ioe.getMessage());
                server.remove(ID);
                stop(); //deprecated
            }
        }
    }
    public void open() throws IOException
    {
        streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }
    public void close() throws IOException
    {
        if (socket != null)
            socket.close();
        if (streamIn != null)
            streamIn.close();
        if (streamOut != null)
            streamOut.close();
    }
    public boolean CheckAccount(String name, String passwort)
    {
        boolean check = false;
        File file = new  File("./Accounts");
        file.mkdir();
        Boolean exist = new File("./Accounts/"+ name +".txt").isFile();
        try
        {
            if (!exist)
            {
                File fileAccount = new File("./Accounts/" + name + ".txt");
                fileAccount.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileAccount));
                writer.write(passwort);
                writer.close();
                check = true;
                System.out.println("New Account " + name + " made");
            }
            else
            {

                BufferedReader in = new BufferedReader(new FileReader("./Accounts/" + name + ".txt"));
                String inhalt = in.readLine();
                if (inhalt.equals(passwort))
                {
                    check = true;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return check;
    }
}
