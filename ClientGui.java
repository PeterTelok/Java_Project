//--IMPORTS--

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.awt.Color;

//--------------------------------------------------------------------------//

public class ClientGui extends Thread {


    // GUI COMPONENTS
    final JTextPane jtextFilDiscu = new JTextPane();
    final JTextPane jtextListUsers = new JTextPane();
    final JTextField jtextInputChat = new JTextField();
    final JFrame jfr;
    final JButton jsbtndeco;
    final JButton jsbtn;

//--------------------------------------------------------------------------//


    // OTHER VARIABLES

    private String oldMsg = "";
    private Thread read;
    private String serverName;
    private int PORT;
    private String name = "Your_Name"; // Initialize name to an empty string
    BufferedReader input;
    PrintWriter output;
    Socket server;
    boolean isCoordinator = false;

//--------------------------------------------------------------------------//


    // COLOR VARIABLES FOR THEMES
    Color buttonsColor;
    Color windowColor;
    Color boxColor;
    Color fontColor;
    Color loggedIn;

//--------------------------------------------------------------------------//

    //CONSTRUCTOR
    public ClientGui() {


        //INITIALIZE LOGGED IN USER VISIBILITY
        jtextListUsers.setVisible(false);

//--------------------------------------------------------------------------//

        // SERVER LOGIN DETAILS SETTINGS
        this.serverName = "localhost";
        this.PORT = 12345;

//--------------------------------------------------------------------------//

        // SETTINGS FOR FONT ADJUSTMENTS
        String fontfamily = "Arial, sans-serif";
        Font font = new Font(fontfamily, Font.PLAIN, 15);

//--------------------------------------------------------------------------//

        // WINDOW ADJUSTMENTS SETTINGS
        jfr = new JFrame("Chat Friendly Assistant 1.0");
        jfr.getContentPane().setLayout(null);
        jfr.setSize(700, 600);
        jfr.setResizable(false);
        jfr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jfr.getContentPane().setBackground(windowColor);

//--------------------------------------------------------------------------//

        //CHAT AREA SETTINGS
        jtextFilDiscu.setBounds(25, 70, 490, 320);
        jtextFilDiscu.setFont(font);
        jtextFilDiscu.setMargin(new Insets(6, 6, 6, 6));
        jtextFilDiscu.setEditable(false);
        JScrollPane jtextFilDiscuSP = new JScrollPane(jtextFilDiscu);
        jtextFilDiscuSP.setBounds(25, 70, 490, 320);
        jtextFilDiscu.setContentType("text/html");
        jtextFilDiscu.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

                            //--VISUAL SETTINGS--
        // CHAT AREA COLOR
        jtextFilDiscu.setBackground(boxColor);
        // CHAT AREA TEXT COLOR
        jtextFilDiscu.setForeground(fontColor);
        jtextFilDiscuSP.setBackground(windowColor);

//--------------------------------------------------------------------------//

        //LOGGED IN USER LIST SETTINGS
        jtextListUsers.setBounds(520, 70, 156, 320);
        jtextListUsers.setEditable(true);
        jtextListUsers.setFont(font);
        jtextListUsers.setMargin(new Insets(6, 6, 6, 6));
        jtextListUsers.setEditable(false);
        JScrollPane jsplistuser = new JScrollPane(jtextListUsers);
        jsplistuser.setBounds(520, 70, 156, 320);
        jtextListUsers.setContentType("text/html");

        jtextListUsers.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        //                  --VISUAL SETTINGS--
        // LOGGED IN MEMBERS TABLE COLOR BACKGROUND
        jtextListUsers.setBackground(loggedIn);
        // LOGGED IN MEMBERS FONT COLOR
        jtextListUsers.setForeground(fontColor);

//--------------------------------------------------------------------------//

        // INPUT TEXT BOXES SETTINGS
        jtextInputChat.setBounds(25, 400, 490, 50);
        jtextInputChat.setFont(font);
        jtextInputChat.setMargin(new Insets(6, 6, 6, 6));
        final JScrollPane jtextInputChatSP = new JScrollPane(jtextInputChat);
        jtextInputChatSP.setBounds(25, 400, 490, 50);

        //                  --VISUAL SETTINGS--
        jtextInputChatSP.setBackground(boxColor);
        //CHAT INPUT FIELD BACKGROUND COLOR
        jtextInputChat.setBackground(boxColor);
        //CHAT INPUT FIELD FONT COLOR
        jtextInputChat.setForeground(fontColor);

//--------------------------------------------------------------------------//

        // SEND BUTTON ADJUSTMENTS

        //SEND BUTTON NAME
        jsbtn = new JButton("Send");
        jsbtn.setFont(font);
        jsbtn.setBounds(525, 400, 150, 50);

        //                  --VISUAL SETTINGS--
        // SEND BUTTON BACKGROUND COLOR
        jsbtn.setBackground(buttonsColor);
        // SEND BUTTON FONT COLOR
        jsbtn.setForeground(fontColor);

//--------------------------------------------------------------------------//

        // DISCONNECT BUTTON ADJUSTMENTS

        //DISCONNECT BUTTON NAME
        jsbtndeco = new JButton("Disconnect");
        jsbtndeco.setFont(font);
        jsbtndeco.setBounds(525, 470, 150, 30);

        //                  --VISUAL SETTINGS--
        // DISCONNECT BUTTON BACKGROUND COLOR
        jsbtndeco.setBackground(buttonsColor);
        // DISCONNECT BUTTON FONT COLOR
        jsbtndeco.setForeground(fontColor);

//--------------------------------------------------------------------------//

        //THEME BOX SETTINGS

        //THEME SELECTION BOX SETTINGS
        String[] themes = {"Standard", "Warm", "Pink", "Earth"};
        JComboBox<String> themeComboBox = new JComboBox<>(themes);
        themeComboBox.setBounds(40, 470, 100, 30);
        jfr.add(themeComboBox);

        //ACTION LISTENER FOR DROP DOWN THEME MENU
        themeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedTheme = (String) themeComboBox.getSelectedItem();
                applyTheme(selectedTheme); // Apply the selected theme
            }
        });

        // Apply the default theme initially
        applyTheme("Standard");

