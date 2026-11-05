import java.util.*;

public class SyntaxAnalyzer {

    public static class TreeNode {
        public String value;
        public TreeNode left, right;
        public TreeNode(String v) { value = v; }
    }

    // Build tree from expression string, tokens are operators and operands (no spaces required)
    public static TreeNode buildExpressionTree(String expr) {
        if (expr == null || expr.isEmpty()) return null;

        // Tokenize
        List<String> tokens = new ArrayList<>();
        StringBuilder tok = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);
            if ("=+-*/()".indexOf(ch) >= 0) {
                if (tok.length() > 0) {
                    tokens.add(tok.toString());
                    tok.setLength(0);
                }
                tokens.add(String.valueOf(ch));
            } else {
                tok.append(ch);
            }
        }
        if (tok.length() > 0) tokens.add(tok.toString());

        // Check for invalid sequences
        if (hasInvalidSequences(tokens)) return null;

        // Check parentheses balance
        int balance = 0;
        for (String t : tokens) {
            if (t.equals("(")) balance++;
            else if (t.equals(")")) balance--;
            if (balance < 0) return null; // more closing than opening
        }
        if (balance != 0) return null; // unbalanced

        // Handle assignment: id = expr
        if (tokens.size() >= 3 && tokens.get(1).equals("=")) {
            TreeNode root = new TreeNode("=");
            root.left = makeLeaf(tokens.get(0));
            List<String> rightToks = tokens.subList(2, tokens.size());
            root.right = parseExpressionWithPrecedence(rightToks);
            return root;
        }

        // Otherwise, parse as expression
        return parseExpressionWithPrecedence(tokens);
    }

    // Detect invalid operator sequences like ++, +*, == etc.
    private static boolean hasInvalidSequences(List<String> tokens) {
        String prev = "";
        for (String t : tokens) {
            if ("=+-*/".contains(t)) {
                if ("=+-*/".contains(prev)) {
                    // Allow single minus for negative numbers if prev is "=" or "("
                    if (!(t.equals("-") && (prev.equals("=") || prev.equals("(")))) {
                        return true; // invalid sequence
                    }
                }
            }
            prev = t;
        }
        return false;
    }

    // Create leaf in canonical form: identifiers -> "id:name", numbers -> "NUM:5"
    private static TreeNode makeLeaf(String t) {
        t = t.trim();
        if (t.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return new TreeNode("id:" + t);
        if (t.matches("\\d+")) return new TreeNode("NUM:" + t);
        if (t.matches("\\d+\\.\\d+")) return new TreeNode("NUM:" + t);
        return new TreeNode(t);
    }

    // parse with operator precedence: handle +,- lower than *,/
    private static TreeNode parseExpressionWithPrecedence(List<String> toks) {
        if (toks == null || toks.isEmpty()) return null;
        return parseExprRange(toks, 0, toks.size() - 1);
    }

    private static TreeNode parseExprRange(List<String> toks, int start, int end) {
        if (start > end) return null;

        // handle parentheses
        if (toks.get(start).equals("(") && findMatchingParen(toks, start) == end) {
            return parseExprRange(toks, start + 1, end - 1);
        }

        // find lowest precedence operator (+ or -) scanning right-to-left
        for (int i = end; i >= start; i--) {
            String t = toks.get(i);
            if (t.equals("+") || t.equals("-")) {
                TreeNode node = new TreeNode(t);
                node.left = parseExprRange(toks, start, i - 1);
                node.right = parseExprRange(toks, i + 1, end);
                return node;
            }
            if (t.equals(")")) i = findMatchingOpening(toks, i);
        }

        // then * or /
        for (int i = end; i >= start; i--) {
            String t = toks.get(i);
            if (t.equals("*") || t.equals("/")) {
                TreeNode node = new TreeNode(t);
                node.left = parseExprRange(toks, start, i - 1);
                node.right = parseExprRange(toks, i + 1, end);
                return node;
            }
            if (t.equals(")")) i = findMatchingOpening(toks, i);
        }

        // single token
        if (start == end) return makeLeaf(toks.get(start));

        // fallback: leftmost leaf and rest subtree
        TreeNode left = makeLeaf(toks.get(start));
        TreeNode right = parseExprRange(toks, start + 1, end);
        TreeNode parent = new TreeNode(right != null ? right.value : "");
        parent.left = left;
        parent.right = right;
        return parent;
    }

    private static int findMatchingParen(List<String> toks, int openIndex) {
        if (!toks.get(openIndex).equals("(")) return -1;
        int depth = 0;
        for (int i = openIndex; i < toks.size(); i++) {
            if (toks.get(i).equals("(")) depth++;
            else if (toks.get(i).equals(")")) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int findMatchingOpening(List<String> toks, int closeIndex) {
        int depth = 0;
        for (int i = closeIndex; i >= 0; i--) {
            if (toks.get(i).equals(")")) depth++;
            else if (toks.get(i).equals("(")) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // utility for debug printing
    public static String printTree(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        buildTreeVisual(sb, root, "", true);
        return sb.toString();
    }

    private static void buildTreeVisual(StringBuilder sb, TreeNode node, String prefix, boolean isTail) {
        if (node == null) return;
        if (node.right != null)
            buildTreeVisual(sb, node.right, prefix + (isTail ? "│   " : "    "), false);

        sb.append(prefix).append(isTail ? "└── " : "┌── ").append(node.value).append("\n");

        if (node.left != null)
            buildTreeVisual(sb, node.left, prefix + (isTail ? "    " : "│   "), true);
    }
}
