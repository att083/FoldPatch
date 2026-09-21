package dev.foldpatch;

import android.graphics.Rect;
import android.view.SurfaceControl;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Holds a constant transform on Samsung's status/shade areas, never on app mirrors.
 * Organizer death replaces its leashes in WMS, so source transforms cannot outlive the helper. */
final class SystemUiFit {
    private AreaOrganizer organizer;
    private boolean combined;
    private final List<SurfaceControl> owned = new ArrayList<>();
    private SurfaceControl shade, status;
    private static Object call(Object o,String n,Class<?>[] types,Object... args)throws Exception {
        return o.getClass().getMethod(n,types).invoke(o,args);
    }
    void start(float scale,boolean combined)throws Exception {
        stop();
        this.combined=combined;
        if(combined)return; // ReflowGuard owns the common app/status/shade ancestor.
        // Registration replaces existing feature owners. Check every display before acquiring.
        ReflowPolicy.requireFree(AreaOrganizer.snapshot(),3);
        try {
            organizer=new AreaOrganizer();
            for(Object item:organizer.register(3)) {
                Object info=call(item,"getDisplayAreaInfo",new Class<?>[0]);
                SurfaceControl surface=(SurfaceControl)call(item,"getLeash",new Class<?>[0]);owned.add(surface);
                if(info.getClass().getField("displayId").getInt(info)!=0)continue;
                // Exact, separately verified Samsung policy groups; never guess an app-containing area.
                if(surface.toString().contains("OneHanded:17:17"))shade=surface;
                if(surface.toString().contains("OneHanded:15:15"))status=surface;
            }
            if(shade==null||status==null)throw new IllegalStateException("지원하지 않는 시스템 화면 구성이에요");
            try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()) {
                for(SurfaceControl s:new SurfaceControl[]{shade,status})matrix(t,s,scale,scale);
                t.apply();
            }
        } catch(Exception e) {stop();throw e;}
    }
    boolean valid(){return combined||(organizer!=null&&organizer.valid()&&shade!=null&&status!=null&&shade.isValid()&&status.isValid());}
    void addFrameBackgroundTail(SurfaceControl root,List<SurfaceControl> layers,int width,int height,int usable)throws Exception {
        if(combined)return;
        // Edge background samples exclude the bottom disclosure text. Stretching a full
        // row also stretches text descenders into bright vertical stripes.
        int band=4,bottom=(int)Math.floor(height*usable/(float)width),half=usable/2;
        try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()) {
            for(int i=0;i<2;i++) {
                SurfaceControl mirror=(SurfaceControl)SurfaceControl.class.getMethod("mirrorSurface",SurfaceControl.class).invoke(null,shade);layers.add(mirror);
                SurfaceControl.Builder b=new SurfaceControl.Builder().setName("FoldPatch frame shade corner "+i).setParent(root);call(b,"setContainerLayer",new Class<?>[0]);SurfaceControl viewport=b.build();layers.add(viewport);
                int x=i==0?0:width-band,part=i==0?half:usable-half;
                float sx=part/(float)band,sy=(height-bottom)/(float)band;
                t.reparent(mirror,viewport).setLayer(viewport,20+i).setCrop(viewport,new Rect(x,height-band,x+band,height));
                matrix(t,viewport,sx,sy);t.setPosition(viewport,(i==0?0:half)-x*sx,bottom-(height-band)*sy);
                call(t,"show",new Class<?>[]{SurfaceControl.class},mirror);call(t,"show",new Class<?>[]{SurfaceControl.class},viewport);
            }
            t.apply();
        }
    }
    private static void matrix(SurfaceControl.Transaction t,SurfaceControl s,float x,float y)throws Exception {
        call(t,"setMatrix",new Class<?>[]{SurfaceControl.class,float.class,float.class,float.class,float.class},s,x,0f,0f,y);
    }
    void stop() {
        combined=false;
        if(organizer==null)return;
        try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()) {
            for(SurfaceControl s:new SurfaceControl[]{shade,status})if(organizer.valid()&&s!=null&&s.isValid())matrix(t,s,1f,1f);
            t.apply();
        }catch(Exception e){android.util.Log.w("FoldPatchNative","Restore system UI",e);}
        try{organizer.close();}
        catch(Exception e){throw new IllegalStateException("System UI cleanup is incomplete",e);}
        organizer=null;shade=null;status=null;
        for(SurfaceControl s:owned)try{s.release();}catch(Exception ignored){}owned.clear();
    }
}