//--------------------------------------------------------------------------//

        //ACTION LISTENERS FOR INPUT FIELDS

        jtextInputChat.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                    jtextInputChat.setText("");
                }

                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    String currentMessage = jtextInputChat.getText().trim();
                    jtextInputChat.setText(oldMsg);
                    oldMsg = currentMessage;
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    String currentMessage = jtextInputChat.getText().trim();
                    jtextInputChat.setText(oldMsg);
                    oldMsg = currentMessage;
                }

            }
        });

//--------------------------------------------------------------------------//

        // INPUT FIELDS ADJUSTMENTS

        // ID FIELD COLOR AND FONT ADJUSTMENTS
        final JTextField jtfName = new JTextField(this.name);
        jtfName.setBackground(boxColor);
        jtfName.setForeground(fontColor);

        // PORT FIELD COLOR AND FONT ADJUSTMENTS

        final JTextField jtfport = new JTextField(Integer.toString(this.PORT));
        jtfport.setBackground(boxColor);
        jtfport.setForeground(fontColor);

        // PORT FIELD COLOR AND FONT ADJUSTMENTS

        final JTextField jtfAddr = new JTextField(this.serverName);
        jtfAddr.setBackground(boxColor);
        jtfport.setForeground(fontColor);

        //CONNECT BUTTON NAME
        final JButton jcbtn = new JButton("Connect");

        // CONNECT BUTTON BACKGROUND COLOR
        jcbtn.setBackground(buttonsColor);

        //CONNECT BUTTON FONT COLOR
        jcbtn.setForeground(fontColor);

//--------------------------------------------------------------------------//

        //DOCUMENT LISTENERS
        jtfName.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jcbtn));
        jtfport.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jcbtn));
        jtfAddr.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jcbtn));

//--------------------------------------------------------------------------//

        //FONT ADJUSTMENTS
        jcbtn.setFont(font);
        jtfAddr.setBounds(25, 30, 135, 30);
        jtfName.setBounds(375, 30, 135, 30);
        jtfport.setBounds(200, 30, 135, 30);
        jcbtn.setBounds(575, 30, 100, 30);

//--------------------------------------------------------------------------//

        //ADDING COMPONENTS TO THE FRAME
        jfr.add(jcbtn);
        jfr.add(jtextFilDiscuSP);
        jfr.add(jsplistuser);
        jfr.add(jtfName);
        jfr.add(jtfport);
        jfr.add(jtfAddr);
        jfr.add(jsbtn);
        jfr.add(jsbtndeco);
        jfr.add(jtextInputChatSP);
        jfr.setVisible(true);

