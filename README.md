# Compiler Design Project

A Java-based Compiler Design project that demonstrates the main phases of compiler construction, from source code analysis to intermediate code generation, optimization, and assembly code generation.

## Overview

This project implements a compiler using Java, combining different compiler phases to analyze and process source code. It focuses on understanding how a compiler transforms source code into a structured and optimized representation.

## Features

- **Lexical Analysis:** Identifies and classifies tokens from the input source code.
- **Syntax Analysis:** Checks the grammatical structure of the source code.
- **Semantic Analysis:** Performs semantic checks to identify logical and meaning-related errors.
- **Intermediate Code Generation:** Converts analyzed source code into an intermediate representation.
- **Code Optimization:** Improves intermediate code by applying optimization techniques.
- **Code Generation:** Generates assembly-like output from optimized intermediate code.
- **GUI:** Provides a graphical user interface for interacting with the compiler.
- **Hybrid Execution:** Includes hybrid direct execution functionality.

## Technologies Used

- Java
- Object-Oriented Programming (OOP)
- Java Swing / GUI components (if used in the implementation)

## Project Structure

| File | Description |
|---|---|
| `GUI.java` | Main graphical interface |
| `HybridGUI.java` | Hybrid compiler graphical interface |
| `HybridLexicalAnalyzer.java` | Hybrid lexical analysis |
| `HybridSyntaxAnalyzer.java` | Hybrid syntax analysis |
| `HybridSemanticAnalyzer.java` | Hybrid semantic analysis |
| `HybridDirectExecution.java` | Hybrid direct execution |
| `LexicalAnalyzerCore.java` | Core lexical analyzer |
| `SyntaxAnalyzer.java` | Syntax analysis |
| `SemanticAnalyzer.java` | Semantic analysis |
| `IntermediateCodeGenerator.java` | Intermediate code generation |
| `CodeOptimizer.java` | Intermediate code optimization |
| `CodeGenerator.java` | Assembly code generation |

## Getting Started

### Prerequisites

- Java Development Kit (JDK) installed.
- An IDE such as Visual Studio Code, IntelliJ IDEA, or Eclipse.

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/alaa-w/Compiler-Design-Project.git
   ```

2. Navigate to the project directory:

   ```bash
   cd Compiler-Design-Project
   ```

3. Compile the Java files:

   ```bash
   javac *.java
   ```

4. Run the project using the class containing the `main` method:

   ```bash
   java MainClassName
   ```

   Replace `MainClassName` with the actual class name containing the program's entry point.

## Learning Objectives

This project demonstrates practical knowledge of:

- Compiler architecture and design.
- Lexical, syntax, and semantic analysis.
- Intermediate representations.
- Code optimization techniques.
- Code generation.
- Java programming and object-oriented design.

## Author

**Alaa Waheed**

GitHub: [@alaa-w](https://github.com/alaa-w)

---

*Developed as part of a Fundamental Compiler Design project.*
