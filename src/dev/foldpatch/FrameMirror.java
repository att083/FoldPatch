package dev.foldpatch;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.view.*;

/** Composites existing display content into a buffer, never into input-bearing window clones.
 * No app is launched/moved to this private output display; no CPU readback or encoding. */
final class FrameMirror {
    private final java.util.ArrayList<SurfaceControl> tails=new java.util.ArrayList<>();
    private VirtualDisplay display;
    private Surface surface;
    private Object queue;
    private SurfaceControl buffer,source,outputRoot;
    private static Object call(Object o,String n,Class<?>[] t,Object... a)throws Exception{return o.getClass().getMethod(n,t).invoke(o,a);}
    SurfaceControl start(Context context,Object wm,SystemUiFit systemUi,int width,int height,int usable)throws Exception {
        try {
            SurfaceControl.Builder builder=new SurfaceControl.Builder().setName("FoldPatch frame buffer");
            call(builder,"setBLASTLayer",new Class<?>[0]);buffer=builder.build();
            Class<?> q=Class.forName("android.graphics.BLASTBufferQueue");
            // Android 16 removed the five-argument convenience constructor. Its
            // underlying create + update sequence is shared with Android 14/15.
            queue=q.getConstructor(String.class,boolean.class).newInstance("FoldPatch frames",true);
            call(queue,"update",new Class<?>[]{SurfaceControl.class,int.class,int.class,int.class},
                buffer,width,height,PixelFormat.RGBA_8888);
            surface=(Surface)call(queue,"createSurface",new Class<?>[0]);
            DisplayManager dm=context.getSystemService(DisplayManager.class);
            android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();dm.getDisplay(0).getRealMetrics(metrics);
            VirtualDisplayConfig config=new VirtualDisplayConfig.Builder("FoldPatch frame output",width,height,metrics.densityDpi)
                .setSurface(surface).setFlags(DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
                .setRequestedRefreshRate(dm.getDisplay(0).getRefreshRate()).build();
            display=dm.createVirtualDisplay(config);
            if(display==null)throw new IllegalStateException("Frame output unavailable");
            Object global=Class.forName("android.hardware.display.DisplayManagerGlobal").getMethod("getInstance").invoke(null);
            Object info=call(global,"getDisplayInfo",new Class<?>[]{int.class},display.getDisplay().getDisplayId());
            int stack=info.getClass().getField("layerStack").getInt(info);
            source=SurfaceControl.class.getConstructor().newInstance();
            if(!(Boolean)call(wm,"mirrorDisplay",new Class<?>[]{int.class,SurfaceControl.class},0,source))throw new IllegalStateException("Mirror unavailable");
            SurfaceControl.Builder rb=new SurfaceControl.Builder().setName("FoldPatch output root");call(rb,"setContainerLayer",new Class<?>[0]);outputRoot=rb.build();
            try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
                call(t,"setLayerStack",new Class<?>[]{SurfaceControl.class,int.class},outputRoot,stack);
                t.reparent(source,outputRoot);call(t,"show",new Class<?>[]{SurfaceControl.class},outputRoot);
                call(t,"show",new Class<?>[]{SurfaceControl.class},source);
                call(t,"setLayerStack",new Class<?>[]{SurfaceControl.class,int.class},buffer,-1);
                call(t,"show",new Class<?>[]{SurfaceControl.class},buffer);t.apply();
            }
            systemUi.addFrameBackgroundTail(outputRoot,tails,width,height,usable);
            return buffer;
        }catch(Exception e){stop();throw e;}
    }
    void stop(){
        if(display!=null){display.release();display=null;}
        if(surface!=null){surface.release();surface=null;}
        if(queue!=null){try{call(queue,"destroy",new Class<?>[0]);}catch(Exception ignored){}queue=null;}
        for(SurfaceControl s:new SurfaceControl[]{source,outputRoot,buffer})if(s!=null){try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){call(t,"remove",new Class<?>[]{SurfaceControl.class},s);t.apply();}catch(Exception ignored){}s.release();}
        for(SurfaceControl tail:tails)tail.release();tails.clear();
        source=null;outputRoot=null;buffer=null;
    }
}
