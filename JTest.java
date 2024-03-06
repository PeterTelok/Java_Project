import org.junit.Test;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import java.net.InetAddress;


public class JTest {

    @Test
    public void testServerIsRunning() {
        // Create an instance of Server
        Server server = new Server(12345);

        // Run the server
        Thread serverThread = new Thread(server::run);
        serverThread.start();

        // Check if the server is running after a delay
        try {
            Thread.sleep(1000); // Adjust the delay as needed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert that the server is running
        assertTrue(serverThread.isAlive());

        // Stop the server
        serverThread.interrupt();
    }

    @Test
    public void testHandleNewClient() {
        Server server = new Server(12345);

        SocketMock clientSocket = new SocketMock();

        try {
            server.handleNewClient(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(1, server.getClients().size());
        assertEquals("test_user", server.getClients().get(0).getNickname());
    }


    class SocketMock extends Socket {
        private InetAddress inetAddress;
        private int port;
        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private ByteArrayInputStream inputStream = new ByteArrayInputStream("test_user".getBytes());

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        public ByteArrayOutputStream getOutStream() {
            return outputStream;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }

        public void setPort(int port) {
            this.port = port;
        }
        public int getPort() {
            return port;
        }

        public void setInetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
        }
    }

    @Test
    public void testRemoveUser() {
        Server server = new Server(12345);
        SocketMock clientSocket = new SocketMock();

        try {
            server.handleNewClient(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(1, server.getClients().size());

        server.removeUser(server.getClients().get(0));

        assertEquals(0, server.getClients().size());
    }

    @Test
    public void testHandlePrivateMessage() throws IOException {
        // Create a new instance of the Server
        Server server = new Server(12345);

        // Create mock sockets for sender and recipient
        SocketMock senderSocket = new SocketMock();
        SocketMock recipientSocket = new SocketMock();

        // Create user instances associated with the server
        Server.User sender = server.new User(senderSocket, "sender");
        Server.User recipient = server.new User(recipientSocket, "recipient");

        // Add sender and recipient to the server's client list
        server.getClients().add(sender);
        server.getClients().add(recipient);

        // Call the handlePrivateMessage method with the sender, recipient, and message
        server.handlePrivateMessage(sender, "recipient", "Hello");

        // Assert that the message is correctly received by the recipient
        assertEquals("(Private)sender: Hello", recipientSocket.getOutStream().toString()
                .replaceAll("<[^>]*>", "")  // Remove HTML markup
                .replaceAll("\\s+", " ")  // Replace multiple consecutive whitespace characters with a single space
                .trim());  // Trim leading and trailing whitespace

    }


    @Test
    public void testHandleBroadcastMessage() throws IOException {
        // Create a new instance of the Server
        Server server = new Server(12345);

        // Create mock sockets for sender and recipients
        SocketMock senderSocket = new SocketMock();
        SocketMock receiver1Socket = new SocketMock();
        SocketMock receiver2Socket = new SocketMock();

        // Create user instances associated with the server
        Server.User sender = server.new User(senderSocket, "sender");
        Server.User receiver1 = server.new User(receiver1Socket, "receiver1");
        Server.User receiver2 = server.new User(receiver2Socket, "receiver2");

        // Add users to the server's client list
        server.getClients().add(sender);
        server.getClients().add(receiver1);
        server.getClients().add(receiver2);

        // Call the handleBroadcastMessage method with the sender and message
        server.handleBroadcastMessage(sender, "Hello, world!");

        // Verify that the message is received by both receivers
        assertTrue(receiver1Socket.getOutStream().toString().contains("<span>: Hello, world!</span>"));
        assertTrue(receiver2Socket.getOutStream().toString().contains("<span>: Hello, world!</span>"));

        // Verify that the sender did not receive the message
        assertFalse(senderSocket.getOutStream().toString().contains("<span>: Hello, world!</span>"));
    }

    @Test
    public void testHandleChatCommand_AcceptRequest() throws IOException {
        // Create a Server instance
        Server server = new Server(12345);

        // Set up mock users
        SocketMock coordinatorSocket = new SocketMock();
        Server.User coordinator = server.new User(coordinatorSocket, "coordinator");
        SocketMock userSocket = new SocketMock();
        Server.User user = server.new User(userSocket, "user");
        List<Server.User> clients = new ArrayList<>();
        clients.add(coordinator);
        clients.add(user);
        server.coordinator = coordinator;
        server.getClients().addAll(clients);

        // Capture system output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Call the handleChatCommand method with /accept_request command
        server.handleChatCommand(coordinator, "/accept_request user");

        // Verify the output
        assertTrue(userSocket.getOutStream().toString().contains("Your request for access to the active members list has been accepted."));

        // Restore standard system output
        System.setOut(System.out);
    }


    @Test
    public void testHandleChatCommand_DenyRequest() throws IOException {
        // Create a Server instance
        Server server = new Server(12345);

        // Set up mock users
        SocketMock coordinatorSocket = new SocketMock();
        Server.User coordinator = server.new User(coordinatorSocket, "coordinator");
        SocketMock userSocket = new SocketMock();
        Server.User user = server.new User(userSocket, "user");
        List<Server.User> clients = new ArrayList<>();
        clients.add(coordinator);
        clients.add(user);
        server.coordinator = coordinator;
        server.getClients().addAll(clients);

        // Capture system output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Call the handleChatCommand method with /deny_request command
        server.handleChatCommand(coordinator, "/deny_request user");

        // Verify the output
        assertTrue(userSocket.getOutStream().toString().contains("Your request for access to the active members list has been denied."));


        // Restore standard system output
        System.setOut(System.out);
    }




        @Test
        public void testGetUserDetails() throws IOException {
            // Create a Server instance
            Server server = new Server(12345);

            // Set up mock users
            SocketMock user1Socket = new SocketMock();
            user1Socket.setInetAddress(InetAddress.getByName("192.168.1.1"));
            user1Socket.setPort(1234);
            Server.User user1 = server.new User(user1Socket, "user1");

            SocketMock user2Socket = new SocketMock();
            user2Socket.setInetAddress(InetAddress.getByName("192.168.1.2"));
            user2Socket.setPort(5678);
            Server.User user2 = server.new User(user2Socket, "user2");

            List<Server.User> clients = new ArrayList<>();
            clients.add(user1);
            clients.add(user2);
            server.getClients().addAll(clients);

            // Call the getUserDetails method
            String userDetails = server.getUserDetails();

            // Verify the output
            String expectedDetails = "ID: user1\nIP: 192.168.1.1\nPort: 1234\n\n" +
                    "ID: user2\nIP: 192.168.1.2\nPort: 5678\n\n";
            assertEquals(expectedDetails, userDetails);
        }
    }















