package dev.foldpatch;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.*;

public final class SetupFlowTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static SharedPreferences prefs(Map<String,Object> data){
        return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
            String n=m.getName();
            if(n.equals("contains"))return data.containsKey(a[0]);
            if(n.startsWith("get"))return data.getOrDefault(a[0],a[1]);
            if(n.equals("edit")){
                Map<String,Object> pending=new HashMap<>();
                return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(e,em,ea)->{
                    if(em.getName().startsWith("put")){pending.put((String)ea[0],ea[1]);return e;}
                    if(em.getName().equals("commit")||em.getName().equals("apply")){data.putAll(pending);return em.getName().equals("commit")?true:null;}
                    throw new UnsupportedOperationException(em.getName());
                });
            }
            throw new UnsupportedOperationException(n);
        });
    }
    static SetupFlow.Step next(Map<String,Object> d,boolean installed,boolean running,boolean permission,boolean helper,boolean overlay){
        SharedPreferences p=prefs(d);
        return SetupFlow.next(p.getBoolean("onboard_side",false),p.getBoolean("onboard_intro",false),installed,running,permission,helper,overlay,
            p.getBoolean("onboard_access",false),p.getBoolean("onboard_keyboard",false),p.getBoolean("onboard_range",false),p.getBoolean("onboard_practice",false));
    }
    public static void main(String[] args){
        Map<String,Object> data=new HashMap<>();SetupFlow.initialize(prefs(data));
        check(next(data,false,false,false,false,false)==SetupFlow.Step.SIDE,"both touch sides accessible before privileged setup");
        data.put("onboard_side",true);data.put("onboard_intro",true);
        check(next(data,false,false,false,false,false)==SetupFlow.Step.INSTALL,"missing app");
        check(next(data,true,false,false,false,false)==SetupFlow.Step.START,"installed but stopped");
        check(next(data,true,true,false,false,false)==SetupFlow.Step.AUTHORIZE,"running is not authorized");
        check(next(data,true,true,true,false,false)==SetupFlow.Step.CONNECT,"permission is not helper readiness");
        check(next(data,true,true,true,true,false)==SetupFlow.Step.OVERLAY,"overlay permission required");
        check(next(data,false,true,true,true,true)==SetupFlow.Step.ACCESS,"live Sui provider does not require manager install");
        data.put("onboard_access",true);
        check(next(data,true,true,true,true,true)==SetupFlow.Step.KEYBOARD,"separate keyboard consent");
        data.put("onboard_keyboard",true);data.put("setup_accepted",true);
        // Recreating the process midway through setup must not migrate it as a completed old installation.
        SetupFlow.initialize(prefs(data));
        check(next(data,true,true,true,true,true)==SetupFlow.Step.RANGE,"process restart retains unfinished calibration");
        check(!prefs(data).getBoolean("onboard_complete",false),"full consent does not imply completion");
        check(next(data,true,false,false,false,true)==SetupFlow.Step.START,"lost Shizuku redirects to start");
        check(next(data,true,true,true,true,true)==SetupFlow.Step.RANGE,"reconnect returns to calibration without asking again");
        data.put("onboard_range",true);
        check(next(data,true,true,true,true,true)==SetupFlow.Step.PRACTICE,"confirmed range unlocks practice");
        data.put("onboard_practice",true);
        check(next(data,true,true,true,true,true)==SetupFlow.Step.READY,"ready after practice");
        data.put("onboard_complete",true);
        check(next(data,true,true,false,false,true)==SetupFlow.Step.AUTHORIZE,"revoked permission defeats saved completion");
        check(next(data,true,true,true,true,false)==SetupFlow.Step.OVERLAY,"revoked overlay defeats saved completion");
        Map<String,Object> legacy=new HashMap<>();legacy.put("setup_accepted",true);legacy.put("left",.445f);legacy.put("right",.555f);legacy.put("native_target",2);legacy.put("touch_right",true);legacy.put("native_control_level",0);
        Map<String,Object> before=new HashMap<>(legacy);SetupFlow.initialize(prefs(legacy));
        for(String key:before.keySet())check(before.get(key).equals(legacy.get(key)),"migration preserved "+key);
        check(prefs(legacy).getBoolean("onboard_complete",false),"existing users are not forced through tutorial");
        check(next(legacy,true,false,false,false,true)==SetupFlow.Step.START,"existing user recovery only needs Shizuku");
        check(next(legacy,true,true,true,true,true)==SetupFlow.Step.READY,"existing calibration and practice skipped");
        System.out.println("SetupFlowTest passed: migration, resumable consent/calibration, live permissions, reconnect, provider without manager");
    }
}
