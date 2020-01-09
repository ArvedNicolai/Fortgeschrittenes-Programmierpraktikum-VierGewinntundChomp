import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class ChatClient extends JFrame implements Runnable, ActionListener
{
    private Socket socket = null;
    private Thread thread = null;
    private BufferedReader console = null;
    public DataOutputStream streamOut = null;
    private ChatClientThread client = null;
    private boolean anmelden = true, getList = false, connected;
    private JLabel label;
    private JTextField tf, tfServer, tfPort, tfName, tfPw;
    private JButton login, logout;
    private JTextArea ta, user;
    private int defaultPort;
    private String defaultHost;
    private JPanel southPanel, nameUndPasswort;

    public static void main(String[] args)
    {
        ChatClient chatClient = new ChatClient("localhost", 5555);

    }

    public ChatClient(String serverName, int serverPort)
    {
        super("Chat Client");
        createUI(serverName, serverPort);

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
    public void message(String msg)
    {
        if (msg.equals("userListBeginn"))
        {
            getList = true;
            return;
        }
        if (msg.equals("userListEnde"))
        {
            getList = false;
            return;
        }
        if (!getList)
        {
            append(msg + "\n");
            if (msg.equals("Anmeldung fehlgeschlagen"))
            {
                connectionFailed(msg);
                stop();
            }
            if (msg.equals("Auf Wiedersehen"))
            {
                disconnect();
                stop();
            }
        }
        else
        {
            user.setText(msg);
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

    void createUI(String host, int port)
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
        JPanel east = new JPanel();
        user = new JTextArea("Mit keinem Chatraum verbunden");
        user.setEditable(false);
        east.add(user);
        add("East", user);

        //Knöpfe
        login = new JButton("Login");
        login.addActionListener(this);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false);
        JPanel northPanel = new JPanel();
        northPanel.add(login);
        northPanel.add(logout);
        add("North", northPanel);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tfName.requestFocus();
    }

    void append(String str)
    {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }

    public void actionPerformed(ActionEvent e)
    {
        Object o = e.getSource();
        if(o == logout)
        {
            try
            {
                if (connected)
                {
                    streamOut.writeUTF(".bye");
                }
            }
            catch (IOException ex)
            {
                connectionFailed("Error: " + ex.getMessage());
                return;
            }
            return;
        }
        if(connected)
        {
            try
            {
                streamOut.writeUTF(tf.getText());
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

            int port = 0;
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

            tf = new JTextField("Anonymous");
            tf.setBackground(Color.WHITE);
            southPanel.remove(nameUndPasswort);
            southPanel.add(tf);
            tf.setText("");
            label.setText("Als " + username + " angemeldet");
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
            login.setEnabled(false);
            logout.setEnabled(true);
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            tf.addActionListener(this);
        }
    }

    void disconnect()
    {
        login.setEnabled(true);
        logout.setEnabled(false);
        tf.setText("");
        southPanel.remove(tf);
        southPanel.add(nameUndPasswort);
        label.setText("Unten Name und Passwort eingeben");
        tfName.setText("Anonymous");
        tfPw.setText("Passwort");
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        user.setText("Mit keinem Chatraum verbunden");
        tfServer.setEditable(true);
        tfPort.setEditable(true);
        tf.removeActionListener(this);
        connected = false;
    }

    void connectionFailed(String msg)
    {
        login.setEnabled(true);
        logout.setEnabled(false);
        southPanel.remove(tf);
        southPanel.add(nameUndPasswort);
        label.setText("Unten Name und Passwort eingeben");
        tfName.setText("Anonymous");
        tfPw.setText("Passwort");
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        user.setText("Mit keinem Chatraum verbunden");
        tfServer.setEditable(true);
        tfPort.setEditable(true);
        tf.removeActionListener(this);
        append(msg);
        if (connected)
        {
            append("\nDisconnecting from the Server");
        }
        connected = false;
    }

    //Aufgabenblatt3 Methoden Ende
}

class ChatClientThread extends Thread
{
    private Socket socket;
    private ChatClient client;
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
                String msg = streamIn.readUTF();
                client.message(msg);
                if (msg.equals("Anmeldung fehlgeschlagen") || msg.equals("Auf Wiedersehen"))
                {
                    break;
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
