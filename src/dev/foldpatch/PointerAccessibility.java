package dev.foldpatch;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;

/** Captures physical touchscreen events while the assisted screen is active.
 * Shell-injected pointer events bypass the hardware input filter, preventing feedback.
 * No window content, text, keys or accessibility events are requested. */
public final class PointerAccessibility extends AccessibilityService {
    static PointerAccessibility instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean capturing,gesture,padGesture;
    private SurfaceControl presentation;
    private long presentationGeneration;
    boolean attachPresentation(SurfaceControl surface,long generation){
        if(surface==null)return false;
        if(presentation!=null&&presentationGeneration==generation){surface.release();return true;}
        try{detachPresentation();attachAccessibilityOverlayToDisplay(0,surface);presentation=surface;presentationGeneration=generation;return true;}
        catch(Exception e){surface.release();android.util.Log.e("FoldPatchNative","Attach presentation",e);return false;}
    }
    void detachPresentation(){
        if(presentation==null)return;
        try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){t.reparent(presentation,null).apply();}
        catch(Exception e){android.util.Log.w("FoldPatchNative","Detach presentation",e);}
        finally{presentation.release();presentation=null;presentationGeneration=0;}
    }
    private final Runnable watch=new Runnable(){public void run(){sync();handler.postDelayed(this,250);}};
    @Override protected void onServiceConnected(){if(Build.VERSION.SDK_INT<34){disableSelf();return;}instance=this;NativeService.settingsChanged();handler.post(watch);}
    static void refresh(){if(instance!=null)instance.sync();}
    private void sync(){
        if(Build.VERSION.SDK_INT<34)return;
        boolean wanted=NativeService.captureTouch();
        if(capturing==wanted)return;
        capturing=wanted;gesture=false;
        AccessibilityServiceInfo info=getServiceInfo();info.setMotionEventSources(wanted?InputDevice.SOURCE_TOUCHSCREEN:0);setServiceInfo(info);
    }
    @Override public void onMotionEvent(MotionEvent event){
        NativeService service=NativeService.instance;
        if(service==null||!NativeService.captureTouch()){sync();return;}
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){gesture=true;padGesture=service.padInput(event.getX(),event.getY());}
        if(!gesture)return;
        if(padGesture)service.physicalPadEvent(event);else service.forwardPhysical(event);
        if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)gesture=false;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){gesture=false;NativeService.cancelPointerGesture();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);detachPresentation();instance=null;capturing=false;NativeService.pointerDisconnected();super.onDestroy();}
}
