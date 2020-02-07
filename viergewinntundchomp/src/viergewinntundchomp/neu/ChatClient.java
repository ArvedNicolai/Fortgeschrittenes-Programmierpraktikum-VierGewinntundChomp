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
    DataOutputStream streamOut = null;
    ChatClientThread client = null;

    private int defaultPort, spielHöhe, spielBreite;
    public String defaultHost, angebotenesSpiel, selectedUser;
    private boolean anmelden = true, getList = false, getInGame = false, connected;

    private JFrame spielAuswahl, errorSpielAuswahl, herausforderung;
    private JPanel southPanel;
    private JPanel nameUndPasswort;
    private JTextField tf, tfServer, tfPort, tfName, tfPw, spielText, herausfText, höhe, breite;
    private JTextArea ta, ingameUser;
    private JButton login, logout, spielen, option1, option2, ok, akzeptieren, ablehnen, senden;
    private JLabel label;
    private List user;

    public static void main(String[] args)
    {
        ChatClient chatClient = new ChatClient("localhost", 5555);
    }

    //Konstruktor
    private ChatClient(String serverName, int serverPort)
    {
        super("Chat Client");
        createGUI(serverName, serverPort);
    }
    
    //ChatClient läuft
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

    //ChatClient startet
    private void start() throws IOException
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

    //ChatClient wird gestoppt
    void stop()
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

    void message(String msg) throws IOException
    {
        if (msg == null)
        {
            return;
        }
        switch (msg)
        {
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
                logout.setEnabled(false);
                String gegner = client.streamIn.readUTF();
                int y = client.streamIn.readInt();
                int x = client.streamIn.readInt();
                String spiel = client.streamIn.readUTF();
                if (!client.imSpiel)
                {
                    selectedUser = gegner;
                    spielHöhe = y;
                    spielBreite = x;
                    angebotenesSpiel = spiel;
                    herausfText.setText(selectedUser + " will mit ihnen " + angebotenesSpiel + " spielen");
                    herausforderung.setVisible(true);
                }
                else
                {
                    try
                    {
                        streamOut.writeUTF("/declineSpiel");
                    }
                    catch (IOException ex)
                    {
                        append("Error declining: " + ex.getMessage());
                    }
                }
                break;

            case "/begin":
                logout.setEnabled(true);
                client.imSpiel = true;
                int amZug = client.streamIn.readInt();
                if (angebotenesSpiel.equals("Vier Gewinnt"))
                {
                    client.spiel = new VierGewinnt(client.name, selectedUser, amZug, spielBreite, spielHöhe, this);
                }
                else
                {
                    client.spiel = new Chomp(client.name, selectedUser, amZug, spielBreite, spielHöhe, this);
                }
                break;

            case "/declineSpiel":
                append(selectedUser + " hat abgelehnt");
                closeGame();
                break;

            case "/busy":
                append(selectedUser + " ist im Moement nicht verfügbar\n");
                closeGame();
                break;

            case "Anmeldung fehlgeschlagen":
                connectionFailed(msg);
                stop();
                break;

            case "Auf Wiedersehen": case "Server wurde geschlossen":
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

    //Aufgabenblatt3 Methoden Anfang

    private void createGUI(String host, int port)
    {
        defaultPort = port;
        defaultHost = host;

        //alle Textfelder
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);
        tfName = new JTextField("Anonymous");
        tfName.addActionListener(this);
        tfName.setBackground(Color.WHITE);
        tfPw = new JTextField("Passwort");
        tfPw.addActionListener(this);
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

        spielAuswahl = new JFrame();
        spielAuswahl.addWindowListener(this);
        spielText = new JTextField();
        spielText.setEditable(false);
        spielText.setHorizontalAlignment(SwingConstants.CENTER);
        spielAuswahl.add("Center", spielText);
        option1 = new JButton();
        option1.addActionListener(this);
        option2 = new JButton();
        option2.addActionListener(this);
        spielAuswahl.setSize(450, 300);
        spielAuswahl.setVisible(false);
        senden = new JButton("Anfrage senden");
        senden.addActionListener(this);

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

        //Felder, die erscheinen, wenn man zum Spiel herausgefordert wird
        herausforderung = new JFrame();
        herausforderung.addWindowListener(this);
        herausfText = new JTextField("");
        herausfText.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel jaNein = new JPanel(new GridLayout(1,2));
        akzeptieren = new JButton("Akzeptieren");
        akzeptieren.addActionListener(this);
        ablehnen = new JButton("Ablehnen");
        ablehnen.addActionListener(this);
        jaNein.add(akzeptieren);
        jaNein.add(ablehnen);
        herausforderung.add("Center", herausfText);
        herausforderung.add("South", jaNein);
        herausforderung.setSize(300, 300);
        herausforderung.setVisible(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tfName.requestFocus();
    }

    private void append(String str)
    {
        ta.append(str);
    }

    private void startSpiel() throws IOException
    {
        streamOut.writeUTF("/startSpiel");
        streamOut.writeUTF(angebotenesSpiel);
        streamOut.writeInt(spielHöhe);
        streamOut.writeInt(spielBreite);
        streamOut.writeUTF(selectedUser);
    }

    private void changeToGrößeAusw()
    {
        spielText.setText("Wollen Sie die Standardgröße benutzen oder eigene Größen benutzen");
        option1.setText("Standard (6x7)");
        option2.setText("Benutzerdefiniert");
        JPanel vierGSouth = new JPanel(new GridLayout(1,2));
        vierGSouth.add(option1);
        vierGSouth.add(option2);

        spielAuswahl.getContentPane().removeAll();
        spielAuswahl.getContentPane().repaint();
        spielAuswahl.add("Center", spielText);
        spielAuswahl.add("South", vierGSouth);
        spielAuswahl.validate();
    }

    //Auswahl der Spielfeldgröße
    private void changeToCustomGröße()
    {
        JPanel south = new JPanel(new GridLayout(2,1));
        JLabel labelHöhe = new JLabel("Höhe: ");
        JLabel labelBreite = new JLabel("Breite: ");
        labelHöhe.setHorizontalAlignment(SwingConstants.CENTER);
        labelBreite.setHorizontalAlignment(SwingConstants.CENTER);
        höhe = new JTextField();
        breite = new JTextField();
        höhe.addActionListener(this);
        breite.addActionListener(this);

        JPanel tempPanel = new JPanel(new GridLayout(1,4));
        tempPanel.add(labelHöhe);
        tempPanel.add(höhe);
        tempPanel.add(labelBreite);
        tempPanel.add(breite);

        south.add(tempPanel);
        south.add(senden);

        if (angebotenesSpiel.equals("Vier Gewinnt"))
        {
            spielText.setText("Wählen Sie die Größe des Spielfeldes aus\n (mindestens 5x5)");
        }
        else	//Wenn Chomp gespielt wird. 
        {
            spielText.setText("Wählen Sie die Größe des Spielfeldes aus\n (mindestens 3x3)");
        }
        spielAuswahl.getContentPane().removeAll();
        spielAuswahl.getContentPane().repaint();
        spielAuswahl.add("Center", spielText);
        spielAuswahl.add("South", south);
        spielAuswahl.validate();
    }

    private void changeBack()
    {
        option1.setText("Vier Gewinnt");
        option2.setText("Chomp");
        spielAuswahl.getContentPane().removeAll();
        spielAuswahl.getContentPane().repaint();

        JPanel frameSouth = new JPanel(new GridLayout(1, 2));
        frameSouth.add(option1);
        frameSouth.add(option2);

        spielAuswahl.add("Center", spielText);
        spielAuswahl.add("South", frameSouth);
        spielAuswahl.validate();
        spielAuswahl.setVisible(false);
    }

    void closeGame()
    {
        spielen.setEnabled(false);
        herausforderung.setVisible(false);
        spielAuswahl.setVisible(false);
        tf.setEditable(true);
        logout.setEnabled(true);
        spielBreite = 0;
        spielHöhe = 0;
        angebotenesSpiel = null;
        if (selectedUser != null)
        {
            selectedUser = null;
        }
        if (client != null)
        {
            client.imSpiel = false;
            if (client.spiel != null)
            {
                client.spiel.frame.setVisible(false);
                if (!client.spiel.gewonnen && !client.spiel.unentschieden)
                {
                    try
                    {
                        streamOut.writeUTF("end");
                    }
                    catch (IOException e)
                    {
                        append("Error: " + e.getMessage());
                    }
                }
            }
            client.spiel = null;
        }
    }

    private void reset()
    {
        login.setEnabled(true);
        logout.setEnabled(false);
        spielen.setEnabled(false);
        if (client != null)
        {
            if (client.spiel != null)
            {
                client.spiel.frame.setVisible(false);
                client.spiel = null;
            }
            client.imSpiel = false;
        }
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
        client.running = false;
    }

    private void disconnect()
    {
        closeGame();
        reset();
    }

    void connectionFailed(String msg)
    {
        closeGame();
        reset();
        append(msg);
        if (connected)
        {
            append("\nDisconnecting from the Server");
        }
    }

    //Aufgabenblatt3 Methoden Ende

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
                append("Error accepting: " + ex.getMessage());
            }
        }
        if (o == ablehnen)
        {
            closeGame();
            try
            {
                streamOut.writeUTF("/declineSpiel");
            }
            catch (IOException ex)
            {
                append("Error declining: " + ex.getMessage());
            }
        }
        if (o == ok)
        {
            errorSpielAuswahl.setVisible(false);
            spielen.setEnabled(true);
        }
        if (o == spielen || o == user)
        {
            changeBack();
            tf.setEditable(false);
            logout.setEnabled(false);
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
            spielen.setEnabled(false);
            user.deselect(user.getSelectedIndex());
            if (option1.getText().equals("Vier Gewinnt"))
            {
                angebotenesSpiel = "Vier Gewinnt";
                changeToGrößeAusw();
            }
            else
            {
                spielHöhe = 6;
                spielBreite = 7;
                changeBack();
                try
                {
                    startSpiel();
                }
                catch (IOException ex)
                {
                    append("Error starting game: " + ex.getMessage());
                }
            }
        }
        if (o == option2)
        {
            spielen.setEnabled(false);
            user.deselect(user.getSelectedIndex());
            if (option2.getText().equals("Chomp"))
            {
                angebotenesSpiel = "Chomp";
            }
            changeToCustomGröße();
        }
        if(o == höhe || o == breite || o == senden)
        {
            int x, y;
            if (höhe.getText() != null && breite.getText() != null)
            {
                x = Integer.parseInt(höhe.getText());
                y = Integer.parseInt(breite.getText());
                if ((angebotenesSpiel.equals("Vier Gewinnt") && x > 4 && y > 4) || (angebotenesSpiel.equals("Chomp") && x > 2 && y > 2))
                {
                    spielHöhe = x;
                    spielBreite = y;
                    changeBack();
                    try
                    {
                        startSpiel();
                    }
                    catch (IOException ex)
                    {
                        append("Error Feldgröße: " + ex.getMessage());
                    }
                }
                else
                {
                    append("Das Feld ist zu klein\n");
                }
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
                append("Error: " + ex.getMessage());
            }
            tf.setText("");
            return;
        }
        if(o == login || o == tfName || o == tfPw)
        {
            tf.setEditable(true);
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
                append("Verbindung wird aufgebaut\n");
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
            }
        }
    }

    public void itemStateChanged(ItemEvent e)
    {
        int o = (int) e.getItem();
        if (o == 0 || user.getItem(o).equals(client.name) || client.imSpiel || client.spiel != null)
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
        if (e.getSource() == spielAuswahl)
        {
            changeBack();
            closeGame();
            spielAuswahl.dispose();
        }
        if (e.getSource() == herausforderung)
        {
            herausforderung.dispose();
            try
            {
                streamOut.writeUTF("/declineSpiel");
            }
            catch (IOException ex)
            {
                append("Error: " + ex.getMessage());
            }
        }
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
    public boolean imSpiel = false, running = true;

    ChatClientThread(ChatClient client, Socket socket)
    {
        this.client   = client;
        this.socket   = socket;
        open();
        start();
    }
    private void open()
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
    void close()
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
        while (running)
        {
            try
            {
                String msg = streamIn.readUTF();
                if (!imSpiel)
                {
                    client.message(msg);
                }
                else
                {
                    spielZüge(msg);
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

    private void spielZüge(String message) throws IOException
    {
        switch (message)
        {
            case "message":
                String msg = streamIn.readUTF();
                client.message(msg);
                break;
            case "close":
                if(spiel != null && !spiel.gewonnen && !spiel.unentschieden)
                {	//Ausgabe, wenn ein Spieler, dass Spiel vorzeitig beendet. 
                    spiel.label.setText(client.selectedUser + " hat aufgegeben");
                    spiel.gewonnen = true;
                }
                imSpiel = false;
                break;
            case "end":
                imSpiel = false;
                client.streamOut.writeUTF("close");
                String status = streamIn.readUTF();
                int x = streamIn.readInt();
                int y = streamIn.readInt();
                spiel.Spielzug(x, y, status, false);
                break;
            case "nothing":
                if (spiel != null && spiel.amZug == 1)
                {

                    status = streamIn.readUTF();
                    x = streamIn.readInt();
                    y = streamIn.readInt();
                    spiel.Spielzug(x, y, status, false);
                    if (status.equals("win") || status.equals("draw"))
                    {
                        imSpiel = false;
                    }
                }
                break;
        }
    }
}

//Aufgabenblatt 1
abstract class Spiel extends JFrame implements MouseListener, WindowListener
{
    ChatClient chatClient;

    int höheSquare, breiteSquare, amZug;
    boolean gewonnen = false, unentschieden = false;
    String loser, winner;

    Spieler[] spieler = new Spieler[2];
    Spielfeld feld;

    JFrame frame = new JFrame("Spiel");
    JLabel label = new JLabel();

    Spiel(String name1, String name2, int amZug, int x, int y, ChatClient client)
    {
        chatClient = client;
        spieler[0] = new Spieler(name1, Color.GREEN);
        spieler[1] = new Spieler(name2, Color.RED);
        breiteSquare = getSize().width / x;
        höheSquare = getSize().height / y;
        this.amZug = amZug;
    }
    abstract void Spielzug(int x, int y, String status, boolean senden) throws IOException;

    void sendFeld(int x, int y, String status)
    {
        try
        {
            chatClient.streamOut.writeUTF(status);
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
    private Spieler spieler;
    Knoten (Color[][] status, Knoten next, Spieler spieler)
    {
        this.status = status;
        this.next = next;
        this.spieler = spieler;
    }
}

class Speicher
{
    private Knoten top;
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

    public void Spielzug(int x, int y, String status, boolean senden)
    {
        if (!gewonnen && !unentschieden)
        {
            winner = spieler[amZug].name;
        }
        feld.status[x][y] = spieler[amZug].farbe;
        SpielzugHinzu(spieler[amZug]);
        amZug = (amZug + 1) % 2;
        loser = spieler[amZug].name;
        setLabel(amZug);
        if (status.equals("draw")) // 2
        {
            unentschieden = true;
            chatClient.client.imSpiel = false;
            label.setText("Unentschieden");
        }
        if (status.equals("win")) // 1
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

    public void mouseClicked(MouseEvent e)
    {
        String status = "nothing";
        if (!gewonnen && !unentschieden && amZug == 0)
        {
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
                    status = "win";
                }
                if (checkUnentschieden4G(feld))
                {
                    status = "draw";
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

//Klasse Chomp aus Aufabenblatt 1
class Chomp extends Spiel implements Protokolierbar
{
    Chomp(String name1, String name2, int amZug, int x, int y, ChatClient client)
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

    //Graphische Benutzeroberfläche
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

    public void Spielzug(int x, int y, String status, boolean senden)
    {
        if (!gewonnen && !unentschieden)
        {
            winner = spieler[amZug].name;
        }
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
        SpielzugHinzu(spieler[amZug]);
        amZug = (amZug + 1) % 2;
        setLabel(amZug);
        if (status.equals("draw"))
        {
            unentschieden = true;
            chatClient.client.imSpiel = false;
            label.setText("Unentschieden");
        }
        if (status.equals("win"))
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

    public void mouseClicked(MouseEvent e)
    {
        if (!gewonnen && !unentschieden && amZug == 0)
        {
            String status = "nothing";
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
                status = "win";
            }
            Spielzug(x, y, status, true);
        }
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e)
    {
        chatClient.closeGame();
    }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
}
