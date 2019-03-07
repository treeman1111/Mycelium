package sample;

public class Helpers {
    public static String get_operation_description(int r, int g, int b) {
        switch (r % 16) {
            case 0:
                return "NO-OP";
            case 1:
                switch (b % 4) {
                    case 0:
                        return "Direction: EAST";
                    case 1:
                        return "Direction: SOUTH";
                    case 2:
                        return "Direction: WEST";
                    case 3:
                        return "Direction: NORTH";
                }
                break;
            case 2:
                return "Jump: " + (256 * g + b);
            case 3:
                return "Conditional jump: " + (256 * g + b);
            case 4:
                switch (b % 4) {
                    case 0:
                        return "Move: stack location";
                    case 1:
                        return "Move: function call";
                    case 2:
                        return "Move: return";
                }
                break;
            case 5:
                switch (b % 8) {
                    case 0:
                        return "Memory: *MP = pop(stack)";
                    case 1:
                        return "Memory: stack.add(*MP)";
                    case 2:
                        return "Memory: MP = pop(stack)";
                    case 3:
                        return "Memory: stack.add(MP)";
                    case 4:
                        return "Memory: MP++";
                    case 5:
                        return "Memory: MP--";
                }
                break;
            case 8:
                return "Constant: " + (256 * g + b) + " (" + ((char) (256 * g + b)) + ")";
            case 9:
                return "Constants: " + g + " (" + (char) g + ") and " + b + " (" + (char) b + ")";
            case 10:
                switch (b % 4) {
                    case 0:
                        return "Stack: pop";
                    case 1:
                        return "Stack: duplicate top";
                    case 2:
                        return "Stack: swap two";
                    case 3:
                        return "Stack: duplicate two";
                }
                break;
            case 11:
                switch (b % 32) {
                    case 0:
                        return "Math: +";
                    case 1:
                        return "Math: -";
                    case 2:
                        return "Math: *";
                    case 3:
                        return "Math: /";
                    case 4:
                        return "Math: %";
                    case 5:
                        return "Math: |";
                    case 6:
                        return "Math: &";
                    case 7:
                        return "Math: ^";
                    case 8:
                        return "Math: ~";
                    case 9:
                        return "Math: >>";
                    case 10:
                        return "Math: >>>";
                    case 11:
                        return "Math: <<";
                    case 12:
                        return "Math: ==";
                    case 13:
                        return "Math: !=";
                    case 14:
                        return "Math: >";
                    case 15:
                        return "Math: <";
                    case 16:
                        return "Math: >=";
                    case 17:
                        return "Math: <=";
                    case 18:
                        return "Math: &&";
                    case 19:
                        return "Math: ||";
                    case 20:
                        return "Math: !";
                }
                break;
            case 12:
                switch (b % 4) {
                    case 0:
                        return "I/O: print char";
                    case 1:
                        return "I/O: read char";
                    case 2:
                        return "I/O: print int";
                }
                break;
        }

        return "Un-implemented";
    }
}
