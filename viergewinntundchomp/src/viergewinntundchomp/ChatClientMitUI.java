import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class ChatClient implements Runnable
{
    private Socket socket = null;
    private Thread thread = null;
    private BufferedReader console = null;
    public DataOutputStream streamOut = null;
    private ChatClientThread client = null;
    private boolean anmelden = true, getList = false;
    private String userList;
    static ClientGUI ui;

    public static void main(String[] args)
    {
        ui = new ClientGUI("localhost", 5555);

    }

    public ChatClient(String serverName, int serverPort)
    {
        ui.append("Verbindung wird aufgebaut\n");
        System.out.println("Verbindung wird aufgebaut");
        try
        {
            socket = new Socket(serverName, serverPort);
            ui.append("Connected: " + socket + "\n");
            System.out.println("Connected: " + socket);
        }
        catch(UnknownHostException e)
        {
            System.out.println("Host unknown: " + e.getMessage());
            ui.disconnect();
        }
        catch(IOException e)
        {
            System.out.println("Unexpected exception: " + e.getMessage());
            ui.disconnect();
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
                ui.disconnect();
                stop();
            }
        }
    }
    public void message(String msg)
    {
        if (msg.equals("userListBeginn"))
        {
            getList = true;
            userList = "";
            return;
        }
        if (msg.equals("userListEnde"))
        {
            getList = false;
            ui.user.setText(userList);
            return;
        }
        if (!getList)
        {
            ui.append(msg + "\n");
            if (msg.equals("Access denied") || msg.equals("Goodbye"))
            {
                ui.disconnect();
                stop();
            }
        }
        else
        {
            userList = userList + msg + "\n";
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
            ui.disconnect();
        }
        client.close();
    }
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
            System.out.println("Error getting input stream: " + e);
            client.ui.disconnect();
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
                client.message(msg);
                if (msg.equals("Access denied") || msg.equals("Goodbye"))
                {
                    break;
                }
            }
            catch(IOException e)
            {
                System.out.println("Listening error: " + e.getMessage());
                client.ui.disconnect();
                client.stop();
                break;
            }
        }
    }
}
class ClientGUI extends JFrame implements ActionListener
{

    // will first hold "Username:", later on "Enter message"
    private JLabel label;
    // to hold the Username and later on the messages
    // to hold the server address an the port number
    private JTextField tf, tfServer, tfPort, tfName, tfPw;
    // to Logout and get the list of the users
    private JButton login, logout, whoIsIn;
    // for the chat room
    public JTextArea ta, user;
    // if it is for connection
    private boolean connected;
    // the Client object
    private ChatClient client;
    // the default port number
    private int defaultPort;
    private String defaultHost;
    public JPanel southPanel, nameUndPasswort;