//--------------------------------------------------------------------------//

        //ACTION LISTENERS FOR BUTTONS

        //CONNECT BUTTON
        jcbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    name = jtfName.getText().trim(); // Get name from text field
                    String port = jtfport.getText();
                    serverName = jtfAddr.getText();
                    PORT = Integer.parseInt(port);

                    appendToPane(jtextFilDiscu, "<span>Connecting to " + serverName + " on port " + PORT + "...</span>");
                    server = new Socket(serverName, PORT);

                    appendToPane(jtextFilDiscu, "<span>Connected to " +
                            server.getRemoteSocketAddress()+"</span>");

                    input = new BufferedReader(new InputStreamReader(server.getInputStream()));
                    output = new PrintWriter(server.getOutputStream(), true);

                    output.println(name);

                    read = new Read();
                    read.start();
                    jfr.remove(jtfName);
                    jfr.remove(jtfport);
                    jfr.remove(jtfAddr);
                    jfr.remove(jcbtn);
                    jfr.revalidate();
                    jfr.repaint();
                } catch (Exception ex) {
                    appendToPane(jtextFilDiscu, "<span>Failed to connect to " + serverName + " on port " + PORT + "</span>");
                    JOptionPane.showMessageDialog(jfr, ex.getMessage());
                }
            }
        });

        //DISCONNECT BUTTON
        jsbtndeco.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                output.println("exit");
                jfr.add(jtfName);
                jfr.add(jtfport);
                jfr.add(jtfAddr);
                jfr.add(jcbtn);
                jfr.revalidate();
                jfr.repaint();
                read.interrupt();
                jtextListUsers.setText(null);
                jtextFilDiscu.setBackground(boxColor);
                jtextListUsers.setBackground(loggedIn);
                clearChat();
                appendToPane(jtextFilDiscu, "<span>Connection closed.</span>");
                output.close();
            }
        });

        jsbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
                jtextInputChat.setText("");
            }
        });
    }

    //SEND BUTTON
    private void setUserListVisibility(boolean visible) {
        SwingUtilities.invokeLater(() -> {
            jtextListUsers.setVisible(visible);
        });
    }

//--------------------------------------------------------------------------//

    //CLASS FOR MONITORING THE CHANGES IN THE TEXT FIELDS
    public class TextListener implements DocumentListener {
        JTextField jtf1;
        JTextField jtf2;
        JTextField jtf3;
        JButton jcbtn;

//--------------------------------------------------------------------------//

        //CONSTRUCTOR
        public TextListener(JTextField jtf1, JTextField jtf2, JTextField jtf3, JButton jcbtn) {
            this.jtf1 = jtf1;
            this.jtf2 = jtf2;
            this.jtf3 = jtf3;
            this.jcbtn = jcbtn;
        }

//--------------------------------------------------------------------------//

        //METHOD IS INVOKED ONCE THERE IS A CHANGE IN THE DOCUMENT
        public void changedUpdate(DocumentEvent e) {
        }

//--------------------------------------------------------------------------//

        //METHOD GETS INVOKED WHEN REMOVING IS PERFORMED FROM DOCUMENT
        public void removeUpdate(DocumentEvent e) {
            if (jtf1.getText().trim().equals("") ||
                    jtf2.getText().trim().equals("") ||
                    jtf3.getText().trim().equals("")) {
                jcbtn.setEnabled(false);
            } else {
                jcbtn.setEnabled(true);
            }
        }

//--------------------------------------------------------------------------//

        //METHOD IS INVOKED WHEN THERE IS TEXT INPUT IN THE DOCUMENT
        public void insertUpdate(DocumentEvent e) {
            if (jtf1.getText().trim().equals("") ||
                    jtf2.getText().trim().equals("") ||
                    jtf3.getText().trim().equals("")) {
                jcbtn.setEnabled(false);
            } else {
                jcbtn.setEnabled(true);
            }
        }
    }

//--------------------------------------------------------------------------//

    //METHOD RESPONSIBLE FOR CLEARING THE CHAT ON RE-CONNECTION TO SERVER
    private void clearChat() {
        SwingUtilities.invokeLater(() -> {
            jtextFilDiscu.setText("");
        });
    }

