import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

/**
 * ChatRoomImpl: Remote object that acts as the central chat room.
 * It maintains the list of subscribed users and broadcasts messages.
 */
public class ChatRoomImpl extends UnicastRemoteObject implements ChatRoom {

    // Maps pseudo -> ChatUser remote reference
    private Map<String, ChatUser> users = new HashMap<>();

    public ChatRoomImpl() throws RemoteException {
        super();
    }

    /**
     * Subscribe a user to the chat room.
     * @param user  the remote reference (stub) of the ChatUser
     * @param pseudo the chosen nickname
     */
    @Override
    public synchronized void subscribe(ChatUser user, String pseudo) throws RemoteException {
        users.put(pseudo, user);
        System.out.println("[Server] " + pseudo + " joined the chat. (" + users.size() + " user(s) connected)");
        broadcastMessage("*** " + pseudo + " has joined the chat ***");
    }

    /**
     * Unsubscribe a user from the chat room.
     * @param pseudo the nickname to remove
     */
    @Override
    public synchronized void unsubscribe(String pseudo) throws RemoteException {
        users.remove(pseudo);
        System.out.println("[Server] " + pseudo + " left the chat. (" + users.size() + " user(s) remaining)");
        broadcastMessage("*** " + pseudo + " has left the chat ***");
    }

    /**
     * Receive a message from a user and broadcast it to all subscribers.
     * @param pseudo  sender's nickname
     * @param message the message text
     */
    @Override
    public synchronized void postMessage(String pseudo, String message) throws RemoteException {
        String formatted = "[" + pseudo + "] " + message;
        System.out.println("[Server] Broadcasting: " + formatted);
        broadcastMessage(formatted);
    }

    /**
     * Send a message to every subscribed user.
     * Users that are unreachable are automatically removed.
     */
    private void broadcastMessage(String message) {
        List<String> disconnected = new ArrayList<>();
        for (Map.Entry<String, ChatUser> entry : users.entrySet()) {
            try {
                entry.getValue().displayMessage(message);
            } catch (RemoteException e) {
                System.out.println("[Server] User " + entry.getKey() + " is unreachable – removing.");
                disconnected.add(entry.getKey());
            }
        }
        users.keySet().removeAll(disconnected);
    }

    // -----------------------------------------------------------------------
    // Main: start the RMI registry and bind the ChatRoom
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        try {
            // Start a local RMI registry on the default port (1099)
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            System.out.println("[Server] RMI registry started on port 1099.");

            ChatRoomImpl room = new ChatRoomImpl();
            Naming.rebind("//localhost/ChatRoom", room);
            System.out.println("[Server] ChatRoom bound and ready.");
        } catch (Exception e) {
            System.err.println("[Server] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
