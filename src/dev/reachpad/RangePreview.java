package dev.reachpad;

import android.content.SharedPreferences;

/** Durable, single-baseline transaction shared by the calibration UI and service watchdog. */
final class RangePreview {
    static final long TIMEOUT=15000;
    static boolean pending(SharedPreferences p){return p.getBoolean("native_regions_pending",false);}
    static float clamp(float value){return Math.max(.20f,Math.min(.50f,Math.round(value*200)/200f));}
    static void apply(SharedPreferences p,float left,float rightWidth){
        SharedPreferences.Editor e=p.edit();
        if(!pending(p))e.putFloat("preview_left",p.getFloat("left",.45f)).putFloat("preview_right",p.getFloat("right",.55f));
        e.putFloat("left",clamp(left)).putFloat("right",1f-clamp(rightWidth))
            .putBoolean("native_regions_pending",true).putLong("native_regions_expires",System.currentTimeMillis()+TIMEOUT).commit();
    }
    static void save(SharedPreferences p){
        if(!pending(p))return;
        float l=baseline(p,"left",.45f),r=baseline(p,"right",.55f);
        SharedPreferences.Editor e=p.edit();
        if(Math.abs(p.getFloat("left",l)-l)>.0001f||Math.abs(p.getFloat("right",r)-r)>.0001f)
            e.putFloat("previous_left",l).putFloat("previous_right",r);
        clear(e).commit();
    }
    // Fallback also recovers an unconfirmed preview created by the previous APK.
    private static float baseline(SharedPreferences p,String side,float fallback){return p.getFloat("preview_"+side,p.getFloat("previous_"+side,fallback));}
    private static SharedPreferences.Editor clear(SharedPreferences.Editor e){return e.putBoolean("native_regions_pending",false).remove("native_regions_expires").remove("preview_left").remove("preview_right");}
    static boolean rollback(SharedPreferences p){
        if(!pending(p))return false;
        clear(p.edit().putFloat("left",baseline(p,"left",.45f)).putFloat("right",baseline(p,"right",.55f))).commit();return true;
    }
    static boolean expire(SharedPreferences p){return pending(p)&&System.currentTimeMillis()>=p.getLong("native_regions_expires",0)&&rollback(p);}
    static void restoreLast(SharedPreferences p){
        if(rollback(p))return;
        float l=p.getFloat("left",.45f),r=p.getFloat("right",.55f);
        p.edit().putFloat("left",p.getFloat("previous_left",l)).putFloat("right",p.getFloat("previous_right",r))
            .putFloat("previous_left",l).putFloat("previous_right",r).commit();
    }
}
