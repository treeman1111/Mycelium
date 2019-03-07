package sample;

import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MyceliumInterpreter {
    final List<Integer> stack;
    final List<Integer> memory;
    final List<Integer> gosubx;
    final List<Integer> gosuby;
    final List<Integer> gosubd;
    final Color[][] image;
    final Consumer<String> output;
    final int img_width;
    final int img_height;
    int ip_x;
    int ip_y;
    int ip_dir;
    int mem_ptr;
    boolean is_finished;

    public MyceliumInterpreter(Color[][] image, Consumer<String> output) {
        this.stack = new ArrayList<>();
        this.memory = new ArrayList<>();
        this.memory.add(1);
        this.gosubx = new ArrayList<>();
        this.gosuby = new ArrayList<>();
        this.gosubd = new ArrayList<>();
        this.image = image;
        this.output = output;
        this.img_width = this.image.length; // TODO
        this.img_height = this.image[0].length; // TODO: check these
        this.ip_x = 0;
        this.ip_y = 0;
        this.ip_dir = 0;
        this.mem_ptr = 0;
        this.is_finished = false;
    }

    private static int pop(List<Integer> list) {
        if (list.size() == 0) {
            return 0;
        } else {
            return list.remove(list.size() - 1);
        }
    }

    public boolean next() {
        if (is_finished) return false;

        final int r = (int) (image[ip_x][ip_y].getRed() * 255);
        final int g = (int) (image[ip_x][ip_y].getGreen() * 255);
        final int b = (int) (image[ip_x][ip_y].getBlue() * 255);

        switch (r % 16) {
            case 0:
                break; // no-op
            case 1:
                change_dir(b);
                break;
            case 2:
                jump(g, b);
                break;
            case 3:
                conditional_jump(g, b);
                break;
            case 4:
                handle_translocate(b);
                break;
            case 5:
                handle_memory(b);
                break;
            case 6:
                // TODO
                break;
            case 7:
                // TODO
                break;
            case 8:
                push_constant(256 * g + b);
                break;
            case 9:
                push_constant(g);
                push_constant(b);
                break;
            case 10:
                handle_stack(b);
                break;
            case 11:
                handle_math(b);
                break;
            case 12:
                handle_io(b);
                break;
            default:
                break;
        }

        move_instruction_pointer(1);
        check_done();

        return true;
    }

    private void change_dir(int b) {
        this.ip_dir = b % 4;
    }

    private void jump(int g, int b) {
        move_instruction_pointer(256 * g + b);
    }

    private void conditional_jump(int g, int b) {
        if (pop(this.stack) == 0) {
            jump(g, b);
        }
    }

    private void handle_translocate(int b) {
        switch (b % 4) {
            case 0: // move to different location stored on stack
            {
                final int ip_y = pop(this.stack);
                final int ip_x = pop(this.stack);
                translocate_ip(ip_x, ip_y);
            }
            break;
            case 1: // make a functional call
            {
                gosubx.add(ip_x);
                gosuby.add(ip_y);
                gosubd.add(ip_dir);
                final int ip_y = pop(this.stack);
                final int ip_x = pop(this.stack);
                translocate_ip(ip_x, ip_y);
            }
            break;
            case 2: // return from a function call
            {
                ip_x = pop(this.gosubx);
                ip_y = pop(this.gosuby);
                ip_dir = pop(this.gosubd);
            }
            break;
            default:
                break;
        }
    }

    private void handle_memory(int b) {
        switch (b % 8) {
            case 0: // store a value in memory popped from the stack
                memory.set(mem_ptr, pop(this.stack));
                break;
            case 1: // store a value on the stack that was in memory
                stack.add(memory.get(mem_ptr));
                break;
            case 2: // set the mem_ptr to a value popped from the stack
                mem_ptr = pop(this.stack);
                if (mem_ptr < 0) mem_ptr = 0;
                if (mem_ptr >= memory.size()) resize_memory(mem_ptr);
                break;
            case 3: // store the mem_ptr on the stack
                stack.add(mem_ptr);
                break;
            case 4: // increment the memory pointer
                mem_ptr++;
                if (mem_ptr >= memory.size()) resize_memory(mem_ptr);
                break;
            case 5: // decrement the memory pointer
                if (mem_ptr > 0) {
                    mem_ptr--;
                }
                break;
            default:
                break;
        }
    }

    private void push_constant(int i) {
        this.stack.add(i);
    }

    private void handle_stack(int b) {
        switch (b % 4) {
            case 0: // dump the top thing on the stack
                pop(this.stack);
                break;
            case 1: // duplicate the top value on the stack
                final int stack_value = pop(this.stack);
                stack.add(stack_value);
                stack.add(stack_value);
                break;
            case 2: // swap the top two values on the stack
                final int old_top = pop(this.stack);
                final int old_bot = pop(this.stack);
                stack.add(old_top);
                stack.add(old_bot);
                break;
            case 3: // duplicate the top two stack values
                final int x = pop(this.stack);
                final int y = pop(this.stack);
                stack.add(x);
                stack.add(y);
                stack.add(x);
                stack.add(y);
                break;
            default:
                break;
        }
    }

    private void handle_math(int b) {
        final int x = pop(this.stack);
        final int y = pop(this.stack);

        switch (b % 32) {
            case 0:
                stack.add(x + y);
                break;
            case 1:
                stack.add(x - y);
                break;
            case 2:
                stack.add(x * y);
                break;
            case 3:
                stack.add(y == 0 ? 0 : x / y);
                break;
            case 4:
                stack.add(y == 0 ? 0 : x % y);
                break;
            case 5:
                stack.add(x | y);
                break;
            case 6:
                stack.add(x & y);
                break;
            case 7:
                stack.add(x ^ y);
                break;
            case 8:
                stack.add(~x);
                break;
            case 9:
                stack.add(x >> y);
                break;
            case 10:
                stack.add(x >>> y);
                break;
            case 11:
                stack.add(x << y);
                break;
            case 12:
                stack.add(boolean_as_int(x == y));
                break;
            case 13:
                stack.add(boolean_as_int(x != y));
                break;
            case 14:
                stack.add(boolean_as_int(x > y));
                break;
            case 15:
                stack.add(boolean_as_int(x < y));
                break;
            case 16:
                stack.add(boolean_as_int(x >= y));
                break;
            case 17:
                stack.add(boolean_as_int(x <= y));
                break;
            case 18:
                stack.add(boolean_as_int(int_as_bool(x) && int_as_bool(y)));
                break;
            case 19:
                stack.add(boolean_as_int(int_as_bool(x) || int_as_bool(y)));
                break;
            case 20:
                stack.add(boolean_as_int(!int_as_bool(x)));
                break;
            default:
                break;
        }
    }

    private void handle_io(int b) {
        switch (b % 4) {
            case 0:
                this.output.accept("" + (char) pop(this.stack));
                break;
            case 1:
                // TODO
                break;
            case 2:
                this.output.accept("" + pop(this.stack));
                break;
            default:
                break;
        }
    }

    private void translocate_ip(int x, int y) {
        this.ip_x = x;
        this.ip_y = y;
    }

    private void move_instruction_pointer(int steps) {
        switch (ip_dir) {
            case 0:
                this.ip_x += steps;
                break;
            case 1:
                this.ip_y += steps;
                break;
            case 2:
                this.ip_x -= steps;
                break;
            case 3:
                this.ip_y -= steps;
                break;
            default:
                break;
        }
    }

    private void check_done() {
        if (ip_x < 0 || ip_x >= img_width || ip_y < 0 || ip_y >= img_height) {
            is_finished = true;
        }
    }

    private void resize_memory(int size) {
        while (memory.size() <= size) {
            memory.add(0);
        }
    }

    /**
     * Converts an integer to a boolean for use in math operations.
     * @param i The integer to be converted to a boolean.
     * @return The boolean value which the integer was converted to.
     */
    private static boolean int_as_bool(int i) {
        return i != 0;
    }

    private static int boolean_as_int(boolean b) {
        return !b ? 0 : 1;
    }
}
