package neu;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class ChatClient extends JFrame implements Runnable, ActionListener, ItemListener, WindowListener
{
    private Socket socket = null;
    private Thread thread = null;
    private BufferedReader console = null;
    public DataOutputStream streamOut = null;
    public ChatClientThread client = null;
    private boolean anmelden = true, getList = false, getInGame = false, connected;
    private JLabel label;
    private JTextField tf, tfServer, tfPort, tfName, tfPw, spielText, herausfText;
    private JButton login, logout, spielen, option1, option2, ok, akzeptieren, ablehnen;
    private JTextArea ta, ingameUser;
    private List user;
    private int defaultPort;
    public int amZug;
    private String defaultHost;
    public String selectedUser;
    public String angebotenesSpiel;
    private JPanel southPanel, nameUndPasswort, frameSouth;
    private JFrame spielAuswahl, errorSpielAuswahl, herausforderung;

    public static void main(String[] args)
    {
        ChatClient chatClient = new ChatClient("localhost", 5555);
    }

    public ChatClient(String serverName, int serverPort)
    {
        super("Chat Client");
        createGUI(serverName, serverPort);

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
                connectionFailed("Sending error : " + e.getMessage());
                stop();
            }
        }
    }


    public void message(String msg) throws IOException
    {
        switch (msg)
        {
            case "":
                break;
            case "/userListBeginn":
                getList = true;
                user.removeAll();
                user.add("Zurzeit im Raum: ");
                break;
            case "/userListEnde":
                getList = false;
                break;
            case "/imSpielListBeginn":
                getInGame = true;
                ingameUser.setText("Zurzeit im Spiel: ");
                break;
            case "/imSpielListEnde":
                getInGame = false;
                break;
            case "/offerSpiel":
                selectedUser = client.streamIn.readUTF();
                angebotenesSpiel = client.streamIn.readUTF();
                herausfText.setText(selectedUser + " will mit ihnen " + angebotenesSpiel + " spielen");
                herausforderung.setVisible(true);
                break;
            case "Anmeldung fehlgeschlagen":
                connectionFailed(msg);
                stop();
                break;
            case "Auf Wiedersehen": case "Server wurde geschlossen" :
                append(msg);
                disconnect();
                stop();
                break;
            default:
                if (getList)
                {
                    user.add(msg);
                }
                else
                {
                    if (getInGame)
                    {
                        ingameUser.append(msg);
                    }
                    else
                    {
                        append(msg + "\n");
                    }
                }
                break;
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
            connectionFailed("Error closing");
        }
        client.close();
    }

    //Aufgabenblatt3 Methoden Anfang

    void createGUI(String host, int port)
    {
        defaultPort = port;
        defaultHost = host;

        //alle Textfelder
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);
        tfName = new JTextField("Anonymous");
        tfName.setBackground(Color.WHITE);
        tfPw = new JTextField("Passwort");
        tfPw.setBackground(Color.WHITE);

        //Serveradresse + Portnummer Felder
        southPanel = new JPanel(new GridLayout(3,1));
        JPanel serverUndPort = new JPanel(new GridLayout(1,5, 1, 3));
        serverUndPort.add(new JLabel("Server Address:  "));
        serverUndPort.add(tfServer);
        serverUndPort.add(new JLabel("Port Number:  "));
        serverUndPort.add(tfPort);
        serverUndPort.add(new JLabel(""));
        southPanel.add(serverUndPort);

        label = new JLabel("Unten name und Passwort eingeben", SwingConstants.CENTER);
        southPanel.add(label);

        //Username + Passwort Felder
        nameUndPasswort = new JPanel(new GridLayout(1,4,1,3));
        JLabel username = new JLabel("Username: ");
        JLabel passwort = new JLabel("Passwort: ");
        nameUndPasswort.add(username);
        nameUndPasswort.add(tfName);
        nameUndPasswort.add(passwort);
        nameUndPasswort.add(tfPw);
        southPanel.add(nameUndPasswort);
        add("South", southPanel);

        tf = new JTextField();

        //Chat
        ta = new JTextArea("", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        add("Center", centerPanel);

        //Userlist
        JPanel east = new JPanel(new GridLayout(2,1));
        user = new List();
        user.add("Mit keinem Chatraum verbunden");
        user.addItemListener(this);
        user.addActionListener(this);
        ingameUser = new JTextArea();
        ingameUser.setText("Mit keinem Chatraum verbunden");
        ingameUser.setEditable(false);
        east.add(user);
        east.add(ingameUser);
        add("East", east);

        //Knöpfe
        login = new JButton("Login");
        login.addActionListener(this);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false);
        spielen = new JButton("Spielen");
        spielen.setEnabled(false);
        spielen.addActionListener(this);
        JPanel northPanel = new JPanel(new GridLayout(1,3));
        northPanel.add(login);
        northPanel.add(logout);
        northPanel.add(spielen);
        add("North", northPanel);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tfName.requestFocus();

        spielAuswahl = new JFrame();
        spielAuswahl.addWindowListener(this);
        frameSouth = new JPanel(new GridLayout(1,2));
        spielText = new JTextField();
        spielText.setHorizontalAlignment(SwingConstants.CENTER);
        spielAuswahl.add("Center", spielText);
        option1 = new JButton();
        option1.addActionListener(this);
        option2 = new JButton();
        option2.addActionListener(this);
        frameSouth.add(option1);
        frameSouth.add(option2);
        spielAuswahl.add("South", frameSouth);
        spielAuswahl.setSize(300, 300);
        spielAuswahl.setVisible(false);

        errorSpielAuswahl = new JFrame();
        errorSpielAuswahl.addWindowListener(this);
        ok = new JButton("Ok");
        ok.addActionListener(this);
        JTextField text = new JTextField("Bitte wählen sie einen Spieler in der Liste aus");
        text.setHorizontalAlignment(SwingConstants.CENTER);
        errorSpielAuswahl.add("Center", text);
        errorSpielAuswahl.add("South", ok);
        errorSpielAuswahl.setSize(300,300);
        errorSpielAuswahl.setVisible(false);

        herausforderung = new JFrame();
        herausforderung.addWindowListener(this);
        akzeptieren = new JButton("Akzeptieren");
        akzeptieren.addActionListener(this);
        ablehnen = new JButton("Ablehen");
        ablehnen.addActionListener(this);
        herausfText = new JTextField("");
        herausfText.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel jaNein = new JPanel(new GridLayout(1,2));
        jaNein.add(akzeptieren);
        jaNein.add(ablehnen);
        herausforderung.add("Center", herausfText);
        herausforderung.add("South", jaNein);
        herausforderung.setSize(300, 300);
        herausforderung.setVisible(false);

    }

    void append(String str)
    {
        ta.append(str);
    }

    public void actionPerformed(ActionEvent e)
    {
        Object o = e.getSource();
        if (o == akzeptieren)
        {
            herausforderung.setVisible(false);
            try
            {
                tf.setEditable(false);
                streamOut.writeUTF("/acceptSpiel");
            }
            catch (IOException ex)
            {
                System.out.println(ex.toString());
                ex.printStackTrace();
            }
        }
        if (o == ablehnen)
        {
            herausforderung.setVisible(false);
            try
            {
                streamOut.writeUTF("/denySpiel");
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        if (o == ok)
        {
            errorSpielAuswahl.setVisible(false);
            spielen.setEnabled(true);
        }
        if (o == spielen || o == user)
        {
            tf.setEditable(false);
            spielen.setEnabled(false);
            option1.setText("Vier Gewinnt");
            option2.setText("Chomp");
            if (selectedUser != null && !selectedUser.equals(client.name))
            {
                spielAuswahl.setVisible(true);
                spielText.setText("Was wollen Sie mit " + selectedUser + " spielen?");
            }
            else
            {
                errorSpielAuswahl.setVisible(true);
            }
        }
        if (o == option1)
        {
            angebotenesSpiel = "Vier Gewinnt";
            spielen.setEnabled(false);
            spielAuswahl.setVisible(false);
            try
            {
                streamOut.writeUTF("/startSpiel");
                streamOut.writeUTF("Vier Gewinnt");
                streamOut.writeUTF(selectedUser);
                user.deselect(user.getSelectedIndex());
            }
            catch (IOException ex)
            {
                reset();
                System.out.println(ex.toString());
                ex.printStackTrace();
            }

        }
        if (o == option2)
        {
            angebotenesSpiel = "Chomp";
            spielen.setEnabled(false);
            spielAuswahl.setVisible(false);
            try
            {
                streamOut.writeUTF("/startSpiel");
                streamOut.writeUTF("Chomp");
                streamOut.writeUTF(selectedUser);
                user.deselect(user.getSelectedIndex());
            }
            catch (IOException ex)
            {
                reset();
                ex.printStackTrace();
            }
        }
        if(o == logout)
        {
            try
            {
                if (connected)
                {
                    if (client.imSpiel)
                    {
                        closeGame();
                    }
                    streamOut.writeUTF("/bye");
                }
            }
            catch (IOException ex)
            {
                connectionFailed("Error: " + ex.getMessage());
                return;
            }
            return;
        }
        if(o == tf && connected)
        {
            String msg = tf.getText();
            try
            {
                if (msg != null)
                {
                    streamOut.writeUTF(msg);
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            tf.setText("");
            return;
        }
        if(o == login)
        {
            tf.setEditable(true);
            append("Verbindung wird aufgebaut\n");
            String username = tfName.getText().trim();
            if(username.length() == 0)
                return;
            String passwort = tfPw.getText().trim();
            if (passwort.length() == 0)
                return;
            String server = tfServer.getText().trim();
            if(server.length() == 0)
                return;
            String portNumber = tfPort.getText().trim();
            if(portNumber.length() == 0)
                return;
            ta.setText("");

            int port;
            try
            {
                port = Integer.parseInt(portNumber);
                socket = new Socket(server, port);
                append("Connected: " + socket + "\n");
                start();
            }
            catch(UnknownHostException ex)
            {
                connectionFailed("Host unknown: " + ex.getMessage());
                return;
            }
            catch(IOException ex)
            {
                connectionFailed("Unexpected exception: " + ex.getMessage());
                return;
            }
            catch (Exception ex)
            {
                connectionFailed("Ungültiger Port: " + ex.getMessage());
                return;
            }
            client.name = username;
            tf = new JTextField("Anonymous");
            tf.setBackground(Color.WHITE);
            southPanel.remove(nameUndPasswort);
            southPanel.add(tf);
            tf.setText("");
            label.setText("Als " + client.name + " angemeldet");
            login.setEnabled(false);
            logout.setEnabled(true);
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            tf.addActionListener(this);
            connected = true;
            try
            {
                streamOut.writeUTF(username);
                streamOut.writeUTF(passwort);
            }
            catch (IOException ex)
            {
                connectionFailed("Error: " + ex.getMessage());
                return;
            }
        }
    }

    void closeGame()
    {
        if (!client.spiel.gewonnen && !client.spiel.unentschieden)
        {
            try
            {
                streamOut.writeInt(-2);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        client.imSpiel = false;
        client.spiel = null;
        spielen.setEnabled(false);
        selectedUser = null;
        tf.setEditable(true);
        angebotenesSpiel = null;
    }

    void reset()
    {
        login.setEnabled(true);
        logout.setEnabled(false);
        spielen.setEnabled(false);
        if (client.spiel != null)
        {
            client.spiel.frame.setVisible(false);
            client.spiel = null;
        }
        client.imSpiel = false;
        user.deselect(user.getSelectedIndex());
        tf.setText("");
        tf.setEditable(true);
        southPanel.remove(tf);
        southPanel.add(nameUndPasswort);
        label.setText("Unten Name und Passwort eingeben");
        tfName.setText("Anonymous");
        tfPw.setText("Passwort");
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        user.removeAll();
        user.add("Mit keinem Chatraum verbunden");
        ingameUser.setText("Mit keinem Chatraum verbunden");
        tfServer.setEditable(true);
        tfPort.setEditable(true);
        tf.removeActionListener(this);
        connected = false;
    }

    void disconnect()
    {
        reset();
    }

    void connectionFailed(String msg)
    {
        reset();
        append(msg);
        if (connected)
        {
            append("\nDisconnecting from the Server");
        }
    }

    //Aufgabenblatt3 Methoden Ende

    public void itemStateChanged(ItemEvent e)
    {
        int o = (int) e.getItem();
        if (o == 0 || user.getItem(o).equals(client.name))
        {
            selectedUser = null;
            user.deselect(o);
            spielen.setEnabled(false);
        }
        else
        {
            selectedUser = user.getItem(o);
            spielen.setEnabled(true);
        }
    }

    public void windowClosing(WindowEvent e)
    {
        selectedUser = null;
        user.deselect(user.getSelectedIndex());
        spielen.setEnabled(false);
    }

    public void windowOpened(WindowEvent e) { }
    public void windowClosed(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
}

class ChatClientThread extends Thread
{
    public Spiel spiel;
    private Socket socket;
    private ChatClient client;
    public DataInputStream streamIn = null;
    public String name;
    boolean imSpiel = false;

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
            client.connectionFailed("Error getting input stream " + e.getMessage());
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
            client.connectionFailed("Error closing input stream: " + e.getMessage());
            client.stop();
        }
    }
    public void run()
    {
        while (true)
        {
            try
            {
                if (!imSpiel)
                {
                    String msg = streamIn.readUTF();
                    if (msg.equals("/begin"))
                    {
                        imSpiel = true;
                        client.amZug = streamIn.readInt();
                        if (client.angebotenesSpiel.equals("Vier Gewinnt"))
                        {
                            spiel = new VierGewinnt(name, client.selectedUser, client.amZug, 7, 6, client);
                        }
                        else
                        {
                            spiel = new Chomp(name, client.selectedUser, client.amZug, 5, 5, client);
                        }
                    }
                    else
                    {
                        client.message(msg);
                    }
                    if (msg.equals("Anmeldung fehlgeschlagen") || msg.equals("Auf Wiedersehen"))
                    {
                        break;
                    }
                }
                else
                {
                    int operating = streamIn.readInt();
                    if (operating == -2)
                    {
                        String msg = streamIn.readUTF();
                        client.message(msg);
                    }
                    if (operating == -1)
                    {
                        imSpiel = false;
                        if(spiel != null && !spiel.gewonnen && !spiel.unentschieden)
                        {
                            spiel.label.setText(spiel.loser + " hat aufgegeben");
                        }
                    }
                    if (operating == 0)
                    {
                        imSpiel = false;
                        spiel.sendFeld(-1,-1,-1);
                    }
                    if (spiel != null && spiel.amZug == 1 && operating >= 0)
                    {
                        int status = streamIn.readInt();
                        if (status == 1 || status == 2)
                        {
                            imSpiel = false;
                        }
                        int x = streamIn.readInt();
                        int y = streamIn.readInt();
                        spiel.Spielzug(x, y, status, false);
                    }
                }
            }
            catch(IOException e)
            {
                client.connectionFailed("Listening error " +  e.getMessage());
                client.stop();
                break;
            }
        }
    }
}

abstract class Spiel extends JFrame implements MouseListener, WindowListener
{
    int höheSquare, breiteSquare, amZug;
    boolean gewonnen = false, unentschieden = false;
    String loser, winner;
    Spieler[] spieler = new Spieler[2];
    Spielfeld feld;
    JFrame frame = new JFrame("Spiel");
    JLabel label = new JLabel();
    ChatClient chatClient;

    Spiel(String name1, String name2, int amZug, int x, int y, ChatClient client)
    {
        chatClient = client;
        spieler[0] = new Spieler(name1, Color.GREEN);
        spieler[1] = new Spieler(name2, Color.RED);
        breiteSquare = getSize().width / x;
        höheSquare = getSize().height / y;
        this.amZug = amZug;
    }
    abstract void Spielzug(int x, int y, int status, boolean senden) throws IOException;
    void setLabel(int i)
    {
        if (i == 1)
        {
            label.setText(spieler[1].name + " ist dran (rot)");
        }
        else
        {
            label.setText("Sie sind dran (grün)");
        }
    }

    public void sendFeld(int x, int y, int status)
    {
        try
        {
            chatClient.streamOut.writeInt(status);
            chatClient.streamOut.flush();
            chatClient.streamOut.writeInt(x);
            chatClient.streamOut.flush();
            chatClient.streamOut.writeInt(y);
            chatClient.streamOut.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}

class Spieler
{
    String name;
    Color farbe;
    Spieler(String name, Color farbe)
    {
        this.name = name;
        this.farbe = farbe;
    }
}

abstract class Spielfeld extends Panel
{
    int reihen, spalten;
    Color[][] status;
    Spielfeld(int x, int y)
    {
        this.spalten = x;
        this.reihen = y;
        status = new Color[x][y];
    }
}

interface Protokolierbar
{
    Speicher stack = new Speicher();
    void SpielzugHinzu(Spieler spieler);
    void SpielzugEntfern();
}

class Knoten
{
    Knoten next;
    Color[][] status;
    Spieler spieler;
    Knoten (Color[][] status, Knoten next, Spieler spieler)
    {
        this.status = status;
        this.next = next;
        this.spieler = spieler;
    }
}

class Speicher
{
    Knoten top;
    Speicher()
    {
        top = null;
    }
    void push(Color[][] status, Spieler spieler)
    {
        top = new Knoten (status, top, spieler);
    }
    Color[][] pop()
    {
        if (top == null)
        {
            return null;
        }
        Color[][] stat = top.status;
        top = top.next;
        return stat;
    }
    Color[][] peek()
    {
        return top.status;
    }
}
// Ende Aufgabe 1

// Anfang Aufgabe 2

class VierGewinnt extends Spiel implements Protokolierbar
{
    VierGewinnt(String name1, String name2, int amZug, int x, int y, ChatClient client)
    {
        super(name1, name2, amZug, x, y, client);
        Speicher stack = new Speicher();
        feld = new Spielfeld(x, y)
        {
            public void paint(Graphics g)
            {
                höheSquare = getSize().height / feld.reihen;
                breiteSquare = getSize().width / feld.spalten;
                farbe(g);
            }
        };
        feld.spalten = x;
        feld.reihen = y;
        feld.status = new Color[x][y];
        feld.addMouseListener(this);
        for (int i = 0; i < feld.spalten; i++)
        {
            for (int j = 0; j < feld.reihen; j++)
            {
                feld.status[i][j] = Color.WHITE;
            }
        }
        createGUI(frame);
        setLabel(amZug);
    }



    public void Spielzug(int x, int y, int status, boolean senden)
    {
        winner = spieler[amZug].name;
        if (gewonnen || unentschieden)
        {
            return;
        }
        feld.status[x][y] = spieler[amZug].farbe;
        SpielzugHinzu(spieler[amZug]);
        amZug = (amZug + 1) % 2;
        loser = spieler[amZug].name;
        setLabel(amZug);
        if (status == 1)
        {
            unentschieden = true;
            chatClient.client.imSpiel = false;
            label.setText("Unentschieden");
        }
        if (status == 2)
        {
            chatClient.client.imSpiel = false;
            gewonnen = true;
            label.setText(winner + " hat gewonnen");
        }
        if (senden)
        {
            sendFeld(x, y, status);
        }
        farbe(feld.getGraphics());

    }
    public void SpielzugHinzu(Spieler spieler)
    {
        stack.push(feld.status, spieler);
    }
    public void SpielzugEntfern()
    {
        stack.pop();
        feld.status = stack.peek();
        farbe(feld.getGraphics());
    }

    private void createGUI(Frame frame)
    {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.add(feld);
        frame.add("Center", centerPanel);
        frame.add("South", label);
        frame.setSize(600, 600);
        frame.setVisible(true);
        frame.addWindowListener(this);
    }

    private void farbe(Graphics g)
    {
        for (int x = 0; x < feld.spalten; x++)
        {
            for (int y = 0; y < feld.reihen; y++)
            {

                if (!feld.status[x][y].equals(Color.WHITE))
                {
                    g.setColor(feld.status[x][y]);
                    g.fillOval(x * breiteSquare, y * höheSquare, breiteSquare, höheSquare);
                    g.setColor(Color.BLACK);
                }
                g.drawRect(x * breiteSquare, y * höheSquare, breiteSquare, höheSquare);
                g.drawOval(x * breiteSquare, y * höheSquare, breiteSquare, höheSquare);
            }
        }
    }

    private int dropChip4G(Spielfeld feld, int x)
    {
        for (int i = feld.reihen - 1; i >= 0; i--)
        {
            if (feld.status[x][i].equals(Color.WHITE))
            {
                return i;
            }
        }
        return -1;
    }

    private boolean checkWin4G(Spielfeld feld, Spieler spieler, int x, int y)
    {
        Color original = feld.status[x][y];
        feld.status[x][y] = spieler.farbe;
        for (int i = 0; i < feld.spalten - 3; i++)     //links nach rechts
        {
            for (int j = 0; j < feld.reihen; j++)
            {
                if (feld.status[i][j].equals(spieler.farbe) && feld.status[i][j].equals(feld.status[i+1][j]) && feld.status[i][j].equals(feld.status[i+2][j]) && feld.status[i][j].equals(feld.status[i+3][j]))
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.spalten; i++)       //oben nach unten
        {
            for (int j = 0; j < feld.reihen - 3; j++)
            {
                if (feld.status[i][j].equals(spieler.farbe) && feld.status[i][j].equals(feld.status[i][j+1]) && feld.status[i][j].equals(feld.status[i][j+2]) && feld.status[i][j].equals(feld.status[i][j+3]))
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
        }
        for (int i = 0; i < feld.spalten - 3; i++)       //obenlinks nach untenrechts
        {
            for (int j = 0; j < feld.reihen - 3; j++)
            {
                if (feld.status[i][j].equals(spieler.farbe) && feld.status[i][j].equals(feld.status[i+1][j+1]) && feld.status[i][j].equals(feld.status[i+2][j+2]) && feld.status[i][j].equals(feld.status[i+3][j+3]))
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
        }
        for (int i = feld.spalten - 1; i > 2; i--)         //obenrechts nach untenlinks
        {
            for (int j = 0; j < feld.reihen - 3; j++)
            {
                if (feld.status[i][j].equals(spieler.farbe) && feld.status[i][j].equals(feld.status[i-1][j+1]) && feld.status[i][j].equals(feld.status[i-2][j+2]) && feld.status[i][j].equals(feld.status[i-3][j+3]))
                {
                    feld.status[x][y] = original;
                    return true;
                }
            }
        }
        feld.status[x][y] = original;
        return false;
    }
    private boolean checkUnentschieden4G(Spielfeld feld)
    {
        for (int x = 0; x < feld.spalten; x++)
        {
            for (int y = 0; y < feld.reihen; y++)
            {
                if (feld.status[x][y].equals(Color.WHITE))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public void mouseClicked(MouseEvent e)
    {
        if ((!gewonnen || !unentschieden) && amZug == 0)
        {
            int status = 0;
            int x = e.getX() / breiteSquare;
            if (x >= feld.spalten)
            {
                x = feld.spalten - 1;
            }
            int y = dropChip4G(feld, x);
            if (y > 0)
            {
                if (checkWin4G(feld, spieler[amZug], x, y))
                {
                    status = 2;
                }
                if (checkUnentschieden4G(feld))
                {
                    status = 1;
                }
                Spielzug(x, y, status, true);
            }

        }
    }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }

    public void windowOpened(WindowEvent e) { }
    public void windowClosing(WindowEvent e)
    {
        chatClient.closeGame();
    }
    public void windowClosed(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
}

class Chomp extends Spiel implements Protokolierbar
{
    public Chomp(String name1, String name2, int amZug, int x, int y, ChatClient client)
    {
        super(name1, name2, amZug, x, y, client);
        Speicher stack = new Speicher();
        feld = new Spielfeld(x, y)
        {
            public void paint(Graphics g)
            {
                höheSquare = getSize().height / feld.reihen;
                breiteSquare = getSize().width / feld.spalten;
                farbe(g);
            }
        };
        feld.spalten = x;
        feld.reihen = y;
        feld.status = new Color[x][y];
        feld.addMouseListener(this);
        for (int i = 0; i < feld.spalten; i++)
        {
            for (int j = 0; j < feld.reihen; j++)
            {
                feld.status[i][j] = Color.WHITE;
            }
        }
        feld.status[0][0] = Color.GRAY;
        createGUI(frame);
        setLabel(amZug);
    }

    private void createGUI(Frame frame)
    {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.add(feld);
        frame.add("Center", centerPanel);
        frame.add("South", label);
        frame.setSize(600, 600);
        frame.setVisible(true);
        frame.addWindowListener(this);
    }

    public void Spielzug(int x, int y, int status, boolean senden)
    {
        if (feld.status[x][y].equals(Color.WHITE))
        {
            for (int i = x; i < feld.spalten; i++)
            {
                for (int j = y; j < feld.reihen; j++)
                {
                    if (feld.status[i][j].equals(Color.WHITE))
                    {
                        feld.status[i][j] = spieler[amZug].farbe;
                    }
                }

            }
        }
        else
        {
            return;
        }
        winner = spieler[amZug].name;
        SpielzugHinzu(spieler[amZug]);
        amZug = (amZug + 1) % 2;
        loser = spieler[amZug].name;
        setLabel(amZug);
        if (status == 1)
        {
            unentschieden = true;
            chatClient.client.imSpiel = false;
            label.setText("Unentschieden");
        }
        if (status == 2)
        {
            chatClient.client.imSpiel = false;
            gewonnen = true;
            label.setText(winner + " hat gewonnen");
        }
        if (senden)
        {
            sendFeld(x, y, status);
        }
        farbe(feld.getGraphics());
    }
    public void SpielzugHinzu(Spieler spieler)
    {
        stack.push(feld.status, spieler);
    }
    public void SpielzugEntfern()
    {
        stack.pop();
        feld.status = stack.peek();
        farbe(feld.getGraphics());
    }
    private boolean CheckWinChomp(Spielfeld feld, int x, int y)
    {
        Color original = feld.status[x][y];
        feld.status[x][y] = spieler[amZug].farbe;
        if (!feld.status[1][0].equals(Color.WHITE) && !feld.status[0][1].equals(Color.WHITE))
        {
            feld.status[x][y] = original;
            return true;
        }
        feld.status[x][y] = original;
        return false;
    }

    private void farbe(Graphics g)
    {
        for (int i = 0; i < feld.spalten; i++)
        {
            for (int k = 0; k < feld.reihen; k++)
            {
                if (!feld.status[i][k].equals(Color.WHITE))
                {
                    g.setColor(feld.status[i][k]);
                    g.fillRect(i * breiteSquare, k * höheSquare, breiteSquare, höheSquare);
                    g.setColor(Color.BLACK);
                }
                g.drawRect(i * breiteSquare, k * höheSquare, breiteSquare, höheSquare);
                if (k == 0 && i == 0 )
                {
                    g.drawLine(0, 0, breiteSquare, höheSquare);
                    g.drawLine(0, höheSquare, breiteSquare, 0);
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ((!gewonnen || !unentschieden) && amZug == 0)
        {
            int status = 0;
            int x = e.getX() / breiteSquare;
            int y = e.getY() / höheSquare;
            if (x >= feld.spalten)
            {
                x = feld.spalten - 1;
            }
            if (y >= feld.reihen)
            {
                y = feld.reihen - 1;
            }
            if (CheckWinChomp(feld, x, y))
            {
                status = 2;
            }
            Spielzug(x, y, status, true);
        }
    }

    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }

    public void windowOpened(WindowEvent e) { }
    public void windowClosing(WindowEvent e)
    {
        chatClient.closeGame();
    }
    public void windowClosed(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
}