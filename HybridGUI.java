// HybridGUI.java - Fixed: User can't choose type for result variable
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class HybridGUI {
    
    private static String analysisMode = "math";
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            showModeSelectionDialog();
            createAndShowGUI();
        });
    }
    
    private static void showModeSelectionDialog() {
        Object[] options = {"Source Code Form", "Math Form"};
        int choice = JOptionPane.showOptionDialog(null,
                "Choose input format:", "Select Input Mode",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);
        analysisMode = (choice == 0) ? "source" : "math";
    }
    
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Hybrid Implementation - Compiler Phases");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 800);
        frame.setLocationRelativeTo(null);
        
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        
        String inputLabel = analysisMode.equals("source") ? 
            "Enter source code (use 'is' for assignment, e.g., x is y + 5):" :
            "Enter math equation (use '=' or 'is'):";
            
        inputPanel.add(new JLabel(inputLabel), BorderLayout.NORTH);
        JTextField inputField = new JTextField();
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        // Output areas
        JTextArea lexicalArea = new JTextArea();
        JTextArea syntaxArea = new JTextArea();
        JTextArea semanticArea = new JTextArea();
        JTextArea executionArea = new JTextArea();
        
        configureTextArea(lexicalArea);
        configureTextArea(syntaxArea);
        configureTextArea(semanticArea);
        configureTextArea(executionArea);
        
        Font monoFont = new Font("Monospaced", Font.BOLD, 13);
        lexicalArea.setFont(monoFont);
        syntaxArea.setFont(monoFont);
        semanticArea.setFont(monoFont);
        executionArea.setFont(monoFont);
        
        // Left panel: Lexical
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("1. Lexical Analysis"));
        leftPanel.add(new JScrollPane(lexicalArea));
        
        // Right panel: Syntax, Semantic, Execution
        JPanel rightPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        rightPanel.add(createTitledPanel("2. Syntax Tree", syntaxArea));
        rightPanel.add(createTitledPanel("3. Semantic Analysis", semanticArea));
        rightPanel.add(createTitledPanel("4. Direct Execution", executionArea));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.3);
        
        // Buttons
        JButton analyzeBtn = new JButton("Analyze");
        JButton clearBtn = new JButton("Clear");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearBtn);
        buttonPanel.add(analyzeBtn);
        
        main.add(inputPanel, BorderLayout.NORTH);
        main.add(splitPane, BorderLayout.CENTER);
        main.add(buttonPanel, BorderLayout.SOUTH);
        
        frame.setContentPane(main);
        frame.setVisible(true);
        
        analyzeBtn.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter an equation", 
                                            "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Store original input for display
            String originalInput = input;
            
            // Validate input based on mode
            if (analysisMode.equals("source")) {
                if (!input.toLowerCase().contains(" is ") && !input.toLowerCase().contains("is=")) {
                    JOptionPane.showMessageDialog(frame, 
                        "In Source Code mode, you must use 'is' for assignment.\n" +
                        "Example: x is y + 5",
                        "Syntax Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String displayInput = input.replace("=", "is");
                if (!displayInput.equals(input)) {
                    int response = JOptionPane.showConfirmDialog(frame,
                        "I detected '=' in source mode. Should I convert it to 'is'?\n" +
                        "Original: " + input + "\n" +
                        "Converted: " + displayInput,
                        "Convert to 'is'?", 
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                    
                    if (response == JOptionPane.YES_OPTION) {
                        input = displayInput;
                        inputField.setText(displayInput);
                    }
                }
            } else {
                // In math mode, accept both "=" and "is"
                if (input.contains("=")) {
                    String displayInput = input.replace("=", "is");
                    input = displayInput;
                }
            }
            
            // --- Phase 1: Lexical Analysis ---
            HybridLexicalAnalyzer lexicalAnalyzer = new HybridLexicalAnalyzer();
            HybridLexicalAnalyzer.Result lexResult = lexicalAnalyzer.analyze(input);
            
            if (lexResult.hasError) {
                lexicalArea.setText("❌ Lexical Error: " + lexResult.errorMessage);
                return;
            }
            
            lexicalArea.setText("Lexical Output: " + lexResult.lexicalOutput + 
                               "\n\nVariables: " + formatVariableTable(lexResult.variableTable));
            
            // --- ASK FOR TYPES AND VALUES ONLY FOR INPUT VARIABLES (right side) ---
            Map<String, String[]> userInput = showTypeAndValueInputDialog(lexResult.variableTable);
            if (userInput == null) {
                lexicalArea.append("\n\n⚠️ Analysis cancelled by user");
                return;
            }
            
            // Extract types and values for INPUT variables only
            Map<String, String> userTypes = new HashMap<>();
            Map<String, Object> userValues = new HashMap<>();
            
            for (Map.Entry<String, String[]> entry : userInput.entrySet()) {
                String originalName = entry.getKey();
                String type = entry.getValue()[0];
                String valueStr = entry.getValue()[1];
                
                userTypes.put(originalName, type);
                
                // Find V name for this original name
                String vName = "";
                for (HybridLexicalAnalyzer.VEntry vEntry : lexResult.variableTable.values()) {
                    if (vEntry.originalName.equals(originalName)) {
                        vName = vEntry.vName;
                        break;
                    }
                }
                
                try {
                    if (type.equals("float")) {
                        userValues.put(vName, Float.parseFloat(valueStr));
                    } else {
                        userValues.put(vName, Integer.parseInt(valueStr));
                    }
                } catch (NumberFormatException ex) {
                    lexicalArea.append("\n\n❌ Invalid number for " + originalName);
                    return;
                }
            }
            
            // Update variable table with user types
            for (Map.Entry<String, String> typeEntry : userTypes.entrySet()) {
                String originalName = typeEntry.getKey();
                String type = typeEntry.getValue();
                
                for (HybridLexicalAnalyzer.VEntry vEntry : lexResult.variableTable.values()) {
                    if (vEntry.originalName.equals(originalName)) {
                        vEntry.type = type;
                        break;
                    }
                }
            }
            
            // --- Phase 2: Syntax Analysis ---
            HybridSyntaxAnalyzer syntaxAnalyzer = new HybridSyntaxAnalyzer();
            // Convert "is" back to "=" for syntax analysis
            String syntaxInput = lexResult.lexicalOutput.replace("is", "=");
            HybridSyntaxAnalyzer.Result syntaxResult = 
                syntaxAnalyzer.analyze(syntaxInput, lexResult.originalToVMap);
            
            if (syntaxResult.hasError) {
                syntaxArea.setText(syntaxResult.message);
                return;
            }
            
            syntaxArea.setText(syntaxResult.message + "\n\n" + 
                              HybridSyntaxAnalyzer.treeToString(syntaxResult.syntaxTree));
            
            // --- Phase 3: Semantic Analysis ---
            // Determine result variable type automatically based on expression
            String resultVarType = determineResultTypeFromExpression(lexResult.variableTable, userTypes, input);
            
            // Add result variable type to userTypes
            String resultVarOriginalName = "";
            for (HybridLexicalAnalyzer.VEntry vEntry : lexResult.variableTable.values()) {
                if (vEntry.vName.equals("V1")) {
                    resultVarOriginalName = vEntry.originalName;
                    break;
                }
            }
            
            if (!resultVarOriginalName.isEmpty()) {
                userTypes.put(resultVarOriginalName, resultVarType);
                
                // Update variable table with calculated result type
                for (HybridLexicalAnalyzer.VEntry vEntry : lexResult.variableTable.values()) {
                    if (vEntry.originalName.equals(resultVarOriginalName)) {
                        vEntry.type = resultVarType;
                        break;
                    }
                }
            }
            
            HybridSemanticAnalyzer semanticAnalyzer = new HybridSemanticAnalyzer();
            HybridSemanticAnalyzer.Result semanticResult = 
                semanticAnalyzer.analyze(syntaxResult.syntaxTree, 
                                       lexResult.variableTable, userTypes);
            
            semanticArea.setText(semanticResult.message + "\n\n" + 
                               HybridSemanticAnalyzer.treeToString(semanticResult.annotatedTree) +
                               "\n\nType Table:\n" + formatTypeTable(semanticResult.typeTable));
            
            // --- Phase 4: Direct Execution ---
            String executionResult = executeWithTreeDisplay(originalInput, 
                                                          lexResult.variableTable, 
                                                          userValues,
                                                          syntaxResult.syntaxTree,
                                                          resultVarType);
            executionArea.setText(executionResult);
        });
        
        clearBtn.addActionListener(e -> {
            inputField.setText("");
            lexicalArea.setText("");
            syntaxArea.setText("");
            semanticArea.setText("");
            executionArea.setText("");
        });
    }
    
    private static String determineResultTypeFromExpression(Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                                                          Map<String, String> inputTypes,
                                                          String equation) {
        
        // Default to int
        String resultType = "int";
        
        // Check if any input variable is float
        for (Map.Entry<String, String> entry : inputTypes.entrySet()) {
            if (entry.getValue().equals("float")) {
                resultType = "float";
                break;
            }
        }
        
        // Check if equation contains decimal constant
        if (equation.matches(".*\\d+\\.\\d+.*")) {
            resultType = "float";
        }
        
        // Check for division operator - division often produces float
        if (equation.contains("/")) {
            resultType = "float";
        }
        
        return resultType;
    }
    
    private static Map<String, String[]> showTypeAndValueInputDialog(
            Map<String, HybridLexicalAnalyzer.VEntry> variableTable) {
        
        if (variableTable == null || variableTable.isEmpty()) {
            return new HashMap<>();
        }
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Header
        JPanel header = new JPanel(new GridLayout(1, 3, 5, 5));
        header.add(new JLabel("Variable", SwingConstants.CENTER));
        header.add(new JLabel("Type", SwingConstants.CENTER));
        header.add(new JLabel("Value", SwingConstants.CENTER));
        panel.add(header);
        
        // Add separator
        panel.add(new JSeparator(SwingConstants.HORIZONTAL));
        
        Map<String, JComboBox<String>> typeBoxes = new HashMap<>();
        Map<String, JTextField> valueFields = new HashMap<>();
        
        // Create row for each INPUT variable only (skip result variable V1)
        for (HybridLexicalAnalyzer.VEntry entry : variableTable.values()) {
            // Skip the result variable (V1)
            if (entry.vName.equals("V1")) {
                continue;
            }
            
            JPanel row = new JPanel(new GridLayout(1, 3, 5, 5));
            
            // Variable name with V1(x) format
            String displayName = entry.vName + " (" + entry.originalName + ")";
            JLabel varLabel = new JLabel(displayName, SwingConstants.CENTER);
            varLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
            row.add(varLabel);
            
            // Type dropdown for INPUT variables only
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"int", "float"});
            
            // Try to guess if this should be int or float
            // Default to int for simple variables
            boolean likelyInt = true;
            
            typeCombo.setSelectedItem(likelyInt ? "int" : "float");
            typeCombo.setFont(new Font("Monospaced", Font.PLAIN, 12));
            typeBoxes.put(entry.originalName, typeCombo);
            row.add(typeCombo);
            
            // Value field for INPUT variables
            JTextField valueField = new JTextField();
            valueField.setFont(new Font("Monospaced", Font.PLAIN, 12));
            
            // Suggest a value based on type
            valueField.setText(typeCombo.getSelectedItem().equals("int") ? "0" : "0.0");
            
            valueFields.put(entry.originalName, valueField);
            row.add(valueField);
            
            panel.add(row);
        }
        
        // Add note about result variable
        if (variableTable.containsKey("V1")) {
            String resultOriginalName = "";
            for (HybridLexicalAnalyzer.VEntry entry : variableTable.values()) {
                if (entry.vName.equals("V1")) {
                    resultOriginalName = entry.originalName;
                    break;
                }
            }
            
            JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            notePanel.add(new JLabel("Note: V1 (" + resultOriginalName + ") is the result variable. Its type will be determined automatically."));
            panel.add(notePanel);
        }
        
        // If no input variables (only result variable), show message
        if (typeBoxes.isEmpty()) {
            JOptionPane.showMessageDialog(null, 
                "No input variables to set. The result will be calculated from constants only.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return new HashMap<>();
        }
        
        // Show dialog
        int result = JOptionPane.showConfirmDialog(
            null, 
            new JScrollPane(panel), 
            "Enter Types and Values for INPUT Variables", 
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            Map<String, String[]> info = new HashMap<>();
            for (String originalName : typeBoxes.keySet()) {
                String type = (String) typeBoxes.get(originalName).getSelectedItem();
                String value = valueFields.get(originalName).getText().trim();
                
                if (value.isEmpty()) {
                    JOptionPane.showMessageDialog(null, 
                        "Please enter a value for " + originalName,
                        "Missing Value", JOptionPane.WARNING_MESSAGE);
                    return null;
                }
                
                info.put(originalName, new String[]{type, value});
            }
            return info;
        }
        
        return null;
    }
    
    private static String executeWithTreeDisplay(String equation,
                                                Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                                                Map<String, Object> inputValues,
                                                HybridSyntaxAnalyzer.TreeNode syntaxTree,
                                                String resultVarType) {
        
        StringBuilder result = new StringBuilder();
        result.append("Direct Execution Results:\n");
        result.append("=========================\n\n");
        
        // Show input values
        if (!inputValues.isEmpty()) {
            result.append("Input Variable Values:\n");
            for (Map.Entry<String, Object> entry : inputValues.entrySet()) {
                String vName = entry.getKey();
                Object value = entry.getValue();
                
                // Skip result variable (V1) from input values display
                if (vName.equals("V1")) continue;
                
                // Find original name
                String originalName = "";
                for (HybridLexicalAnalyzer.VEntry vEntry : variableTable.values()) {
                    if (vEntry.vName.equals(vName)) {
                        originalName = vEntry.originalName;
                        break;
                    }
                }
                
                String type = value instanceof Float ? "float" : "int";
                result.append("  ").append(vName).append(" (").append(originalName)
                      .append(") = ").append(value).append(" [").append(type).append("]\n");
            }
            result.append("\n");
        }
        
        try {
            // Prepare equation for evaluation
            String evalEquation = equation.replaceAll("(?i)\\bis\\b", "=");
            
            // Replace input variable names with their values
            for (Map.Entry<String, Object> entry : inputValues.entrySet()) {
                String vName = entry.getKey();
                
                // Skip result variable (V1) - we don't replace it
                if (vName.equals("V1")) continue;
                
                // Replace V1, V2, etc. with values
                evalEquation = evalEquation.replaceAll("\\b" + vName + "\\b", 
                                                      entry.getValue().toString());
                
                // Also replace original names with values
                for (HybridLexicalAnalyzer.VEntry vEntry : variableTable.values()) {
                    if (vEntry.vName.equals(vName)) {
                        evalEquation = evalEquation.replaceAll("\\b" + vEntry.originalName + "\\b",
                                                              entry.getValue().toString());
                    }
                }
            }
            
            // Evaluate based on result type
            Object evalResult;
            if (resultVarType.equals("float")) {
                evalResult = evaluateExpressionFloat(evalEquation);
            } else {
                evalResult = evaluateExpressionInt(evalEquation);
            }
            
            // Find result variable
            String resultVar = "V1";
            String resultOriginalName = "";
            for (HybridLexicalAnalyzer.VEntry vEntry : variableTable.values()) {
                if (vEntry.vName.equals("V1")) {
                    resultOriginalName = vEntry.originalName;
                    break;
                }
            }
            
            // Build tree with values
            result.append("Execution Tree with Values:\n");
            result.append("───────────────────────────\n");
            
            if (syntaxTree != null) {
                String treeWithValues = buildTreeWithValues(syntaxTree, inputValues, variableTable, evalResult);
                result.append(treeWithValues).append("\n");
            }
            
            result.append("\nResult:\n");
            result.append("  ").append(resultVar).append(" (").append(resultOriginalName).append(") is ");
            
            // Format based on type
            if (evalResult instanceof Float || evalResult instanceof Double) {
                result.append(String.format("%.2f", evalResult));
            } else {
                result.append(evalResult);
            }
            
            result.append(" [").append(resultVarType).append("]\n");
            
            if (!resultOriginalName.isEmpty()) {
                result.append("  ").append(resultOriginalName).append(" = ");
                if (evalResult instanceof Float || evalResult instanceof Double) {
                    result.append(String.format("%.2f", evalResult));
                } else {
                    result.append(evalResult);
                }
                result.append("\n");
            }
            
        } catch (Exception e) {
            result.append("\n❌ Evaluation Error: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    // All other helper methods remain the same...
    // evaluateExpressionFloat, evaluateExpressionInt, evaluateArithmeticFloat, evaluateArithmeticInt
    // buildTreeWithValues, buildTreeVisualWithValues, formatNodeWithValue, formatValue, formatResultValue
    // formatVariableTable, formatTypeTable, createTitledPanel, configureTextArea
    
    // I'll include the key evaluation methods:
    
    private static Object evaluateExpressionFloat(String expr) {
        expr = expr.replaceAll("\\s+", "");
        
        if (expr.contains("=")) {
            String[] parts = expr.split("=");
            if (parts.length > 1) {
                expr = parts[1];
            }
        }
        
        return evaluateArithmeticFloat(expr);
    }
    
    private static Object evaluateExpressionInt(String expr) {
        expr = expr.replaceAll("\\s+", "");
        
        if (expr.contains("=")) {
            String[] parts = expr.split("=");
            if (parts.length > 1) {
                expr = parts[1];
            }
        }
        
        try {
            return evaluateArithmeticInt(expr);
        } catch (ArithmeticException e) {
            // If integer division produces fraction, fall back to float
            return evaluateArithmeticFloat(expr);
        }
    }
    
    private static double evaluateArithmeticFloat(String expr) {
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException e) {
            while (expr.contains("(")) {
                int open = expr.lastIndexOf("(");
                int close = expr.indexOf(")", open);
                if (close == -1) break;
                
                String inner = expr.substring(open + 1, close);
                double innerResult = evaluateArithmeticFloat(inner);
                expr = expr.substring(0, open) + innerResult + expr.substring(close + 1);
            }
            
            String[] mulDivParts = expr.split("(?<=[*/])|(?=[*/])");
            if (mulDivParts.length > 1) {
                double result = Double.parseDouble(mulDivParts[0]);
                for (int i = 1; i < mulDivParts.length; i += 2) {
                    String op = mulDivParts[i];
                    double num = Double.parseDouble(mulDivParts[i + 1]);
                    if (op.equals("*")) {
                        result *= num;
                    } else {
                        result /= num;
                    }
                }
                return result;
            }
            
            String[] addSubParts = expr.split("(?<=[+-])|(?=[+-])");
            if (addSubParts.length > 1) {
                double result = Double.parseDouble(addSubParts[0]);
                for (int i = 1; i < addSubParts.length; i += 2) {
                    String op = addSubParts[i];
                    double num = Double.parseDouble(addSubParts[i + 1]);
                    if (op.equals("+")) {
                        result += num;
                    } else {
                        result -= num;
                    }
                }
                return result;
            }
            
            return Double.parseDouble(expr);
        }
    }
    
    private static int evaluateArithmeticInt(String expr) {
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            while (expr.contains("(")) {
                int open = expr.lastIndexOf("(");
                int close = expr.indexOf(")", open);
                if (close == -1) break;
                
                String inner = expr.substring(open + 1, close);
                int innerResult = evaluateArithmeticInt(inner);
                expr = expr.substring(0, open) + innerResult + expr.substring(close + 1);
            }
            
            String[] mulDivParts = expr.split("(?<=[*/])|(?=[*/])");
            if (mulDivParts.length > 1) {
                int result = Integer.parseInt(mulDivParts[0]);
                for (int i = 1; i < mulDivParts.length; i += 2) {
                    String op = mulDivParts[i];
                    int num = Integer.parseInt(mulDivParts[i + 1]);
                    if (op.equals("*")) {
                        result *= num;
                    } else {
                        if (result % num != 0) {
                            throw new ArithmeticException("Integer division produces fraction");
                        }
                        result /= num;
                    }
                }
                return result;
            }
            
            String[] addSubParts = expr.split("(?<=[+-])|(?=[+-])");
            if (addSubParts.length > 1) {
                int result = Integer.parseInt(addSubParts[0]);
                for (int i = 1; i < addSubParts.length; i += 2) {
                    String op = addSubParts[i];
                    int num = Integer.parseInt(addSubParts[i + 1]);
                    if (op.equals("+")) {
                        result += num;
                    } else {
                        result -= num;
                    }
                }
                return result;
            }
            
            return Integer.parseInt(expr);
        }
    }
    
    private static String buildTreeWithValues(HybridSyntaxAnalyzer.TreeNode node,
                                            Map<String, Object> values,
                                            Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                                            Object resultValue) {
        
        if (node == null) return "Empty tree";
        StringBuilder sb = new StringBuilder();
        buildTreeVisualWithValues(sb, node, "", true, values, variableTable, resultValue);
        return sb.toString();
    }
    
    private static void buildTreeVisualWithValues(StringBuilder sb,
                                                 HybridSyntaxAnalyzer.TreeNode node,
                                                 String prefix,
                                                 boolean isTail,
                                                 Map<String, Object> values,
                                                 Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                                                 Object resultValue) {
        
        if (node == null) return;
        
        if (node.right != null) {
            buildTreeVisualWithValues(sb, node.right, prefix + (isTail ? "│   " : "    "), 
                                     false, values, variableTable, resultValue);
        }
        
        sb.append(prefix).append(isTail ? "└── " : "┌── ");
        String displayValue = formatNodeWithValue(node.value, values, variableTable, resultValue);
        sb.append(displayValue).append("\n");
        
        if (node.left != null) {
            buildTreeVisualWithValues(sb, node.left, prefix + (isTail ? "    " : "│   "), 
                                     true, values, variableTable, resultValue);
        }
    }
    
    private static String formatNodeWithValue(String nodeValue,
                                            Map<String, Object> values,
                                            Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                                            Object resultValue) {
        
        if (nodeValue == null) return "";
        
        if (nodeValue.equals("=")) {
            return "is";
        }
        
        if (nodeValue.matches("V\\d+")) {
            String originalName = "";
            for (HybridLexicalAnalyzer.VEntry entry : variableTable.values()) {
                if (entry.vName.equals(nodeValue)) {
                    originalName = entry.originalName;
                    break;
                }
            }
            
            boolean isResultVar = nodeValue.equals("V1");
            
            if (isResultVar) {
                return nodeValue + "(" + originalName + "): " + formatResultValue(resultValue);
            } else if (values.containsKey(nodeValue)) {
                Object value = values.get(nodeValue);
                return nodeValue + "(" + originalName + "): " + formatValue(value);
            } else {
                return nodeValue + "(" + originalName + ")";
            }
        }
        
        if (nodeValue.matches("\\d+(\\.\\d+)?")) {
            return nodeValue;
        }
        
        return nodeValue;
    }
    
    private static String formatValue(Object value) {
        if (value instanceof Float) {
            return String.format("%.2f", (Float) value);
        } else if (value instanceof Integer) {
            return value.toString();
        } else if (value instanceof Double) {
            return String.format("%.2f", (Double) value);
        }
        return value.toString();
    }
    
    private static String formatResultValue(Object value) {
        if (value instanceof Float) {
            float f = (Float) value;
            if (f == (int) f) {
                return String.format("%d", (int) f);
            }
            return String.format("%.2f", f);
        } else if (value instanceof Integer) {
            return value.toString();
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (d == (int) d) {
                return String.format("%d", (int) d);
            }
            return String.format("%.2f", d);
        }
        return value.toString();
    }
    
    private static String formatVariableTable(Map<String, HybridLexicalAnalyzer.VEntry> table) {
        StringBuilder sb = new StringBuilder();
        for (HybridLexicalAnalyzer.VEntry entry : table.values()) {
            sb.append("\n  ").append(entry.originalName).append(" → ").append(entry.vName);
        }
        return sb.toString();
    }
    
    private static String formatTypeTable(Map<String, String> typeTable) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : typeTable.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
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
        area.setBackground(new Color(240, 240, 240));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }
}