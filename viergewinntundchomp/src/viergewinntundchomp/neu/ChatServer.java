package neu;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatServer extends JFrame implements Runnable, ActionListener, WindowListener
{
    private ChatServerThread[] clients = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread thread = null;
    private int clientCount = 0;
    private JButton stopStart;
    public JTextArea chat, event, user, ingame;
    private JTextField tPortNumber;

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
                addThread(server.accept());
            }
            catch (IOException e)
            {
                appendEvent("Server accept error: " + e.getMessage());
            }
        }
    }
    public void start()
    {
        if (thread == null)
        {
            appendEvent("Server gestartet: " + server);
            user.setText(userListString());
            ingame.setText(imSpielString());
            thread = new Thread(this);
            thread.start();
        }
    }
    public void stop()
    {
        user.setText("Kein Server gestartet");
        ingame.setText("Kein Server gestartet");
        if (thread != null)
        {
            thread = null;
        }
        try
        {
            if (!server.isClosed())
                server.close();
        }
        catch (IOException e)
        {
            appendEvent("Error closing server: " + e.getMessage());
        }
        server = null;
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

    public ChatServerThread findClientByName(String name)
    {
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].name.equals(name))
            {
                return clients[i];
            }
        }
        return null;
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

    public void sendAmZug(int ID1, int ID2)
    {
        ChatServerThread spieler1 = clients[findClient(ID1)];
        ChatServerThread spieler2 = clients[findClient(ID2)];
        spieler1.sendInt(spieler1.amZug);
        spieler2.sendInt(spieler2.amZug);
    }


    public synchronized void sendMessage(int ID, String input)
    {
        ChatServerThread client = clients[findClient(ID)];
        if (input.equals("/bye"))
        {
            leaveMessage(client.name);
            return;
        }
        if (input.equals("/getlist"))
        {
            sendList();
            return;
        }

        String message = client.name + ": " + input;
        appendRoom(message);
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                if (clients[i].imSpiel)
                {
                    clients[i].sendInt(-2);
                }
                clients[i].send(message);
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
                appendEvent("Error closing thread: " + e.getMessage());
            }
        }
        user.setText(userListString());
        ingame.setText(imSpielString());
        sendList();
    }
    public void removeAll()
    {
        int clientAnzahl = clientCount;
        for (int i = clientAnzahl - 1; i >= 0; i--)
        {
            remove(clients[i].getID());
        }
    }

    private void addThread(Socket socket)
    {
        if (clientCount < clients.length)
        {
            appendEvent("Client accepted: " + socket);
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
            }
        }
        else
        {
            appendEvent("Maximale Anzahl von " + clients.length + " Clients erreicht.");
        }
    }

    public void sendList()
    {
        ChatServerThread[] accepted = getUser();
        String[] ingame = getIngameUser();
        for (int i = 0; i < accepted.length; i++)
        {
            if(accepted[i] != null)
            {
                accepted[i].send("/userListBeginn");
                for (int k = 0; k < accepted.length; k++)
                {
                    accepted[i].send(getUser()[k].name);
                }
                accepted[i].send("/userListEnde");
                accepted[i].send("/imSpielListBeginn");
                for (int k = 0; k < ingame.length; k++)
                {
                    if (ingame[k] == null)
                    {
                        break;
                    }
                    accepted[i].send(ingame[k]);
                }
                accepted[i].send("/imSpielListEnde");
            }
        }
    }
    ChatServerThread[] getUser()
    {
        int k = 0,j = 0;
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted && !clients[i].imSpiel)
            {
                j++;
            }
        }
        ChatServerThread[] acceptedClients = new ChatServerThread[j];
        for (int i = 0; i < j; i++)
        {
            if (clients[i].accepted && !clients[i].imSpiel)
            {
                acceptedClients[k++] = clients[i];
            }

        }
        return acceptedClients;
    }

    String[] getIngameUser()
    {
        int x = 0;
        ChatServerThread[] accepted = getUser();
        String[] ingame = new String[accepted.length];
        for (int i = 0; i < accepted.length; i++)
        {
            if (accepted[i] != null && accepted[i].imSpiel && !accepted[i].marked)
            {
                ingame[x++] = accepted[i].name + " vs. " + accepted[i].gegner + " (" + accepted[i].spiel + ")\n";
                accepted[i].marked = true;
                accepted[i].gegner.marked = true;
            }
        }
        for (int i = 0; i < accepted.length; i++)
        {
            if (accepted[i] != null && accepted[i].marked)
            {
                accepted[i].marked = false;
            }
        }
        return ingame;
    }

    boolean istDrin(String name)
    {
        for (int i = 0; i < getUser().length; i++)
        {
            if (name.equals(getUser()[i].name))
                return true;
        }
        return false;
    }

    String userListString()
    {
        String namen = "Zurzeit im Raum: \n";
        ChatServerThread[] accepted = getUser();
        for (int i = 0; i < accepted.length; i++)
        {
            if (accepted[i].name != null)
            {
                namen = namen + accepted[i].name + "\n";
            }
        }
        return namen;
    }

    String imSpielString()
    {
        String namen = "Zurzeit im Spiel:  \n";
        String[] ingame = getIngameUser();
        for (int i = 0; i < ingame.length; i++)
        {
            if (ingame[i] == null)
            {
                break;
            }
            namen = namen + ingame[i];
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
        JPanel east = new JPanel(new GridLayout(2, 1));
        user = new JTextArea("Server noch nicht gestartet");
        user.setEditable(false);
        ingame = new JTextArea("Server noch nicht gestartet");
        ingame.setEditable(false);
        east.add(user);
        east.add(ingame);
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
            for (int i = 0; i < clientCount; i++)
            {
                if (clients[i].imSpiel)
                {
                    clients[i].sendInt(-1);
                }
                clients[i].send("Server wurde geschlossen");
            }
            removeAll();
            stop();
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            user.setText("Kein Server gestartet");
            ingame.setText("Kein Server gestartet");
        }
        else
        {
            int port;
            try
            {
                port = Integer.parseInt(tPortNumber.getText().trim());
                server = new ServerSocket(port);
            }
            catch(Exception ex)
            {
                appendEvent("Invalid port number: " + ex.getMessage());
                return;
            }
            start();
            stopStart.setText("Exit");
            chat.setText("Chat room. \n");
            tPortNumber.setEditable(false);
        }
    }

    void appendRoom(String str)
    {
        chat.append(str + "\n");
    }

    void appendEvent(String str)
    {
        event.append(str + "\n");
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
    public int ID, xK, yK, status, amZug;
    public DataInputStream streamIn = null;
    private DataOutputStream streamOut = null;
    public boolean accepted = false, imSpiel = false, marked = false, connected = true;
    public String name, spiel, msg = "";
    public ChatServerThread gegner = null;

    public ChatServerThread(ChatServer server, Socket socket)
    {
        super();
        this.server = server;
        this.socket = socket;
        ID = socket.getPort();
    }
    public void send(String message)
    {
        try
        {
            streamOut.writeUTF(message);
            streamOut.flush();
        }
        catch(IOException e)
        {
            server.appendEvent(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
        }
    }
    public synchronized void sendInt(int zahl)
    {
        try
        {
            streamOut.writeInt(zahl);
            streamOut.flush();
        }
        catch(IOException e)
        {
            server.appendEvent(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
        }
    }

    public int getID()
    {
        return ID;
    }

    public void antwortSpiel(String antwort) throws IOException
    {
        if (antwort.equals("ja"))
        {
            imSpiel = true;
            server.ingame.setText(server.imSpielString());
            send("/begin");
            gegner.send("/begin");
            server.sendAmZug(ID, gegner.ID);
        }
        else
        {
            imSpiel = false;
            gegner.imSpiel = false;
            gegner.gegner = null;
            gegner.send("/denySpiel");
            spiel = null;
            gegner = null;
        }
    }

    public void startSpiel() throws IOException
    {
        imSpiel = true;
        amZug = (int) (Math.random() + .5);
        spiel = streamIn.readUTF();
        gegner = server.findClientByName(streamIn.readUTF());
        gegner.amZug = (amZug + 1) % 2;
        gegner.gegner = this;
        gegner.send("/offerSpiel");
        gegner.send(name);
        gegner.send(spiel);
    }

    public void run()
    {
        server.appendEvent("Server Thread " + ID + " running.");
        while (connected)
        {
            if (accepted)
            {
                try
                {
                    if (!imSpiel)
                    {
                        msg = streamIn.readUTF();
                        switch (msg)
                        {
                            case "/bye":
                                server.leaveMessage(name);
                                send("Auf Wiedersehen");
                                server.remove(ID);
                                connected = false;
                                break;
                            case "/startSpiel":
                                startSpiel();
                                break;
                            case "/acceptSpiel":
                                antwortSpiel("ja");
                                break;
                            case "/denySpiel":
                                antwortSpiel("nein");
                                break;
                            default:
                                server.sendMessage(ID, msg);
                                break;
                        }
                    }
                    else
                    {
                        int status = streamIn.readInt();
                        if (status > 0)
                        {
                            imSpiel = false;
                            gegner.imSpiel = false;
                            sendInt(-1);
                            gegner.sendInt(0);
                        }
                        if (status == 0)
                        {
                            gegner.sendInt(1);
                        }
                        if (status == -2)
                        {
                            imSpiel = false;
                            gegner.imSpiel = false;
                            gegner.sendInt(-1);
                        }
                        else
                        {
                            int x = streamIn.readInt();
                            int y = streamIn.readInt();
                            if (x >= 0 && y >= 0 && status >= 0)
                            {
                                gegner.sendInt(status);
                                gegner.sendInt(x);
                                gegner.sendInt(y);
                            }
                        }
                    }

                }
                catch (IOException e)
                {
                    server.appendEvent(ID + " ERROR reading: " + e.getMessage());
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
                    if (checkAccount(tempName, tempPasswort) && !server.istDrin(tempName))
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
                        server.remove(ID);
                        break;
                    }
                }
                catch (IOException e)
                {
                    server.appendEvent(ID + " ERROR reading: " + e.getMessage());
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
                server.appendEvent("Neuen Account " + name + " erstellt");
                return true;
            }
            else
            {

                BufferedReader in = new BufferedReader(new FileReader("./Accounts/" + name + ".txt"));
                String inhalt = in.readLine();
                if (inhalt.equals(passwort))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }
}