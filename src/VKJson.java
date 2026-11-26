public class VKJson {

    private String data;
    private int pos;

    public VKJson(String json) {
        data = json;
        pos = 0;
    }

    public Object parse() {
        skip();
        return value();
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

    private java.util.Hashtable object() {
        java.util.Hashtable map =
            new java.util.Hashtable();

        pos++;
        skip();

        while (pos < data.length()
                && data.charAt(pos) != '}') {

            String key = string();

            skip();
            pos++;
            skip();

            Object val = value();

            if (val != null) {
                map.put(key, val);
            }

            skip();

            if (pos < data.length()
                    && data.charAt(pos) == ',') {
                pos++;
                skip();
            } else {
                break;
            }
        }

        if (pos < data.length()) pos++;

        return map;
    }

    private java.util.Vector array() {
        java.util.Vector list =
            new java.util.Vector();

        pos++;
        skip();

        while (pos < data.length()
                && data.charAt(pos) != ']') {

            list.addElement(value());
            skip();

            if (pos < data.length()
                    && data.charAt(pos) == ',') {
                pos++;
                skip();
            } else {
                break;
            }
        }

        if (pos < data.length()) pos++;

        return list;
    }

    private String string() {
        StringBuffer result = new StringBuffer();

        if (data.charAt(pos) == '"') pos++;

        while (pos < data.length()) {
            char c = data.charAt(pos++);

            if (c == '"') break;

            if (c == '\\' && pos < data.length()) {
                char n = data.charAt(pos++);

                if (n == 'n') result.append('\n');
                else if (n == 'r') result.append('\r');
                else if (n == 't') result.append('\t');
                else if (n == 'u' && pos + 4 <= data.length()) {
                    try {
                        result.append((char)
                            Integer.parseInt(
                                data.substring(pos, pos + 4),
                                16));
                        pos += 4;
                    } catch (Exception e) {
                        result.append('?');
                    }
                } else {
                    result.append(n);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private Number number() {
        int start = pos;

        while (pos < data.length()) {
            char c = data.charAt(pos);

            if ((c >= '0' && c <= '9')
                    || c == '-'
                    || c == '+'
                    || c == '.'
                    || c == 'e'
                    || c == 'E') {
                pos++;
            } else {
                break;
            }
        }

        String n = data.substring(start, pos);

        try {
            return new Double(n);
        } catch (Exception e) {
            return new Integer(0);
        }
    }

    private void skip() {
        while (pos < data.length()) {
            char c = data.charAt(pos);

            if (c == ' ' || c == '\n'
                    || c == '\r' || c == '\t') {
                pos++;
            } else {
                break;
            }
        }
    }
}
