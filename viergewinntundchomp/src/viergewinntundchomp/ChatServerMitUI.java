import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatServer implements Runnable
{
    private ChatServerThread[] clients = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread thread = null;
    private int clientCount = 0;
    static ServerOberfläche ui;

    public static void main(String[] args)
    {
        ui = new ServerOberfläche(5555);

    }
    public ChatServer(int port)
    {
        try
        {
            server = new ServerSocket(port);
            userList();
            ui.appendEvent("Server gestartet: " + server);
            System.out.println("Server gestartet: " + server);
        }
        catch(IOException e)
        {
            System.out.println("Ungültiger Port " + port + ": " + e.getMessage());
        }
    }
    public void run()
    {
        while (thread != null)
        {
            try
            {
                ui.appendEvent("Wartet auf Clients");
                System.out.println("Wartet auf Clients");
                addThread(server.accept());
            }
            catch(IOException e)
            {
                ui.appendEvent("Server accept error: " + e);
                System.out.println("Server accept error: " + e);
                stop();
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
            thread = null;
        }
    }
    private int findClient(int ID)
    {
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].getID() == ID)
            {
                return i;
            }
        }
        return -1;
    }
    public void joinMessage(ChatServerThread client)
    {
        // client.Send("Zurzeit im Raum: " + GetList());
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                clients[i].send(client.name + " joined");
            }
        }
        sendList();
        userList();
    }
    public void leaveMessage(String name)
    {
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                clients[i].send(name + " disconnected");
            }
        }
        sendList();
    }
    public synchronized void sendMessage(int ID, String input)
    {
        ChatServerThread client = clients[findClient(ID)];
        if (input.equals("/getlist"))
        {
            sendList();
        }
        else
        {
            if (input.equals(".bye"))
            {
                leaveMessage(client.name);
            }
            else
            {
                String message = client.name + ": " + input;
                ui.appendRoom(message);
                for (int i = 0; i < clientCount; i++)
                {
                    if (clients[i].accepted)
                    {
                        clients[i].send(message);
                    }
                }
            }
        }
    }
    public synchronized void remove(int ID)
    {
        int pos = findClient(ID);
        if (pos >= 0)
        {
            ChatServerThread toTerminate = clients[pos];
            ui.appendEvent("Client Thread " + ID + " auf Platz " + pos + " wird entfernt");
            System.out.println("Client Thread " + ID + " auf Platz " + pos + " wird entfernt");
            if (pos < clientCount - 1)
            {
                for (int i = pos+1; i < clientCount; i++)
                {
                    clients[i-1] = clients[i];
                }
            }
            clientCount--;
            try
            {
                toTerminate.close();
            }
            catch(IOException e)
            {
                ui.appendEvent("Error closing thread: " + e);
                System.out.println("Error closing thread: " + e);
            }
        }
        userList();
    }
    private void addThread(Socket socket)
    {
        if (clientCount < clients.length)
        {
            ui.appendEvent("Client accepted: " + socket);
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try
            {
                clients[clientCount].open();
                clients[clientCount].start();
                clientCount++;
            }
            catch(IOException e)
            {
                ui.appendEvent("Error opening thread: " + e);
                System.out.println("Error opening thread: " + e);
            }
        }
        else
        {
            ui.appendEvent("Maximale Anzahl von " + clients.length + " Clients erreicht.");
            System.out.println("Maximale Anzahl von " + clients.length + " Clients erreicht.");
        }
    }
    public void sendList()
    {
        int j = 0;
        ChatServerThread[] acceptedClients = new ChatServerThread[50];
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                acceptedClients[j++] = clients[i];
            }
        }
        for (int i = 0; i < j; i++)
        {
            acceptedClients[i].send("userListBeginn");
            for (int k = 0; k < j; k++)
            {
                acceptedClients[i].send(acceptedClients[k].name);
            }
            acceptedClients[i].send("userListEnde");
        }
    }
    void userList()
    {
        ui.user.setText("Zurzeit in Raum:\n");
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                ui.user.append(clients[i].name +"\n");
            }
        }
    }
}

