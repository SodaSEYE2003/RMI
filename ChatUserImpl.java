import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * ChatUserImpl: Remote object representing a chat client.
 * Extends UnicastRemoteObject so it can receive remote calls (displayMessage).
 * Also provides a Swing GUI for the user to type and send messages.
 */
public class ChatUserImpl extends UnicastRemoteObject implements ChatUser {

    private String title   = "Logiciel de discussion en ligne";
    private String pseudo  = null;
    private ChatRoom room  = null;

    // --- Swing components ---
    private JFrame     window    = new JFrame(this.title);
    private JTextArea  txtOutput = new JTextArea();
    private JTextField txtMessage= new JTextField();
    private JButton    btnSend   = new JButton("Envoyer");
    private JButton    btnQuit   = new JButton("Quitter");


    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public ChatUserImpl() throws RemoteException {
        super();            // export this object as a remote object
        this.createIHM();
        this.requestPseudo();
        this.connectToServer();
    }

    // -----------------------------------------------------------------------
    // GUI creation (unchanged from the original skeleton)
    // -----------------------------------------------------------------------
    public void createIHM() {
        JPanel panel = (JPanel) this.window.getContentPane();
        JScrollPane sclPane = new JScrollPane(txtOutput);
        panel.add(sclPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(this.txtMessage, BorderLayout.CENTER);
        southPanel.add(this.btnSend,    BorderLayout.EAST);
        panel.add(southPanel, BorderLayout.SOUTH);

        // Window close → unsubscribe then exit
        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                window_windowClosing(e);
            }
        });

btnQuit.addActionListener(e -> window_windowClosing(null));
southPanel.add(btnQuit, BorderLayout.WEST);


        // Button click → send message
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnSend_actionPerformed(e);
            }
        });

        // Enter key → send message
        txtMessage.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                if (event.getKeyChar() == '\n')
                    btnSend_actionPerformed(null);
            }
        });

        this.txtOutput.setBackground(new Color(220, 220, 220));
        this.txtOutput.setEditable(false);
        this.window.setSize(500, 400);
        this.window.setVisible(true);
        this.txtMessage.requestFocus();
    }

    // -----------------------------------------------------------------------
    // Ask the user for a nickname
    // -----------------------------------------------------------------------
    public void requestPseudo() {
        this.pseudo = JOptionPane.showInputDialog(
                this.window,
                "Entrez votre pseudo : ",
                this.title,
                JOptionPane.OK_OPTION);
        if (this.pseudo == null || this.pseudo.trim().isEmpty())
            System.exit(0);
    }

    // -----------------------------------------------------------------------
    // Connect to the ChatRoom server via RMI
    // -----------------------------------------------------------------------
    public void connectToServer() {
        try {
            // Look up the ChatRoom on the server
            // Change "localhost" to the server's hostname/IP if needed
            this.room = (ChatRoom) Naming.lookup("//localhost/ChatRoom");
            // Subscribe: pass our own remote stub + pseudo
            this.room.subscribe(this, this.pseudo);
            this.window.setTitle(this.title + " — " + this.pseudo);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this.window,
                    "Impossible de se connecter au serveur :\n" + e.getMessage(),
                    "Erreur de connexion",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // -----------------------------------------------------------------------
    // Remote method called by the server to display a message in the GUI
    // (runs on the RMI thread pool → must update Swing on the EDT)
    // -----------------------------------------------------------------------
    @Override
    public void displayMessage(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            txtOutput.append(message + "\n");
            // Auto-scroll to the bottom
            txtOutput.setCaretPosition(txtOutput.getDocument().getLength());
        });
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------
    public void window_windowClosing(WindowEvent e) {
        try {
            if (this.room != null)
                this.room.unsubscribe(this.pseudo);
        } catch (RemoteException ex) {
            // Server may already be down – ignore
        }
        System.exit(0);
    }

    public void btnSend_actionPerformed(ActionEvent e) {
        String text = this.txtMessage.getText().trim();
        if (text.isEmpty()) return;
        try {
            this.room.postMessage(this.pseudo, text);
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this.window,
                    "Erreur lors de l'envoi du message :\n" + ex.getMessage(),
                    "Erreur réseau",
                    JOptionPane.ERROR_MESSAGE);
        }
        this.txtMessage.setText("");
        this.txtMessage.requestFocus();
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        try {
            new ChatUserImpl();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}


