package org.example.a311cardgame24;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HelloController {
    @FXML
    private HBox cardBox;
    @FXML
    private TextField expressionField;
    @FXML
    private TextField expressionInput;
    @FXML
    private Button findSolutionButton;
    @FXML
    private Button refreshButton;

    private final Random random = new Random();
    private final List<Card> currentCards = new ArrayList<>();
    private final String[] suits = {"clubs", "diamonds", "hearts", "spades"};

    // Runs when the window is created. It starts the game by picking cards.
    @FXML
    private void initialize() {
        refreshCards();
        expressionField.clear();
        expressionField.setEditable(false);
        expressionField.setFocusTraversable(false);
    }

    // Finds one valid 24 expression for the current four cards and shows it.
    @FXML
    private void onFindSolution() {
        String solution = solveFor24(cardValues(currentCards));
        if (solution == null) {
            expressionField.clear();
            showAlert("No solution exists for the current cards.");
            return;
        }
        expressionField.setText(solution);
    }

    // Picks a new set of cards and clears any player input.
    @FXML
    private void onRefresh() {
        refreshCards();
        expressionField.clear();
        expressionInput.clear();
    }

    // Checks whether the player entered a valid expression for the current cards.
    @FXML
    private void onVerify() {
        String value = expressionInput.getText() == null ? "" : expressionInput.getText().trim();
        if (value.isEmpty()) {
            showAlert("Please enter an expression.");
            return;
        }

        if (!validateExpression(value, currentCards)) {
            showAlert("That expression does not use the displayed cards exactly once or does not evaluate to 24.");
            return;
        }

        showAlert("Correct! You made 24.");
    }

    // Creates a new valid set of four cards for the game.
    private void refreshCards() {
        List<Card> deck = buildDeck();
        currentCards.clear();

        while (currentCards.size() < 4) {
            currentCards.add(drawRandomCard(deck));
        }

        while (solveFor24(cardValues(currentCards)) == null) {
            currentCards.clear();
            deck = buildDeck();
            while (currentCards.size() < 4) {
                currentCards.add(drawRandomCard(deck));
            }
        }

        renderCards();
    }

    // Creates one full deck of cards without duplicates.
    private List<Card> buildDeck() {
        List<Card> deck = new ArrayList<>();
        for (String suit : suits) {
            for (int value = 1; value <= 13; value++) {
                deck.add(new Card(value, suit));
            }
        }
        return deck;
    }

    // Draws one random card from the deck and removes it so no card is used twice.
    private Card drawRandomCard(List<Card> deck) {
        int index = random.nextInt(deck.size());
        return deck.remove(index);
    }

    // Displays the current cards in the window.
    private void renderCards() {
        cardBox.getChildren().clear();
        for (Card card : currentCards) {
            cardBox.getChildren().add(createCard(card));
        }
    }

    // Builds one card image using the correct rank and suit file.
    private ImageView createCard(Card card) {
        String fileName = getCardFileName(card.value, card.suit);
        String path = "/cards/" + fileName;
        Image image = new Image(getClass().getResourceAsStream(path));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(90);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    // Converts a numeric value into the matching card file name, like ace_of_hearts.png.
    private String getCardFileName(int value, String suit) {
        String rank = switch (value) {
            case 1 -> "ace";
            case 11 -> "jack";
            case 12 -> "queen";
            case 13 -> "king";
            default -> String.valueOf(value);
        };
        return rank + "_of_" + suit + ".png";
    }

    // Checks whether the entered math expression is legal and equals 24 using the current cards.
    private boolean validateExpression(String expression, List<Card> cards) {
        String cleaned = expression.replace(" ", "");
        if (cleaned.isEmpty() || !cleaned.matches("[0-9+\\-*/()]+")) {
            return false;
        }

        List<Integer> numbers = extractNumbers(cleaned);
        if (numbers.size() != 4) {
            return false;
        }

        Map<Integer, Integer> required = cardCounts(cardValues(cards));
        Map<Integer, Integer> used = cardCounts(numbers);
        if (!required.equals(used)) {
            return false;
        }

        double result;
        try {
            result = evaluateExpression(cleaned);
        } catch (RuntimeException ex) {
            return false;
        }

        return Math.abs(result - 24.0) < 1e-8;
    }

    // Tries to find a valid expression that makes 24 from the given cards.
    private String solveFor24(List<Integer> cards) {
        if (cards == null || cards.size() == 0) {
            return null;
        }

        List<ExpressionNode> nodes = new ArrayList<>();
        for (int card : cards) {
            nodes.add(new ExpressionNode(card, String.valueOf(card)));
        }

        return solveExpressionSet(nodes);
    }

    // Recursively combines numbers to see if any operation chain reaches 24.
    private String solveExpressionSet(List<ExpressionNode> nodes) {
        if (nodes.size() == 1) {
            if (Math.abs(nodes.get(0).value - 24.0) < 1e-8) {
                return nodes.get(0).text;
            }
            return null;
        }

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                List<ExpressionNode> reduced = new ArrayList<>();
                for (int k = 0; k < nodes.size(); k++) {
                    if (k != i && k != j) {
                        reduced.add(nodes.get(k));
                    }
                }

                ExpressionNode left = nodes.get(i);
                ExpressionNode right = nodes.get(j);
                List<ExpressionNode> candidates = new ArrayList<>();

                candidates.add(new ExpressionNode(left.value + right.value, "(" + left.text + "+" + right.text + ")"));
                candidates.add(new ExpressionNode(left.value - right.value, "(" + left.text + "-" + right.text + ")"));
                candidates.add(new ExpressionNode(right.value - left.value, "(" + right.text + "-" + left.text + ")"));
                candidates.add(new ExpressionNode(left.value * right.value, "(" + left.text + "*" + right.text + ")"));
                if (Math.abs(right.value) > 1e-9) {
                    candidates.add(new ExpressionNode(left.value / right.value, "(" + left.text + "/" + right.text + ")"));
                }
                if (Math.abs(left.value) > 1e-9) {
                    candidates.add(new ExpressionNode(right.value / left.value, "(" + right.text + "/" + left.text + ")"));
                }

                for (ExpressionNode candidate : candidates) {
                    List<ExpressionNode> next = new ArrayList<>(reduced);
                    next.add(candidate);
                    String result = solveExpressionSet(next);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    // Pulls out the numbers from a text expression so we can compare them to the cards.
    private List<Integer> extractNumbers(String expression) {
        List<Integer> numbers = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (Character.isDigit(ch)) {
                current.append(ch);
            } else {
                if (current.length() > 0) {
                    numbers.add(Integer.parseInt(current.toString()));
                    current.setLength(0);
                }
            }
        }
        if (current.length() > 0) {
            numbers.add(Integer.parseInt(current.toString()));
        }
        return numbers;
    }

    // Counts how many times each card value appears in a list.
    private Map<Integer, Integer> cardCounts(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int value : values) {
            counts.put(value, counts.getOrDefault(value, 0) + 1);
        }
        return counts;
    }

    // Converts a list of cards into just their numeric values.
    private List<Integer> cardValues(List<Card> cards) {
        List<Integer> values = new ArrayList<>();
        for (Card card : cards) {
            values.add(card.value);
        }
        return values;
    }

    // Evaluates a math expression using Java's parser logic.
    private double evaluateExpression(String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        return parser.parse();
    }

    // Shows a pop-up message to the user.
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Card 24 Game");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // This class stores one real card using its value and its suit.
    private static class Card {
        private final int value;
        private final String suit;

        // Stores a card's rank and suit together.
        private Card(int value, String suit) {
            this.value = value;
            this.suit = suit;
        }
    }

    // This class stores a number and the expression text that created it.
    private static class ExpressionNode {
        private final double value;
        private final String text;

        // Saves the current value and its matching expression string.
        private ExpressionNode(double value, String text) {
            this.value = value;
            this.text = text;
        }
    }

    // This parser reads a math expression and calculates its result.
    private static class ExpressionParser {
        private final String expression;
        private int index;

        // Saves the expression that will be evaluated.
        private ExpressionParser(String expression) {
            this.expression = expression;
        }

        // Starts parsing and makes sure the whole expression was used.
        private double parse() {
            double value = parseExpression();
            if (index != expression.length()) {
                throw new IllegalArgumentException("Invalid expression");
            }
            return value;
        }

        // Parses addition and subtraction.
        private double parseExpression() {
            double value = parseTerm();
            while (index < expression.length()) {
                char operation = expression.charAt(index);
                if (operation == '+' || operation == '-') {
                    index++;
                    double next = parseTerm();
                    value = operation == '+' ? value + next : value - next;
                } else {
                    break;
                }
            }
            return value;
        }

        // Parses multiplication and division.
        private double parseTerm() {
            double value = parseFactor();
            while (index < expression.length()) {
                char operation = expression.charAt(index);
                if (operation == '*' || operation == '/') {
                    index++;
                    double next = parseFactor();
                    if (operation == '/') {
                        if (Math.abs(next) < 1e-9) {
                            throw new ArithmeticException("Division by zero");
                        }
                        value = value / next;
                    } else {
                        value = value * next;
                    }
                } else {
                    break;
                }
            }
            return value;
        }

        // Handles positive and negative numbers.
        private double parseFactor() {
            if (index < expression.length() && (expression.charAt(index) == '+' || expression.charAt(index) == '-')) {
                char sign = expression.charAt(index++);
                double value = parseFactor();
                return sign == '-' ? -value : value;
            }
            return parsePrimary();
        }

        // Reads a number or a parenthesized expression.
        private double parsePrimary() {
            if (index >= expression.length()) {
                throw new IllegalArgumentException("Unexpected end of expression");
            }

            char current = expression.charAt(index);
            if (current == '(') {
                index++;
                double value = parseExpression();
                if (index >= expression.length() || expression.charAt(index) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                index++;
                return value;
            }

            int start = index;
            while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("Invalid number");
            }
            return Integer.parseInt(expression.substring(start, index));
        }
    }
}