//--------------------------------------------------------------------------//

    //METHOD TO SEND MESSAGE
    public void sendMessage() {
        try {
            String message = jtextInputChat.getText().trim();
            if (message.equals("")) {
                return;
            }

            if (message.startsWith("/")) {
                output.println(message);
            } else {
                this.oldMsg = message;
                output.println(message);
                jtextInputChat.setText("");
                appendToPane(jtextFilDiscu, "You: " + message);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.exit(0);
        }
    }

//--------------------------------------------------------------------------//

    //CLASS IS RESPONSIBLE FOR CONSTANTLY READING MESSAGES FROM THE SERVER
    class Read extends Thread {
        @Override
        public void run() {
            String message;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    message = input.readLine();
                    if (message != null) {
                        if (message.equals("coordinator:true")) {
                            isCoordinator = true;
                            SwingUtilities.invokeLater(() -> {
                                setUserListVisibility(true);
                                appendToPane(jtextFilDiscu, "<span><b>You are the coordinator</b></span>");
                            });
                        } else if (message.startsWith("[") && message.endsWith("]")) {
                            final String[] loggedInUsers = message.substring(1, message.length() - 1).split(", ");
                            updateUsersList(loggedInUsers);
                        } else {
                            appendToPane(jtextFilDiscu, message);
                        }
                    }
                } catch (IOException ex) {
                    System.err.println("Failed to parse incoming message");
                }
            }
        }
    }

//--------------------------------------------------------------------------//

    //METHOD TO UPDATE THE USER LIST IN THE GUI
    private void updateUsersList(final String[] users) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder("<html>");
            for (String user : users) {
                sb.append(user).append("<br>");
            }
            sb.append("</html>");
            jtextListUsers.setText(sb.toString());
        });
    }

//--------------------------------------------------------------------------//

    //METHOD TO APPLY HTML FORMAT TEXT TO A JTEXTPANE
    private void appendToPane(JTextPane tp, String msg) {
        HTMLDocument doc = (HTMLDocument) tp.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) tp.getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), msg, 0, 0, null);
            tp.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//--------------------------------------------------------------------------//

    // THEME CHANGER METHOD
    private void applyTheme(String theme) {
        switch (theme) {
            case "Standard":
                buttonsColor = new Color(173, 196, 206);
                windowColor = new Color(150, 182, 197);
                boxColor = new Color(238, 224, 201);
                fontColor = new Color(0, 0, 0);
                loggedIn = new Color(241, 240, 232);
                break;
            case "Warm":
                buttonsColor = new Color(18, 110, 130);
                windowColor = new Color(19, 44, 51);
                boxColor = new Color(216, 227, 231);
                fontColor = new Color(0, 0, 0);
                loggedIn = new Color(216, 227, 241);
                Color fontWarm = new Color(255,255,255);
                jtextInputChat.setForeground(fontColor);

                break;
            case "Pink":
                buttonsColor = new Color(255, 62, 165);
                windowColor = new Color(100, 32, 170);
                boxColor = new Color(255, 181, 218);
                fontColor = new Color(0, 0, 0);
                loggedIn = new Color(255, 126, 212);
                break;
            case "Earth":
                buttonsColor = new Color(229, 228, 131);
                windowColor = new Color(178, 179, 119);
                boxColor = new Color(241, 245, 168);
                fontColor = new Color(0, 0, 0);
                loggedIn = new Color(241, 245, 168);
                break;
            default:
                // Default theme
                buttonsColor = new Color(173, 196, 206);
                windowColor = new Color(150, 182, 197);
                boxColor = new Color(238, 224, 201);
                fontColor = new Color(0, 0, 0);
                loggedIn = new Color(241, 240, 232);
        }

        // Update UI components with the selected theme
        jfr.getContentPane().setBackground(windowColor);
        jtextFilDiscu.setBackground(boxColor);
        jtextFilDiscu.setForeground(fontColor);
        jtextListUsers.setBackground(loggedIn);
        jtextListUsers.setForeground(fontColor);
        jtextInputChat.setBackground(boxColor);
        jtextInputChat.setForeground(fontColor);
        jsbtndeco.setBackground(buttonsColor);

        //VISUAL ADJUSTMENTS

        // DISCONNECT BUTTON FONT COLOR
        jsbtndeco.setForeground(fontColor);
        jsbtn.setBackground(buttonsColor);
        // SEND BUTTON FONT COLOR
        jsbtn.setForeground(fontColor);

//--------------------------------------------------------------------------//

    }

    //MAIN METHOD TO START GUI
    public static void main(String[] args) throws Exception {
        ClientGui client = new ClientGui();
    }
}
