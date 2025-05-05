import javazoom.jl.player.Player;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MusicPlayer extends JFrame implements ActionListener {

    private JButton playButton, pauseButton, stopButton, openButton;
    private JLabel fileNameLabel;
    private JProgressBar progressBar;
    private File selectedFile;
    private Player player;
    private Thread playerThread, progressThread;
    private boolean isPaused = false;
    private FileInputStream fis;
    private BufferedInputStream bis;
    private long totalLength;
    private long pauseLocation;

    // Simple clean color scheme
    private Color bgColor = new Color(30, 30, 30);
    private Color buttonColor = new Color(60, 60, 60);
    private Color textColor = Color.BLACK;
    private Color accentColor = new Color(0, 120, 215);

    public MusicPlayer() {
        super("Music Player");
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgColor);
        
        initComponents();
        setupLayout();
        
        setSize(500, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initComponents() {
        // Create buttons with clear text labels
        playButton = new JButton("PLAY");
        pauseButton = new JButton("PAUSE");
        stopButton = new JButton("STOP");
        openButton = new JButton("OPEN FILE");
        
        // Style all buttons
        styleButton(playButton);
        styleButton(pauseButton);
        styleButton(stopButton);
        styleButton(openButton);
        
        // File name display
        fileNameLabel = new JLabel("No file selected");
        fileNameLabel.setForeground(textColor);
        fileNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setBackground(buttonColor);
        progressBar.setForeground(accentColor);
        progressBar.setPreferredSize(new Dimension(getWidth(), 20));
        
        // Add action listeners
        playButton.addActionListener(this);
        pauseButton.addActionListener(this);
        stopButton.addActionListener(this);
        openButton.addActionListener(this);
    }
    
    private void styleButton(JButton button) {
        button.setBackground(buttonColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(120, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(accentColor);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(buttonColor);
            }
        });
    }
    
    private void setupLayout() {
        // Top panel with file name and open button
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(bgColor);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        topPanel.add(fileNameLabel, BorderLayout.CENTER);
        topPanel.add(openButton, BorderLayout.EAST);
        
        // Center panel with progress bar
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(bgColor);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        centerPanel.add(progressBar, BorderLayout.CENTER);
        
        // Bottom panel with control buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomPanel.setBackground(bgColor);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        
        bottomPanel.add(playButton);
        bottomPanel.add(pauseButton);
        bottomPanel.add(stopButton);
        
        // Add all panels to main frame
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select MP3 File");
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                fileNameLabel.setText(selectedFile.getName());
                progressBar.setValue(0);
            }
        } else if (e.getSource() == playButton) {
            if (selectedFile != null) {
                if (isPaused) {
                    resumeMusic();
                } else {
                    playMusic();
                }
            } else {
                showMessage("Please select a file first");
            }
        } else if (e.getSource() == pauseButton) {
            if (player != null) {
                pauseMusic();
            }
        } else if (e.getSource() == stopButton) {
            if (player != null) {
                stopMusic();
            }
        }
    }
    
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void playMusic() {
        try {
            stopMusic(); // Stop any currently playing music
            
            fis = new FileInputStream(selectedFile);
            bis = new BufferedInputStream(fis);
            player = new Player(bis);
            totalLength = fis.available();

            playerThread = new Thread(() -> {
                try {
                    player.play();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            playerThread.start();
            
            // Start updating progress
            updateProgress();
            
        } catch (Exception ex) {
            showMessage("Error playing file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void updateProgress() {
        // Stop existing progress thread if running
        if (progressThread != null && progressThread.isAlive()) {
            progressThread.interrupt();
        }
        
        progressThread = new Thread(() -> {
            try {
                while (player != null && !player.isComplete() && !Thread.currentThread().isInterrupted()) {
                    try {
                        long current = fis.available();
                        int progress = (int)(100 - (current * 100) / totalLength);
                        progress = Math.max(0, Math.min(100, progress)); // Keep in bounds
                        
                        final int finalProgress = progress;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(finalProgress);
                        });
                        
                        Thread.sleep(500);
                    } catch (IOException ex) {
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                // Thread interrupted, exit normally
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        progressThread.start();
    }

    private void pauseMusic() {
        try {
            if (player != null) {
                pauseLocation = fis.available();
                player.close();
                isPaused = true;
                
                // Interrupt progress thread
                if (progressThread != null) {
                    progressThread.interrupt();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void resumeMusic() {
        try {
            fis = new FileInputStream(selectedFile);
            bis = new BufferedInputStream(fis);
            player = new Player(bis);
            fis.skip(totalLength - pauseLocation);

            playerThread = new Thread(() -> {
                try {
                    player.play();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            playerThread.start();
            isPaused = false;
            
            // Restart progress tracking
            updateProgress();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void stopMusic() {
        if (player != null) {
            player.close();
            isPaused = false;
            progressBar.setValue(0);
            
            // Interrupt progress thread
            if (progressThread != null) {
                progressThread.interrupt();
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Use system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            MusicPlayer player = new MusicPlayer();
            player.setVisible(true);
        });
    }
}