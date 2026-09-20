package dev.reachpad.experiment;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Looper;
import android.view.SurfaceControl;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Bounded shell-only reflow experiment. Restores the forced display size on exit. */
public final class ReflowMirrorProbe {
    private static final List<SurfaceControl> surfaces = new ArrayList<>();
    private static SurfaceControl root;
    private static boolean cleaned;
    private static Object wm;
    private static boolean resized;

    private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
        return owner.getClass().getMethod(name, types).invoke(owner, args);
    }
    private static int number(Object info, String name) throws Exception {
        return info.getClass().getField(name).getInt(info);
    }
    private static SurfaceControl layer(String name, String type, SurfaceControl parent) throws Exception {
        SurfaceControl.Builder b = new SurfaceControl.Builder().setName(name).setHidden(true);
        call(b, type, new Class<?>[0]);
        if (parent != null) b.setParent(parent);
        SurfaceControl result = b.build();
        surfaces.add(result);
        return result;
    }
    private static SurfaceControl mirror(Object wm) throws Exception {
        SurfaceControl result = SurfaceControl.class.getConstructor().newInstance();
        surfaces.add(result);
        boolean ok = (Boolean) call(wm, "mirrorDisplay", new Class<?>[]{int.class, SurfaceControl.class}, 0, result);
        if (!ok || !result.isValid()) throw new IllegalStateException("Default display cannot be mirrored");
        return result;
    }
    private static void visible(SurfaceControl.Transaction t, SurfaceControl s) throws Exception {
        call(t, "show", new Class<?>[]{SurfaceControl.class}, s);
    }
    private static synchronized void cleanup() {
        if (cleaned) return;
        cleaned = true;
        if (root != null) {
            try (SurfaceControl.Transaction t = new SurfaceControl.Transaction()) {
                call(t, "remove", new Class<?>[]{SurfaceControl.class}, root);
                t.apply();
            } catch (Exception e) { System.err.println("REMOVE: " + e); }
        }
        for (SurfaceControl s : surfaces) try { s.release(); } catch (Exception ignored) {}
        if (resized) try { call(wm,"clearForcedDisplaySize",new Class<?>[]{int.class},0); } catch(Exception e) {System.err.println("RESTORE SIZE: "+e);}
        System.out.println("NATIVE_REFLOW_RESTORED");
    }
    public static void main(String[] args) throws Exception {
        int seconds = args.length == 0 ? 12 : Integer.parseInt(args[0]);
        if (seconds < 1 || seconds > 60) throw new IllegalArgumentException("Timeout must be 1..60 seconds");
        Looper.prepareMainLooper();
        Runtime.getRuntime().addShutdownHook(new Thread(ReflowMirrorProbe::cleanup));
        // Even if setup or Binder hangs, all process-owned presentation layers disappear on exit.
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep((seconds + 5L) * 1000); } catch (InterruptedException ignored) {}
            cleanup(); Runtime.getRuntime().halt(124);
        }, "native-mirror-watchdog");
        watchdog.setDaemon(true); watchdog.start();
        try {
            Object dm = Class.forName("android.hardware.display.DisplayManagerGlobal").getMethod("getInstance").invoke(null);
            Object info = call(dm, "getDisplayInfo", new Class<?>[]{int.class}, 0);
            int w = number(info, "logicalWidth"), h = number(info, "logicalHeight");
            int stack = number(info, "layerStack");
            if (w != 1768 || h != 2208 || number(info, "rotation") != 0 || number(info, "state") != 2)
                throw new IllegalStateException("Requires awake portrait inner display; got " + w + "x" + h);
            IBinder binder = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "window");
            wm = Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, binder);
            resized=true;
            call(wm,"setForcedDisplaySize",new Class<?>[]{int.class,int.class,int.class},0,1592,2208);
            Thread.sleep(1200);
            root = layer("ReachPad Reflow Test (auto restore)", "setContainerLayer", null);
            SurfaceControl black = layer("ReachPad Native Gap", "setColorLayer", root);
            SurfaceControl left = mirror(wm), right = mirror(wm);
            int cut = 796, leftEdge = 796, rightEdge = 972;
            w=1592;
            float leftScale = 1f;
            float rightScale = 1f;
            try (SurfaceControl.Transaction t = new SurfaceControl.Transaction()) {
                call(t, "setLayerStack", new Class<?>[]{SurfaceControl.class, int.class}, root, stack);
                t.setLayer(root, 2000000000);
                t.setPosition(root,-88,0);
                t.setLayer(black, 0).setCrop(black, new Rect(0, 0, 1768, h));
                call(t, "setColor", new Class<?>[]{SurfaceControl.class, float[].class}, black, new float[]{0, 0, 0});
                t.reparent(left, root).setLayer(left, 1).setCrop(left, new Rect(0, 0, cut, h));
                t.reparent(right, root).setLayer(right, 2).setCrop(right, new Rect(cut, 0, w, h));
                call(t, "setMatrix", new Class<?>[]{SurfaceControl.class, float.class, float.class, float.class, float.class}, left, leftScale, 0f, 0f, 1f);
                call(t, "setMatrix", new Class<?>[]{SurfaceControl.class, float.class, float.class, float.class, float.class}, right, rightScale, 0f, 0f, 1f);
                t.setPosition(right, rightEdge - cut * rightScale, 0);
                visible(t, root); visible(t, black); visible(t, left); visible(t, right);
                t.apply();
            }
            System.out.println("NATIVE_REFLOW_READY display=0 size=" + w + "x" + h + " edges=" + leftEdge + "," + rightEdge + " timeout=" + seconds);
            long end = android.os.SystemClock.uptimeMillis() + seconds * 1000L;
            while (android.os.SystemClock.uptimeMillis() < end) {
                Thread.sleep(100);
                Object now = call(dm, "getDisplayInfo", new Class<?>[]{int.class}, 0);
                if (number(now, "logicalWidth") != w || number(now, "logicalHeight") != h || number(now, "rotation") != 0 || number(now, "state") != 2) break;
            }
        } finally { cleanup(); }
        System.exit(0);
    }
}
