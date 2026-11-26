
import java.util.Hashtable;
import java.util.Vector;

public class VKJson {

    private String data;
    private int pos;

    public static Object parse(String json) {
        VKJson parser = new VKJson();
        parser.data = json;
        parser.pos = 0;
        return parser.value();
    }

    private Object value() {
        skip();
        if (pos >= data.length()) return null;

        char c = data.charAt(pos);

        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();

        if (c == 't') {
            pos += 4;
            return Boolean.TRUE;
        }

        if (c == 'f') {
            pos += 5;
            return Boolean.FALSE;
        }

        if (c == 'n') {
            pos += 4;
            return null;
        }

        return number();
    }

    private Hashtable object() {
        Hashtable result = new Hashtable();
        pos++;

        while (true) {
            skip();

            if (pos >= data.length()) break;
            if (data.charAt(pos) == '}') {
                pos++;
                break;
            }

            String key = string();

            skip();
            if (pos < data.length() && data.charAt(pos) == ':') {
                pos++;
            }

            Object val = value();
            if (val != null) {
                result.put(key, val);
            }

            skip();
            if (pos < data.length() && data.charAt(pos) == ',') {
                pos++;
            }
        }

        return result;
    }

    private Vector array() {
        Vector result = new Vector();
        pos++;

        while (true) {
            skip();

            if (pos >= data.length()) break;
            if (data.charAt(pos) == ']') {
                pos++;
                break;
            }

            result.addElement(value());

            skip();
            if (pos < data.length() && data.charAt(pos) == ',') {
                pos++;
            }
        }

        return result;
    }

    private String string() {
        StringBuffer result = new StringBuffer();
        pos++;

        while (pos < data.length()) {
            char c = data.charAt(pos++);

            if (c == '"') break;

            if (c == '\\' && pos < data.length()) {
                char e = data.charAt(pos++);

                if (e == 'n') result.append('\n');
                else if (e == 'r') result.append('\r');
                else if (e == 't') result.append('\t');
                else if (e == 'b') result.append('\b');
                else if (e == 'f') result.append('\f');
                else if (e == 'u' && pos + 4 <= data.length()) {
                    try {
                        int code = Integer.parseInt(
                            data.substring(pos, pos + 4), 16);
                        result.append((char) code);
                        pos += 4;
                    } catch (Exception ex) {
                        result.append('?');
                    }
                } else {
                    result.append(e);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private Double number() {
        int start = pos;

        while (pos < data.length()) {
            char c = data.charAt(pos);

            if ((c >= '0' && c <= '9') ||
                c == '-' || c == '+' ||
                c == '.' || c == 'e' || c == 'E') {
                pos++;
            } else {
                break;
            }
        }

        try {
            return new Double(data.substring(start, pos));
        } catch (Exception ex) {
            return new Double(0);
        }
    }

    private void skip() {
        while (pos < data.length()) {
            char c = data.charAt(pos);

            if (c == ' ' || c == '\n' ||
                c == '\r' || c == '\t') {
                pos++;
            } else {
                break;
            }
        }
    }

    public static Hashtable asObject(Object value) {
        if (value instanceof Hashtable) {
            return (Hashtable) value;
        }
        return new Hashtable();
    }

    public static Vector asArray(Object value) {
        if (value instanceof Vector) {
            return (Vector) value;
        }
        return new Vector();
    }

    public static String getString(Hashtable object, String key) {
        Object value = object.get(key);
        return value == null ? "" : value.toString();
    }

    public static Hashtable getObject(Hashtable object, String key) {
        return asObject(object.get(key));
    }

    public static Vector getArray(Hashtable object, String key) {
        return asArray(object.get(key));
    }
}
