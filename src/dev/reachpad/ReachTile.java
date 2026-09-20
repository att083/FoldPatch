package dev.reachpad;
import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.TileService;
public final class ReachTile extends TileService {
    @Override public void onStartListening(){super.onStartListening();if(getQsTile()!=null){getQsTile().setLabel(getString(R.string.tile_label));getQsTile().updateTile();}}
    @Override public void onClick(){unlockAndRun(()->{
        ReachService.paused=false;
        Intent intent=new Intent(this,NativeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(android.os.Build.VERSION.SDK_INT>=34)startActivityAndCollapse(PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        else startActivityAndCollapse(intent);
    });}
}
