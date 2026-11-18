package com.retail;

import java.sql.*;
import java.util.Scanner;

public class InventoryManagementSystem {

    static final String URL = "jdbc:mysql://localhost:3307/retail_db";
    static final String USER = "root";
    static final String PASSWORD = "root";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con =
                     DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.println("MySQL Connected Successfully!");

            int choice;

            do {
                System.out.println("\n===== INVENTORY MANAGEMENT =====");
                System.out.println("1. Insert 3 Products");
                System.out.println("2. Retrieve Products by Category");
                System.out.println("3. Add Product Stock");
                System.out.println("4. Search and Update Price");
                System.out.println("5. Show Inventory Value and Delete");
                System.out.println("0. Exit");
                System.out.print("Enter your choice: ");

                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1 -> insertProducts(con);
                    case 2 -> retrieveProducts(con);
                    case 3 -> updateStock(con);
                    case 4 -> updatePrice(con);
                    case 5 -> inventoryAndDelete(con);
                    case 0 -> System.out.println("Program ended.");
                    default -> System.out.println("Invalid choice!");
                }

            } while (choice != 0);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // (a) Insert at least three product records
    static void insertProducts(Connection con) throws SQLException {

        String sql = "INSERT INTO Product " +
                "(product_id, product_name, category, quantity, price) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 1; i <= 3; i++) {
                System.out.println("\nEnter details for product " + i);

                System.out.print("Product ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Product name: ");
                String name = sc.nextLine();

                System.out.print("Category: ");
                String category = sc.nextLine();

                System.out.print("Quantity: ");
                int quantity = sc.nextInt();

                System.out.print("Price: ");
                double price = sc.nextDouble();
                sc.nextLine();

                if (quantity < 0 || price <= 0) {
                    System.out.println("Invalid quantity or price. Skipped.");
                    continue;
                }

                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setString(3, category);
                ps.setInt(4, quantity);
                ps.setDouble(5, price);

                try {
                    ps.executeUpdate();
                    System.out.println("Product inserted successfully.");
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Duplicate product ID: " + id);
                }
            }
        }
    }

    // (b) Category retrieval, quantity below 20, price descending
    static void retrieveProducts(Connection con) throws SQLException {

        System.out.print("Enter category: ");
        String category = sc.nextLine();

        String sql = "SELECT * FROM Product " +
                "WHERE category = ? AND quantity < 20 " +
                "ORDER BY price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    displayProduct(rs);
                }

                if (!found) {
                    System.out.println("No matching products found.");
                }
            }
        }
    }

    // (c) Add quantity to existing stock
    static void updateStock(Connection con) throws SQLException {

        System.out.print("Enter product ID: ");
        int id = sc.nextInt();

        System.out.print("Quantity to add: ");
        int added = sc.nextInt();
        sc.nextLine();

        if (added <= 0) {
            System.out.println("Quantity to add must be positive.");
            return;
        }

        String sql = "UPDATE Product SET quantity = quantity + ? " +
                "WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, added);
            ps.setInt(2, id);

            if (ps.executeUpdate() > 0) {
                System.out.println("Stock updated successfully.");
            } else {
                System.out.println("Product does not exist.");
            }
        }
    }

    // (d) Search product and update price if positive
    static void updatePrice(Connection con) throws SQLException {

        System.out.print("Enter product ID: ");
        int id = sc.nextInt();

        System.out.print("Enter new price: ");
        double newPrice = sc.nextDouble();
        sc.nextLine();

        if (newPrice <= 0) {
            System.out.println("Price must be greater than zero.");
            return;
        }

        String search = "SELECT * FROM Product WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(search)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Product does not exist.");
                    return;
                }

                String update =
                        "UPDATE Product SET price = ? WHERE product_id = ?";

                try (PreparedStatement ups = con.prepareStatement(update)) {
                    ups.setDouble(1, newPrice);
                    ups.setInt(2, id);
                    ups.executeUpdate();
                }

                System.out.println("Updated product details:");
                try (PreparedStatement check = con.prepareStatement(search)) {
                    check.setInt(1, id);

                    try (ResultSet updated = check.executeQuery()) {
                        if (updated.next()) {
                            displayProduct(updated);
                        }
                    }
                }
            }
        }
    }

    // (e) Calculate inventory value and delete only zero-stock products
    static void inventoryAndDelete(Connection con) throws SQLException {

        String totalSql =
                "SELECT COALESCE(SUM(quantity * price), 0) AS total_value " +
                        "FROM Product";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(totalSql)) {

            if (rs.next()) {
                System.out.printf("Total inventory value: %.2f%n",
                        rs.getDouble("total_value"));
            }
        }

        System.out.print("Enter product ID to delete: ");
        int id = sc.nextInt();
        sc.nextLine();

        String sql = "DELETE FROM Product " +
                "WHERE product_id = ? AND quantity = 0";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            if (ps.executeUpdate() > 0) {
                System.out.println("Zero-stock product deleted successfully.");
            } else {
                System.out.println(
                        "Cannot delete: product does not exist or quantity is not zero.");
            }
        }
    }

    static void displayProduct(ResultSet rs) throws SQLException {
        System.out.println(
                "ID: " + rs.getInt("product_id") +
                        " | Name: " + rs.getString("product_name") +
                        " | Category: " + rs.getString("category") +
                        " | Quantity: " + rs.getInt("quantity") +
                        " | Price: " + rs.getDouble("price"));
    }
}