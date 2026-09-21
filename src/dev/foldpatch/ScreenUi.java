package dev.foldpatch;

import android.app.Activity;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.window.OnBackInvokedDispatcher;

/** Insets belong to controls, never to the physical calibration coordinate space. */
final class ScreenUi {
    private ScreenUi() {}

    static void prepare(Activity activity, Runnable back, boolean immersive) {
        activity.getWindow().setDecorFitsSystemWindows(false);
        activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, back::run);
        if (immersive) {
            // The range activity calls this before setContentView. Create the decor
            // before PhoneWindow looks up its insets controller.
            activity.getWindow().getDecorView();
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.systemBars());
            }
        }
    }

    static void fitControls(View panel) {
        panel.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            // Keep dispatching: the IME and other views still need the original insets.
            return insets;
        });
        panel.requestApplyInsets();
    }
}
