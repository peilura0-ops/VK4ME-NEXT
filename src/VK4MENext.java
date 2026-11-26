import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

public class VK4MENext extends MIDlet
        implements CommandListener {

    private Display display;
    private Form loginForm;
    private TextField tokenField;
    private TextBox resultBox;

    private Command connectCommand;
    private Command backCommand;
    private Command exitCommand;

    private static final String API =
        "https://api.vk.com/method/newsfeed.get";

    public void startApp() {
        display = Display.getDisplay(this);

        loginForm = new Form("VK4ME NEXT");

        loginForm.append(new StringItem(
            null,
            "Подключение к VK\n\nВведите пользовательский токен:"
        ));

        tokenField = new TextField(
            "VK token:", "", 500, TextField.ANY
        );

        loginForm.append(tokenField);

        connectCommand = new Command(
            "Загрузить ленту", Command.OK, 1
        );

        exitCommand = new Command(
            "Выход", Command.EXIT, 2
        );

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
                showResult("Введите токен VK.");
                return;
            }

            showResult("Подключение к VK...");
            new Thread(new Loader(token)).start();
        }

        if (command == backCommand) {
            display.setCurrent(loginForm);
        }
    }

    private void showResult(String message) {
        resultBox = new TextBox(
            "VK4ME NEXT",
            message,
            4000,
            TextField.ANY
        );

        backCommand = new Command(
            "Назад", Command.BACK, 1
        );

        resultBox.addCommand(backCommand);
        resultBox.setCommandListener(this);

        display.setCurrent(resultBox);
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
                    HttpConnection.GET
                );

                connection.setRequestProperty(
                    "User-Agent", "VK4ME-NEXT"
                );

                int status =
                    connection.getResponseCode();

                input = connection.openInputStream();

                ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

                int data;

                while ((data = input.read()) != -1) {
                    output.write(data);

                    if (output.size() >= 3500) {
                        break;
                    }
                }

                String response =
                    new String(output.toByteArray(), "UTF-8");

                showResult(
                    "HTTP: " + status + "\n\n" + response
                );

            } catch (Exception e) {
                showResult(
                    "Ошибка подключения:\n" +
                    e.toString()
                );

            } finally {
                try {
                    if (input != null) input.close();
                    if (connection != null)
                        connection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
}
