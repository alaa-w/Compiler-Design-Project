// HybridLexicalAnalyzer.java - Fix for variable detection
import java.util.*;

public class HybridLexicalAnalyzer {
    
    private int vCounter = 1;
    private Map<String, String> varToVMap = new HashMap<>();
    private Map<String, VEntry> vTable = new LinkedHashMap<>();
    
    public static class Result {
        public String lexicalOutput;
        public Map<String, VEntry> variableTable;
        public String originalEquation;
        public Map<String, String> originalToVMap;
        public boolean hasError = false;
        public String errorMessage = "";
        
        public Result() {
            variableTable = new LinkedHashMap<>();
            originalToVMap = new HashMap<>();
        }
    }
    
    public static class VEntry {
        public String vName;      // V1, V2, V3, etc.
        public String originalName; // x, y, z, etc.
        public String type;       // unknown, int, float
        public int id;
        
        public VEntry(String vName, String originalName, String type, int id) {
            this.vName = vName;
            this.originalName = originalName;
            this.type = type;
            this.id = id;
        }
    }
    
    public Result analyze(String input) {
        reset();
        Result result = new Result();
        result.originalEquation = input;
        
        if (input == null || input.trim().isEmpty()) {
            result.hasError = true;
            result.errorMessage = "Empty input";
            return result;
        }
        
        // Store original input with "is"
        String originalWithIs = input.replaceAll("(?i)\\b(is|=)\\b", " is ");
        originalWithIs = originalWithIs.replaceAll("\\s+", " ").trim();
        
        // Replace "is" with "=" for internal processing
        String processed = input.replaceAll("(?i)\\bis\\b", "=").trim();
        
        // Tokenization
        List<String> tokens = tokenize(processed);
        
        // Validate tokens
        if (!validateTokens(tokens, result)) {
            return result;
        }
        
        // Build variable table and output
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (isVariable(token)) {
                // Get or create V name
                String vName = getVName(token);
                output.append(vName).append(" ");
                
                // Add to variable table if not already there
                if (!vTable.containsKey(vName)) {
                    vTable.put(vName, new VEntry(vName, token, "unknown", vCounter));
                    varToVMap.put(token, vName);
                    vCounter++;
                }
            } else if (token.equals("=")) {
                // Replace "=" with "is" in output
                output.append("is ");
            } else {
                output.append(token).append(" ");
            }
        }
        
        result.lexicalOutput = output.toString().trim();
        result.variableTable = new LinkedHashMap<>(vTable);
        result.originalToVMap = new HashMap<>(varToVMap);
        
        return result;
    }
    
    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            
            if (isOperator(c) || c == '(' || c == ')') {
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
    
    private boolean validateTokens(List<String> tokens, Result result) {
        for (String token : tokens) {
            if (isVariable(token) || isNumber(token) || isOperator(token) || 
                token.equals("(") || token.equals(")")) {
                continue;
            }
            
            result.hasError = true;
            result.errorMessage = "Invalid token: '" + token + "'";
            return false;
        }
        return true;
    }
    
    private boolean isVariable(String token) {
        // A variable is a sequence of letters/underscores starting with a letter/underscore
        // and not matching a number pattern
        return token.matches("[a-zA-Z_][a-zA-Z0-9_]*") && !isNumber(token);
    }
    
    private boolean isNumber(String token) {
        return token.matches("-?\\d+(\\.\\d+)?");
    }
    
    private boolean isOperator(String token) {
        return token.matches("[=+\\-*/]");
    }
    
    private boolean isOperator(char c) {
        return c == '=' || c == '+' || c == '-' || c == '*' || c == '/';
    }
    
    private String getVName(String originalName) {
        if (!varToVMap.containsKey(originalName)) {
            String vName = "V" + vCounter;
            varToVMap.put(originalName, vName);
            return vName;
        }
        return varToVMap.get(originalName);
    }
    
    private void reset() {
        vCounter = 1;
        varToVMap.clear();
        vTable.clear();
    }
}