package siropuTelegram.XenForo;

import siropuTelegram.Properties;
import siropuTelegram.Solution;
import siropuTelegram.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
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

    public XenForo() {
        this.host = Properties.db_host;
        this.user = Properties.db_user;
        this.password = Properties.db_password;

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

    public ArrayList<Post> whatsNew(User user) {
        ResultSet result;

        int lastPostId = 0;
        result = query("SELECT last_post_id FROM " + Properties.users_table + " WHERE xf_user_id = " + user.getXfUserId());
        if (result != null) {
            try {
                if (result.next()) {
                    lastPostId = result.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        int date = timestamp() - 86400;
        // honestly, mysql is a hell
        // all hail our lord and saviour stackoverflow
        // https://stackoverflow.com/questions/1313120/retrieving-the-last-record-in-each-group-mysql
        String exclude = "";
        if (Properties.exclude_nodes != null) {
            exclude = "AND thread.node_id NOT IN (" + Properties.exclude_nodes + ")";
        }

        // i don't want to see mysql ever again
        result = query("SELECT p1.post_id, p1.thread_id, p1.username, p1.message\n" +
                "FROM " + Properties.xf_prefix + "post p1\n" +
                "INNER JOIN (SELECT pi.thread_id, MAX(pi.post_id) AS maxpostid, thread.node_id\n" +
                "            FROM " + Properties.xf_prefix + "post pi join " + Properties.xf_prefix + "thread as thread on pi.thread_id = thread.thread_id\n" +
                "            WHERE pi.post_date >= " + date + " AND pi.message_state = 'visible' AND pi.post_id > " + lastPostId + "  " + exclude + " GROUP BY pi.thread_id) p2\n" +
                "  ON (p1.post_id = p2.maxpostid)\n" +
                " ORDER BY post_id DESC LIMIT 5;");

        ArrayList<Post> posts = new ArrayList<>();
        if (result != null) {
            try {
                while (result.next()) {
                    posts.add(new Post(
                            result.getInt(1),
                            result.getInt(2),
                            result.getString(3),
                            result.getString(4))
                    );
                }

                if (posts.size() > 0) {
                    lastPostId = posts.get(0).getPost_id();
                    update("UPDATE " + Properties.users_table + " SET last_post_id = " + lastPostId + " WHERE xf_user_id = " + user.getXfUserId());
                }

                Collections.reverse(posts);
                return posts;
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
            try {
                PreparedStatement statement = con.prepareStatement("insert into " + Properties.xf_prefix + "siropu_shoutbox_shout (shout_user_id, shout_message, shout_date) values (?, ?, ?)");
                statement.setInt(1, user);
                statement.setString(2, message);
                statement.setInt(3, timestamp());
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
                        "  `chat_id` mediumtext,\n" +
                        "  `last_post_id` int(11) DEFAULT NULL\n" +
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

    private int timestamp() {
        return Integer.valueOf(String.valueOf(System.currentTimeMillis()).substring(0, 10));
    }

    public void updateTables(int ver) {
        if (ver == 2) {
            try {
                Statement statement = con.createStatement();
                statement.executeUpdate("ALTER TABLE " + Properties.users_table + " ADD last_post_id INT NULL");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                System.exit(1);
            }
        }
    }
}
