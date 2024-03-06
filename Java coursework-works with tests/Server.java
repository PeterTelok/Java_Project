//--IMPORTS--

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//--------------------------------------------------------------------------//

public class Server {

    //INITIALIZE VARIABLES

    public int port;
    private final List<User> clients = new ArrayList<>();
    private ServerSocket server;
    User coordinator;
    private ScheduledExecutorService scheduler;

//--------------------------------------------------------------------------//

    //MAIN METHOD TO START THE SERVER

    public static void main(String[] args) throws IOException {
        new Server(12345).run();
    }

//--------------------------------------------------------------------------//

    //CONSTRUCTOR TO INITIALIZE THE SERVER WITH PORT
    public Server(int port) {
        this.port = port;
        startCoordinatorTask();
    }

//--------------------------------------------------------------------------//

    //METHOD TO START SERVER AND HANDLE INCOMING CONNECTIONS
    public void run() {
        try {
            server = new ServerSocket(port);
            System.out.println("Port " + port + " is now open.");

            // Start the periodic task for the coordinator
            startCoordinatorTask();

            while (true) {
                Socket client = server.accept();
                handleNewClient(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }
    }

//--------------------------------------------------------------------------//

    //METHOD TO HANDLE NEW CLIENT CONNECTIONS
    public void handleNewClient(Socket client) throws IOException {
        String nickname = (new Scanner(client.getInputStream())).nextLine().replaceAll("[, ]", "_");

        //creating new user object for the client
        User newUser = new User(client, nickname);
        synchronized (clients) {
            this.clients.add(newUser);
        }
        //assigning new coordinator if coordinator is null (not assigned)
        if (coordinator == null) {
            coordinator = newUser;
            coordinator.getOutStream().println("coordinator:true"); // Inform the new coordinator
            broadcastAllUsers(); // Send the list of users to coordinator
        } else {
            newUser.getOutStream().println("<b>Current coordinator:</b> " + coordinator.getNickname());
        }

        //Starting new thread to handle the user
        new Thread(new UserHandler(this, newUser)).start();
    }

//--------------------------------------------------------------------------//

    //METHOD TO REMOVE USER FROM SERVER
    public synchronized void removeUser(User user) {
        this.clients.remove(user);
        this.broadcastAllUsers();  // Inform remaining users about the change

        // If the leaving user is the coordinator, select a new coordinator
        if (user.equals(coordinator)) {
            selectNewCoordinator();
        }
    }

//--------------------------------------------------------------------------//

    //METHOD TO HANDLE PRIVATE MESSAGING BETWEEN USERS
    public synchronized void handlePrivateMessage(User sender, String recipientNickname, String message) {
        boolean recipientFound = false;
        for (User client : this.clients) {
            if (client.getNickname().equals(recipientNickname)) {
                recipientFound = true;
                client.getOutStream().println(
                        "(<b>Private</b>)" + sender.toString() + "<span>: " + message + "</span>");
                sender.getOutStream().println(sender.toString() + " -> " + client.toString() + ": " + message);
                break;
            }
        }
        if (!recipientFound) {
            sender.getOutStream().println(sender.toString() + " -> (<b>Recipient not found!</b>): " + message);
        }
    }

//--------------------------------------------------------------------------//

    // METHOD TO HANDLE MESSAGE BROADCAST TO ALL USERS
    public synchronized void handleBroadcastMessage(User sender, String message) {
        for (User client : this.clients) {
            if (!client.equals(sender)) {
                client.getOutStream().println(
                        sender.toString() + "<span>: " + message + "</span>");
            }
        }
    }
//--------------------------------------------------------------------------//

    //METHOD TO COLLECT THE USERNAMES OF ALL LOGGED IN USERS
    private Stream<String> getUserNames() {
        return clients.stream().map(User::getNickname);
    }
//--------------------------------------------------------------------------//

    //METHOD TO CHECK IF THE USER IS THE COORDINATOR
    private boolean isUserCoordinator(User user) {
        return user == clients.get(0);
    }

//--------------------------------------------------------------------------//
    //METHOD TO START COORDINATOR TASK (EVERY 20 SECONDS)
    private void startCoordinatorTask() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (coordinator != null) {
                System.out.println("Coordinator checking state of active group members...");
                StringBuilder builder = new StringBuilder();
                for (User user : clients) {
                    builder.append("ID: ").append(user.getNickname()).append("\n");
                    builder.append("IP: ").append(user.getClient().getInetAddress().getHostAddress()).append("\n");
                    builder.append("Port: ").append(user.getClient().getPort()).append("\n\n");
                }
                coordinator.getOutStream().println("<b>Active members:</b> \n" + builder.toString());
            }
        }, 0, 20, TimeUnit.SECONDS); // Run every 20 seconds
    }