class ChatServerThread extends Thread
{
    private ChatServer server;
    private Socket socket;
    private int ID;
    private DataInputStream streamIn = null;
    private DataOutputStream streamOut = null;
    public boolean accepted = false;
    public String name;

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
        catch(IOException e)
        {
            server.ui.appendEvent(ID + " ERROR sending: " + e.getMessage());
            System.out.println(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
        }
    }
    public int getID()
    {
        return ID;
    }
    public void run()
    {
        server.ui.appendEvent("Server Thread " + ID + " running.");
        System.out.println("Server Thread " + ID + " running.");
        while (true)
        {
            if (accepted)
            {
                try
                {
                    String msg = streamIn.readUTF();
                    server.sendMessage(ID, msg);
                    if (msg.equals(".bye"))
                    {
                        send("Goodbye");
                        server.remove(ID);
                        break;
                    }
                }
                catch (IOException e)
                {
                    server.ui.appendEvent(ID + " ERROR reading: " + e.getMessage());
                    System.out.println(ID + " ERROR reading: " + e.getMessage());
                    server.remove(ID);
                    server.leaveMessage(name);
                    break;
                }
            }
            else
            {
                try
                {
                    String tempName = streamIn.readUTF();
                    String tempPasswort = streamIn.readUTF();
                    if (checkAccount(tempName, tempPasswort))
                    {
                        send("Access granted");
                        name = tempName;
                        accepted = true;
                        server.joinMessage(this);
                    }
                    else
                    {
                        send("Access denied");
                        server.ui.appendEvent("Stopping Server Thread " + ID);
                        System.out.println("Stopping Server Thread " + ID);
                        server.remove(ID);
                        break;
                    }
                }
                catch (IOException e)
                {
                    server.ui.appendEvent(ID + " ERROR reading: " + e.getMessage());
                    System.out.println(ID + " ERROR reading: " + e.getMessage());
                    break;
                }
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
        {
            socket.close();
        }
        if (streamIn != null)
        {
            streamIn.close();
        }
        if (streamOut != null)
        {
            streamOut.close();
        }
    }
    public boolean checkAccount(String name, String passwort)
    {
        boolean check = false;
        File file = new  File("./Accounts");
        file.mkdir();
        boolean exist = new File("./Accounts/"+ name +".txt").isFile();
        try
        {
            if (!exist)
            {
                File fileAccount = new File("./Accounts/" + name + ".txt");
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileAccount));
                writer.write(passwort);
                writer.close();
                check = true;
                server.ui.appendEvent("New Account " + name + " created");
                System.out.println("New Account " + name + " created");
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

class ServerOberfläche extends JFrame implements ActionListener, WindowListener
{
    private JButton stopStart;
    private JTextArea chat, event;
    public JTextArea user;
    private JTextField tPortNumber;
    private ChatServer server;

    ServerOberfläche(int port)
    {
        super("Chat Server");
        server = null;
        JPanel north = new JPanel();
        north.add(new JLabel("Port number: "));
        tPortNumber = new JTextField("" + port);
        north.add(tPortNumber);
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);
        north.add(stopStart);
        add("North", north);


        JPanel center = new JPanel(new GridLayout(2,1));
        chat = new JTextArea(80,80);
        chat.setEditable(false);
        appendRoom("Chat room.\n");
        center.add(new JScrollPane(chat));
        event = new JTextArea(80,80);
        event.setEditable(false);
        appendEvent("Events log.\n");
        center.add(new JScrollPane(event));
        add("Center", center);

        JPanel south = new JPanel();
        user = new JTextArea(160,20);
        user.setEditable(false);
        south.add(new JScrollPane(user));
        add("East", user);

        addWindowListener(this);
        setSize(400, 600);
        setVisible(true);
    }
    void appendRoom(String str)
    {
        chat.append(str + "\n");
        chat.setCaretPosition(chat.getText().length() - 1);
    }
    void appendEvent(String str)
    {
        event.append(str + "\n");
        event.setCaretPosition(chat.getText().length() - 1);

    }

    public void actionPerformed(ActionEvent e)
    {
        // if running we have to stop
        if(server != null)
        {
            appendEvent("Server geschlossen");
            server.stop();
            server = null;

            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        // OK start the server
        int port;
        try
        {
            port = Integer.parseInt(tPortNumber.getText().trim());
        }
        catch(Exception ex)
        {
            appendEvent("Invalid port number");
            return;
        }

        // ceate a new Server
        server = new ChatServer(port);
        server.start();
        stopStart.setText("Stop");
        tPortNumber.setEditable(false);
    }

    public void windowClosing(WindowEvent e)
    {
        if(server != null)
        {
            try
            {
                server.stop();
            }
            catch(Exception eClose) {}
            server = null;
        }
        dispose();
        System.exit(0);
    }
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
}
