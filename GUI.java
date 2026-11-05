import javax.swing.*;
import java.awt.*;
import java.util.*;

public class GUI {

    private static String analysisMode = "math";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            showModeSelectionDialog();
            createAndShowGUI();
        });
    }

    private static void showModeSelectionDialog() {
        Object[] options = {"Source Code", "Math Form"};
        int choice = JOptionPane.showOptionDialog(null,
                "Choose input type:", "Select Mode",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);
        analysisMode = (choice == 0) ? "source" : "math";
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Compiler Phases");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 800);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField inputField = new JTextField();

        JTextArea outputArea = new JTextArea();   // Lexical
        JTextArea syntaxArea = new JTextArea();   // Syntax Tree
        JTextArea semanticArea = new JTextArea(); // Semantic
        JTextArea icgArea = new JTextArea();      // ICG
        JTextArea optimizedArea = new JTextArea(); // Code Optimizer
        JTextArea assemblyArea = new JTextArea();  // Assembly / Code Generator

        configureTextArea(outputArea);
        configureTextArea(syntaxArea);
        configureTextArea(semanticArea);
        configureTextArea(icgArea);
        configureTextArea(optimizedArea);
        configureTextArea(assemblyArea);

        Font monoFont = new Font("Monospaced", Font.BOLD, 13);
        outputArea.setFont(monoFont);
        syntaxArea.setFont(monoFont);
        semanticArea.setFont(monoFont);
        icgArea.setFont(monoFont);
        optimizedArea.setFont(monoFont);
        assemblyArea.setFont(monoFont);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(new JLabel("Enter your " + (analysisMode.equals("math") ? "equation:" : "source code:")), BorderLayout.NORTH);
        top.add(inputField, BorderLayout.CENTER);

        // Left panel: Lexical
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("1. Lexical Output"));
        left.add(new JScrollPane(outputArea));

        // Right panel: All other phases
        JPanel right = new JPanel(new GridLayout(5, 1, 5, 5));

        right.add(createTitledPanel("2. Syntax Tree", syntaxArea));
        right.add(createTitledPanel("3. Semantic Analysis", semanticArea));
        right.add(createTitledPanel("4. Intermediate Code (3AC)", icgArea));
        right.add(createTitledPanel("5. Code Optimizer Output", optimizedArea));
        right.add(createTitledPanel("6. Assembly / Code Generator", assemblyArea));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.3);

        JButton analyzeBtn = new JButton("Analyze"), clearBtn = new JButton("Clear");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(clearBtn);
        bottom.add(analyzeBtn);

        main.add(top, BorderLayout.NORTH);
        main.add(splitPane, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        frame.setContentPane(main);
        frame.setVisible(true);

        analyzeBtn.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (input.isEmpty()) return;

            // --- 1. Lexical ---
            LexicalAnalyzerCore.Result lexResult = analysisMode.equals("math")
                    ? LexicalAnalyzerCore.analyzeMath(input)
                    : LexicalAnalyzerCore.analyzeSourceCode(input);
            outputArea.setText(lexResult.output);

            // --- 2. Syntax ---
            if (lexResult.syntaxTree != null) {
                syntaxArea.setText(lexResult.syntaxMessage + "\n\n" + SyntaxAnalyzer.printTree(lexResult.syntaxTree));
            } else {
                syntaxArea.setText("❌ Syntax Error");
                return;
            }

            // --- 3. User Input for Types ---
            Set<String> identifiers = new HashSet<>();
            for(Map.Entry<String, LexicalAnalyzerCore.Result.SymbolEntry> ent : lexResult.variableMap.entrySet()) {
                if(ent.getValue().id > 0) identifiers.add(ent.getKey());
            }

            Map<String, String> userTypes = showTypeInputDialog(identifiers);
            for (Map.Entry<String, String> userEntry : userTypes.entrySet()) {
                LexicalAnalyzerCore.Result.SymbolEntry entry = lexResult.variableMap.get(userEntry.getKey());
                if (entry != null) entry.type = userEntry.getValue();
            }

            // --- 4. Semantic ---
            Map<String, String> typeMapSimple = new HashMap<>();
            for (Map.Entry<String, LexicalAnalyzerCore.Result.SymbolEntry> entry : lexResult.variableMap.entrySet()) {
                typeMapSimple.put(entry.getKey(), entry.getValue().type);
            }
            SemanticAnalyzer.Result semResult = SemanticAnalyzer.analyze(lexResult.syntaxTree, typeMapSimple);
            if (semResult.treeRoot != null) {
                semanticArea.setText(semResult.message + "\n\n" + SyntaxAnalyzer.printTree(semResult.treeRoot));
            } else {
                semanticArea.setText(semResult.message);
            }

            // --- 5. Intermediate Code ---
            String icg = IntermediateCodeGenerator.generate(lexResult.syntaxTree, lexResult.variableMap);
            icgArea.setText(icg);

            // --- 6. Code Optimizer ---
            String optimized = CodeOptimizer.optimize(icg);
            optimizedArea.setText(optimized);

            // --- 7. Assembly / Code Generator ---
            String assembly = CodeGenerator.generateAssembly(optimized);
            assemblyArea.setText(assembly);
        });

        clearBtn.addActionListener(e -> {
            inputField.setText("");
            outputArea.setText("");
            syntaxArea.setText("");
            semanticArea.setText("");
            icgArea.setText("");
            optimizedArea.setText("");
            assemblyArea.setText("");
        });
    }

    private static JPanel createTitledPanel(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(area));
        return panel;
    }

    private static void configureTextArea(JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }

    private static Map<String, String> showTypeInputDialog(Set<String> tokens) {
        if(tokens.isEmpty()) return new HashMap<>();
        JTextField floatField = new JTextField();
        JTextField intField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.add(new JLabel("Detected Variables: " + tokens.toString()));
        panel.add(new JLabel("Enter float variables (comma separated):"));
        panel.add(floatField);
        panel.add(new JLabel("Enter int variables (comma separated):"));
        panel.add(intField);
        int result = JOptionPane.showConfirmDialog(null, panel, "Define Variable Types", JOptionPane.OK_CANCEL_OPTION);
        Map<String, String> types = new HashMap<>();
        if (result == JOptionPane.OK_OPTION) {
            for (String f : floatField.getText().split(",")) if (!f.trim().isEmpty()) types.put(f.trim(), "float");
            for (String i : intField.getText().split(",")) if (!i.trim().isEmpty()) types.put(i.trim(), "int");
        }
        return types;
    }
}
