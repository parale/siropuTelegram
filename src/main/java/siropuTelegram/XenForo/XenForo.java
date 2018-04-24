package siropuTelegram.XenForo;

import siropuTelegram.Logger;
import siropuTelegram.Properties;
import siropuTelegram.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class XenForo {
    private Connection con = null;

    public XenForo() {
        connect();
    }

    public void close() {
        disconnect();
    }

    private void connect() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://" +
                            Properties.db_host,
                    Properties.db_user,
                    Properties.db_password
            );
        } catch (SQLException e) {
            siropuTelegram.Logger.logSevere("Can't connect to the database.");
            siropuTelegram.Logger.logException(e);
        }
    }

    private void disconnect() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                siropuTelegram.Logger.logSevere("Can't disconnect from the database.");
                siropuTelegram.Logger.logException(e);
            }
        }
    }

    private ResultSet query(String sql) {
        Statement statement;
        try {
            statement = con.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            siropuTelegram.Logger.logException(e);
            return null;
        }
    }

    private void update(String sql) throws SQLException {
        Statement statement;
        statement = con.createStatement();
        statement.executeUpdate(sql);
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
                siropuTelegram.Logger.logException(e);
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
                siropuTelegram.Logger.logException(e);
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
                siropuTelegram.Logger.logException(e);
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
                siropuTelegram.Logger.logException(e);
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
        if (!Properties.exclude_nodes.isEmpty()) {
            exclude = "AND thread.node_id NOT IN (" + Properties.exclude_nodes + ")";
        }

        // i don't want to see mysql ever again
        result = query("SELECT p1.post_id, p1.thread_id, p1.username, p1.message, title\n" +
                "FROM " + Properties.xf_prefix + "post p1\n" +
                "INNER JOIN (SELECT pi.thread_id, MAX(pi.post_id) AS maxpostid, thread.node_id, thread.title\n" +
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
                            result.getString(4),
                            result.getString(5))
                    );
                }

                if (posts.size() > 0) {
                    lastPostId = posts.get(0).getPostId();
                    update("UPDATE " + Properties.users_table + " SET last_post_id = " + lastPostId + " WHERE xf_user_id = " + user.getXfUserId());
                }

                Collections.reverse(posts);
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }

        return posts;
    }

    public void updateLastPostId() {
        ResultSet result = query("SELECT post_id FROM " + Properties.xf_prefix + "post ORDER BY post_id DESC LIMIT 1");

        if (result != null) {
            try {
                result.next();
                Properties.lastPostId = result.getInt(1);
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }
    }

    public ArrayList<Post> getNewMessages(User user) {
        ResultSet result;
        ArrayList<Integer> threads = new ArrayList<>();
        ArrayList<Post> posts = new ArrayList<>();

        result = query("SELECT thread FROM " + Properties.follow_table + " WHERE xf_user_id = " + user.getXfUserId());
        if (result != null) {
            try {
                while (result.next()) {
                    threads.add(result.getInt(1));
                }
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }

        if (!threads.isEmpty()) {
            for (int thread : threads) {
                result = query(
                        "SELECT " + Properties.xf_prefix + "post.post_id, " + Properties.xf_prefix + "post.thread_id, " + Properties.xf_prefix + "post.username, " + Properties.xf_prefix + "post.message, t.title " +
                                "FROM " + Properties.xf_prefix + "post JOIN xf_thread t " +
                                "ON " + Properties.xf_prefix + "post.thread_id = t.thread_id " +
                                "WHERE " + Properties.xf_prefix + "post.thread_id = " + thread +
                                " AND post_id > " + Properties.lastPostId
                );

                if (result != null) {
                    try {
                        while (result.next()) {
                            posts.add(new Post(
                                    result.getInt(1),
                                    result.getInt(2),
                                    result.getString(3),
                                    result.getString(4),
                                    result.getString(5)
                            ));
                        }
                    } catch (SQLException e) {
                        Logger.logException(e);
                    }
                }
            }
        }

        return posts;
    }

    public ArrayList<User> getFollowers() {
        ResultSet result = query("SELECT " + Properties.follow_table + ".xf_user_id, u.chat_id" +
                " FROM " + Properties.follow_table + " JOIN " + Properties.users_table + " u" +
                " ON " + Properties.follow_table + ".xf_user_id = u.xf_user_id GROUP BY chat_id;");

        ArrayList<User> users = new ArrayList<>();

        if (result != null) {
            try {
                while (result.next()) {
                    users.add(new User(
                            result.getInt(1),
                            result.getLong(2)
                    ));
                }
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }

        return users;
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
                Logger.logException(e);
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
            siropuTelegram.Logger.logException(e);
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
            siropuTelegram.Logger.logException(e);
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
            siropuTelegram.Logger.logException(e);
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
            siropuTelegram.Logger.logException(e);
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
            siropuTelegram.Logger.logException(e);
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
                siropuTelegram.Logger.logException(e);
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isThreadExists(Thread thread) {
        ResultSet result = query("SELECT thread_id FROM " + Properties.xf_prefix + "thread WHERE thread_id = " + thread.getId());
        if (result != null) {
            try {
                return result.next();
            } catch (SQLException e) {
                Logger.logException(e);
                return false;
            }
        }

        return false;
    }

    public boolean isFollowingThread(User user, Thread thread) {
        ResultSet result = query("SELECT * FROM " + Properties.follow_table +
                " WHERE xf_user_id = " + user.getXfUserId() +
                " AND thread = " + thread.getId()
        );

        if (result != null) {
            try {
                return result.next();
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }

        return false;
    }

    public boolean followThread(User user, Thread thread) {
        if (!isFollowingThread(user, thread)) {
            try {
                PreparedStatement statement = con.prepareStatement(
                        "INSERT INTO " + Properties.follow_table + " (xf_user_id, thread) VALUES (?, ?)"
                );

                statement.setInt(1, user.getXfUserId());
                statement.setInt(2, thread.getId());
                statement.executeUpdate();

                return true;
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }

        return false;
    }

    public boolean unfollowThread(User user, Thread thread) {
        if (isFollowingThread(user, thread)) {
            try {
                PreparedStatement statement = con.prepareStatement(
                        "DELETE FROM " + Properties.follow_table + " WHERE xf_user_id = ? AND thread = ?"
                );

                statement.setInt(1, user.getXfUserId());
                statement.setInt(2, thread.getId());
                statement.executeUpdate();

                return true;
            } catch (SQLException e) {
                Logger.logException(e);
            }
        }

        return false;
    }

    public void createTables() {
        ResultSet result = query("show tables like \"" + Properties.users_table + "\"");
        try {
            if (!Objects.requireNonNull(result).next()) {
                update("CREATE TABLE `" + Properties.users_table + "` (\n" +
                        "  `xf_user_id` int(11) NOT NULL,\n" +
                        "  `telegram_user_id` varchar(32) NOT NULL,\n" +
                        "  `chat_id` mediumtext,\n" +
                        "  `last_post_id` int(11) DEFAULT NULL\n" +
                        ")");
            }
        } catch (SQLException e) {
            siropuTelegram.Logger.logException(e);
            System.exit(1);
        }

        result = query("show tables like \"" + Properties.settings_table + "\"");
        try {
            if (!Objects.requireNonNull(result).next()) {
                update("CREATE TABLE `" + Properties.settings_table + "` (\n" +
                        "  `name` varchar(32) NOT NULL,\n" +
                        "  `value` varchar(32) DEFAULT NULL\n" +
                        ")");
            }
        } catch (SQLException e) {
            siropuTelegram.Logger.logException(e);
            System.exit(1);
        }

        result = query("show tables like \"" + Properties.follow_table + "\"");
        try {
            if (!Objects.requireNonNull(result).next()) {
                update("CREATE TABLE " + Properties.follow_table + "\n" +
                        "(\n" +
                        "    xf_user_id INT NOT NULL,\n" +
                        "    thread INT NOT NULL,\n" +
                        ");");
            }
        } catch (SQLException e) {
            siropuTelegram.Logger.logException(e);
            System.exit(1);
        }
    }

    public void checkUserField() {
        ResultSet result = query("select * from " + Properties.xf_prefix + "user_field where field_id = \"telegram\"");
        try {
            if (!Objects.requireNonNull(result).next()) {
                siropuTelegram.Logger.logSevere("Please create custom user field \"Telegram\" in XF CP.");
                System.exit(1);
            }
        } catch (SQLException e) {
            siropuTelegram.Logger.logException(e);
            System.exit(1);
        }
    }

    private int timestamp() {
        return Integer.valueOf(String.valueOf(System.currentTimeMillis()).substring(0, 10));
    }
}