//--------------------------------------------------------------------------//

    //METHOD FOR SELECTING NEW COORDINATOR
    private void selectNewCoordinator() {
        if (!clients.isEmpty()) {
            coordinator = clients.get(0);
            coordinator.getOutStream().println("coordinator:true");
            broadcastAllUsers();
        } else {
            coordinator = null;
        }
    }

//--------------------------------------------------------------------------//

    //METHOD TO BROADCAST THE LIST TO ALL ACCEPTED USERS BY COORDINATOR
    public synchronized void broadcastAllUsers() {
        if (coordinator != null) {
            String usersList = getUserNames().collect(Collectors.joining(", "));
            coordinator.getOutStream().println("[" + usersList + "]");
        }
    }

//--------------------------------------------------------------------------//

    public List<User> getClients() {
        return clients;
    }


    //INNER CLASS TO HANDLE INDIVIDUAL USER CONNECTIONS
    class UserHandler implements Runnable {
        private Server server;
        private User user;

        //CONSTRUCTOR
        public UserHandler(Server server, User user) {
            this.server = server;
            this.user = user;
            this.server.broadcastAllUsers();
        }

//--------------------------------------------------------------------------//

        //METHOD TO HANDLE USERS INTERACTIONS
        public void run() {
            try {
                String message;
                Scanner sc = new Scanner(this.user.getInputStream());

                //listening for incoming messages from user
                while (sc.hasNextLine()) {
                    message = sc.nextLine();
                    //checking if the user wants to exit chat
                    if (message.equals("exit")) {
                        server.removeUser(user);
                        break;  // Exit the loop and terminate the thread
                    }
                    //checking if the message is a command or general or private message
                    if (message.startsWith("/")) {
                        server.handleChatCommand(user, message);
                    } else if (message.charAt(0) == '@') {
                        if (message.contains(" ")) {
                            System.out.println("private msg : " + message);
                            int firstSpace = message.indexOf(" ");
                            String userPrivate = message.substring(1, firstSpace);
                            server.handlePrivateMessage(
                                    user, userPrivate,
                                    message.substring(firstSpace + 1, message.length())
                            );
                        }
                    } else {
                        server.handleBroadcastMessage(user, message);
                    }
                }
            } finally {
                try {
                    user.getInputStream().close();
                    user.getOutStream().close();
                    user.getClient().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }}

//--------------------------------------------------------------------------//

    //INNER CLASS REPRESENTING CONNECTED USERS
    class User {
        //VARIABLES INITIALIZATION
        private static int newUser = 0;
        private int userId;
        private PrintStream streamOut;
        private InputStream streamIn;
        private String nickname;
        private Socket client;
        private String color;

//--------------------------------------------------------------------------//


        //CONSTRUCTOR
        public User(Socket client, String name) throws IOException {
            this.streamOut = new PrintStream(client.getOutputStream());
            this.streamIn = client.getInputStream();
            this.client = client;
            this.nickname = name;
            this.userId = newUser;
            this.color = ColorInt.getColor(this.userId);
            newUser += 1;
        }

//--------------------------------------------------------------------------//

        //GETTERS
        public PrintStream getOutStream() {
            return this.streamOut;
        }

        public InputStream getInputStream() {
            return this.streamIn;
        }

        public String getNickname() {
            return this.nickname;
        }

        public Socket getClient() {
            return this.client;
        }

//--------------------------------------------------------------------------//

        //METHOD TO REPRESENT THE USER AS HTML FORMATTED TEXT
        public String toString() {
            return "<u><span style='color:" + this.color
                    + "'>" + this.getNickname() + "</span></u>";
        }
    }

//--------------------------------------------------------------------------//

    //INNER CLASS TO GENERATE COLORS BASED ON USER ID
    class ColorInt {
        public static String[] mColors = {
                "#3079ab", "#e15258", "#f9845b", "#7d669e", "#53bbb4", "#51b46d",
                "#e0ab18", "#f092b0", "#e8d174", "#e39e54", "#d64d4d", "#4d7358"
        };

        public static String getColor(int i) {
            return mColors[i % mColors.length];
        }
    }

//--------------------------------------------------------------------------//

    //METHOD TO HANDLE COMMANDS FOR REQUEST/ACCEPT/DENY
    public synchronized void handleChatCommand(User sender, String message) {
        if (message.equals("/request_access")) {
            if (!isUserCoordinator(sender)) {
                coordinator.getOutStream().println(sender.getNickname() + " requested access to active members list. Type /accept_request " +
                        sender.getNickname() + " to grant access or /deny_request " + sender.getNickname() + " to deny access.");
            } else {
                sender.getOutStream().println("You are the coordinator. You don't need to request access.");
            }
        } else if (message.startsWith("/accept_request") || message.startsWith("/deny_request")) {
            if (isUserCoordinator(sender)) {
                String[] parts = message.split(" ");
                if (parts.length == 2) {
                    String userNickname = parts[1];
                    User requestedUser = clients.stream().filter(user -> user.getNickname().equals(userNickname)).findFirst().orElse(null);
                    if (requestedUser != null) {
                        if (message.startsWith("/accept_request")) {
                            requestedUser.getOutStream().println("Your request for access to the active members list has been accepted.");
                            startUserListTask(requestedUser); // Start periodic task for the accepted user
                        } else {
                            requestedUser.getOutStream().println("Your request for access to the active members list has been denied.");
                        }
                    } else {
                        sender.getOutStream().println("User with nickname " + userNickname + " not found.");
                    }
                } else {
                    sender.getOutStream().println("Invalid command. Usage: /accept_request <nickname> or /deny_request <nickname>.");
                }
            } else {
                sender.getOutStream().println("You don't have permission to accept or deny requests.");
            }
        } else {
            handleBroadcastMessage(sender, message);
        }
    }

//--------------------------------------------------------------------------//

    //METHOD TO START LOGGED IN USERS LIST FOR SPECIFIC USER
    private void startUserListTask(User user) {
        ScheduledExecutorService userScheduler = Executors.newScheduledThreadPool(1);
        userScheduler.scheduleAtFixedRate(() -> {
            user.getOutStream().println("<b>Active members:</b> \n" + getUserDetails());
        }, 0, 20, TimeUnit.SECONDS); // Run every 20 seconds
    }

//--------------------------------------------------------------------------//

    //METHOD TO COLLECT DETAILS OF LOGGED IN USERS
    String getUserDetails() {
        StringBuilder userDetails = new StringBuilder();
        for (User user : clients) {
            userDetails.append("ID: ").append(user.getNickname()).append("\n");
            userDetails.append("IP: ").append(user.getClient().getInetAddress().getHostAddress()).append("\n");
            userDetails.append("Port: ").append(user.getClient().getPort()).append("\n\n");
        }
        return userDetails.toString();
    }}