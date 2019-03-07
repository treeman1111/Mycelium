package sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class Controller {
    @FXML
    private Canvas canvas;
    @FXML
    private VBox right_pane;
    @FXML
    private Button compile_btn;
    @FXML
    private Button fore_btn;
    @FXML
    private Button clear_btn;
    @FXML
    private Button autoplay_btn;
    @FXML
    private Button info_mode_btn;
    @FXML
    private Button add_mode_btn;
    @FXML
    private HBox stack_box;
    @FXML
    private HBox memory_box;
    @FXML
    private Label output;

    private Color[][] mycelium_program;
    private int cell_render_size;
    private int program_width;
    private int program_height;
    private MyceliumInterpreter interpreter;
    private boolean is_compiled;
    private boolean in_info_mode;
    private int ip_x;
    private int ip_y;

    public void initialize() {
        this.canvas.setWidth(600);
        this.canvas.setHeight(500);

        this.cell_render_size = 20;
        this.program_width = 600 / this.cell_render_size;
        this.program_height = 500 / this.cell_render_size;
        this.mycelium_program = new Color[this.program_width][this.program_height];
        this.interpreter = null; // :0
        this.is_compiled = false;
        this.in_info_mode = true;
        this.ip_x = 0;
        this.ip_y = 0;

        this.initialize_mycelium_program();
        this.setup_canvas_click_handler();
        this.setup_button_click_handlers();
        this.draw_canvas();
        this.handle_state_changed(); // make it impossible to run program without compiling first
        right_pane.getChildren().add(new Label("Click somewhere to get info"));
    }

    private void initialize_mycelium_program() {
        for (int x = 0; x < program_width; x++) {
            for (int y = 0; y < program_height; y++) {
                mycelium_program[x][y] = Color.rgb(255, 255, 255);
            }
        }
    }

    private void draw_canvas() {
        final GraphicsContext gfx = this.canvas.getGraphicsContext2D();

        // clear the drawing from last time
        gfx.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // draw the cells
        for (int x = 0; x < program_width; x++) {
            for (int y = 0; y < program_height; y++) {
                gfx.setFill(mycelium_program[x][y]);
                gfx.fillRect(x * cell_render_size, y * cell_render_size, cell_render_size, cell_render_size);
            }
        }

        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();

        // draw the columns
        gfx.setStroke(Color.DARKGRAY);
        gfx.setLineWidth(1);
        for (int x = cell_render_size; x < width; x += cell_render_size) {
            gfx.strokeLine(x, 0, x, height);
        }

        // draw the rows
        for (int y = cell_render_size; y < height; y += cell_render_size) {
            gfx.strokeLine(0, y, width, y);
        }

        // draw the location of the instruction pointer
        if (ip_x >= 0 && ip_x < program_width && ip_y >= 0 && ip_y < program_height) {
            gfx.setStroke(Color.BLACK);
            gfx.setLineWidth(3);
            gfx.fillRect(ip_x * cell_render_size, ip_y * cell_render_size, cell_render_size, cell_render_size);
        }
    }

    private void setup_canvas_click_handler() {
        canvas.setOnMouseClicked(me -> {
            final int x_tile = (int) me.getX() / cell_render_size;
            final int y_tile = (int) me.getY() / cell_render_size;

            if (!(x_tile < 0 || y_tile < 0 || x_tile > program_width || y_tile > program_height)) {
                if (in_info_mode) {
                    handle_info_mode(x_tile, y_tile);
                } else {
                    handle_add_mode(x_tile, y_tile);
                    handle_state_changed();
                }
            }

            draw_canvas();
        });
    }

    private void setup_button_click_handlers() {
        compile_btn.setOnMouseClicked(me -> {
            handle_compile();
            draw_canvas();
            memory_box.getChildren().clear();
            stack_box.getChildren().clear();
            clear_output();
        });

        fore_btn.setOnMouseClicked(me -> {
            if (this.is_compiled) {
                final boolean not_done = interpreter.next();
                if (!not_done) fore_btn.setDisable(true);

                ip_x = this.interpreter.ip_x;
                ip_y = this.interpreter.ip_y;
                draw_stack();
                draw_memory();
                draw_canvas();
            }
        });

        clear_btn.setOnMouseClicked(me -> {
            mycelium_program = new Color[this.program_width][this.program_height];
            ip_x = 0;
            ip_y = 0;
            initialize_mycelium_program();
            handle_state_changed();
            draw_canvas();
            memory_box.getChildren().clear();
            stack_box.getChildren().clear();
        });

        this.autoplay_btn.setOnMouseClicked(me -> {
            handle_state_changed();
            handle_compile();

            // calculate how many cycles it will take to run this program
            int times_run = 0;
            boolean has_next = true;
            while (has_next) {
                times_run++;
                has_next = interpreter.next();
            }

            handle_state_changed();
            handle_compile();

            final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.1), e -> {
                interpreter.next();
                this.ip_x = interpreter.ip_x;
                this.ip_y = interpreter.ip_y;
                this.draw_stack();
                this.draw_memory();
                this.draw_canvas();
            }));
            timeline.setCycleCount(times_run - 1);
            timeline.play();
        });

        this.info_mode_btn.setOnMouseClicked(me -> {
            in_info_mode = true;
            right_pane.getChildren().clear();
            right_pane.getChildren().add(new Label("Click somewhere to get info"));
        });

        this.add_mode_btn.setOnMouseClicked(me -> {
            in_info_mode = false;
            right_pane.getChildren().clear();
            right_pane.getChildren().add(new Label("Click a tile to modify its contents"));
        });
    }

    private void handle_add_mode(int x_tile, int y_tile) {
        final Label info = new Label("Select a color for the tile:");
        final HBox r_slider_box = new HBox();
        final Label r_slider_label = new Label("R");
        final Slider r_slider = new Slider();
        final HBox g_slider_box = new HBox();
        final Label g_slider_label = new Label("G");
        final Slider g_slider = new Slider();
        final HBox b_slider_box = new HBox();
        final Label b_slider_label = new Label("B");
        final Slider b_slider = new Slider();
        final HBox status_box = new HBox();
        final Label status = new Label("NO-OP");
        final Canvas color_patch = new Canvas(15, 15);
        final GraphicsContext patch_context = color_patch.getGraphicsContext2D();
        final Button add_button = new Button("Done");

        r_slider.setMin(0);
        r_slider.setMax(255);
        r_slider.setValue(0);
        r_slider.setMajorTickUnit(1);
        r_slider.setBlockIncrement(1);
        r_slider.setSnapToTicks(true);

        g_slider.setMin(0);
        g_slider.setMax(255);
        g_slider.setValue(0);
        g_slider.setMajorTickUnit(1);
        g_slider.setBlockIncrement(1);
        g_slider.setSnapToTicks(true);

        b_slider.setMin(0);
        b_slider.setMax(255);
        b_slider.setValue(0);
        b_slider.setMajorTickUnit(1);
        b_slider.setBlockIncrement(1);
        b_slider.setSnapToTicks(true);

        status.setWrapText(true);

        r_slider_box.getChildren().addAll(r_slider, r_slider_label);
        g_slider_box.getChildren().addAll(g_slider, g_slider_label);
        b_slider_box.getChildren().addAll(b_slider, b_slider_label);

        status_box.getChildren().addAll(color_patch, status);

        final Runnable when_changed = () -> {
            patch_context.setFill(Color.rgb((int) r_slider.getValue(), (int) g_slider.getValue(), (int) b_slider.getValue()));
            patch_context.fillRect(0, 0, 15, 15);
            status.setText(Helpers.get_operation_description(
                    (int) r_slider.getValue(),
                    (int) g_slider.getValue(),
                    (int) b_slider.getValue())
            );
        };

        r_slider.valueProperty().addListener(l -> when_changed.run());
        g_slider.valueProperty().addListener(l -> when_changed.run());
        b_slider.valueProperty().addListener(l -> when_changed.run());
        add_button.setOnMouseClicked(me -> {
            this.mycelium_program[x_tile][y_tile] = Color.rgb(
                    (int) r_slider.getValue(),
                    (int) g_slider.getValue(),
                    (int) b_slider.getValue()
            );
            this.draw_canvas();
            this.handle_state_changed();
        });

        right_pane.getChildren().clear();
        right_pane.getChildren().addAll(info, r_slider_box, g_slider_box, b_slider_box, status_box, add_button);
    }

    private void handle_info_mode(int x_tile, int y_tile) {
        final Color clicked = this.mycelium_program[x_tile][y_tile];
        final int r = (int) (clicked.getRed() * 255);
        final int g = (int) (clicked.getGreen() * 255);
        final int b = (int) (clicked.getBlue() * 255);

        final Label location = new Label(String.format("Tile@(%d, %d)", x_tile, y_tile));
        final Label color = new Label("" + clicked);
        final Label operation_info = new Label(Helpers.get_operation_description(r, g, b));

        right_pane.getChildren().clear();
        right_pane.getChildren().addAll(location, color, operation_info);
    }

    private void draw_stack() {
        stack_box.getChildren().clear();
        stack_box.getChildren().addAll(this.interpreter.stack.parallelStream()
                .map(i -> {
                    final Label l = new Label("" + i);
                    l.setStyle("-fx-border-color: #000000; -fx-border-width: 0 1 0 1;");
                    return l;
                })
                .collect(Collectors.toList())
        );
    }

    private void draw_memory() {
        memory_box.getChildren().clear();
        memory_box.getChildren().addAll(this.interpreter.memory.parallelStream()
                .map(i -> {
                    final Label l = new Label("" + i);
                    l.setStyle("-fx-border-color: #000000; -fx-border-width: 0 1 0 1;");
                    return l;
                })
                .collect(Collectors.toList())
        );
    }

    private void write_output(String output) {
        this.output.setText(this.output.getText() + output);
    }

    private void clear_output() {
        this.output.setText("");
    }

    private void handle_state_changed() {
        this.is_compiled = false;
        this.interpreter = null;
        fore_btn.setDisable(true);
        this.stack_box.getChildren().clear();
        this.memory_box.getChildren().clear();
        clear_output();
    }

    private void handle_compile() {
        this.is_compiled = true;
        this.interpreter = new MyceliumInterpreter(mycelium_program, this::write_output);
        this.ip_x = 0;
        this.ip_y = 0;
        fore_btn.setDisable(false);
    }

    public void handle_load_file() {
        final FileChooser png_load_dialog = new FileChooser();
        png_load_dialog.setTitle("Load Mycelium Program");
        png_load_dialog.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG files", "*.png")
        );

        final File load_target = png_load_dialog.showOpenDialog(null);
        if (load_target == null) return;
        System.out.println(load_target);

        BufferedImage input_image;
        try {
            input_image = ImageIO.read(load_target);
        } catch (IOException ioe) {
            System.out.println("failed to open file");
            return;
        }

        if (input_image.getWidth() != program_width || input_image.getHeight() != program_height) {
            System.out.println("sizing of image is wrong.");
        } else {
            final Color[][] input_program = new Color[program_width][program_height];

            for (int x = 0; x < program_width; x++) {
                for (int y = 0; y < program_height; y++) {
                    final int rgb = input_image.getRGB(x, y);
                    final int b = rgb & 0xFF;
                    final int g = (rgb >> 8) & 0xFF;
                    final int r = (rgb >> 16) & 0xFF;
                    input_program[x][y] = Color.rgb(r, g, b);
                }
            }

            mycelium_program = input_program;
            handle_state_changed();
            draw_canvas();
        }
    }

    public void handle_save_file() {
        final FileChooser png_save_dialog = new FileChooser();
        png_save_dialog.setTitle("Save Mycelium Program");
        png_save_dialog.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG files", "*.png")
        );

        final File save_target = png_save_dialog.showSaveDialog(null);
        if (save_target == null) return;

        final BufferedImage output_image = new BufferedImage(program_width, program_height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < program_width; x++) {
            for (int y = 0; y < program_height; y++) {
                final Color color = mycelium_program[x][y];
                final int r = (int) (color.getRed() * 255);
                final int g = (int) (color.getGreen() * 255);
                final int b = (int) (color.getBlue() * 255);

                int result = r;
                result = (result << 8) + g;
                result = (result << 8) + b;

                output_image.setRGB(x, y, result);
            }
        }

        try {
            ImageIO.write(output_image, "png", save_target);
        } catch (IOException ioe) {
            System.out.println("there was an error saving the file");
        }
    }
}
