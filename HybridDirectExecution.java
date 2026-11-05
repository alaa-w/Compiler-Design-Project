// HybridDirectExecution.java - Direct Execution for Hybrid Mode
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class HybridDirectExecution {
    
    public static class ExecutionResult {
        public String output;
        public Map<String, Object> values;
        public String finalResult;
        
        public ExecutionResult() {
            output = "";
            values = new HashMap<>();
            finalResult = "";
        }
    }
    
    public static ExecutionResult execute(String originalEquation,
                                         Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                                         Map<String, String> userTypes) {
        ExecutionResult result = new ExecutionResult();
        
        if (variableTable.isEmpty()) {
            result.output = "No variables to execute";
            return result;
        }
        
        // Show dialog to get values
        Map<String, String[]> valueInfo = showValueInputDialog(variableTable, userTypes);
        if (valueInfo == null || valueInfo.isEmpty()) {
            result.output = "Execution cancelled";
            return result;
        }
        
        // Store values
        for (Map.Entry<String, String[]> entry : valueInfo.entrySet()) {
            String vName = entry.getKey();
            String type = entry.getValue()[0];
            String valueStr = entry.getValue()[1];
            
            try {
                if (type.equals("float")) {
                    result.values.put(vName, Float.parseFloat(valueStr));
                } else {
                    result.values.put(vName, Integer.parseInt(valueStr));
                }
            } catch (NumberFormatException e) {
                result.output = "Error: Invalid number for " + vName;
                return result;
            }
        }
        
        // Evaluate the expression
        try {
            // Convert equation to evaluatable form
            String evaluatable = originalEquation.replaceAll("(?i)\\bis\\b", "=");
            
            // Replace variable names with their V names
            for (HybridLexicalAnalyzer.VEntry entry : variableTable.values()) {
                evaluatable = evaluatable.replaceAll("\\b" + entry.originalName + "\\b", entry.vName);
            }
            
            // Replace V names with values
            for (Map.Entry<String, Object> valueEntry : result.values.entrySet()) {
                evaluatable = evaluatable.replaceAll("\\b" + valueEntry.getKey() + "\\b", 
                                                    valueEntry.getValue().toString());
            }
            
            // Evaluate
            double evalResult = evaluate(evaluatable);
            result.finalResult = String.valueOf(evalResult);
            result.output = buildOutputString(variableTable, valueInfo, evalResult);
            
        } catch (Exception e) {
            result.output = "Evaluation error: " + e.getMessage();
        }
        
        return result;
    }
    
    private static Map<String, String[]> showValueInputDialog(
            Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
            Map<String, String> userTypes) {
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Header
        JPanel header = new JPanel(new GridLayout(1, 3));
        header.add(new JLabel("Variable", SwingConstants.CENTER));
        header.add(new JLabel("Type", SwingConstants.CENTER));
        header.add(new JLabel("Value", SwingConstants.CENTER));
        panel.add(header);
        
        Map<String, JComboBox<String>> typeBoxes = new HashMap<>();
        Map<String, JTextField> valueFields = new HashMap<>();
        
        for (HybridLexicalAnalyzer.VEntry entry : variableTable.values()) {
            JPanel row = new JPanel(new GridLayout(1, 3, 5, 5));
            
            // Variable name with original name in parentheses
            String displayName = entry.vName + " (" + entry.originalName + ")";
            row.add(new JLabel(displayName, SwingConstants.CENTER));
            
            // Type dropdown (pre-select user type if available)
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"int", "float"});
            String defaultType = userTypes.getOrDefault(entry.originalName, "float");
            typeCombo.setSelectedItem(defaultType);
            typeBoxes.put(entry.vName, typeCombo);
            row.add(typeCombo);
            
            // Value field
            JTextField valueField = new JTextField();
            valueFields.put(entry.vName, valueField);
            row.add(valueField);
            
            panel.add(row);
        }
        
        int result = JOptionPane.showConfirmDialog(
            null, panel, "Enter Values for Variables", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            Map<String, String[]> info = new HashMap<>();
            for (String vName : typeBoxes.keySet()) {
                String type = (String) typeBoxes.get(vName).getSelectedItem();
                String value = valueFields.get(vName).getText().trim();
                if (!value.isEmpty()) {
                    info.put(vName, new String[]{type, value});
                }
            }
            return info;
        }
        
        return null;
    }
    
    private static double evaluate(String expression) {
        // Remove spaces
        expression = expression.replaceAll("\\s+", "");
        
        // Handle assignment (extract right side)
        if (expression.contains("=")) {
            String[] parts = expression.split("=");
            if (parts.length > 1) {
                expression = parts[1];
            }
        }
        
        // Simple expression evaluator
        return evaluateExpression(expression);
    }
    
    private static double evaluateExpression(String expr) {
        // Handle parentheses
        while (expr.contains("(")) {
            int open = expr.lastIndexOf("(");
            int close = expr.indexOf(")", open);
            if (close == -1) break;
            
            String inner = expr.substring(open + 1, close);
            double innerResult = evaluateExpression(inner);
            expr = expr.substring(0, open) + innerResult + expr.substring(close + 1);
        }
        
        // Handle multiplication and division
        String[] tokens = expr.split("(?<=[*/])|(?=[*/])");
        if (tokens.length > 1) {
            double result = Double.parseDouble(tokens[0]);
            for (int i = 1; i < tokens.length; i += 2) {
                String op = tokens[i];
                double num = Double.parseDouble(tokens[i + 1]);
                if (op.equals("*")) {
                    result *= num;
                } else {
                    result /= num;
                }
            }
            return result;
        }
        
        // Handle addition and subtraction
        tokens = expr.split("(?<=[+-])|(?=[+-])");
        if (tokens.length > 1) {
            double result = Double.parseDouble(tokens[0]);
            for (int i = 1; i < tokens.length; i += 2) {
                String op = tokens[i];
                double num = Double.parseDouble(tokens[i + 1]);
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
    
    private static String buildOutputString(
            Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
            Map<String, String[]> valueInfo,
            double result) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Direct Execution Results:\n");
        sb.append("=========================\n\n");
        
        sb.append("Variable Values:\n");
        for (Map.Entry<String, String[]> entry : valueInfo.entrySet()) {
            String vName = entry.getKey();
            String[] info = entry.getValue();
            String type = info[0];
            String value = info[1];
            
            // Find original name
            String originalName = "";
            for (HybridLexicalAnalyzer.VEntry vEntry : variableTable.values()) {
                if (vEntry.vName.equals(vName)) {
                    originalName = vEntry.originalName;
                    break;
                }
            }
            
            sb.append("  ").append(vName).append(" (").append(originalName)
              .append(") = ").append(value).append(" [").append(type).append("]\n");
        }
        
        sb.append("\nEvaluation Result:\n");
        sb.append("  ").append(result).append("\n");
        
        return sb.toString();
    }
}