    // Constructor connection receiving a socket number
    ClientGUI(String host, int port)
    {

        super("Chat Client");
        defaultPort = port;
        defaultHost = host;

        // The NorthPanel with:
        southPanel = new JPanel(new GridLayout(3,1));
        // the server name and the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));
        nameUndPasswort = new JPanel(new GridLayout(1,4,1,3));
        // the two JTextField with default value for server address and port number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

        serverAndPort.add(new JLabel("Server Address:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));
        // adds the Server an port field to the GUI
        southPanel.add(serverAndPort);

        JLabel username = new JLabel("Username: ");
        JLabel passwort = new JLabel("Passwort: ");
        nameUndPasswort.add(username);
        tfName = new JTextField("Anonymous");
        tfPw = new JTextField("Passwort");
        nameUndPasswort.add(tfName);
        nameUndPasswort.add(passwort);
        nameUndPasswort.add(tfPw);
        tfName.setBackground(Color.WHITE);
        tfPw.setBackground(Color.WHITE);
        label = new JLabel("Unten name und Passwort eingeben", SwingConstants.CENTER);
        southPanel.add(label);
        southPanel.add(nameUndPasswort);
        add("South", southPanel);

        /* the Label and the TextField
        label = new JLabel("Unten Name eingeben", SwingConstants.CENTER);
        southPanel.add(label);
        tf = new JTextField("Anonymous");
        tf.setBackground(Color.WHITE);
        southPanel.add(tf);
        add("South", southPanel);

         */

        // The CenterPanel which is the chat room
        ta = new JTextArea("", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        add("Center", centerPanel);

        JPanel east = new JPanel();
        user = new JTextArea(160,20);
        user.setEditable(false);
        east.add(new JScrollPane(user));
        add("East", user);

        // the 3 buttons
        login = new JButton("Login");
        login.addActionListener(this);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false);		// you have to login before being able to logout
        /*whoIsIn = new JButton("Who is in");
        whoIsIn.addActionListener(this);
        whoIsIn.setEnabled(false);		// you have to login before being able to Who is in

         */

        JPanel northPanel = new JPanel();
        northPanel.add(login);
        northPanel.add(logout);
        //southPanel.add(whoIsIn);
        add("North", northPanel);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tfName.requestFocus();

    }

    // called by the Client to append text in the TextArea
    void append(String str)
    {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }
    // called by the GUI is the connection failed
    // we reset our buttons, label, textfield
    void connectionFailed()
    {
        login.setEnabled(true);
        logout.setEnabled(false);
        //whoIsIn.setEnabled(false);
        southPanel.remove(tf);
        southPanel.add(nameUndPasswort);
        label.setText("Unten Name und Passwort eingeben");
        tfName.setText("Anonymous");
        tfPw.setText("Passwort");
        // reset port number and host name as a construction time
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        user.setText("");
        // let the user change them
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        // don't react to a <CR> after the username
        tf.removeActionListener(this);
        connected = false;
    }
    void disconnect()
    {
        login.setEnabled(true);
        logout.setEnabled(false);
        //whoIsIn.setEnabled(false);
        tf.setText("");
        southPanel.remove(tf);
        southPanel.add(nameUndPasswort);
        label.setText("Unten Name und Passwort eingeben");
        tfName.setText("Anonymous");
        tfPw.setText("Passwort");
        // reset port number and host name as a construction time
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        user.setText("");
        // let the user change them
        tfServer.setEditable(true);
        tfPort.setEditable(true);
        // don't react to a <CR> after the username
        //tf.removeActionListener(this);
        connected = false;
    }
    /*
     * Button or JTextField clicked
     */
    public void actionPerformed(ActionEvent e)
    {
        Object o = e.getSource();
        // if it is the Logout button
        if(o == logout) {
            try
            {
                if (connected)
                {
                    client.streamOut.writeUTF(".bye");
                }
                disconnect();
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
            return;
        }
        // if it the who is in button
        /*if(o == whoIsIn) {
            client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            return;
        }

         */
        // ok it is coming from the JTextField
        if(connected)
        {
            // just have to send the message
            try
            {
                client.streamOut.writeUTF(tf.getText());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            tf.setText("");
            return;
        }


        if(o == login)
        {
            // ok it is a connection request
            String username = tfName.getText().trim();
            // empty username ignore it
            if(username.length() == 0)
                return;
            String passwort = tfPw.getText().trim();
            if (passwort.length() == 0)
                return;
            // empty serverAddress ignore it
            String server = tfServer.getText().trim();
            if(server.length() == 0)
                return;
            // empty or invalid port numer, ignore it
            String portNumber = tfPort.getText().trim();
            if(portNumber.length() == 0)
                return;
            ta.setText("");
            int port = 0;
            try
            {
                port = Integer.parseInt(portNumber);
            }
            catch(Exception en)
            {
                return;   // nothing I can do if port number is not valid
            }

            // try creating a new Client with GUI
            client = new ChatClient(server, port);
            // test if we can start the Client
            try
            {
                client.start();
            } catch (IOException ex)
            {
                ex.printStackTrace();
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
                client.streamOut.writeUTF(username);
                client.streamOut.writeUTF(passwort);
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }


            // disable login button
            login.setEnabled(false);
            // enable the 2 buttons
            logout.setEnabled(true);
            //whoIsIn.setEnabled(true);
            // disable the Server and Port JTextField
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            // Action listener for when the user enter a message
            tf.addActionListener(this);
        }

    }
}