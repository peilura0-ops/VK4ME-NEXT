import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import java.io.*;

public class VK4MENext extends MIDlet
        implements CommandListener {

    private Display display;
    private List menu;
    private Form loginForm;
    private Form musicForm;

    private TextField tokenField;
    private TextField streamField;
    private TextField localField;
    private StringItem statusItem;

    private String token = "";
    private Player player;

    private Command saveCommand;
    private Command exitCommand;
    private Command backCommand;
    private Command playStreamCommand;
    private Command playLocalCommand;
    private Command stopCommand;

    private static final String API =
        "https://api.vk.com/method/";

    private static final String STORE =
        "VK4ME_TOKEN";

    private static final String VERSION =
        "5.199";

    private static final String[] ITEMS = {
        "Новости",
        "Сообщения",
        "Друзья",
        "Профиль",
        "Фотографии",
        "Музыка",
        "Настройки / токен",
        "Выход"
    };

    public void startApp() {
        display = Display.getDisplay(this);

        exitCommand = new Command(
            "Выход", Command.EXIT, 2);

        token = loadToken();

        if (token.length() > 0) {
            showMenu();
            loadNews();
        } else {
            showLogin();
        }
    }

    private void showLogin() {
        loginForm = new Form("VK4ME NEXT");

        loginForm.append("VK4ME NEXT Classic 0.3\n\n");
        loginForm.append(
            "Введи пользовательский токен VK.\n");

        tokenField = new TextField(
            "Токен:", token, 1000, TextField.ANY);

        loginForm.append(tokenField);

        saveCommand = new Command(
            "Сохранить", Command.OK, 1);

        loginForm.addCommand(saveCommand);
        loginForm.addCommand(exitCommand);
        loginForm.setCommandListener(this);

        display.setCurrent(loginForm);
    }

    private void showMenu() {
        menu = new List(
            "VK4ME NEXT", Choice.IMPLICIT);

        for (int i = 0; i < ITEMS.length; i++) {
            menu.append(ITEMS[i], null);
        }

        menu.setSelectCommand(
            new Command("Открыть", Command.OK, 1));

        menu.addCommand(exitCommand);
        menu.setCommandListener(this);

        display.setCurrent(menu);
    }

    public void commandAction(
        Command c, Displayable s) {

        if (c == exitCommand) {
            destroyApp(true);
            notifyDestroyed();
            return;
        }

        if (s == loginForm && c == saveCommand) {
            String entered =
                tokenField.getString().trim();

            if (entered.length() == 0) {
                showMessage("Введи токен VK");
                return;
            }

            token = entered;
            saveToken(token);
            showMenu();
            loadNews();
            return;
        }

        if (s == menu && c == List.SELECT_COMMAND) {
            int selected = menu.getSelectedIndex();

            if (selected == 0) {
                loadNews();
            } else if (selected == 1) {
                request("messages.getConversations?count=15");
            } else if (selected == 2) {
                request("friends.get?count=50&fields=first_name,last_name");
            } else if (selected == 3) {
                request("users.get?fields=photo_100,city");
            } else if (selected == 4) {
                request("photos.get?album_id=profile&count=20");
            } else if (selected == 5) {
                showMusic();
            } else if (selected == 6) {
                showLogin();
            } else if (selected == 7) {
                destroyApp(true);
                notifyDestroyed();
            }
            return;
        }

        if (c == backCommand) {
            showMenu();
            return;
        }

        if (c == playStreamCommand) {
            playAddress(streamField.getString().trim());
            return;
        }

        if (c == playLocalCommand) {
            playAddress(localField.getString().trim());
            return;
        }

        if (c == stopCommand) {
            stopMusic();
            return;
        }
    }

    private void loadNews() {
        request("newsfeed.get?filters=post&count=10");
    }

    private void request(final String method) {
        showMessage("Подключение к VK...\nЗагрузка...");

        Thread worker = new Thread(new Runnable() {
            public void run() {
                HttpConnection con = null;
                InputStream in = null;

                try {
                    String url = API + method
                        + "&access_token=" + urlEncode(token)
                        + "&v=" + VERSION;

                    con = (HttpConnection)
                        Connector.open(url);

                    con.setRequestMethod(
                        HttpConnection.GET);

                    int status = con.getResponseCode();

                    if (status < 200 || status >= 300) {
                        throw new IOException(
                            "HTTP " + status);
                    }

                    in = con.openInputStream();

                    ByteArrayOutputStream out =
                        new ByteArrayOutputStream();

                    byte[] buffer = new byte[512];
                    int count;
                    int total = 0;

                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                        total += count;
                        if (total >= 20000) break;
                    }

                    final String json =
                        new String(out.toByteArray(), "UTF-8");

                    final String result;

                    if (json.indexOf("\"error\"") >= 0) {
                        result = "Ошибка VK API:\n\n" + json;
                    } else if (method.startsWith("newsfeed.get")) {
                        result = formatNews(json);
                    } else {
                        result = "Ответ VK:\n\n" + json;
                    }

                    display.callSerially(new Runnable() {
                        public void run() {
                            showResult(result);
                        }
                    });

                } catch (Exception e) {
                    final String error = e.toString();

                    display.callSerially(new Runnable() {
                        public void run() {
                            showResult(
                                "Ошибка соединения:\n" + error);
                        }
                    });

                } finally {
                    try {
                        if (in != null) in.close();
                        if (con != null) con.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        worker.start();
    }

    private String formatNews(String json) {
        StringBuffer result = new StringBuffer();
        result.append("НОВОСТИ VK\n\n");

        int position = 0;
        int number = 0;

        while (position < json.length() && number < 10) {
            int key = json.indexOf("\"text\"", position);
            if (key < 0) break;

            int colon = json.indexOf(':', key + 6);
            if (colon < 0) break;

            int quote = json.indexOf('"', colon + 1);
            if (quote < 0) break;

            StringBuffer post = new StringBuffer();
            int i = quote + 1;
            boolean closed = false;

            while (i < json.length()) {
                char ch = json.charAt(i++);

                if (ch == '"') {
                    closed = true;
                    break;
                }

                if (ch == '\\' && i < json.length()) {
                    char next = json.charAt(i++);

                    if (next == 'n') {
                        post.append('\n');
                    } else if (next == 'r' || next == 't') {
                        post.append(' ');
                    } else if (next == 'u'
                            && i + 4 <= json.length()) {
                        try {
                            int code = Integer.parseInt(
                                json.substring(i, i + 4), 16);
                            post.append((char) code);
                            i += 4;
                        } catch (Exception e) {
                            post.append('?');
                        }
                    } else {
                        post.append(next);
                    }
                } else {
                    post.append(ch);
                }
            }

            position = i;

            String text = post.toString().trim();

            if (closed && text.length() > 0) {
                number++;
                result.append("Пост ");
                result.append(number);
                result.append("\n");
                result.append(text);
                result.append("\n\n----------------\n\n");
            }
        }

        if (number == 0) {
            result.append("Текстовых записей не найдено.\n\n");
            result.append(json);
        }

        return result.toString();
    }

    private void showMusic() {
        musicForm = new Form("VK4ME - Музыка");

        musicForm.append("ИНТЕРНЕТ-ПОТОК\n");

        streamField = new TextField(
            "HTTP/HTTPS URL:",
            "http://example.com/radio.mp3",
            500, TextField.URL);

        musicForm.append(streamField);

        playStreamCommand = new Command(
            "Играть поток", Command.OK, 1);

        musicForm.append("\nЛОКАЛЬНЫЙ ФАЙЛ\n");

        localField = new TextField(
            "Путь к файлу:",
            "file:///E:/Music/song.mp3",
            500, TextField.URL);

        musicForm.append(localField);

        playLocalCommand = new Command(
            "Играть файл", Command.OK, 2);

        statusItem = new StringItem(
            "Статус:", "Ожидание");

        musicForm.append(statusItem);

        stopCommand = new Command(
            "Стоп", Command.STOP, 3);

        backCommand = new Command(
            "Назад", Command.BACK, 4);

        musicForm.addCommand(playStreamCommand);
        musicForm.addCommand(playLocalCommand);
        musicForm.addCommand(stopCommand);
        musicForm.addCommand(backCommand);

        musicForm.setCommandListener(this);
        display.setCurrent(musicForm);
    }

    private void playAddress(String address) {
        try {
            stopMusic();

            if (address.length() == 0) {
                statusItem.setText("Нет адреса");
                return;
            }

            if (!(address.startsWith("http://")
                    || address.startsWith("https://")
                    || address.startsWith("file://"))) {
                statusItem.setText("Неверный адрес");
                return;
            }

            statusItem.setText("Подключение...");

            player = Manager.createPlayer(address);
            player.realize();
            player.start();

            statusItem.setText("Воспроизведение");

        } catch (Exception e) {
            statusItem.setText(
                "Ошибка плеера: " + e.toString());
        }
    }

    private void stopMusic() {
        try {
            if (player != null) {
                player.stop();
                player.close();
                player = null;
            }
        } catch (Exception ignored) {
        }

        if (statusItem != null) {
            statusItem.setText("Остановлено");
        }
    }

    private String urlEncode(String value) {
        try {
            byte[] data = value.getBytes("UTF-8");
            StringBuffer result = new StringBuffer();
            String hex = "0123456789ABCDEF";

            for (int i = 0; i < data.length; i++) {
                int b = data[i] & 255;

                if ((b >= 65 && b <= 90)
                    || (b >= 97 && b <= 122)
                    || (b >= 48 && b <= 57)
                    || b == 45 || b == 46
                    || b == 95 || b == 126) {
                    result.append((char) b);
                } else {
                    result.append('%');
                    result.append(hex.charAt(b >> 4));
                    result.append(hex.charAt(b & 15));
                }
            }

            return result.toString();

        } catch (Exception e) {
            return value;
        }
    }

    private void showMessage(String message) {
        TextBox box = new TextBox(
            "VK4ME NEXT", message, 12000,
            TextField.ANY);

        backCommand = new Command(
            "Назад", Command.BACK, 1);

        box.addCommand(backCommand);
        box.setCommandListener(this);
        display.setCurrent(box);
    }

    private void showResult(String message) {
        TextBox box = new TextBox(
            "VK4ME NEXT", message, 12000,
            TextField.ANY);

        backCommand = new Command(
            "Назад", Command.BACK, 1);

        box.addCommand(backCommand);
        box.setCommandListener(this);
        display.setCurrent(box);
    }

    private String loadToken() {
        RecordStore store = null;

        try {
            store = RecordStore.openRecordStore(
                "VK4ME_TOKEN", true);

            if (store.getNumRecords() > 0) {
                byte[] data = store.getRecord(1);
                return new String(data, "UTF-8");
            }

        } catch (Exception e) {
        } finally {
            try {
                if (store != null) store.closeRecordStore();
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private void saveToken(String value) {
        RecordStore store = null;

        try {
            store = RecordStore.openRecordStore(
                "VK4ME_TOKEN", true);

            byte[] data = value.getBytes("UTF-8");

            if (store.getNumRecords() > 0) {
                store.setRecord(1, data, 0, data.length);
            } else {
                store.addRecord(data, 0, data.length);
            }

        } catch (Exception e) {
            showMessage("Ошибка сохранения токена:\n"
                + e.toString());
        } finally {
            try {
                if (store != null) store.closeRecordStore();
            } catch (Exception ignored) {
            }
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        stopMusic();
    }
}
