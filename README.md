# Tree Visualizer (BST · AVL · Red-Black Tree)

A modern, interactive Java Swing application to visualize and learn Self-Balancing Binary Search Trees. It provides step-by-step visual animations for insertion, deletion, and tree rotations.

---

## 🚀 Features

* **Supported Trees:**
  * **Binary Search Tree (BST):** Standard binary search tree insertion/deletion.
  * **AVL Tree:** Adelson-Velsky and Landis tree featuring auto-balancing with real-time **Balance Factor (BF)** badges.
  * **Red-Black Tree:** Left-Leaning Red-Black (LLRB) implementation featuring Red/Black color properties, node badges, and **Black Height (BH)** tracking.
* **Canvas Interactions:**
  * **Pan:** Click and drag the canvas to explore large trees.
  * **Zoom:** Use your scroll wheel to zoom in and out.
  * **Reset:** Double-click anywhere on the canvas to reset the pan and zoom to default.
* **Interactive Step Mode:**
  * Step-by-step debugger with **Play**, **Pause**, **Next**, and **Previous** buttons.
  * Adjustable simulation speed slider.
  * Detailed textual descriptions of the operations (e.g., comparing values, rotation types, color flips).
* **Live Analytics & Traversals:**
  * Live stats panel displaying tree height, total node count, and balance status.
  * Interactive tree traversals (**Inorder**, **Preorder**, **Postorder**, and **Level Order**) with visual step chips.

---

## 🛠️ Requirements

* **Java Development Kit (JDK) 11 or higher** (Java 19 recommended)

---

## 🏃 How to Run the Project

### Option 1: Direct Run (Without explicit compilation)
If you are using **Java 11 or newer**, you can run the source file directly:
```bash
java BSTVisualizer.java
```

### Option 2: Compile & Run (Standard)
Alternatively, compile the java files and run the main class:
```bash
# Compile
javac BSTVisualizer.java

# Run
java BSTVisualizer
```

---

## 🎨 Design Tokens & Aesthetics
The visualizer has been customized using a cohesive modern design system:
* **Indigo Accent Color Palette** (`#4F46E5` Indigo Primary, `#10B981` Emerald Success, `#EF4444` Rose Danger).
* **Grid Background Layer** to enhance pan and zoom clarity.
* **Visual Node Badges** representing step status (comparing, rotating, inserted) and node types (balance factors, red-black properties).
