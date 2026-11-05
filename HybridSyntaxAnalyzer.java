// HybridSyntaxAnalyzer.java - Separate Syntax Analyzer for Hybrid Mode
import java.util.*;

public class HybridSyntaxAnalyzer {
    
    public static class TreeNode {
        public String value;
        public String displayValue; // For showing V1, V2 instead of id:name
        public TreeNode left, right;
        
        public TreeNode(String value) {
            this.value = value;
            this.displayValue = value;
        }
        
        public TreeNode(String value, String displayValue) {
            this.value = value;
            this.displayValue = displayValue;
        }
    }
    
    public static class Result {
        public TreeNode syntaxTree;
        public String message;
        public boolean hasError = false;
        
        public Result() {
            message = "";
        }
    }
    
    public Result analyze(String lexicalOutput, Map<String, String> originalToVMap) {
        Result result = new Result();
        
        if (lexicalOutput == null || lexicalOutput.isEmpty()) {
            result.hasError = true;
            result.message = "Empty lexical output";
            return result;
        }
        
        // Remove spaces and convert to expression string
        String expr = lexicalOutput.replaceAll("\\s+", "");
        
        try {
            TreeNode root = buildExpressionTree(expr, originalToVMap);
            if (root != null) {
                result.syntaxTree = root;
                result.message = "✅ Syntax OK";
            } else {
                result.hasError = true;
                result.message = "❌ Syntax Error: Invalid expression";
            }
        } catch (Exception e) {
            result.hasError = true;
            result.message = "❌ Syntax Error: " + e.getMessage();
        }
        
        return result;
    }
    
    private TreeNode buildExpressionTree(String expr, Map<String, String> originalToVMap) {
        List<String> tokens = tokenize(expr);
        
        // Check for assignment
        int equalsIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("=")) {
                equalsIndex = i;
                break;
            }
        }
        
        if (equalsIndex != -1) {
            // Assignment expression: V1 = expr
            TreeNode root = new TreeNode("=", "is");
            
            // Left side (V1)
            if (equalsIndex > 0) {
                String leftToken = tokens.get(equalsIndex - 1);
                root.left = createLeafNode(leftToken, originalToVMap);
            } else {
                return null; // Invalid: nothing before "="
            }
            
            // Right side (expression)
            List<String> rightTokens = tokens.subList(equalsIndex + 1, tokens.size());
            root.right = parseExpression(rightTokens, originalToVMap);
            
            return root;
        } else {
            // Just an expression
            return parseExpression(tokens, originalToVMap);
        }
    }
    
    private TreeNode parseExpression(List<String> tokens, Map<String, String> originalToVMap) {
        if (tokens.isEmpty()) return null;
        
        // Handle parentheses
        if (tokens.get(0).equals("(") && tokens.get(tokens.size() - 1).equals(")")) {
            return parseExpression(tokens.subList(1, tokens.size() - 1), originalToVMap);
        }
        
        // Find lowest precedence operator (right to left)
        int parenCount = 0;
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (token.equals(")")) parenCount++;
            else if (token.equals("(")) parenCount--;
            else if (parenCount == 0 && (token.equals("+") || token.equals("-"))) {
                TreeNode node = new TreeNode(token, token);
                node.left = parseExpression(tokens.subList(0, i), originalToVMap);
                node.right = parseExpression(tokens.subList(i + 1, tokens.size()), originalToVMap);
                return node;
            }
        }
        
        // Find multiplication/division
        parenCount = 0;
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (token.equals(")")) parenCount++;
            else if (token.equals("(")) parenCount--;
            else if (parenCount == 0 && (token.equals("*") || token.equals("/"))) {
                TreeNode node = new TreeNode(token, token);
                node.left = parseExpression(tokens.subList(0, i), originalToVMap);
                node.right = parseExpression(tokens.subList(i + 1, tokens.size()), originalToVMap);
                return node;
            }
        }
        
        // Single token
        if (tokens.size() == 1) {
            return createLeafNode(tokens.get(0), originalToVMap);
        }
        
        return null;
    }
    
    private TreeNode createLeafNode(String token, Map<String, String> originalToVMap) {
        if (token.matches("V\\d+")) {
            // Find original name for display
            String originalName = "";
            for (Map.Entry<String, String> entry : originalToVMap.entrySet()) {
                if (entry.getValue().equals(token)) {
                    originalName = entry.getKey();
                    break;
                }
            }
            String display = token + "(" + originalName + ")";
            return new TreeNode(token, display);
        } else if (token.matches("\\d+(\\.\\d+)?")) {
            return new TreeNode(token, token);
        }
        return new TreeNode(token, token);
    }
    
    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            
            if (c == '=' || c == '+' || c == '-' || c == '*' || c == '/' || 
                c == '(' || c == ')') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        
        return tokens;
    }
    
    public static String treeToString(TreeNode root) {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, root, "", true);
        return sb.toString();
    }
    
    private static void buildTreeString(StringBuilder sb, TreeNode node, String prefix, boolean isTail) {
        if (node == null) return;
        
        if (node.right != null) {
            buildTreeString(sb, node.right, prefix + (isTail ? "│   " : "    "), false);
        }
        
        sb.append(prefix).append(isTail ? "└── " : "┌── ").append(node.displayValue).append("\n");
        
        if (node.left != null) {
            buildTreeString(sb, node.left, prefix + (isTail ? "    " : "│   "), true);
        }
    }
}