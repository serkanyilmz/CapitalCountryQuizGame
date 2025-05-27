package org.example;

import java.util.List;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.net.URL;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * QuizGame:
 * - Uses Scraper.getCountryCapitalMap() to fetch a <Country→Capital> map.
 * - Runs a 10-question quiz: “X is the capital of which country?”
 * - Displays info (flag, languages, currency, capital, summary) on the right panel.
 * - Logs important steps using Log4j 2.
 */
public class QuizGame extends JFrame {
    private static final Logger logger = LogManager.getLogger(QuizGame.class);

    private static final int TOTAL_QUESTIONS = 10;
    private static final int TIME_PER_QUESTION = 10; 

    private final Map<String, String> countryCapitalMap;
    private final List<Map.Entry<String, String>> allPairs = new ArrayList<>();
    private final List<Map.Entry<String, String>> quizPool = new ArrayList<>();

    private JLabel questionLabel;
    private JButton[] optionButtons = new JButton[4];
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JLabel questionNoLabel;
    private JButton nextButton;

    private JLabel countryNameLabel;
    private JLabel flagLabel;
    private JLabel languagesLabel;
    private JLabel currencyLabel;
    private JLabel capitalLabel;
    private JTextArea infoArea;

    private int currentQuestionIndex = 0;
    private int score = 0;
    private int timeRemaining = TIME_PER_QUESTION;
    private Timer countdownTimer;
    private Map.Entry<String, String> correctEntry;
    private boolean answered;

