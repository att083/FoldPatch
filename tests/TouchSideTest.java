package dev.reachpad;

import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;

public final class TouchSideTest {
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
    static void near(float a,float b){expect(Math.abs(a-b)<.001f,a+" != "+b);}
    public static void main(String[] args){
        prefs=(SharedPreferences)proxy(SharedPreferences.class,(o,m,a)->{if(m.getName().equals("edit"))return editor();if(m.getName().startsWith("get"))return values.getOrDefault(a[0],a[1]);throw new UnsupportedOperationException(m.getName());});
        expect(!TouchSide.right(prefs),"existing installations default left");
        values.put("left",.40f);values.put("right",.65f);values.put("native_target",0);values.put("native_bar_x",69);
        TouchSide.apply(prefs,true);expect(TouchSide.right(prefs),"right selected");near(TouchSide.ratio(prefs),.35f);expect(prefs.getInt("native_target",-1)==1,"direct mode follows side");
        TouchSide.apply(prefs,false);TouchSide.apply(prefs,true);TouchSide.rollback(prefs);
        expect(!TouchSide.right(prefs)&&prefs.getInt("native_target",-1)==0,"multiple changes roll back to original");
        values.put("native_target",1);TouchSide.apply(prefs,true);expect(prefs.getInt("native_target",-1)==0,"opposite mode follows side");
        values.put("touch_side_expires",0L);expect(TouchSide.expire(prefs),"expired preview recovers");expect(prefs.getInt("native_target",-1)==1,"target recovered");
        values.put("native_target",2);TouchSide.apply(prefs,true);TouchSide.save(prefs);expect(TouchSide.right(prefs)&&prefs.getInt("native_target",-1)==2,"whole mode unchanged");expect(!TouchSide.rollback(prefs),"saved side durable");
        expect(prefs.getInt("native_bar_x",0)==69,"left bar position preserved");near(prefs.getFloat("left",0),.4f);near(prefs.getFloat("right",0),.65f);
        for(boolean right:new boolean[]{false,true})for(int size:new int[]{80,220,600})for(int wanted:new int[]{-100,50,900,3000}){
            int x=TouchSide.barX(wanted,size,right,700,1100,1768);
            expect(x>=TouchSide.start(right,1100)&&x+size<=TouchSide.end(right,700,1768),"bar inside asymmetric healthy side");
        }
        near(PointerGeometry.moveTarget(10,-50,700,1100,1768,0),0);near(PointerGeometry.moveTarget(680,100,700,1100,1768,0),699);
        near(PointerGeometry.moveTarget(1105,-100,700,1100,1768,1),1100);
        near(PointerGeometry.moveTarget(698,5,700,1100,1768,2),1103);near(PointerGeometry.moveTarget(1103,-5,700,1100,1768,2),698);
        System.out.println("TouchSideTest passed: migration, direct/opposite/whole targets, save/rollback/expiry, asymmetric bounds, left and right pointer limits");
    }
}
