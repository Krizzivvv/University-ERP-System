package edu.univ.erp.ui;

import edu.univ.erp.model.AuthUser;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import net.miginfocom.swing.MigLayout;

public class LoginFrame extends JFrame {

    private static final Color BACKGROUND = new Color(245, 239, 221);      // Light sepia background
    private static final Color PANEL = new Color(231, 220, 197);           // Deeper sepia for panels
    private static final Color PRIMARY = new Color(46, 79, 79);            // Wine green for primary actions
    private static final Color ACCENT = new Color(158, 179, 132);          // Sage green for accents
    private static final Color TEXT_PRIMARY = new Color(62, 39, 35);       // Coffee brown for main text
    private static final Color SUCCESS = new Color(34, 139, 34);           // Success green
    private static final Color ERROR = new Color(165, 42, 42);             // Error red

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton showPassBtn = new JButton("SHOW"); 
    private final JLabel statusLabel = new JLabel(" "); 
    private final JButton loginBtn = new JButton("Login");
    private static LoginFrame INSTANCE = null;
    private char defaultEchoChar; 

    public LoginFrame() {
        setTitle("ERP System - Login");
        setSize(450, 500); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        defaultEchoChar = passwordField.getEchoChar();

        // main background
        getContentPane().setBackground(BACKGROUND);
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));

        // Creating lOgin Card
        JPanel loginCard = new JPanel(new MigLayout("wrap 1, fillx, insets 30 40 30 40", "[grow]", "[]5[]20[]5[]15[]5[]20[]10[]"));
        loginCard.setBackground(PANEL);
        loginCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Header
        JLabel lblTitle = new JLabel("ERP Portal");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblTitle.setForeground(PRIMARY);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblSubtitle = new JLabel("Sign in to continue");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSubtitle.setForeground(ACCENT);
        lblSubtitle.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblUser.setForeground(TEXT_PRIMARY);

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPass.setForeground(TEXT_PRIMARY);

        styleTextField(usernameField);
        styleTextField(passwordField);
        styleTextButton(showPassBtn);
        showPassBtn.addActionListener(e -> togglePasswordVisibility());

        styleButton(loginBtn);
        loginBtn.addActionListener(e -> login());

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        loginCard.add(lblTitle, "center");
        loginCard.add(lblSubtitle, "center, gapbottom 20");
        
        loginCard.add(lblUser);
        loginCard.add(usernameField, "growx, h 35!");
        
        loginCard.add(lblPass, "split 2"); 
        loginCard.add(showPassBtn, "gapleft push, wrap"); 
        
        loginCard.add(passwordField, "growx, h 35!");
        loginCard.add(loginBtn, "growx, h 40!");
        loginCard.add(statusLabel, "growx, center");

        add(loginCard);
    }

    // LOGIC 
    private void togglePasswordVisibility() {
        if (passwordField.getEchoChar() == (char) 0) {
            // visible, 
            passwordField.setEchoChar(defaultEchoChar);
            showPassBtn.setText("SHOW");
        } else {
            // hidden, 
            passwordField.setEchoChar((char) 0);
            showPassBtn.setText("HIDE");
        }
    }

    private void login() {
        loginBtn.setEnabled(false);
        setStatus("Checking...", TEXT_PRIMARY);

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            setStatus("Please enter username & password", ERROR);
            loginBtn.setEnabled(true);
            return;
        }

        long lockoutMinutes = AuthService.getLockoutRemainingMinutes(username);
        if (lockoutMinutes > 0) {
            setStatus("Account locked. Try again in " + lockoutMinutes + " min.", ERROR);
            loginBtn.setEnabled(true);
            return;
        }

        AuthUser user = AuthService.login(username, password);

        if (user == null) {
            lockoutMinutes = AuthService.getLockoutRemainingMinutes(username);
            if (lockoutMinutes > 0) {
                setStatus("Too many failed attempts. Locked for " + lockoutMinutes + " min.", ERROR);
            } else {
                int remaining = AuthService.getRemainingAttempts(username);
                if (remaining <= 2 && remaining > 0) {
                    setStatus("Wrong credentials. " + remaining + " attempts left.", ERROR);
                } else {
                    setStatus("Login failed - wrong credentials.", ERROR);
                }
            }
            loginBtn.setEnabled(true);
            return;
        }

        setStatus("Login success! Welcome " + user.getUsername() + " (" + user.getRole() + ")", SUCCESS);
        
        Session.setCurrentUser(user);
        SwingUtilities.invokeLater(() -> {
            this.dispose();
            String role = user.getRole() == null ? "" : user.getRole().toLowerCase();
            switch (role) {
                case "admin":
                    new AdminDashboard(user).setVisible(true);
                    break;
                case "instructor":
                    new InstructorDashboard(user).setVisible(true);
                    break;
                case "student":
                default:
                    new StudentDashboard(user).setVisible(true);
                    break;
            }
        });
    }

//interface 
    private void styleTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BACKGROUND);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
    }

    private void styleTextButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 10)); // Short font size
        btn.setForeground(ACCENT);
        btn.setBackground(PANEL);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setForeground(PRIMARY);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setForeground(ACCENT);
            }
        });
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(BACKGROUND); 
        btn.setBackground(PRIMARY);    
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(PRIMARY.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(PRIMARY);
            }
        });
    }

    private void setStatus(String text, Color c) {
        statusLabel.setText(text);
        statusLabel.setForeground(c);
    }

    // SINGLETON / STATIC METHODS
    public static synchronized LoginFrame getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LoginFrame();
        }
        return INSTANCE;
    }

    public static synchronized void showFresh() {
        if (INSTANCE != null) {
            try { INSTANCE.dispose(); } catch (Exception ignored) {}
            INSTANCE = null;
        }
        getInstance().setVisible(true);
    }
}