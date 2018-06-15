import java.sql.*;

public class DBHelper {

    private static Connection connection;
    private static PreparedStatement ps;

    public static void main(String[] args) {

    }

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkUser(String username, String pass) {
        try {
            ps = connection.prepareStatement("SELECT COUNT(username) AS Count FROM users WHERE username = ? AND password = ?;");
            ps.setString(1, username);
            ps.setString(2, pass);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                if (resultSet.getInt("Count") > 0)
                    return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
