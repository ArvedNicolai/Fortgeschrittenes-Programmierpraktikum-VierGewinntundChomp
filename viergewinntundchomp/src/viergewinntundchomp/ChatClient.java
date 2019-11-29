import java.net.*;
import java.io.*;

public class ChatClient implements Runnable
{
    private Socket socket = null;
    private Thread thread = null;
    private BufferedReader console = null;
    private DataOutputStream streamOut = null;
    private ChatClientThread client = null;

    public ChatClient(String serverName, int serverPort)
    {
        System.out.println("Establishing connection. Please wait ...");
        try
        {
            socket = new Socket(serverName, serverPort);
            System.out.println("Connected: " + socket);
            start();
        }
        catch(UnknownHostException uhe)
        {
            System.out.println("Host unknown: " + uhe.getMessage());
        }
        catch(IOException ioe)
        {
            System.out.println("Unexpected exception: " + ioe.getMessage());
        }
    }
    public void run()
    {
        while (thread != null)
        {
            try
            {
                streamOut.writeUTF(console.readLine()); //deprecated (readLine())
                streamOut.flush();
            }
            catch(IOException ioe)
            {
                System.out.println("Sending error : " + ioe.getMessage());
                stop();
            }
        }
    }
    public void handle(String msg)
    {
        if (msg.equals(".bye"))
        {
            System.out.println("Good bye. Press RETURN to exit ...");
            stop();
        }
        else
            System.out.println(msg);
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
            thread.stop(); //deprecated
            thread = null;
        }
        try
        {
            if (console != null)
                console.close();
            if (streamOut != null)
                streamOut.close();
            if (socket != null)
                socket.close();
        }
        catch(IOException ioe)
        {
            System.out.println("Error closing ...");
        }
        client.close();
        client.stop(); //deprecated
    }
    public static void main(String args[])
    {
        ChatClient client = null;
        client = new ChatClient("localhost", 5555);
    }
}

class ChatClientThread extends Thread
{
    private Socket socket = null;
    private ChatClient client = null;
    private DataInputStream streamIn = null;

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
        catch(IOException ioe)
        {
            System.out.println("Error getting input stream: " + ioe);
            client.stop();
        }
    }
    public void close()
    {
        try
        {
            if (streamIn != null)
                streamIn.close();
        }
        catch(IOException ioe)
        {
            System.out.println("Error closing input stream: " + ioe);
        }
    }
    public void run()
    {
        while (true)
        {
            try
            {
                client.handle(streamIn.readUTF());
            }
            catch(IOException ioe)
            {
                System.out.println("Listening error: " + ioe.getMessage());
                client.stop();
            }
        }
    }
}

