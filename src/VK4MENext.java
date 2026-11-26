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
    private Command openCommand;
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
        "Мой профиль",
        "Фотографии",
        "Музыка",
        "Настройки / токен",
        "Выход"
    };

    public void startApp() {
        display = Display.getDisplay(this);

        exitCommand = new Command(
            "Выход", Command.EXIT, 2);

        openCommand = new Command(
            "Открыть", Command.OK, 1);

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

        loginForm.append(
            "VK4ME NEXT\n" +
            "Клиент VK для Nokia S60\n\n");

        loginForm.append(
            "Вставь пользовательский токен VK.\n\n");

        tokenField = new TextField(
            "Токен:",
            token,
            1000,
            TextField.ANY);

        loginForm.append(tokenField);

        saveCommand = new Command(
            "Сохранить и войти",
            Command.OK,
            1);

        loginForm.addCommand(saveCommand);
        loginForm.addCommand(exitCommand);
        loginForm.setCommandListener(this);

        display.setCurrent(loginForm);
    }

    private void showMenu() {
        menu = new List(
            "VK4ME NEXT",
            Choice.IMPLICIT);

        for (int i = 0; i < ITEMS.length; i++) {
            menu.append(ITEMS[i], null);
        }

        menu.setSelectCommand(openCommand);
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
                showMessage("Введи токен VK.");
                return;
            }

            token = entered;

            if (saveToken(token)) {
                showMenu();
                loadNews();
            } else {
                showMessage(
                    "Не удалось сохранить токен.");
            }
            return;
        }

        if (s == menu && c == openCommand) {
            int selected = menu.getSelectedIndex();

            if (selected == 0) {
                loadNews();

            } else if (selected == 1) {
                request(
                    "messages.getConversations?count=15");

            } else if (selected == 2) {
                request(
                    "friends.get?count=50&fields=first_name,last_name");

            } else if (selected == 3) {
                request(
                    "users.get?fields=photo_100,city");

            } else if (selected == 4) {
                request(
                    "photos.get?album_id=profile&count=20");

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
            playAddress(
                streamField.getString().trim());
            return;
        }

        if (c == playLocalCommand) {
            playAddress(
                localField.getString().trim());
            return;
        }

        if (c == stopCommand) {
            stopMusic();
            return;
        }
    }

    private void loadNews() {
        request(
            "newsfeed.get?filters=post&count=10");
    }

    private void request(final String method) {
        showMessage("Подключение к VK...\nЗагрузка...");

        Thread worker = new Thread(new Runnable() {
            public void run() {
                HttpConnection con = null;
                InputStream in = null;

                try {
                    String url = API + method
                        + "&access_token="
                        + urlEncode(token)
                        + "&v=" + VERSION;

                    con = (HttpConnection)
                        Connector.open(url);

                    con.setRequestMethod(
                        HttpConnection.GET);

                    con.setRequestProperty(
                        "User-Agent",
                        "VK4ME-NEXT/1.0");

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

                        if (total >= 20000) {
                            break;
                        }
                    }

                    final String json =
                        new String(
                            out.toByteArray(),
                            "UTF-8");

                    final String result;

                    if (json.indexOf("\"error\"") >= 0) {
                        result =
                            "Ошибка VK API:\n\n" + json;

                    } else if (
                        method.startsWith("newsfeed.get")) {

                        result = formatNews(json);

                    } else {
                        result =
                            "Ответ VK:\n\n" + json;
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
                                "Ошибка соединения:\n\n"
                                + error);
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

    try {
        java.util.Hashtable root =
            VKJson.asObject(VKJson.parse(json));

        java.util.Hashtable response =
            VKJson.getObject(root, "response");

        java.util.Vector items =
            VKJson.getArray(response, "items");

        int shown = 0;

        for (int i = 0; i < items.size(); i++) {
            java.util.Hashtable item =
                VKJson.asObject(items.elementAt(i));

            java.util.Hashtable post =
                VKJson.getObject(item, "post");

            String text =
                VKJson.getString(post, "text");

            if (text.length() == 0) {
                text = VKJson.getString(item, "text");
            }

            if (text.length() > 0) {
                shown++;

                result.append("ПУБЛИКАЦИЯ ");
                result.append(shown);
                result.append("\n\n");
                result.append(text);
                result.append("\n\n");
                result.append("----------------\n\n");
            }
        }

        if (shown == 0) {
            result.append(
                "В ленте пока нет текстовых публикаций.");
        }

    } catch (Exception e) {
        result.append("Не удалось обработать ленту VK.\n");
        result.append(e.toString());
    }

    return result.toString();
}


    private void showMusic() {
        musicForm = new Form("VK4ME - Музыка");

        musicForm.append(
            "ВОСПРОИЗВЕДЕНИЕ АУДИО\n\n");

        streamField = new TextField(
            "HTTP/HTTPS ссылка:",
            "",
            500,
            TextField.URL);

        musicForm.append(streamField);

        playStreamCommand = new Command(
            "Играть поток",
            Command.OK,
            1);

        musicForm.append(
            "\nЛОКАЛЬНЫЙ АУДИОФАЙЛ\n");

        localField = new TextField(
            "Путь к файлу:",
            "",
            500,
            TextField.URL);

        musicForm.append(localField);

        playLocalCommand = new Command(
            "Играть файл",
            Command.OK,
            2);

        statusItem = new StringItem(
            "Статус:",
            "Ожидание");

        musicForm.append(statusItem);

        stopCommand = new Command(
            "Стоп",
            Command.STOP,
            3);

        backCommand = new Command(
            "Назад",
            Command.BACK,
            4);

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
                statusItem.setText(
                    "Укажи адрес аудио.");
                return;
            }

            if (!(address.startsWith("http://")
                    || address.startsWith("https://")
                    || address.startsWith("file://"))) {

                statusItem.setText(
                    "Неверный адрес.");
                return;
            }

            statusItem.setText(
                "Подключение...");

            player = Manager.createPlayer(address);
            player.realize();
            player.start();

            statusItem.setText(
                "Воспроизведение.");

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
            statusItem.setText("Остановлено.");
        }
    }

    private String urlEncode(String value) {
        try {
            byte[] data = value.getBytes("UTF-8");

            StringBuffer result =
                new StringBuffer();

            String hex =
                "0123456789ABCDEF";

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
            "VK4ME NEXT",
            message,
            12000,
            TextField.ANY);

        backCommand = new Command(
            "Назад",
            Command.BACK,
            1);

        box.addCommand(backCommand);
        box.setCommandListener(this);

        display.setCurrent(box);
    }

    private void showResult(String message) {
        TextBox box = new TextBox(
            "VK4ME NEXT",
            message,
            12000,
            TextField.ANY);

        backCommand = new Command(
            "Назад",
            Command.BACK,
            1);

        box.addCommand(backCommand);
        box.setCommandListener(this);

        display.setCurrent(box);
    }

    private String loadToken() {
        RecordStore store = null;

        try {
            store = RecordStore.openRecordStore(
                STORE,
                true);

            if (store.getNumRecords() > 0) {
                byte[] data = store.getRecord(1);

                return new String(
                    data,
                    "UTF-8");
            }

        } catch (Exception e) {
        } finally {
            try {
                if (store != null) {
                    store.closeRecordStore();
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private boolean saveToken(String value) {
        RecordStore store = null;

        try {
            store = RecordStore.openRecordStore(
                STORE,
                true);

            byte[] data = value.getBytes("UTF-8");

            if (store.getNumRecords() > 0) {
                store.setRecord(
                    1,
                    data,
                    0,
                    data.length);

            } else {
                store.addRecord(
                    data,
                    0,
                    data.length);
            }

            return true;

        } catch (Exception e) {
            return false;

        } finally {
            try {
                if (store != null) {
                    store.closeRecordStore();
                }
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
