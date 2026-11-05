import java.util.*;

public class CodeOptimizer {

    public static String optimize(String icg) {
        if (icg == null || icg.trim().isEmpty()) {
            return "";
        }
        
        String[] lines = icg.split("\n");
        List<String> instructions = new ArrayList<>();
        Map<String, String> constantMap = new HashMap<>();
        Map<String, String> tempMap = new HashMap<>();
        
        // First pass: Process instructions and handle inttofloat
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.contains("inttofloat(")) {
                // Extract temp and constant
                String[] parts = line.split(" = ");
                String tempVar = parts[0].trim();
                String constStr = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
                // Convert to float constant
                constantMap.put(tempVar, constStr + ".0");
            } else {
                instructions.add(line);
            }
        }
        
        // Second pass: Apply constant propagation
        List<String> optimized1 = new ArrayList<>();
        for (String line : instructions) {
            String optimizedLine = line;
            for (Map.Entry<String, String> entry : constantMap.entrySet()) {
                optimizedLine = optimizedLine.replace(entry.getKey(), entry.getValue());
            }
            optimized1.add(optimizedLine);
        }
        
        // Third pass: Eliminate redundant assignments
        Map<String, String> usageMap = new HashMap<>();
        Map<String, Integer> useCount = new HashMap<>();
        
        // Count usage of each temp
        for (String line : optimized1) {
            String[] parts = line.split(" = ");
            if (parts.length >= 2) {
                String leftVar = parts[0].trim();
                String rightExpr = parts[1].trim();
                
                // Check if right side is a simple variable (temp or id)
                if (!rightExpr.contains("+") && !rightExpr.contains("-") && 
                    !rightExpr.contains("*") && !rightExpr.contains("/")) {
                    tempMap.put(leftVar, rightExpr);
                }
                
                // Count usage of variables on right side
                String[] tokens = rightExpr.split(" ");
                for (String token : tokens) {
                    if (token.startsWith("temp") || token.startsWith("t") || token.startsWith("id")) {
                        useCount.put(token, useCount.getOrDefault(token, 0) + 1);
                    }
                }
            }
        }
        
        // Fourth pass: Propagate temps and eliminate dead code
        List<String> finalInstructions = new ArrayList<>();
        for (String line : optimized1) {
            String[] parts = line.split(" = ");
            if (parts.length >= 2) {
                String leftVar = parts[0].trim();
                String rightExpr = parts[1].trim();
                
                // Apply temp propagation
                for (Map.Entry<String, String> entry : tempMap.entrySet()) {
                    if (!entry.getKey().equals(leftVar)) {
                        rightExpr = rightExpr.replace(entry.getKey(), entry.getValue());
                    }
                }
                
                // Only keep if:
                // 1. It's an assignment to an id (user variable)
                // 2. OR the temp is used more than once
                // 3. OR it's a complex expression (not simple copy)
                if (leftVar.startsWith("id") || 
                    useCount.getOrDefault(leftVar, 0) > 1 ||
                    rightExpr.contains("+") || rightExpr.contains("-") || 
                    rightExpr.contains("*") || rightExpr.contains("/")) {
                    
                    finalInstructions.add(leftVar + " = " + rightExpr);
                }
            } else {
                finalInstructions.add(line);
            }
        }
        
        // Fifth pass: Clean up and format
        StringBuilder result = new StringBuilder();
        for (String line : finalInstructions) {
            // Clean up any remaining .0.0 issues
            line = line.replaceAll("(\\.0)+\\.0", ".0");
            result.append(line).append("\n");
        }
        
        return result.toString().trim();
    }
}