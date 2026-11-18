import java.sql.*;

public class Main {
    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3307/retail_db";
        String user = "root";
        String password = "root";

        try (Connection con =
                     DriverManager.getConnection(url, user, password)) {

            System.out.println("Database Connected Successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}