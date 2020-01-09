import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.sql.Timestamp;

public class ChatServer extends JFrame implements Runnable, ActionListener, WindowListener
{
    private ChatServerThread[] clients = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread thread = null;
    private int clientCount = 0;
    private JButton stopStart;
    private JTextArea chat, event, user;
    private JTextField tPortNumber;
    private Timestamp ts;

    public static void main(String[] args)
    {
        ChatServer server = new ChatServer(5555);
    }
    public ChatServer(int port)
    {
        super("Chat Server");
        createUI(port);
    }
    public void run()
    {
        while (thread != null)
        {
            try
            {
                appendEvent("Wartet auf Clients");
                //System.out.println("Wartet auf Clients");
                addThread(server.accept());
            }
            catch(IOException e)
            {
                appendEvent("Server accept error: " + e);
                //System.out.println("Server accept error: " + e);
                stop();
            }
        }
    }
    public void start()
    {
        if (thread == null)
        {
            appendEvent("Server gestartet: " + server);
            //System.out.println("Server gestartet: " + server);
            user.setText(userListString());
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
        String msg = client.name + " joined";
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                clients[i].send(client.name + " joined");
            }
        }
        appendRoom(msg);
        sendList();
        user.setText(userListString());
    }
    public void leaveMessage(String name)
    {
        String msg = name + " disconnected";
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                clients[i].send(msg);
            }
        }
        appendRoom(msg);
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
                appendRoom(message);
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
            toTerminate.accepted = false;
            appendEvent("Client Thread " + ID + " auf Platz " + pos + " wird entfernt");
            //System.out.println("Client Thread " + ID + " auf Platz " + pos + " wird entfernt");
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
                appendEvent("Error closing thread: " + e);
                //System.out.println("Error closing thread: " + e);
            }
        }
        user.setText(userListString());
        sendList();
    }
    private void addThread(Socket socket)
    {
        if (clientCount < clients.length)
        {
            appendEvent("Client accepted: " + socket);
            //System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try
            {
                clients[clientCount].open();
                clients[clientCount].start();
                clientCount++;
            }
            catch(IOException e)
            {
                appendEvent("Error opening thread: " + e);
                //System.out.println("Error opening thread: " + e);
            }
        }
        else
        {
            appendEvent("Maximale Anzahl von " + clients.length + " Clients erreicht.");
            //System.out.println("Maximale Anzahl von " + clients.length + " Clients erreicht.");
        }
    }
    public void sendList()
    {
        for (int i = 0; i < getUser().length; i++)
        {
            getUser()[i].send("userListBeginn");
            getUser()[i].send(userListString());
            getUser()[i].send("userListEnde");
        }
    }
    ChatServerThread[] getUser()
    {
        int k = 0,j = 0;
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                j++;
            }
        }
        ChatServerThread[] acceptedClients = new ChatServerThread[j];
        for (int i = 0; i < j; i++)
        {
            if (clients[i].accepted)
            {
                acceptedClients[k++] = clients[i];
            }

        }
        return acceptedClients;
    }
    String userListString()
    {
        String namen = "Zurzeit im Raum: \n";
        for (int i = 0; i < getUser().length; i++)
        {
            namen = namen + getUser()[i].name + "\n";
        }
        return namen;
    }
    //Aufgabenblatt3 Methoden Anfang

    void createUI(int port)
    {
        //Start und Stop Button
        JPanel north = new JPanel();
        north.add(new JLabel("Port number: "));
        tPortNumber = new JTextField("" + port);
        north.add(tPortNumber);
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);
        north.add(stopStart);
        add("North", north);

        //chat und eventlog
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

        //Userliste
        JPanel east = new JPanel();
        user = new JTextArea("Server noch nicht gestartet");
        user.setEditable(false);
        east.add(user);
        add("East", east);

        addWindowListener(this);
        setSize(600, 600);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
        if(server != null)
        {
            appendEvent("Server geschlossen");
            stop();
            //tPortNumber.setEditable(true); Nachfragen!
            //stopStart.setText("Start"); Nachfragen!
            System.exit(0);
        }
        int port;
        try
        {
            port = Integer.parseInt(tPortNumber.getText().trim());
            server = new ServerSocket(port);
        }
        catch(Exception ex)
        {
            appendEvent("Invalid port number");
            return;
        }
        start();
        stopStart.setText("Exit");
        tPortNumber.setEditable(false);
    }

    void appendRoom(String str)
    {
        ts = new Timestamp(new Date().getTime());
        chat.append("[" + ts + "] " + str + "\n");
        //chat.append(str + "\n"); nur eins von beiden
        chat.setCaretPosition(chat.getText().length() - 1);
    }

    void appendEvent(String str)
    {
        ts = new Timestamp(new Date().getTime());
        event.append("[" + ts + "] " + str + "\n");
        //event.append(str + "\n"); nur eins von beiden
        event.setCaretPosition(chat.getText().length() - 1);
    }

    public void windowClosing(WindowEvent e)
    {
        if(server != null)
        {
            try
            {
                stop();
            }
            catch(Exception eClose) {}
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

    //Aufgabenblatt3 Methoden Ende
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
            server.appendEvent(ID + " ERROR sending: " + e.getMessage());
            //System.out.println(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
        }
    }
    public int getID()
    {
        return ID;
    }
    public void run()
    {
        server.appendEvent("Server Thread " + ID + " running.");
        //System.out.println("Server Thread " + ID + " running.");
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
                        send("Auf Wiedersehen");
                        server.remove(ID);
                        break;
                    }
                }
                catch (IOException e)
                {
                    server.appendEvent(ID + " ERROR reading: " + e.getMessage());
                    //System.out.println(ID + " ERROR reading: " + e.getMessage());
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
                        send("Anmeldung erfolgreich");
                        name = tempName;
                        accepted = true;
                        server.joinMessage(this);
                    }
                    else
                    {
                        send("Anmeldung fehlgeschlagen");
                        server.appendEvent("Stopping Server Thread " + ID);
                        //System.out.println("Stopping Server Thread " + ID);
                        server.remove(ID);
                        break;
                    }
                }
                catch (IOException e)
                {
                    server.appendEvent(ID + " ERROR reading: " + e.getMessage());
                    //System.out.println(ID + " ERROR reading: " + e.getMessage());
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
                server.appendEvent("Neuen Account " + name + " erstellt");
                //System.out.println("New Account " + name + " created");
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
