import java.util.Map;

public class SemanticAnalyzer {

    public static class Result {
        public String message;
        public SyntaxAnalyzer.TreeNode treeRoot;
    }

    // Entry point
    public static Result analyze(SyntaxAnalyzer.TreeNode root, Map<String, String> symbolTable) {
        Result res = new Result();
        if (root == null) {
            res.message = "❌ No syntax tree to analyze.";
            res.treeRoot = null;
            return res;
        }

        Annotated a = annotate(root, symbolTable);
        res.treeRoot = a.node;
        res.message = "✅ Semantic analysis completed successfully.";
        return res;
    }

    // Helper to return annotated node and effective type ("int"/"float"/"unknown")
    private static class Annotated {
        SyntaxAnalyzer.TreeNode node;
        String type;
        Annotated(SyntaxAnalyzer.TreeNode n, String t) { node = n; type = t; }
    }

    private static Annotated annotate(SyntaxAnalyzer.TreeNode node, Map<String, String> types) {
        if (node == null) return new Annotated(null, "unknown");

        String val = node.value;

        // 1. Handle Identifiers: "id:name"
        if (val.startsWith("id:")) {
            String name = val.substring(3);
            String declared = types.getOrDefault(name, "unknown");
            SyntaxAnalyzer.TreeNode newN = new SyntaxAnalyzer.TreeNode("id:" + name + ":" + declared);
            return new Annotated(newN, declared.equals("unknown") ? "unknown" : declared);
        }

        // 2. Handle Numeric Literals: "NUM:5" or "NUM:60.0"
        if (val.startsWith("NUM:")) {
            String literal = val.substring(4);

            // --- NEW: Check for Decimal Point ---
            // If it has a dot, it is NATURALLY a float. No conversion needed.
            if (literal.contains(".")) {
                SyntaxAnalyzer.TreeNode newN = new SyntaxAnalyzer.TreeNode("NUM:" + literal + ":float");
                return new Annotated(newN, "float");
            }

            // If no dot, it is an Integer. Check if user forced it to be float via input.
            String declared = types.get(literal); 

            if ("float".equals(declared)) {
                // Integer forced to float -> Add conversion tag
                SyntaxAnalyzer.TreeNode newN = new SyntaxAnalyzer.TreeNode("NUM:" + literal + ":int (int→float)");
                return new Annotated(newN, "float"); 
            } else {
                // Standard Integer
                SyntaxAnalyzer.TreeNode newN = new SyntaxAnalyzer.TreeNode("NUM:" + literal + ":int");
                return new Annotated(newN, "int");
            }
        }

        // 3. Handle Non-leaf: operators or assignment
        if (node.left == null && node.right == null) {
            return new Annotated(new SyntaxAnalyzer.TreeNode(val), "unknown");
        }

        Annotated leftA = annotate(node.left, types);
        Annotated rightA = annotate(node.right, types);

        String leftType = leftA.type;
        String rightType = rightA.type;

        // Case: Assignment "="
        if ("=".equals(val)) {
            String lhsDeclared = "unknown";
            // Check LHS type
            if (leftA.node != null && leftA.node.value.startsWith("id:")) {
                String[] parts = leftA.node.value.split(":");
                if (parts.length >= 3) lhsDeclared = parts[2];
            }

            SyntaxAnalyzer.TreeNode newNode = new SyntaxAnalyzer.TreeNode("=");
            newNode.left = leftA.node;

            // If LHS is float and RHS is int, convert RHS
            if ("float".equals(lhsDeclared) && "int".equals(rightType)) {
                SyntaxAnalyzer.TreeNode conv = new SyntaxAnalyzer.TreeNode(describeNodeWithConversion(rightA.node));
                newNode.right = conv;
                return new Annotated(newNode, "float");
            } else {
                newNode.right = rightA.node;
                String eff = !"unknown".equals(lhsDeclared) ? lhsDeclared : rightType;
                return new Annotated(newNode, eff);
            }
        }

        // Case: Arithmetic operators (+, -, *, /)
        String resultType = "int";
        if ("float".equals(leftType) || "float".equals(rightType)) resultType = "float";

        SyntaxAnalyzer.TreeNode leftNode = leftA.node;
        SyntaxAnalyzer.TreeNode rightNode = rightA.node;

        // If result is float, but one child is int, mark that child for conversion
        if ("float".equals(resultType) && "int".equals(leftType)) {
            leftNode = new SyntaxAnalyzer.TreeNode(describeNodeWithConversion(leftA.node));
        }
        if ("float".equals(resultType) && "int".equals(rightType)) {
            rightNode = new SyntaxAnalyzer.TreeNode(describeNodeWithConversion(rightA.node));
        }

        SyntaxAnalyzer.TreeNode opNode = new SyntaxAnalyzer.TreeNode(val);
        opNode.left = leftNode;
        opNode.right = rightNode;

        return new Annotated(opNode, resultType);
    }

    // Helper to append the conversion text to the node value
    private static String describeNodeWithConversion(SyntaxAnalyzer.TreeNode node) {
        if (node == null) return "(int→float)";
        // If it's already marked, don't mark again (optional safety)
        if (node.value.contains("(int→float)")) return node.value;
        
        return node.value + " (int→float)";
    }
}