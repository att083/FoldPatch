package dev.reachpad;

import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;

/** JVM test of durable preview recovery, without needing a device or an Android runtime. */
public final class RangePreviewTest {
    static final Map<String,Object> values=new HashMap<>();
    static SharedPreferences prefs;
    static Object proxy(Class<?> type,InvocationHandler fn){return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},fn);}
    static SharedPreferences.Editor editor(){
        Map<String,Object> changes=new HashMap<>();Set<String> removed=new HashSet<>();
        return (SharedPreferences.Editor)proxy(SharedPreferences.Editor.class,(o,m,a)->{
            String name=m.getName();
            if(name.startsWith("put")){changes.put((String)a[0],a[1]);return o;}
            if(name.equals("remove")){removed.add((String)a[0]);return o;}
            if(name.equals("commit")||name.equals("apply")){for(String k:removed)values.remove(k);values.putAll(changes);return name.equals("commit")?Boolean.TRUE:null;}
            throw new UnsupportedOperationException(name);
        });
    }
    static void expect(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void bounds(float l,float r){expect(Math.abs(prefs.getFloat("left",0)-l)<.0001f,"left");expect(Math.abs(prefs.getFloat("right",0)-r)<.0001f,"right start");}
    public static void main(String[] args){
        prefs=(SharedPreferences)proxy(SharedPreferences.class,(o,m,a)->{if(m.getName().equals("edit"))return editor();if(m.getName().startsWith("get"))return values.getOrDefault(a[0],a[1]);throw new UnsupportedOperationException(m.getName());});
        values.put("left",.45f);values.put("right",.55f);values.put("auto_open",true);
        values.put("previous_left",.46f);values.put("previous_right",.54f);
        RangePreview.apply(prefs,.43f,.41f);bounds(.43f,.59f);
        RangePreview.apply(prefs,.425f,.415f);bounds(.425f,.585f);
        expect(RangePreview.rollback(prefs),"preview should roll back");bounds(.45f,.55f);
        expect(!RangePreview.rollback(prefs),"rollback should be idempotent");
        expect(prefs.getFloat("previous_left",0)==.46f&&prefs.getFloat("previous_right",0)==.54f,"cancel must preserve prior saved history");
        RangePreview.apply(prefs,.45f,.45f);RangePreview.save(prefs);
        expect(prefs.getFloat("previous_left",0)==.46f,"saving unchanged bounds must preserve history");
        RangePreview.apply(prefs,.44f,.42f);RangePreview.save(prefs);bounds(.44f,.58f);
        expect(!RangePreview.expire(prefs),"saved value must not expire");
        RangePreview.restoreLast(prefs);bounds(.45f,.55f);
        RangePreview.apply(prefs,.40f,.40f);values.put("native_regions_expires",0L);
        expect(RangePreview.expire(prefs),"abandoned preview must expire");bounds(.45f,.55f);
        expect(Math.abs(prefs.getFloat("previous_left",0)-.44f)<.0001f,"expiry must preserve restore history");
        RangePreview.restoreLast(prefs);bounds(.44f,.58f);
        values.put("native_regions_pending",true);values.put("previous_left",.45f);values.put("previous_right",.55f);
        expect(RangePreview.rollback(prefs),"legacy pending preview should recover");bounds(.45f,.55f);
        expect(prefs.getBoolean("auto_open",false),"range changes must not change automatic start");
        expect(RangePreview.clamp(.1f)==.2f&&RangePreview.clamp(.6f)==.50f,"limits");
        RangePreview.apply(prefs,.50f,.50f);bounds(.50f,.50f);
        values.put("native_regions_expires",0L);
        expect(RangePreview.expire(prefs),"full-width preview must expire");bounds(.45f,.55f);
        RangePreview.apply(prefs,.50f,.50f);RangePreview.save(prefs);bounds(.50f,.50f);
        RangePreview.restoreLast(prefs);bounds(.45f,.55f);
        RangePreview.restoreLast(prefs);bounds(.50f,.50f);
        RangePreview.apply(prefs,.40f,.50f);bounds(.40f,.50f);
        RangePreview.rollback(prefs);bounds(.50f,.50f);
        RangePreview.apply(prefs,.50f,.40f);bounds(.50f,.60f);
        RangePreview.rollback(prefs);bounds(.50f,.50f);
        System.out.println("RangePreviewTest: passed (baseline, rollback, save, restore, expiry, 50% on either/both sides)");
    }
}
