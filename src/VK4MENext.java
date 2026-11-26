import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.rms.*;
import java.io.*;

public class VK4MENext extends MIDlet
        implements CommandListener {

    private Display display;
    private List menu;
    private TextBox viewer;
    private Form loginForm;

    private TextField tokenField;
    private String token = "";

    private Command openCommand;
    private Command backCommand;
    private Command saveCommand;
    private Command exitCommand;

    private static final String API =
        "https://api.vk.com/method/";

    private static final String VERSION = "5.199";
    private static final String STORE = "VK4ME_TOKEN";

    private static final String[] ITEMS = {
        "Новости",
        "Сообщения",
        "Друзья",
        "Мой профиль",
        "Фотографии",
        "Музыка",
        "Настройки",
        "Выход"
    };

    public void startApp() {
        display = Display.getDisplay(this);

        token = loadToken();

        exitCommand = new Command(
            "Выход", Command.EXIT, 2);

        openCommand = new Command(
            "Открыть", Command.OK, 1);

        if (token.length() == 0) {
            showLogin();
        } else {
            showMenu();
        }
    }

    private void showLogin() {
        loginForm = new Form("VK4ME NEXT 2.0");

        loginForm.append(
            "VK4ME NEXT 2.0\n\n" +
            "Клиент VK для Nokia\n\n" +
            "Введи токен доступа VK:\n");

        tokenField = new TextField(
            "Токен:",
            token,
            1000,
            TextField.ANY);

        loginForm.append(tokenField);

        saveCommand = new Command(
            "Войти", Command.OK, 1);

        loginForm.addCommand(saveCommand);
        loginForm.addCommand(exitCommand);
        loginForm.setCommandListener(this);

        display.setCurrent(loginForm);
    }

    private void showMenu() {
        menu = new List(
            "VK4ME NEXT 2.0",
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
            token = tokenField.getString().trim();

            if (token.length() == 0) {
                showText("Введи токен доступа.");
                return;
            }

            if (saveToken(token)) {
                showMenu();
            } else {
                showText("Ошибка сохранения токена.");
            }
            return;
        }

        if (s == menu && c == openCommand) {
            int index = menu.getSelectedIndex();

            if (index == 0) {
                request("newsfeed.get?filters=post&count=10");
            } else if (index == 1) {
                request("messages.getConversations?count=15");
            } else if (index == 2) {
                request("friends.get?count=50&fields=first_name,last_name");
            } else if (index == 3) {
                request("users.get?fields=photo_100,city");
            } else if (index == 4) {
                request("photos.get?album_id=profile&count=20");
            } else if (index == 5) {
                showText(
                    "МУЗЫКА\n\n" +
                    "Аудиоплеер будет добавлен следующим этапом.");
            } else if (index == 6) {
                showLogin();
            } else {
                destroyApp(true);
                notifyDestroyed();
            }
            return;
        }

        if (c == backCommand) {
            showMenu();
        }
    }

    private void request(final String method) {
        showText("Подключение к VK...\nЗагрузка...");

        Thread worker = new Thread(new Runnable() {
            public void run() {
                HttpConnection con = null;
                InputStream in = null;

                try {
                    String url = API + method
                        + "&access_token=" + encode(token)
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

                    final String result =
                        new String(out.toByteArray(), "UTF-8");

                    display.callSerially(new Runnable() {
                        public void run() {
                            showText(result);
                        }
                    });

                } catch (Exception e) {
                    final String error = e.toString();

                    display.callSerially(new Runnable() {
                        public void run() {
                            showText("Ошибка:\n" + error);
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

    private void showText(String text) {
        viewer = new TextBox(
            "VK4ME NEXT 2.0",
            text,
            12000,
            TextField.ANY);

        backCommand = new Command(
            "Назад", Command.BACK, 1);

        viewer.addCommand(backCommand);
        viewer.setCommandListener(this);

        display.setCurrent(viewer);
    }

    private String encode(String value) {
        try {
            byte[] data = value.getBytes("UTF-8");
            StringBuffer result = new StringBuffer();
            String hex = "0123456789ABCDEF";

            for (int i = 0; i < data.length; i++) {
                int b = data[i] & 255;

                if ((b >= 65 && b <= 90) ||
                    (b >= 97 && b <= 122) ||
                    (b >= 48 && b <= 57) ||
                    b == 45 || b == 46 ||
                    b == 95 || b == 126) {
                    result.append((char)b);
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

    private String loadToken() {
        RecordStore store = null;

        try {
            store = RecordStore.openRecordStore(
                STORE, true);

            if (store.getNumRecords() > 0) {
                return new String(
                    store.getRecord(1), "UTF-8");
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
                STORE, true);

            byte[] data = value.getBytes("UTF-8");

            if (store.getNumRecords() > 0) {
                store.setRecord(
                    1, data, 0, data.length);
            } else {
                store.addRecord(
                    data, 0, data.length);
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
    }
}
