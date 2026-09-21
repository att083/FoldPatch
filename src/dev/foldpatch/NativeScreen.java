package dev.foldpatch;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.SurfaceControl;
import java.util.ArrayList;

/** Shell-owned mirrors of the real display. Apps and System UI stay on display 0. */
final class NativeScreen {
    private final ArrayList<SurfaceControl> layers = new ArrayList<>();
    private final Object displayManager, windowManager;
    private SurfaceControl root,background;
    private SurfaceControl pointer;
    private final ReflowArea reflow;
    private final SystemUiFit systemUi=new SystemUiFit();
    private final FrameMirror frames=new FrameMirror();
    private final android.content.Context context;
    private SurfaceControl frame;
    private long lease,generation;
    // A new helper must also recover an abandoned journal while on the cover screen.
    private boolean cleanupPending=true;
    int width, height, cut, leftEdge, rightEdge, sourceWidth;
    private float leftRatio, rightRatio;
    NativeScreen(android.content.Context context,String apk) throws Exception {
        this.context=context;
        reflow=new ReflowArea(apk);
        displayManager = Class.forName("android.hardware.display.DisplayManagerGlobal").getMethod("getInstance").invoke(null);
        IBinder b = (IBinder)Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null,"window");
        windowManager = Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,b);
    }
    private static Object call(Object o,String name,Class<?>[] types,Object... args) throws Exception {return o.getClass().getMethod(name,types).invoke(o,args);}
    private static int field(Object o,String name) throws Exception {return o.getClass().getField(name).getInt(o);}
    private Object display() throws Exception {return call(displayManager,"getDisplayInfo",new Class<?>[]{int.class},0);}
    synchronized boolean active(){return root!=null;}
    private boolean needsReflow(){return sourceWidth<width;}
    synchronized Bundle start(float left,float right) throws Exception {
        if(!(left>=.20f&&left<=.50f&&right>=.50f&&right<=.80f))throw new IllegalArgumentException("Invalid healthy regions");
        Object info=display();int w=field(info,"logicalWidth"),h=field(info,"logicalHeight");
        if(w<1200||h<1200||field(info,"rotation")!=0||field(info,"state")!=2)throw new IllegalStateException("내부 화면을 세로로 펼쳐 주세요");
        lease=SystemClock.uptimeMillis();
        if(root!=null&&(!needsReflow()||reflow.alive())&&w==width&&h==height&&left==leftRatio&&right==rightRatio){
            if(needsReflow()){reflow.ping();if(!systemUi.valid())throw new IllegalStateException("시스템 화면 연결이 끊겼어요");}
            return status();
        }
        stop(); width=w;height=h;leftRatio=left;rightRatio=right;
        leftEdge=Math.round(w*left);rightEdge=Math.round(w*right);
        sourceWidth=leftEdge+w-rightEdge;cut=leftEdge;
        try {
            root=layer("FoldPatch native screen","setContainerLayer",null);generation=SystemClock.elapsedRealtimeNanos();
            // With no gap, leave apps and System UI on their original surfaces.
            // Only the pointer overlay and touch routing are needed. In particular,
            // do not acquire display areas or create a full-width recovery journal.
            if(needsReflow()){
                reflow.start(w,h,sourceWidth);
                systemUi.start(sourceWidth/(float)width,reflow.combined());
                SurfaceControl black=background=layer("FoldPatch damaged gap","setColorLayer",root);
                frame=frames.start(context,windowManager,systemUi,w,h,sourceWidth);
                SurfaceControl a=mirror(),b=mirror();
                try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
                    t.setLayer(black,0).setCrop(black,new Rect(0,0,w,h));
                    call(t,"setColor",new Class<?>[]{SurfaceControl.class,float[].class},black,new float[]{0,0,0});
                    t.reparent(a,root).setLayer(a,1).setCrop(a,new Rect(0,0,cut,h));
                    t.reparent(b,root).setLayer(b,2).setCrop(b,new Rect(cut,0,sourceWidth,h));
                    call(t,"setMatrix",new Class<?>[]{SurfaceControl.class,float.class,float.class,float.class,float.class},a,1f,0f,0f,1f);
                    call(t,"setMatrix",new Class<?>[]{SurfaceControl.class,float.class,float.class,float.class,float.class},b,1f,0f,0f,1f);
                    t.setPosition(b,rightEdge-cut,0);t.apply();
                }
            }
            try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
                call(t,"setLayerStack",new Class<?>[]{SurfaceControl.class,int.class},root,-1);
                t.setLayer(root,2000000000);
                for(SurfaceControl s:layers)if(s!=root)call(t,"show",new Class<?>[]{SurfaceControl.class},s);
                t.apply();
            }
            pointer=layer("FoldPatch cursor","setContainerLayer",root);
            try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
                t.setLayer(pointer,100);
                // Even a color layer with no input channel can obscure touches below it.
                // Trust only our visual pointer subtree, never the mirrored apps or damage sink.
                call(t,"setTrustedOverlay",new Class<?>[]{SurfaceControl.class,boolean.class},pointer,true);
                for(int i=0;i<4;i++){
                    SurfaceControl line=layer("FoldPatch cursor stroke","setColorLayer",pointer);
                    boolean horizontal=(i%2)==0;int thick=i<2?8:4;
                    t.setLayer(line,i).setCrop(line,new Rect(0,0,horizontal?44:thick,horizontal?thick:44));
                    t.setPosition(line,horizontal?0:(44-thick)/2f,horizontal?(44-thick)/2f:0);
                    call(t,"setColor",new Class<?>[]{SurfaceControl.class,float[].class},line,i<2?new float[]{0,0,0}:new float[]{1,1,0});
                    call(t,"show",new Class<?>[]{SurfaceControl.class},line);
                }
                t.apply();
            }
            lease=SystemClock.uptimeMillis();
            return status();
        }catch(Exception e){stop();throw e;}
    }
    private SurfaceControl layer(String name,String type,SurfaceControl parent) throws Exception {
        SurfaceControl.Builder b=new SurfaceControl.Builder().setName(name).setHidden(true);
        call(b,type,new Class<?>[0]);if(parent!=null)b.setParent(parent);
        SurfaceControl s=b.build();layers.add(s);return s;
    }
    private SurfaceControl mirror() throws Exception {
        SurfaceControl s=(SurfaceControl)SurfaceControl.class.getMethod("mirrorSurface",SurfaceControl.class).invoke(null,frame);layers.add(s);
        // Mirror-root properties can follow the source during WMS transitions. Keep our
        // crop/transform on an independent parent so a status strip never reveals a full clone.
        SurfaceControl viewport=layer("FoldPatch mirror viewport","setContainerLayer",null);
        try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
            t.reparent(s,viewport).setPosition(s,0,0);call(t,"show",new Class<?>[]{SurfaceControl.class},s);t.apply();
        }
        return viewport;
    }
    synchronized boolean check(){
        if(root==null){if(cleanupPending)stop();return false;}
        try{Object d=display();if(SystemClock.uptimeMillis()-lease<10000&&field(d,"logicalWidth")==width&&field(d,"logicalHeight")==height&&field(d,"rotation")==0&&field(d,"state")==2&&(!needsReflow()||(reflow.alive()&&systemUi.valid()))){return true;}}
        catch(Exception ignored){}
        stop();return false;
    }
    synchronized Bundle status(){Bundle b=new Bundle();b.putInt("width",width);b.putInt("height",height);b.putInt("cut",cut);b.putInt("sourceWidth",sourceWidth);b.putFloat("scale",1f);b.putInt("left",leftEdge);b.putInt("right",rightEdge);b.putBoolean("active",root!=null);b.putParcelable("surface",root);b.putLong("generation",generation);return b;}
    synchronized void presented(long expectedGeneration)throws Exception {
        if(root==null||expectedGeneration!=generation)return;
        // Trust the pixel presentation so it cannot obscure original input windows.
        // All original window input and occlusion policies remain in effect.
        try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
            call(t,"setTrustedOverlay",new Class<?>[]{SurfaceControl.class,boolean.class},root,true);
            call(t,"show",new Class<?>[]{SurfaceControl.class},root);t.apply();
        }
    }
    synchronized void cursor(boolean visible,float x,float y) throws Exception {
        if(pointer==null)return;
        try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
            t.setPosition(pointer,PointerGeometry.move(x,0,leftEdge,rightEdge,width,true)-22,Math.max(0,Math.min(height-1,y))-22);
            call(t,visible?"show":"hide",new Class<?>[]{SurfaceControl.class},pointer);t.apply();
        }
    }
    synchronized void stop(){
        cleanupPending=true;
        if(root!=null)try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){call(t,"remove",new Class<?>[]{SurfaceControl.class},root);t.apply();}catch(Exception e){android.util.Log.w("FoldPatchNative","Remove",e);}
        root=null;pointer=null;background=null;
        for(SurfaceControl s:layers)try{s.release();}catch(Exception ignored){}
        layers.clear();frames.stop();frame=null;
        // WMS replaces organizer leashes on unregister. Restore the ancestor first,
        // and wait for its guard to exit, BEFORE replacing the status/shade children.
        // During a fold the two migrations can land in different sync transactions:
        // child-first teardown can later attach a new child to the removed old parent,
        // leaving StatusBar/NotificationShade offscreen while still holding focus.
        reflow.stop();
        systemUi.stop();
        cleanupPending=false;
    }
}
