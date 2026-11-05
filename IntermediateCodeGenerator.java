import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IntermediateCodeGenerator {

    private int tempCounter = 1;
    private List<String> instructions;
    private Map<String, LexicalAnalyzerCore.Result.SymbolEntry> symbolTable;

    public IntermediateCodeGenerator() {
        this.instructions = new ArrayList<>();
    }

    // Helper class to hold the result of a node processing:
    // address: the string used in code (e.g., "id1", "temp1", "7")
    // type: the data type (e.g., "int", "float")
    private class ExprResult {
        String address;
        String type;

        public ExprResult(String address, String type) {
            this.address = address;
            this.type = type;
        }
    }

    public static String generate(SyntaxAnalyzer.TreeNode root, Map<String, LexicalAnalyzerCore.Result.SymbolEntry> map) {
        IntermediateCodeGenerator icg = new IntermediateCodeGenerator();
        icg.symbolTable = map;
        
        if (root != null) {
            icg.generateRecursive(root);
        }

        StringBuilder sb = new StringBuilder();
        for (String line : icg.instructions) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // Helper to strip semantic tags if present (e.g. "id:t:float" -> "t")
    private String cleanName(String raw) {
        if (raw.contains(":")) {
            // If Semantic Analyzer changed "t" to "id:t:float", we want "t"
            // Usually the variable name is the part that exists in the symbol table keys.
            String[] parts = raw.split(":");
            for (String p : parts) {
                if (symbolTable.containsKey(p)) return p;
            }
            // Fallback for numbers like NUM:7:int -> return 7
            if (parts.length > 1 && parts[1].matches("\\d+")) return parts[1];
            return parts[0]; 
        }
        return raw;
    }

    private ExprResult generateRecursive(SyntaxAnalyzer.TreeNode node) {
        if (node == null) return new ExprResult("", "unknown");

        // Clean the value to get the raw name/number
        String cleanVal = cleanName(node.value);

        // --- 1. Leaf Nodes (Variables or Numbers) ---
        if (node.left == null && node.right == null) {
            // Case A: Number Literal
            if (cleanVal.matches("\\d+\\.\\d+")) return new ExprResult(cleanVal, "float");
            if (cleanVal.matches("\\d+")) return new ExprResult(cleanVal, "int");

            // Case B: Variable (Lookup in Map)
            if (symbolTable.containsKey(cleanVal)) {
                LexicalAnalyzerCore.Result.SymbolEntry entry = symbolTable.get(cleanVal);
                String idName = (entry.id > 0) ? "id" + entry.id : cleanVal;
                return new ExprResult(idName, entry.type);
            }
            // Fallback
            return new ExprResult(cleanVal, "unknown");
        }

        // --- 2. Assignment (=) ---
        if (node.value.equals("=")) {
            ExprResult left = generateRecursive(node.left);
            ExprResult right = generateRecursive(node.right);
            
            // Just print the assignment
            instructions.add(left.address + " = " + right.address);
            return left;
        }

        // --- 3. Binary Operations (+, -, *, /) ---
        ExprResult left = generateRecursive(node.left);
        ExprResult right = generateRecursive(node.right);

        // *** TYPE CHECKING & CONVERSION LOGIC ***
        
        // Scenario 1: Left is INT, Right is FLOAT -> Convert Left
        if (left.type.equals("int") && right.type.equals("float")) {
            String tempConv = "temp" + tempCounter++;
            instructions.add(tempConv + " = inttofloat(" + left.address + ")");
            // Update left to point to the new temp
            left = new ExprResult(tempConv, "float");
        }
        // Scenario 2: Left is FLOAT, Right is INT -> Convert Right
        else if (left.type.equals("float") && right.type.equals("int")) {
            String tempConv = "temp" + tempCounter++;
            instructions.add(tempConv + " = inttofloat(" + right.address + ")");
            // Update right to point to the new temp
            right = new ExprResult(tempConv, "float");
        }

        // Generate the operation code
        String newTemp = "temp" + tempCounter++;
        instructions.add(newTemp + " = " + left.address + " " + node.value + " " + right.address);

        // Determine result type
        String resType = (left.type.equals("float") || right.type.equals("float")) ? "float" : "int";
        
        return new ExprResult(newTemp, resType);
    }
}