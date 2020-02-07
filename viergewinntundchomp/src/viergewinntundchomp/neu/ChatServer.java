package neu;

//Fehler von der letzten Veranstaltung war in der Methode getIngameUser in der Klasse ChatServer
//mittlerweile sollten zwei Spiele bei vier gleichzeitigen Usern funktionieren. 

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
    private JTextArea chat, event, user, ingame;
    private JTextField tPortNumber;

    public static void main(String[] args)
    {
        ChatServer server = new ChatServer(5555);
    }
    private ChatServer(int port)
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

    private void start()
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

    private void stop()
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

    ChatServerThread findClientByName(String name)
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

    void joinMessage(ChatServerThread client)
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
        updateUserList();
    }

    void leaveMessage(String name)
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

    //Nachrichten im Server, die die Spielergebnisse anzeigen
    synchronized void sendMessage(int ID, String input)
    {	
        String message = null;
        ChatServerThread client = clients[findClient(ID)];
        switch (input)
        {
            case "/bye":
                leaveMessage(client.name);
                break;
            case "/getlist":
                updateUserList();
                break;
            case "/win":
                message = client.name + " hat in " + client.spiel + " gegen " + client.gegner.name + " gewonnen";
                break;
            case "/draw":
                message = "Unentschieden zwischen " + client.name + " und " + client.gegner.name + " in " + client.spiel;
                break;
            case "/end":
                message = client.name + " hat in " + client.spiel + " gegen " + client.gegner.name + " aufgegeben";
                break;
            default:
                message = client.name + ": " + input;
                break;
        }
        if (message != null)
        {
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

    void sendAmZug(int ID1, int ID2)
    {
        ChatServerThread spieler1 = clients[findClient(ID1)];
        ChatServerThread spieler2 = clients[findClient(ID2)];
        spieler1.sendInt(spieler1.amZug);
        spieler2.sendInt(spieler2.amZug);
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
        updateUserList();
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

    private void sendList()
    {
        ChatServerThread[] accepted = getChatUser();
        String[] ingame = getIngameUser();
        for (int i = 0; i < accepted.length; i++)
        {
            accepted[i].send("/userListBeginn");
            for (int k = 0; k < accepted.length; k++)
            {
                accepted[i].send(getChatUser()[k].name);
            }
            accepted[i].send("/userListEnde");
            accepted[i].send("/imSpielListBeginn");
            for (int k = 0; k < ingame.length; k++)
            {
                accepted[i].send(ingame[k]);
            }
            accepted[i].send("/imSpielListEnde");
        }
    }

    //Herausfinden der User, die auf dem Server sind
    private ChatServerThread[] getChatUser()
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
        for (int i = 0; i < acceptedClients.length; i++)
        {
            if (clients[i].accepted)
            {
                acceptedClients[k++] = clients[i];
            }
        }
        return acceptedClients;
    }

    //Herausfinden, der gerade spielenden User
    //User die gerade im Spiel sind, werden markiert, damit sie nicht mehrere Male in die Liste 
    //aufgenommen werden, am Ende der Erstellung des spielInfo Arrays, werden alle User
    //wieder entmarkiert
    
    private String[] getIngameUser()    //Fehler war in dieser Methode, hatte null Werte und hat exceptions ausgelöst
    {
        int x = 0, y = 0;
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted && clients[i].imSpiel && !clients[i].marked)
            {
                clients[i].marked = true;
                clients[i].gegner.marked = true;
                x++;
            }
        }
        for (int i = 0; i < clientCount; i++)
        {
            clients[i].marked = false;
        }
        String[] spielInfo = new String[x];
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i] != null && clients[i].imSpiel && !clients[i].marked)
            {
                spielInfo[y++] = "\n" + clients[i].name + " vs. " + clients[i].gegner.name + " (" + clients[i].spiel + ")";
                clients[i].marked = true;
                clients[i].gegner.marked = true;
            }
        }
        for (int i = 0; i < clientCount; i++)
        {
            clients[i].marked = false;
        }
        return spielInfo;
    }

    //Anzeige, der User auf dem Server
    private String userListString()
    {
        String namen = "Zurzeit im Raum: \n";
        ChatServerThread[] accepted = getChatUser();
        for (int i = 0; i < accepted.length; i++)
        {
            if (accepted[i].name != null)
            {
                namen = namen + accepted[i].name + "\n";
            }
        }
        return namen;
    }
    
    //Anzeige der User, die im Spiel sind
    private String imSpielString()
    {
        String namen = "Zurzeit im Spiel: ";
        String[] ingame = getIngameUser();
        for (int i = 0; i < ingame.length; i++)
        {
            namen = namen + ingame[i];
        }
        return namen;
    }

    void updateUserList()
    {
        user.setText(userListString());
        ingame.setText(imSpielString());
        sendList();
    }

    boolean istDrin(String name)
    {
        for (int i = 0; i < getChatUser().length; i++)
        {
            if (name.equals(getChatUser()[i].name))
                return true;
        }
        return false;
    }

    //Aufgabenblatt3 Methoden Anfang

    private void createUI(int port)
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

    private void appendRoom(String str)
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
            catch(Exception ignored) {}
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
    public int ID, amZug;
    public DataInputStream streamIn = null;
    private DataOutputStream streamOut = null;
    public boolean accepted = false, imSpiel = false, marked = false, connected = true;
    public String name, spiel;
    public ChatServerThread gegner = null;

    ChatServerThread(ChatServer server, Socket socket)
    {
        super();
        this.server = server;
        this.socket = socket;
        ID = socket.getPort();
    }

    int getID()
    {
        return ID;
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
                    String msg = streamIn.readUTF();
                    if (!imSpiel)
                    {
                        commands(msg);
                    }
                    else
                    {
                        sendSpielInfo(msg);
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

    void open() throws IOException
    {
        streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    void close() throws IOException
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

    void send(String message)
    {
        try
        {
            if (imSpiel)
            {
                streamOut.writeUTF("message");
                streamOut.flush();
            }
            streamOut.writeUTF(message);
            streamOut.flush();
        }
        catch(IOException e)
        {
            server.appendEvent(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
        }
    }

    private void commands(String msg) throws IOException
    {
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
                antwortSpiel(true);
                break;
            case "/declineSpiel":
                antwortSpiel(false);
                break;
            default:
                server.sendMessage(ID, msg);
                break;
        }
    }

    private void sendStatus(String status)
    {
        try
        {
            streamOut.writeUTF(status);
            streamOut.flush();
        }
        catch(IOException e)
        {
            server.appendEvent(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
        }
    }

    private void sendSpielInfo(String status) throws IOException
    {
        switch (status)
        {
            case "win": case "draw":
            imSpiel = false;
            server.sendMessage(ID, "/" + status);
            gegner.sendStatus("end");
            sendFeld(status);
            break;
            case "nothing":
                gegner.sendStatus("nothing");
                sendFeld(status);
                break;
            case "end":
                gegner.imSpiel = false;
                server.sendMessage(ID, "/" + status);
                gegner.sendStatus("close");
                gameEnd();
                server.updateUserList();
                break;
            case "close":
                imSpiel = false;
                gegner.imSpiel = false;
                server.updateUserList();
                break;
        }
    }

    synchronized void sendInt(int zahl)
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

    private void sendFeld(String status) throws IOException
    {
        int x, y;
        x = streamIn.readInt();
        y = streamIn.readInt();
        gegner.sendStatus(status);
        gegner.sendInt(x);
        gegner.sendInt(y);
    }

    private void startSpiel() throws IOException
    {
        amZug = (int) (Math.random() + .5);
        spiel = streamIn.readUTF();
        int spielHöhe = streamIn.readInt();
        int spielBreite = streamIn.readInt();
        try
        {
            gegner = server.findClientByName(streamIn.readUTF());
            if (!gegner.imSpiel)
            {
                gegner.amZug = (amZug + 1) % 2;
                gegner.spiel = spiel;
                gegner.gegner = this;
                gegner.send("/offerSpiel");
                gegner.send(name);
                gegner.sendInt(spielHöhe);
                gegner.sendInt(spielBreite);
                gegner.send(spiel);
            }
            else
            {
                send("/busy");
            }
        }
        catch (NullPointerException e)
        {
            send("/busy");
        }
    }

    private void antwortSpiel(boolean antwort)
    {
        if (antwort)
        {
            try
            {
                gegner = server.findClientByName(gegner.name);
                send("/begin");
                gegner.send("/begin");
                server.sendAmZug(ID, gegner.ID);
                imSpiel = true;
                gegner.imSpiel = true;
                server.updateUserList();
            }
            catch (NullPointerException e)
            {
                send("/busy");
            }
        }
        else
        {
            gegner.send("/declineSpiel");
            if (!imSpiel)
            {
                gameEnd();
            }
        }
    }

    private void gameEnd()
    {
        if (gegner != null)
        {
            gegner.imSpiel = false;
            gegner.spiel = null;
            gegner.gegner = null;
        }
        imSpiel = false;
        spiel = null;
        gegner = null;
    }

    private boolean checkAccount(String name, String passwort)
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

