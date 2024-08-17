import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

public class SECPASS extends JFrame {
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 24;
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:'\",.<>?/`~";
    private static final String EXCLUDED_WORDS_FILE = "src/excluded_words.txt";
    private static final String PASSWORD_HISTORY_FILE = "password_history.txt";
    private static Set<String> excludedWords = new HashSet<>();

    private JTextArea passwordDisplayArea;
    private JTextArea historyDisplayArea;
    private JSlider lengthSlider;

    public SECPASS() {
        setTitle("Generador de contraseñas seguras");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Carga la lista de palbras excluidas
        try {
            loadExcludedWords();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar la lista de palabras excluidas: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        //Componentes de Interfaz de usuario
        JButton generateButton = new JButton("Generar Contraseña");
        JButton viewHistoryButton = new JButton("Ver contraseñas generadas");
        passwordDisplayArea = new JTextArea(2, 20);
        historyDisplayArea = new JTextArea(10, 20);
        historyDisplayArea.setEditable(false);
        JLabel label=new JLabel("Longitud de la contraseña");
        lengthSlider = new JSlider(JSlider.HORIZONTAL, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH);
        lengthSlider.setMajorTickSpacing(4);
        lengthSlider.setMinorTickSpacing(1);
        lengthSlider.setPaintTicks(true);
        lengthSlider.setPaintLabels(true);

        // Layout
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(generateButton, BorderLayout.NORTH);
        topPanel.add(passwordDisplayArea, BorderLayout.SOUTH);
        topPanel.add(lengthSlider, BorderLayout.EAST);
        topPanel.add(label, BorderLayout.WEST);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(viewHistoryButton, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(historyDisplayArea), BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.CENTER);

        add(panel);

        // Botones
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int passwordLength = lengthSlider.getValue(); // Obtiene la longitud del slider
                    String password = generateSecurePassword(passwordLength);
                    savePassword(password);
                    passwordDisplayArea.setText("Contraseña generada: " + password);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(SECPASS.this, "Error generando contraseña: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        viewHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> history = Files.readAllLines(Paths.get(PASSWORD_HISTORY_FILE), StandardCharsets.UTF_8);
                    StringBuilder historyText = new StringBuilder();
                    for (String line : history) {
                        historyText.append(line).append("\n");
                    }
                    historyDisplayArea.setText(historyText.toString());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(SECPASS.this, "Erorr al cargar el historial: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static void loadExcludedWords() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(EXCLUDED_WORDS_FILE), StandardCharsets.UTF_8);
        excludedWords.addAll(lines);
    }

    private static String generateSecurePassword(int passwordLength) {
        // Tiempo del sistema en milisegundos
        long currentTimeMillis = System.currentTimeMillis();

        // nueva semilla de securerandom con el tiempo actual
        SecureRandom random = new SecureRandom();
        random.setSeed(currentTimeMillis);

        StringBuilder password = new StringBuilder(passwordLength);
        char lastChar = '\0';

        while (password.length() < passwordLength) {
            // usa el tiempo actual para decidir el tipo de carater que se usara en la contraseña
            int choice = (int) ((currentTimeMillis + random.nextInt()) % 3);

            char nextChar;
            switch (choice) {
                case 0:
                    nextChar = LETTERS.charAt(random.nextInt(LETTERS.length()));
                    break;
                case 1:
                    nextChar = NUMBERS.charAt(random.nextInt(NUMBERS.length()));
                    break;
                default:
                    nextChar = SYMBOLS.charAt(random.nextInt(SYMBOLS.length()));
                    break;
            }

            if (nextChar != lastChar) {
                // decide si la letra es mayuscula o minuscula basado en el tiempo
                if (currentTimeMillis % 2 == 0) {
                    if(random.nextBoolean()==true){
                        nextChar = Character.toUpperCase(nextChar);
                    }else{
                        nextChar = Character.toLowerCase(nextChar);
                    }
                } else {
                    if(random.nextBoolean()==true){
                        nextChar = Character.toLowerCase(nextChar);
                    }else{
                        nextChar = Character.toUpperCase(nextChar);
                    }
                }

                password.append(nextChar);
                lastChar = nextChar;
            }

            if (password.length() >= passwordLength && containsExcludedWord(password.toString())) {
                password.setLength(0); // resetea la contraseña y si alguna de las palabras excluidas aparece
                lastChar = '\0';
                currentTimeMillis = System.currentTimeMillis();
                random.setSeed(currentTimeMillis); // resetea la semilla random
            }
        }

        return password.toString();
    }

    private static boolean containsExcludedWord(String password) {
        for (String word : excludedWords) {
            if (password.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static void savePassword(String password) throws IOException {
        try (FileWriter writer = new FileWriter(PASSWORD_HISTORY_FILE, true)) {
            writer.write(password + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SECPASS().setVisible(true);
            }
        });
    }
}
