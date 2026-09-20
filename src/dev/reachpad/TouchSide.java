package dev.reachpad;

import android.content.SharedPreferences;

/** Physical input side, independent of which physical screen region the pointer controls. */
final class TouchSide {
    static final long TIMEOUT=15000;
    static boolean right(SharedPreferences p){return p.getBoolean("touch_right",false);}
    static int directTarget(SharedPreferences p){return right(p)?1:0;}
    static float ratio(SharedPreferences p){return right(p)?1-p.getFloat("right",.55f):p.getFloat("left",.45f);}
    static boolean pending(SharedPreferences p){return p.getBoolean("touch_side_pending",false);}
    static void apply(SharedPreferences p,boolean right){
        if(right==right(p))return;
        int target=p.getInt("native_target",directTarget(p));
        SharedPreferences.Editor e=p.edit();
        if(!pending(p))e.putBoolean("touch_side_before",right(p)).putInt("touch_target_before",target);
        e.putBoolean("touch_right",right).putInt("native_target",target==2?2:1-target)
            .putBoolean("touch_side_pending",true).putLong("touch_side_expires",System.currentTimeMillis()+TIMEOUT).commit();
    }
    private static SharedPreferences.Editor clear(SharedPreferences.Editor e){return e.remove("touch_side_pending").remove("touch_side_before").remove("touch_target_before").remove("touch_side_expires");}
    static void save(SharedPreferences p){clear(p.edit()).commit();}
    static boolean rollback(SharedPreferences p){
        if(!pending(p))return false;
        clear(p.edit().putBoolean("touch_right",p.getBoolean("touch_side_before",false)).putInt("native_target",p.getInt("touch_target_before",0))).commit();return true;
    }
    static boolean expire(SharedPreferences p){return pending(p)&&System.currentTimeMillis()>=p.getLong("touch_side_expires",0)&&rollback(p);}
    static int start(boolean right,int rightEdge){return right?rightEdge:0;}
    static int end(boolean right,int leftEdge,int width){return right?width:leftEdge;}
    static int barX(int desired,int barWidth,boolean right,int leftEdge,int rightEdge,int width){
        int start=start(right,rightEdge),end=end(right,leftEdge,width);
        return Math.max(start,Math.min(desired,end-barWidth));
    }
}
