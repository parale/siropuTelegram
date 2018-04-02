package siropuTelegram.XenForo;

import siropuTelegram.Properties;
import siropuTelegram.Solution;
import siropuTelegram.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XenForo {
    private Logger LOGGER = Solution.LOGGER;
    private Connection con = null;

    private static int connections = 0;

    private String host;
    private String user;
    private String password;

    // assign db host, username and password upon creating
    public XenForo(String host, String user, String password) {
        this.host = host;
        this.user = user;
        this.password = password;

        connect();
    }

    public void close() {
        disconnect();
    }

    private void connect() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://" + host, user, password);
            if (Properties.sqlconnections.equals("1")) {
                connections++;
                System.out.println("+ " + connections + " " + java.lang.Thread.currentThread().getStackTrace()[2]);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Can't connect to the database.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    private void disconnect() {
        if (con != null) {
            try {
                con.close();
                if (Properties.sqlconnections.equals("1")) {
                    connections--;
                    System.out.println("- " + connections + " " + java.lang.Thread.currentThread().getStackTrace()[2]);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Can't disconnect from the database.");
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    private ResultSet query(String sql) {
        Statement statement;
        try {
            statement = con.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return null;
        }
    }

    private void update(String sql) {
        Statement statement;
        try {
            statement = con.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    public ArrayList<ChatMessage> getAllMessages() {
        ResultSet result;
        if (Properties.lastMessageId == 0) {
            result = query("select value from " + Properties.settings_table + " where name = \"lastMessageId\"");
            try {
                if (result != null) {
                    result.next();
                    int newMessageId = result.getInt(1);
                    if (Properties.lastMessageId == newMessageId) {
                        return null;
                    } else {
                        Properties.lastMessageId = newMessageId;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        }

        result = query("select shout_id, " + Properties.xf_prefix + "user.username, shout_message, shout_date, " + Properties.xf_prefix + "user.user_id from " + Properties.xf_prefix + "siropu_shoutbox_shout inner join " + Properties.xf_prefix + "user on " + Properties.xf_prefix + "siropu_shoutbox_shout.shout_user_id = " + Properties.xf_prefix + "user.user_id where shout_id > " + Properties.lastMessageId);
        ArrayList<ChatMessage> messages = new ArrayList<>();
        if (result != null) {
            try {
                while (result.next()) {
                    ChatMessage chatMessage = new ChatMessage(
                            result.getString(3),
                            result.getString(2),
                            result.getInt(5)
                    );

                    Properties.lastMessageId = result.getInt(1);
                    messages.add(chatMessage);
                }

                update("update " + Properties.settings_table + " set value = " + Properties.lastMessageId + " where name = \"lastMessageId\"");
                return messages;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    public ArrayList<Thread> getNewThreads() {
        ResultSet result;

        if (Properties.lastThreadId == 0) {
            result = query("select thread_id from " + Properties.xf_prefix + "thread order by thread_id desc limit 1");
            try {
                if (result != null) {
                    result.next();
                    Properties.lastThreadId = result.getInt(1);
                }
                return null;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        }

        result = query("select thread_id, title, username from " + Properties.xf_prefix + "thread where thread_id > " + Properties.lastThreadId);
        ArrayList<Thread> threads = new ArrayList<>();
        if (result != null) {
            try {
                while (result.next()) {
                    threads.add(new Thread(
                            result.getInt(1),
                            result.getString(2),
                            result.getString(3))
                    );

                    Properties.lastThreadId = result.getInt(1);
                }

                return threads;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    public void sendMessage(int user, String message) {
        if (Properties.dev.equals("0")) {
            String time = String.valueOf(System.currentTimeMillis()).substring(0, 10);
            try {
                PreparedStatement statement = con.prepareStatement("insert into " + Properties.xf_prefix + "siropu_shoutbox_shout (shout_user_id, shout_message, shout_date) values (?, ?, ?)");
                statement.setInt(1, user);
                statement.setString(2, message);
                statement.setInt(3, Integer.valueOf(time));
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        } else {
            System.out.println(String.format("%s: %s", user, message));
        }
    }

    public boolean isUserActive(String telegram_user_id) {
        try {
            PreparedStatement statement = con.prepareStatement("select * from " + Properties.users_table + " where telegram_user_id = ?");
            statement.setString(1, telegram_user_id);
            ResultSet result = statement.executeQuery();

            return result.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return isUserActive(telegram_user_id);
        }
    }

    public int getActiveUserId(String telegram_user_id) {
        try {
            PreparedStatement statement = con.prepareStatement("select xf_user_id from " + Properties.users_table + " where telegram_user_id = ?");
            statement.setString(1, telegram_user_id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getInt("xf_user_id");
            } else {
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return 0;
        }
    }

    public int getUserIdByTelegram(String telegram_user_id) {
        try {
            PreparedStatement statement = con.prepareStatement("select user_id from " + Properties.xf_prefix + "user_field_value where field_value = ? and field_id = ?");
            statement.setString(1, telegram_user_id);
            statement.setString(2, "telegram");
            ResultSet result = statement.executeQuery();

            int rows = 0;
            int xf_user_id = 0;

            while (result.next()) {
                rows++;
                xf_user_id = result.getInt("user_id");
            }

            if (rows == 1) {
                return xf_user_id;
            } else if (rows > 1) {
                return -1;
            } else {
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return 0;
        }
    }

    public boolean createUser(int xf_user_id, String telegram_user_id, long chat_id) {
        try {
            PreparedStatement statement = con.prepareStatement("insert into " + Properties.users_table + " (xf_user_id, telegram_user_id, chat_id) values(?, ?, ?)");
            statement.setInt(1, xf_user_id);
            statement.setString(2, telegram_user_id);
            statement.setLong(3, chat_id);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }
    }

    public boolean deleteUser(String telegram_user_id) {
        try {
            PreparedStatement statement = con.prepareStatement("delete from " + Properties.users_table + " where telegram_user_id = ?");
            statement.setString(1, telegram_user_id);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }
    }

    public ArrayList<User> getClientsList() {
        ResultSet result;

        result = query("select xf_user_id, chat_id from " + Properties.users_table);

        if (result != null) {
            ArrayList<User> clients = new ArrayList<>();
            try {
                while (result.next()) {
                    User user = new User();
                    user.setXfUserId(result.getInt(1));
                    user.setTelegramChatId(result.getLong(2));
                    clients.add(user);
                }
                return clients;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    public void createTables() {
        ResultSet result = query("show tables like \"" + Properties.users_table + "\"");
        try {
            if (!Objects.requireNonNull(result).next()) {
                Statement statement = con.createStatement();
                statement.executeUpdate("CREATE TABLE `" + Properties.users_table + "` (\n" +
                        "  `xf_user_id` int(11) NOT NULL,\n" +
                        "  `telegram_user_id` varchar(32) NOT NULL,\n" +
                        "  `chat_id` mediumtext\n" +
                        ")");
                statement.executeUpdate("CREATE TABLE `" + Properties.settings_table + "` (\n" +
                        "  `name` varchar(32) NOT NULL,\n" +
                        "  `value` varchar(32) DEFAULT NULL\n" +
                        ")");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }
    }

    public void checkUserField() {
        ResultSet result = query("select * from " + Properties.xf_prefix + "user_field where field_id = \"telegram\"");
        try {
            if (!Objects.requireNonNull(result).next()) {
                LOGGER.log(Level.SEVERE, "Please create custom user field \"Telegram\" in XF CP.");
                System.exit(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }
    }
}
