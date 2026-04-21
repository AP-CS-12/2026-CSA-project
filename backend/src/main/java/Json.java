import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Object parse(String json) {
        return new Parser(json).parseValue();
    }

    public static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON input.");
            }

            char current = input.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected token at position " + index + ".");
                }
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return list;
            }

            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();

            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current != '\\') {
                    builder.append(current);
                    continue;
                }
                if (index >= input.length()) {
                    throw new IllegalArgumentException("Invalid escape sequence.");
                }
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> builder.append(escaped);
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicode());
                    default -> throw new IllegalArgumentException("Unsupported escape sequence: \\" + escaped);
                }
            }

            throw new IllegalArgumentException("Unterminated string literal.");
        }

        private char parseUnicode() {
            if (index + 4 > input.length()) {
                throw new IllegalArgumentException("Invalid unicode escape sequence.");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                throw new IllegalArgumentException("Unexpected token at position " + index + ".");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (input.charAt(index) == '-') {
                index++;
            }
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (index < input.length() && input.charAt(index) == '.') {
                index++;
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (index < input.length() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }

            String value = input.substring(start, index);
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean peek(char expected) {
            return index < input.length() && input.charAt(index) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + index + ".");
            }
            index++;
        }
    }
}
