import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.media.*;
import java.io.*;

public class VK4MENext extends MIDlet
        implements CommandListener {

    private Display display;
    private Form menu;
    private TextField tokenField;
    private TextField musicPath;
    private TextBox result;

    private Command openCommand;
    private Command backCommand;
    private Command exitCommand;
    private Command playCommand;
    private Command stopCommand;

    private Player player;
    private String token = "";

    private static final String API =
        "https://api.vk.com/method/";

    public void startApp() {
        display = Display.getDisplay(this);
        showLogin();
    }

    private void showLogin() {
        Form form = new Form("VK4ME NEXT");

        form.append("VK4ME NEXT Classic\n\n");
        tokenField = new TextField(
            "Токен VK:", "", 500, TextField.ANY);

        form.append(tokenField);

        openCommand = new Command(
            "Продолжить", Command.OK, 1);

        exitCommand = new Command(
            "Выход", Command.EXIT, 2);

        form.addCommand(openCommand);
        form.addCommand(exitCommand);
        form.setCommandListener(this);

        display.setCurrent(form);
    }

    private void showMenu() {
        menu = new Form("VK4ME NEXT");

        menu.append("Главное меню\n\n");
        menu.append("Новости\n");
        menu.append("Сообщения\n");
        menu.append("Друзья\n");
        menu.append("Профиль\n");
        menu.append("Фото\n");
        menu.append("Музыка\n");

        StringItem news = new StringItem(
            "Новости", "Открыть ленту");
        StringItem messages = new StringItem(
            "Сообщения", "Открыть диалоги");
        StringItem friends = new StringItem(
            "Друзья", "Список друзей");
        StringItem profile = new StringItem(
            "Профиль", "Мой профиль");
        StringItem photos = new StringItem(
            "Фото", "Мои фотографии");
        StringItem music = new StringItem(
            "Музыка", "Локальные MP3");

        menu.deleteAll();

        menu.append(news);
        menu.append(messages);
        menu.append(friends);
        menu.append(profile);
        menu.append(photos);
        menu.append(music);

        menu.addCommand(new Command(
            "Новости", Command.ITEM, 1));
        menu.addCommand(new Command(
            "Сообщения", Command.ITEM, 2));
        menu.addCommand(new Command(
            "Друзья", Command.ITEM, 3));
        menu.addCommand(new Command(
            "Профиль", Command.ITEM, 4));
        menu.addCommand(new Command(
            "Фото", Command.ITEM, 5));
        menu.addCommand(new Command(
            "Музыка", Command.ITEM, 6));
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

        if (c == openCommand) {
            token = tokenField.getString().trim();

            if (token.length() == 0) {
                showText("Введите токен VK");
                return;
            }

            showMenu();
            return;
        }

        if (c == backCommand) {
            showMenu();
            return;
        }

        if (s == menu) {
            if (c.getLabel().equals("Новости")) {
                request("newsfeed.get?filters=post&count=10");
            } else if (c.getLabel().equals("Сообщения")) {
                request("messages.getConversations?count=10");
            } else if (c.getLabel().equals("Друзья")) {
                request("friends.get?count=50&fields=first_name,last_name");
            } else if (c.getLabel().equals("Профиль")) {
                request("users.get?fields=photo_100,city");
            } else if (c.getLabel().equals("Фото")) {
                request("photos.get?album_id=profile&count=20");
            } else if (c.getLabel().equals("Музыка")) {
                showMusic();
            }
        }

        if (c == playCommand) {
            playMusic();
        }

        if (c == stopCommand) {
            stopMusic();
        }
    }

    private void request(final String method) {
        showText("Загрузка...");

        new Thread(new Runnable() {
            public void run() {
                HttpConnection con = null;
                InputStream in = null;

                try {
                    String url = API + method
                        + "&access_token=" + token
                        + "&v=5.199";

                    con = (HttpConnection)
                        Connector.open(url);

                    con.setRequestMethod(
                        HttpConnection.GET);

                    int status = con.getResponseCode();
                    in = con.openInputStream();

                    ByteArrayOutputStream out =
                        new ByteArrayOutputStream();

                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                        if (out.size() > 12000) break;
                    }

                    final String answer =
                        "HTTP " + status + "\n\n"
                        + new String(
                            out.toByteArray(), "UTF-8");

                    display.callSerially(new Runnable() {
                        public void run() {
                            showText(answer);
                        }
                    });

                } catch (Exception e) {
                    final String error = e.toString();

                    display.callSerially(new Runnable() {
                        public void run() {
                            showText("Ошибка: " + error);
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
        }).start();
    }

    private void showMusic() {
        Form form = new Form("Музыкальный плеер");

        form.append("Путь к MP3-файлу:\n");

        musicPath = new TextField(
            "Файл:", "file:///E:/Music/song.mp3",
            250, TextField.URL);

        form.append(musicPath);

        playCommand = new Command(
            "Воспроизвести", Command.OK, 1);

        stopCommand = new Command(
            "Стоп", Command.STOP, 2);

        backCommand = new Command(
            "Назад", Command.BACK, 3);

        form.addCommand(playCommand);
        form.addCommand(stopCommand);
        form.addCommand(backCommand);
        form.setCommandListener(this);

        display.setCurrent(form);
    }

    private void playMusic() {
        try {
            stopMusic();

            player = Manager.createPlayer(
                musicPath.getString().trim());

            player.realize();
            player.start();

            showText("Воспроизведение запущено");

        } catch (Exception e) {
            showText("Не удалось открыть MP3:\n"
                + e.toString());
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
    }

    private void showText(String text) {
        result = new TextBox(
            "VK4ME NEXT", text, 12000,
            TextField.ANY);

        backCommand = new Command(
            "Назад", Command.BACK, 1);

        result.addCommand(backCommand);
        result.setCommandListener(this);

        display.setCurrent(result);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        stopMusic();
    }
}
