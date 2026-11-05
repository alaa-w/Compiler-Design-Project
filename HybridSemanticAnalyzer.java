// HybridSemanticAnalyzer.java - Separate Semantic Analyzer for Hybrid Mode
import java.util.*;

public class HybridSemanticAnalyzer {
    
    public static class TreeNode {
        public String value;
        public String type; // int, float, unknown
        public TreeNode left, right;
        public String displayValue;
        
        public TreeNode(String value, String type) {
            this.value = value;
            this.type = type;
            this.displayValue = value;
        }
        
        public TreeNode(String value, String type, String displayValue) {
            this.value = value;
            this.type = type;
            this.displayValue = displayValue;
        }
    }
    
    public static class Result {
        public TreeNode annotatedTree;
        public String message;
        public Map<String, String> typeTable; // V1 -> type, V2 -> type
        
        public Result() {
            message = "";
            typeTable = new HashMap<>();
        }
    }
    
    public Result analyze(HybridSyntaxAnalyzer.TreeNode syntaxTree, 
                         Map<String, HybridLexicalAnalyzer.VEntry> variableTable,
                         Map<String, String> userTypes) {
        Result result = new Result();
        
        if (syntaxTree == null) {
            result.message = "❌ No syntax tree to analyze";
            return result;
        }
        
        // Apply user types to variable table
        for (Map.Entry<String, String> entry : userTypes.entrySet()) {
            String originalName = entry.getKey();
            String type = entry.getValue();
            
            // Find V name for this original name
            for (HybridLexicalAnalyzer.VEntry vEntry : variableTable.values()) {
                if (vEntry.originalName.equals(originalName)) {
                    vEntry.type = type;
                    break;
                }
            }
        }
        
        try {
            TreeNode annotated = annotateTree(syntaxTree, variableTable);
            result.annotatedTree = annotated;
            
            // Build type table for display
            for (HybridLexicalAnalyzer.VEntry vEntry : variableTable.values()) {
                result.typeTable.put(vEntry.vName, vEntry.type);
            }
            
            result.message = "✅ Semantic analysis completed";
        } catch (Exception e) {
            result.message = "❌ Semantic Error: " + e.getMessage();
        }
        
        return result;
    }
    
    private TreeNode annotateTree(HybridSyntaxAnalyzer.TreeNode node, 
                                 Map<String, HybridLexicalAnalyzer.VEntry> variableTable) {
        if (node == null) return null;
        
        // Leaf node: variable or number
        if (node.left == null && node.right == null) {
            String value = node.value;
            
            if (value.matches("V\\d+")) {
                // Variable
                HybridLexicalAnalyzer.VEntry entry = variableTable.get(value);
                String type = (entry != null) ? entry.type : "unknown";
                String display = value + ":" + type;
                if (entry != null && !entry.originalName.isEmpty()) {
                    display = value + "(" + entry.originalName + "):" + type;
                }
                return new TreeNode(value, type, display);
            } else if (value.matches("\\d+")) {
                // Integer
                return new TreeNode(value, "int", value + ":int");
            } else if (value.matches("\\d+\\.\\d+")) {
                // Float
                return new TreeNode(value, "float", value + ":float");
            } else {
                return new TreeNode(value, "unknown", value);
            }
        }
        
        // Internal node: operator
        TreeNode left = annotateTree(node.left, variableTable);
        TreeNode right = annotateTree(node.right, variableTable);
        
        String leftType = (left != null) ? left.type : "unknown";
        String rightType = (right != null) ? right.type : "unknown";
        
        if (node.value.equals("=")) {
            // Assignment
            String resultType = leftType; // Type of left side
            
            // Check for type conversions
            if (leftType.equals("float") && rightType.equals("int")) {
                // int to float conversion
                if (right != null) {
                    right.displayValue = right.displayValue + " (int→float)";
                }
            }
            
            TreeNode resultNode = new TreeNode(node.value, resultType, "is");
            resultNode.left = left;
            resultNode.right = right;
            return resultNode;
        } else {
            // Arithmetic operation
            String resultType = determineResultType(leftType, rightType);
            
            // Add conversion annotations if needed
            if (resultType.equals("float")) {
                if (leftType.equals("int") && left != null) {
                    left.displayValue = left.displayValue + " (int→float)";
                }
                if (rightType.equals("int") && right != null) {
                    right.displayValue = right.displayValue + " (int→float)";
                }
            }
            
            TreeNode resultNode = new TreeNode(node.value, resultType, node.value);
            resultNode.left = left;
            resultNode.right = right;
            return resultNode;
        }
    }
    
    private String determineResultType(String type1, String type2) {
        if (type1.equals("float") || type2.equals("float")) {
            return "float";
        } else if (type1.equals("int") && type2.equals("int")) {
            return "int";
        } else {
            return "unknown";
        }
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