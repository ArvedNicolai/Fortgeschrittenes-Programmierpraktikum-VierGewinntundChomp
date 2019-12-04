import java.net.*;
import java.io.*;

public class ChatClient implements Runnable
{
    private Socket socket = null;
    private Thread thread = null;
    private BufferedReader console = null;
    private DataOutputStream streamOut = null;
    private ChatClientThread client = null;
    private boolean anmelden = true;

    public static void main(String args[])
    {
        ChatClient client = null;
        client = new ChatClient("localhost", 5555);
    }

    public ChatClient(String serverName, int serverPort)
    {
        System.out.println("Verbindung wird aufgebaut");
        try
        {
            socket = new Socket(serverName, serverPort);
            System.out.println("Connected: " + socket);
            start();
        }
        catch(UnknownHostException e)
        {
            System.out.println("Host unknown: " + e.getMessage());
        }
        catch(IOException e)
        {
            System.out.println("Unexpected exception: " + e.getMessage());
        }
    }
    public void run()
    {
        while (thread != null)
        {
            try
            {
                String msg = console.readLine();
                if (anmelden)
                {
                    client.name = msg;
                    anmelden = false;
                }
                streamOut.writeUTF(msg);
                streamOut.flush();
            }
            catch(IOException e)
            {
                System.out.println("Sending error : " + e.getMessage());
                stop();
            }
        }
    }
    public void Message(String msg)
    {
        System.out.println(msg);
        if (msg.equals("Access denied") || msg.equals((client.name + " disconnected")))
        {
            System.out.println("Good bye. Press RETURN to exit ...");
            stop();
        }
    }
    public void start() throws IOException
    {
        console = new BufferedReader(new InputStreamReader(System.in));
        streamOut = new DataOutputStream(socket.getOutputStream());
        if (thread == null)
        {
            client = new ChatClientThread(this, socket);
            thread = new Thread(this);
            thread.start();
        }
    }
    public void stop()
    {
        if (thread != null)
        {
            thread = null;
        }
        try
        {
            if (console != null)
            {
                console.close();
            }
            if (streamOut != null)
            {
                streamOut.close();
            }
            if (socket != null)
            {
                socket.close();
            }
        }
        catch(IOException e)
        {
            System.out.println("Error closing");
        }
        client.close();
    }
}

class ChatClientThread extends Thread
{
    private Socket socket = null;
    private ChatClient client = null;
    private DataInputStream streamIn = null;
    public String name;

    public ChatClientThread(ChatClient client, Socket socket)
    {
        this.client   = client;
        this.socket   = socket;
        open();
        start();
    }
    public void open()
    {
        try
        {
            streamIn  = new DataInputStream(socket.getInputStream());
        }
        catch(IOException e)
        {
            System.out.println("Error getting input stream: " + e);
            client.stop();
        }
    }
    public void close()
    {
        try
        {
            if (streamIn != null)
            {
                streamIn.close();
            }
        }
        catch(IOException e)
        {
            System.out.println("Error closing input stream: " + e);
        }
    }
    public void run()
    {
        while (true)
        {
            try
            {
                String msg = streamIn.readUTF();
                client.Message(msg);
                if (msg.equals("Access denied") || msg.equals((name + " disconnected")))
                {
                    break;
                }
            }
            catch(IOException e)
            {
                System.out.println("Listening error: " + e.getMessage());
                client.stop();
                break;
            }
        }
    }
}