    public QuizGame(Map<String, String> countryCapitalMap) {
        logger.info("Starting QuizGame application");
        this.countryCapitalMap = countryCapitalMap;
        this.allPairs.addAll(countryCapitalMap.entrySet());

        setTitle("Capital → Country Quiz");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 600));
        setLocationRelativeTo(null);

        initComponents();
        logger.debug("UI components created, starting quiz");
        startQuiz();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        Color backgroundColor = new Color(30, 30, 30);
        Color buttonColor = new Color(50, 50, 50);
        Color buttonTextColor = new Color(255, 255, 255);
        Color questionTextColor = new Color(220, 220, 220);


        //LEFT
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        questionLabel = new JLabel("Question loading...", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        questionLabel.setBackground(backgroundColor);
        questionLabel.setForeground(questionTextColor);
        topPanel.setPreferredSize(new Dimension(500, 50));
        topPanel.add(questionLabel, BorderLayout.CENTER);

        timerLabel = new JLabel("Time: " + TIME_PER_QUESTION, SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        topPanel.add(timerLabel, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        centerPanel.setBackground(backgroundColor);
        for (int i = 0; i < 4; i++) {
            JButton btn = new JButton((char) ('A' + i) + ") Option");
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            btn.setFocusPainted(false);
            btn.setBackground(buttonColor);
            btn.setForeground(buttonTextColor);
            btn.setMargin(new Insets(10, 20, 10, 20));
            btn.addActionListener(new OptionButtonListener(i));
            optionButtons[i] = btn;
            centerPanel.add(btn);
        }

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        scoreLabel = new JLabel("Score: 0 / " + TOTAL_QUESTIONS);
        scoreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        bottomPanel.add(scoreLabel);

        questionNoLabel = new JLabel("Question No: 0 / " + TOTAL_QUESTIONS);
        questionNoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        bottomPanel.add(questionNoLabel);

        nextButton = new JButton("Next");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nextButton.setEnabled(false);
        nextButton.setFocusPainted(false);
        nextButton.setMargin(new Insets(8, 20, 8, 20));
        nextButton.addActionListener(e -> loadNextQuestion());
        bottomPanel.add(nextButton);

        JPanel quizPanel = new JPanel(new BorderLayout(15, 15));
        quizPanel.add(topPanel, BorderLayout.NORTH);
        quizPanel.add(centerPanel, BorderLayout.CENTER);
        quizPanel.add(bottomPanel, BorderLayout.SOUTH);

        //RIGHT
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(null);
        Dimension size = new Dimension(240, 0);
        infoPanel.setPreferredSize(size);
        infoPanel.setMinimumSize(size);
        infoPanel.setMaximumSize(new Dimension(240, Integer.MAX_VALUE));

        countryNameLabel = new JLabel("", SwingConstants.CENTER);
        countryNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        countryNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(countryNameLabel);

        flagLabel = new JLabel("", SwingConstants.CENTER);
        flagLabel.setPreferredSize(new Dimension(260, 140));
        flagLabel.setMaximumSize(new Dimension(260, 140));
        flagLabel.setMinimumSize(new Dimension(260, 140));
        flagLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(flagLabel);

        languagesLabel = new JLabel("", SwingConstants.CENTER);
        languagesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        languagesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(languagesLabel);

        currencyLabel = new JLabel("", SwingConstants.CENTER);
        currencyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        currencyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(currencyLabel);

        capitalLabel = new JLabel("", SwingConstants.CENTER);
        capitalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        capitalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(capitalLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(infoArea);
        //scroll.setBorder(BorderFactory.createTitledBorder("Country Info"));
        scroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(scroll);

        JPanel mainPanel = new JPanel(new BorderLayout());


        quizPanel.setPreferredSize(new Dimension(800, 600));
        infoPanel.setPreferredSize(new Dimension(340, 400));



        mainPanel.add(quizPanel, BorderLayout.WEST);
        mainPanel.add(infoPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

    }

    private void startQuiz() {
        logger.info("Starting quiz: shuffling questions.");
        Collections.shuffle(allPairs);
        quizPool.clear();
        for (int i = 0; i < Math.min(TOTAL_QUESTIONS, allPairs.size()); i++) {
            quizPool.add(allPairs.get(i));
        }
        currentQuestionIndex = 0;
        score = 0;
        scoreLabel.setText("Score: 0 / " + TOTAL_QUESTIONS);
        questionNoLabel.setText("Question No: 0 / " + TOTAL_QUESTIONS);
        countryNameLabel.setText(" ");
        flagLabel.setIcon(null);
        flagLabel.setText(" ");
        languagesLabel.setText(" ");
        currencyLabel.setText(" ");
        capitalLabel.setText(" ");
        infoArea.setText(" ");
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        if (currentQuestionIndex >= quizPool.size()) {
            endQuiz();
            return;
        }

        for (JButton btn : optionButtons) {
            btn.setEnabled(true);
            btn.setBackground(UIManager.getColor("Button.background"));
            btn.setIcon(null);
        }
        nextButton.setEnabled(false);
        answered = false;

        countryNameLabel.setText(" ");
        flagLabel.setIcon(null);
        flagLabel.setText(" ");
        languagesLabel.setText(" ");
        currencyLabel.setText(" ");
        capitalLabel.setText(" ");
        infoArea.setText(" ");

        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        timeRemaining = TIME_PER_QUESTION;
        timerLabel.setText("Time: " + timeRemaining);

        correctEntry = quizPool.get(currentQuestionIndex);
        String correctCountry = correctEntry.getKey();
        String capital = correctEntry.getValue();
        questionLabel.setText(capital + " is the capital of which country?");

        int displayIndex = currentQuestionIndex + 1;
        questionNoLabel.setText("Question No: " + displayIndex + " / " + TOTAL_QUESTIONS);
        logger.debug("Loading new question: [#" + displayIndex + "] Capital=" + capital);

        Set<String> choiceSet = new LinkedHashSet<>();
        choiceSet.add(correctCountry);
        Random rnd = new Random();
        while (choiceSet.size() < 4) {
            String randomCountry = allPairs.get(rnd.nextInt(allPairs.size())).getKey();
            if (!randomCountry.equals(correctCountry)) {
                choiceSet.add(randomCountry);
            }
        }
        List<String> choiceList = new ArrayList<>(choiceSet);
        Collections.shuffle(choiceList);
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText((char) ('A' + i) + ") " + choiceList.get(i));
        }

        countdownTimer = new Timer(1000, e -> {
            timeRemaining--;
            timerLabel.setText("Time: " + timeRemaining);
            if (timeRemaining <= 0) {
                ((Timer) e.getSource()).stop();
                logger.warn("Time expired: Question #" + displayIndex);

                SwingUtilities.invokeLater(() -> {
                    int result = JOptionPane.showConfirmDialog(
                            QuizGame.this,
                            "Time's up! Do you want to play again?",
                            "Game Over",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (result == JOptionPane.YES_OPTION) {
                        logger.info("User chose to restart the quiz.");
                        startQuiz();
                    } else {
                        logger.info("User chose to exit after time expired.");
                        dispose();
                    }
                });
            }

        });
        countdownTimer.start();

        currentQuestionIndex++;
    }

    private void endQuiz() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        logger.info("Quiz ended. Final score: " + score + " / " + TOTAL_QUESTIONS);
        JOptionPane.showMessageDialog(
                this,
                "Quiz finished!\nYour final score: " + score + " / " + TOTAL_QUESTIONS,
                "Quiz Over",
                JOptionPane.INFORMATION_MESSAGE
        );
        int result = JOptionPane.showConfirmDialog(
                this,
                "Do you want to play again?",
                "Play Again?",
                JOptionPane.YES_NO_OPTION
        );
        if (result == JOptionPane.YES_OPTION) {
            startQuiz();
        } else {
            System.exit(0);
        }
    }

    private class OptionButtonListener implements ActionListener {
        private final int index;
        public OptionButtonListener(int index) {
            this.index = index;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (answered) {
                JButton clickedLater = optionButtons[index];
                String chosenLater = clickedLater.getText()
                        .substring(clickedLater.getText().indexOf(") ") + 2);
                logger.debug("After answered, new choice: " + chosenLater);
                loadCountryInfoAndFlag(chosenLater);
                return;
            }
            answered = true;

            if (countdownTimer != null && countdownTimer.isRunning()) {
                countdownTimer.stop();
            }

            JButton clicked = optionButtons[index];
            String chosenCountry = clicked.getText()
                    .substring(clicked.getText().indexOf(") ") + 2);
            logger.debug("User selected option: " + chosenCountry);

            if (chosenCountry.equals(correctEntry.getKey())) {
                score++;
                scoreLabel.setText("Score: " + score + " / " + TOTAL_QUESTIONS);
                clicked.setBackground(new Color(34, 139, 34));
                clicked.setForeground(Color.WHITE);
                logger.info("Correct answer given: " + chosenCountry);
            } else {
                clicked.setBackground(new Color(178, 34, 34));
                clicked.setForeground(Color.WHITE);
                logger.warn("Wrong answer given: " + chosenCountry +
                        ", correct: " + correctEntry.getKey());
                for (JButton btn : optionButtons) {
                    String text = btn.getText();
                    String countryOption = text.substring(text.indexOf(") ") + 2);
                    if (countryOption.equals(correctEntry.getKey())) {
                        btn.setBackground(new Color(34, 139, 34));
                        btn.setForeground(Color.WHITE);
                        break;
                    }
                }
            }

            Icon infoIcon = UIManager.getIcon("OptionPane.informationIcon");
            for (JButton btn : optionButtons) {
                btn.setIcon(infoIcon);
                btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            }

            loadCountryInfoAndFlag(correctEntry.getKey());
            nextButton.setEnabled(true);
        }
    }

    private void loadCountryInfoAndFlag(String countryName) {
        logger.info("Loading country info: " + countryName);
        countryNameLabel.setText(countryName);
        infoArea.setText("Loading info for \"" + countryName + "\"...");
        flagLabel.setIcon(null);
        flagLabel.setText(" ");
        languagesLabel.setText(" ");
        currencyLabel.setText(" ");
        capitalLabel.setText(" ");

        new Thread(() -> {
            Scraper scraper = new Scraper();

            String flagUrl = scraper.getCountryFlagUrl(countryName);
            ImageIcon flagIcon = null;
            if (!flagUrl.isEmpty()) {
                try {
                    URL url = new URL(flagUrl);
                    ImageIcon original = new ImageIcon(url);
                    Image img = original.getImage().getScaledInstance(-1, 120, Image.SCALE_SMOOTH);
                    flagIcon = new ImageIcon(img);
                    logger.debug("Flag downloaded: " + flagUrl);
                } catch (Exception ex) {
                    logger.error("Failed to download flag: " + ex.getMessage());
                }
            }

            String languagesText = scraper.getCountryLanguages(countryName);
            logger.debug("Languages retrieved: " + languagesText);
            String currencyText = scraper.getCountryCurrency(countryName);
            logger.debug("Currency retrieved: " + currencyText);
            String summary = scraper.getCountrySummary(countryName);
            logger.debug("Summary retrieved: " + summary);
            String capitalText = countryCapitalMap.getOrDefault(countryName, "N/A");

            final ImageIcon finalFlag = flagIcon;
            final String finalLanguages = languagesText;
            final String finalCurrency = currencyText;
            final String finalCapital = capitalText;
            final String displaySummary = summary.startsWith("Summary: ")
                    ? summary.substring(9)
                    : summary;

            SwingUtilities.invokeLater(() -> {
                if (finalFlag != null) {
                    flagLabel.setIcon(finalFlag);
                    flagLabel.setText(" ");
                } else {
                    flagLabel.setText("No flag available");
                    flagLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                }
                languagesLabel.setText(finalLanguages);
                currencyLabel.setText(finalCurrency);
                capitalLabel.setText("Capital: " + finalCapital);
                infoArea.setText(displaySummary);
                infoArea.setCaretPosition(0);
                logger.info("Country info displayed on screen: " + countryName);
            });
        }).start();
    }

    public static void main(String[] args) {

        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("TextField.foreground", Color.WHITE);
        UIManager.put("TextArea.foreground", Color.WHITE);
        UIManager.put("ComboBox.foreground", Color.WHITE);
        UIManager.put("List.foreground", Color.WHITE);
        UIManager.put("JOptionPane.foreground", Color.WHITE);
        UIManager.put("Panel.foreground", Color.WHITE);

        UIManager.put("Label.background", new Color(30, 30, 30));
        UIManager.put("Button.background", new Color(50, 50, 50));
        UIManager.put("TextField.background", new Color(40, 40, 40));
        UIManager.put("TextArea.background", new Color(40, 40, 40));
        UIManager.put("ComboBox.background", new Color(40, 40, 40));
        UIManager.put("List.background", new Color(40, 40, 40));
        UIManager.put("Panel.background", new Color(30, 30, 30));
        UIManager.put("JOptionPane.background", new Color(30, 30, 30));

        UIManager.put("OptionPane.background", new Color(40, 40, 40));
        UIManager.put("Panel.background", new Color(40, 40, 40));

        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("Button.background", new Color(60, 60, 60));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 14));

        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 16));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 14));

        Font uiFont = new Font("Segoe UI", Font.PLAIN, 14);
        for (Object key : UIManager.getLookAndFeelDefaults().keySet()) {
            if (key != null && key.toString().toLowerCase().contains("font")) {
                UIManager.put(key, uiFont);
            }
        }

        SwingUtilities.invokeLater(() -> {
            JFrame loadingFrame = new JFrame("Loading data…");
            JLabel loadingLabel = new JLabel(
                    "Please wait, downloading capital data…", SwingConstants.CENTER
            );
            loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            loadingFrame.add(loadingLabel);
            loadingFrame.setSize(400, 120);
            loadingFrame.setLocationRelativeTo(null);
            loadingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loadingFrame.setVisible(true);

            new Thread(() -> {
                Map<String, String> map;
                try {
                    map = new Scraper().getCountryCapitalMap();
                    logger.info("Country→Capital data retrieved, total: " + (map != null ? map.size() : 0));
                } catch (Throwable t) {
                    logger.error("Error fetching data: " + t.getMessage());
                    final String stack = getStackTraceAsString(t);
                    SwingUtilities.invokeLater(() -> {
                        loadingFrame.dispose();
                        JOptionPane.showMessageDialog(
                                null,
                                "Error while fetching/parsing data:\n" +
                                        t.getClass().getSimpleName() + ": " + t.getMessage() +
                                        "\n\nStack trace:\n" + stack,
                                "Data Load Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        System.exit(1);
                    });
                    return;
                }

                if (map == null || map.isEmpty()) {
                    logger.warn("Fetched data is null or empty");
                    SwingUtilities.invokeLater(() -> {
                        loadingFrame.dispose();
                        JOptionPane.showMessageDialog(
                                null,
                                "Failed to fetch any country-capital pairs.\nCheck your Internet connection and try again.",
                                "Data Load Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        System.exit(1);
                    });
                    return;
                }

                final Map<String, String> finalMap = map;
                SwingUtilities.invokeLater(() -> {
                    loadingFrame.dispose();
                    QuizGame game = new QuizGame(finalMap);
                    game.setVisible(true);
                });
            }).start();
        });
    }

    private static String getStackTraceAsString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement elem : t.getStackTrace()) {
            sb.append(elem.toString()).append("\n");
        }
        return sb.toString();
    }
}
