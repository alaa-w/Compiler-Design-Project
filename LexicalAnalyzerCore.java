import java.util.*;

public class LexicalAnalyzerCore {

    private static int idCounter = 1; 

    private static void putSymbol(Map<String, Result.SymbolEntry> map, String key, String type) {
        if (!map.containsKey(key)) {
            map.put(key, new Result.SymbolEntry(type, idCounter++));
        }
    }

    public static class Result {
        public String output;
        public Map<String, SymbolEntry> variableMap = new LinkedHashMap<>();
        public SyntaxAnalyzer.TreeNode syntaxTree;
        public String syntaxMessage;
        public boolean lexicalError = false; // NEW: track lexical error

        public static class SymbolEntry {
            public String type;
            public int id;
            public SymbolEntry(String type, int id) {
                this.type = type;
                this.id = id;
            }
        }
    }

    public static void reset() { idCounter = 1; }

    // analyze math expressions
    public static Result analyzeMath(String input) {
        reset();
        Result r = new Result();
        if (input == null) { r.output = ""; return r; }

        input = input.replaceAll("//.*", "").trim();
        input = input.replaceAll("(?<=[0-9])(?=[a-zA-Z])", "*");
        input = input.replaceAll("(?<=[a-zA-Z])(?=[0-9])", "*");

        List<String> tokens = new ArrayList<>();
        StringBuilder tok = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (Character.isWhitespace(ch)) continue;
            if ("=+-*/()".indexOf(ch) >= 0) {
                if (tok.length() > 0) {
                    tokens.add(tok.toString());
                    tok.setLength(0);
                }
                tokens.add(Character.toString(ch));
            } else {
                tok.append(ch);
            }
        }
        if (tok.length() > 0) tokens.add(tok.toString());

        // Lexical check
        for (String t : tokens) {
            if (!t.matches("[a-zA-Z_][a-zA-Z0-9_]*") && 
                !t.matches("\\d+") && !t.matches("\\d+\\.\\d+") &&
                !"=+-*/()".contains(t)) {
                r.output = "❌ Lexical Error: Invalid token '" + t + "'";
                r.lexicalError = true;
                return r;
            }
        }

        // Populate map
        for (String t : tokens) {
            if (t.matches("[a-zA-Z_][a-zA-Z0-9_]*")) putSymbol(r.variableMap, t, "unknown");
            else if (t.matches("\\d+")) r.variableMap.putIfAbsent(t, new Result.SymbolEntry("int", -1));
            else if (t.matches("\\d+\\.\\d+")) r.variableMap.putIfAbsent(t, new Result.SymbolEntry("float", -1));
        }

        StringBuilder out = new StringBuilder();
        for (String t : tokens) {
            if (t.matches("[a-zA-Z_][a-zA-Z0-9_]*")) out.append("id").append(r.variableMap.get(t).id).append(" ");
            else out.append(t).append(" ");
        }
        r.output = out.toString().trim();

        String expr = String.join("", tokens);
        SyntaxAnalyzer.TreeNode root = SyntaxAnalyzer.buildExpressionTree(expr);
        if (root != null) { r.syntaxTree = root; r.syntaxMessage = "✅ Syntax OK"; }
        else { r.syntaxTree = null; r.syntaxMessage = "❌ Syntax Error"; }

        return r;
    }

    // analyze source code
    public static Result analyzeSourceCode(String input) {
        reset();
        Result r = new Result();
        if (input == null) { r.output = ""; return r; }

        input = input.replaceAll("//.*", "").trim();
        String[] stmts = input.split(";");
        StringBuilder out = new StringBuilder();

        for (String s : stmts) {
            s = s.trim();
            if (s.isEmpty()) continue;

            String[] parts = s.split("(?=[=+\\-*/()])|(?<=[=+\\-*/()])|\\s+");

            // Lexical check
            for (String t : parts) {
                if (t.isEmpty()) continue;
                if (!t.matches("[a-zA-Z_][a-zA-Z0-9_]*") && 
                    !t.matches("\\d+") && !t.matches("\\d+\\.\\d+") &&
                    !"=+-*/()".contains(t)) {
                    r.output = "❌ Lexical Error: Invalid token '" + t + "'";
                    r.lexicalError = true;
                    return r;
                }
            }

            for (String t : parts) {
                if (t.isEmpty()) continue;
                if (t.matches("[a-zA-Z_][a-zA-Z0-9_]*")) putSymbol(r.variableMap, t, "unknown");
                else if (t.matches("\\d+")) r.variableMap.putIfAbsent(t, new Result.SymbolEntry("int", -1));
                else if (t.matches("\\d+\\.\\d+")) r.variableMap.putIfAbsent(t, new Result.SymbolEntry("float", -1));
            }

            StringBuilder lineBuilder = new StringBuilder();
            for (String t : parts) {
                if (t.isEmpty()) continue;
                if (r.variableMap.containsKey(t) && r.variableMap.get(t).id != -1)
                    lineBuilder.append("id").append(r.variableMap.get(t).id).append(" ");
                else lineBuilder.append(t).append(" ");
            }
            out.append(lineBuilder.toString().trim()).append("\n");
        }

        r.output = out.toString().trim();

        if (stmts.length > 0) {
            SyntaxAnalyzer.TreeNode root = SyntaxAnalyzer.buildExpressionTree(stmts[0].trim());
            if (root != null) { r.syntaxTree = root; r.syntaxMessage = "✅ Syntax OK"; }
            else { r.syntaxTree = null; r.syntaxMessage = "❌ Syntax Error"; }
        }

        return r;
    }
}
