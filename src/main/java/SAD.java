import com.developingstorm.games.sad.fx.SaDFxApplication;
import javafx.application.Application;

/**
 * Main entry point for Search and Destroy game.
 * Launches the JavaFX UI.
 *
 * Note: The Swing UI (SaDFrame) is deprecated and kept as reference code only.
 * To run the old Swing UI, call SaDFrame.main(args) directly.
 */
public class SAD {

    public static void main(String[] args) {
        // Launch JavaFX application
        Application.launch(SaDFxApplication.class, args);
    }
}
