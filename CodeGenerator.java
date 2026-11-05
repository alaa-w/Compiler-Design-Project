import java.util.*;

public class CodeGenerator {

    public static String generateAssembly(String optimizedCode) {
        if (optimizedCode == null || optimizedCode.trim().isEmpty()) {
            return "No code to generate assembly for";
        }
        
        StringBuilder assembly = new StringBuilder();
        
        String[] lines = optimizedCode.split("\n");
        Map<String, String> varToReg = new HashMap<>();
        
        // Generate assembly
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(" = ");
            if (parts.length < 2) continue;
            
            String dest = parts[0].trim();
            String expr = parts[1].trim();
            
            if (expr.contains("+") || expr.contains("-") || expr.contains("*") || expr.contains("/")) {
                // Binary operation
                String[] tokens = expr.split(" ");
                String op1 = tokens[0];
                String operator = tokens[1];
                String op2 = tokens[2];
                
                // Determine assembly operator
                String asmOp = "";
                switch (operator) {
                    case "+": asmOp = "ADDF"; break;
                    case "-": asmOp = "SUBF"; break;
                    case "*": asmOp = "MULF"; break;
                    case "/": asmOp = "DIVF"; break;
                }
                
                // Handle multiplication (use R2)
                if (operator.equals("*")) {
                    // Load first operand into R2
                    if (op1.startsWith("id")) {
                        assembly.append("LDF R2, " + op1 + "\n");
                    } else if (op1.startsWith("temp") && varToReg.containsKey(op1)) {
                        String reg = varToReg.get(op1);
                        assembly.append("LDF R2, " + reg + "\n");
                    } else if (isNumber(op1)) {
                        assembly.append("LDF R2, #" + op1 + "\n");
                    } else {
                        assembly.append("LDF R2, " + op1 + "\n");
                    }
                    
                    // Multiply with second operand
                    if (op2.startsWith("id")) {
                        assembly.append("MULF R2, R2, " + op2 + "\n");
                    } else if (isNumber(op2)) {
                        assembly.append("MULF R2, R2, #" + op2 + "\n");
                    } else if (op2.startsWith("temp") && varToReg.containsKey(op2)) {
                        String reg = varToReg.get(op2);
                        assembly.append("MULF R2, R2, " + reg + "\n");
                    } else {
                        assembly.append("MULF R2, R2, " + op2 + "\n");
                    }
                    
                    // Store result mapping
                    if (dest.startsWith("temp")) {
                        varToReg.put(dest, "R2");
                    } else if (dest.startsWith("id")) {
                        assembly.append("STF " + dest + ", R2\n");
                    }
                    
                } else {
                    // Addition/Subtraction (use R1)
                    // Load first operand into R1
                    if (op1.startsWith("id")) {
                        assembly.append("LDF R1, " + op1 + "\n");
                    } else if (op1.startsWith("temp") && varToReg.containsKey(op1)) {
                        String reg = varToReg.get(op1);
                        assembly.append("LDF R1, " + reg + "\n");
                    } else if (isNumber(op1)) {
                        assembly.append("LDF R1, #" + op1 + "\n");
                    } else {
                        assembly.append("LDF R1, " + op1 + "\n");
                    }
                    
                    // Add/Subtract second operand
                    if (op2.startsWith("id")) {
                        assembly.append(asmOp + " R1, R1, " + op2 + "\n");
                    } else if (isNumber(op2)) {
                        assembly.append(asmOp + " R1, R1, #" + op2 + "\n");
                    } else if (op2.startsWith("temp") && varToReg.containsKey(op2)) {
                        String reg = varToReg.get(op2);
                        assembly.append(asmOp + " R1, R1, " + reg + "\n");
                    } else {
                        assembly.append(asmOp + " R1, R1, " + op2 + "\n");
                    }
                    
                    // Store result
                    if (dest.startsWith("temp")) {
                        varToReg.put(dest, "R1");
                    } else if (dest.startsWith("id")) {
                        assembly.append("STF " + dest + ", R1\n");
                    }
                }
                
            } else {
                // Simple assignment
                if (dest.startsWith("id")) {
                    // id = something
                    if (expr.startsWith("temp") && varToReg.containsKey(expr)) {
                        String reg = varToReg.get(expr);
                        assembly.append("STF " + dest + ", " + reg + "\n");
                    } else if (expr.startsWith("id")) {
                        assembly.append("LDF R1, " + expr + "\n");
                        assembly.append("STF " + dest + ", R1\n");
                    } else if (isNumber(expr)) {
                        assembly.append("LDF R1, #" + expr + "\n");
                        assembly.append("STF " + dest + ", R1\n");
                    }
                } else if (dest.startsWith("temp")) {
                    // temp = something
                    if (expr.startsWith("id")) {
                        assembly.append("LDF R1, " + expr + "\n");
                        varToReg.put(dest, "R1");
                    } else if (isNumber(expr)) {
                        assembly.append("LDF R1, #" + expr + "\n");
                        varToReg.put(dest, "R1");
                    } else if (expr.startsWith("temp") && varToReg.containsKey(expr)) {
                        // temp1 = temp2 (copy)
                        String reg = varToReg.get(expr);
                        varToReg.put(dest, reg);
                    }
                }
            }
        }
        
        return assembly.toString().trim();
    }
    
    private static boolean isNumber(String str) {
        return str.matches("\\d+\\.\\d+") || str.matches("\\d+");
    }
    
    // Test method
    public static void main(String[] args) {
        // Test 1: Your first example
        String code1 = "temp1 = id3 * 60.0\nid1 = id2 + temp1";
        System.out.println("Test 1 Output:");
        System.out.println(generateAssembly(code1));
        
        // Test 2: Your second example
        String code2 = "temp2 = id2 + id3\ntemp4 = temp2 + 5.0\nid1 = temp4";
        System.out.println("\nTest 2 Output:");
        System.out.println(generateAssembly(code2));
        
        // Test 3: More complex example
        String code3 = "temp1 = id1 * 2.0\ntemp2 = id2 + 3.5\ntemp3 = temp1 - temp2\nid3 = temp3";
        System.out.println("\nTest 3 Output:");
        System.out.println(generateAssembly(code3));
    }
}