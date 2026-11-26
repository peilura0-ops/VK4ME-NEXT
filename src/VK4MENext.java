import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

public class VK4MENext extends MIDlet
        implements CommandListener {

    private Display display;
    private Form loginForm;
    private TextField tokenField;
    private Form feedForm;

    private Command connectCommand;
    private Command exitCommand;
    private Command backCommand;

    private static final String API =
        "https://api.vk.com/method/newsfeed.get";

    public void startApp() {
        display = Display.getDisplay(this);
        loginForm = new Form("VK4ME NEXT");

        loginForm.append(new StringItem(null,
            "VK4ME NEXT Classic\n\nВведите токен VK:"));

        tokenField = new TextField(
            "VK token:", "", 500, TextField.ANY);

        loginForm.append(tokenField);

        connectCommand = new Command(
            "Загрузить ленту", Command.OK, 1);

        exitCommand = new Command(
            "Выход", Command.EXIT, 2);

        loginForm.addCommand(connectCommand);
        loginForm.addCommand(exitCommand);
        loginForm.setCommandListener(this);

        display.setCurrent(loginForm);
    }

    public void commandAction(
        Command command, Displayable screen) {

        if (command == exitCommand) {
            destroyApp(true);
            notifyDestroyed();
        }

        if (command == connectCommand) {
            String token = tokenField.getString().trim();

            if (token.length() == 0) {
                showMessage("Введите токен VK.");
                return;
            }

            showMessage("Загрузка новостей...");
            new Thread(new Loader(token)).start();
        }

        if (command == backCommand) {
            display.setCurrent(loginForm);
        }
    }

    private void showMessage(String message) {
        final String text = message;

        display.callSerially(new Runnable() {
            public void run() {
                TextBox box = new TextBox(
                    "VK4ME NEXT", text, 4000,
                    TextField.ANY);

                backCommand = new Command(
                    "Назад", Command.BACK, 1);

                box.addCommand(backCommand);
                box.setCommandListener(VK4MENext.this);
                display.setCurrent(box);
            }
        });
    }

    private class Loader implements Runnable {
        private String token;

        Loader(String value) {
            token = value;
        }

        public void run() {
            HttpConnection connection = null;
            InputStream input = null;

            try {
                String url = API
                    + "?filters=post"
                    + "&count=10"
                    + "&access_token=" + token
                    + "&v=5.199";

                connection = (HttpConnection)
                    Connector.open(url);

                connection.setRequestMethod(
                    HttpConnection.GET);

                int status = connection.getResponseCode();
                input = connection.openInputStream();

                ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

                int data;
                while ((data = input.read()) != -1) {
                    output.write(data);
                    if (output.size() >= 12000) break;
                }

                String json = new String(
                    output.toByteArray(), "UTF-8");

                if (status != 200) {
                    showMessage("HTTP " + status + "\n" + json);
                } else {
                    showFeed(json);
                }

            } catch (Exception e) {
                showMessage("Ошибка:\n" + e.toString());
            } finally {
                try {
                    if (input != null) input.close();
                    if (connection != null) connection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void showFeed(final String json) {
        display.callSerially(new Runnable() {
            public void run() {
                feedForm = new Form("Новости VK");

                feedForm.append(new StringItem(
                    null,
                    "Ответ VK получен.\n\n"
                    + "Пока показываем данные JSON.\n"
                    + "Следующий этап — разбор публикаций."
                ));

                feedForm.append(new StringItem(
                    "Ответ сервера:", json));

                backCommand = new Command(
                    "Назад", Command.BACK, 1);

                feedForm.addCommand(backCommand);
                feedForm.setCommandListener(VK4MENext.this);
                display.setCurrent(feedForm);
            }
        });
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
}
