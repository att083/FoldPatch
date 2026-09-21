package dev.foldpatch;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){if(c.getSharedPreferences("regions",0).getBoolean("auto_open",false))try{c.startForegroundService(new Intent(c,NativeService.class));}catch(Exception e){android.util.Log.w("FoldPatch","Boot recovery",e);}}
}
