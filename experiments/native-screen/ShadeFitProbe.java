package dev.reachpad.experiment;
import android.graphics.Rect;
import android.os.*;
import android.view.SurfaceControl;
import java.util.*;
import java.io.*;
/** Bounded prototype: constant app scale, source-only shade/status transform. */
public class ShadeFitProbe {
 static Object areas,fitAreas,wm;static SurfaceControl root;static List<SurfaceControl> layers=new ArrayList<>(),fits=new ArrayList<>();static List<Object> tokens=new ArrayList<>();static boolean done;static List<Object> shadeTokens=new ArrayList<>();
 static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{return o.getClass().getMethod(n,t).invoke(o,a);}
 static SurfaceControl layer(String n,String type)throws Exception{SurfaceControl.Builder b=new SurfaceControl.Builder().setName(n);call(b,type,new Class<?>[0]);SurfaceControl s=b.build();layers.add(s);return s;}
 static void bounds(Rect r)throws Exception{if(areas==null)return;Object t=Class.forName("android.window.WindowContainerTransaction").getConstructor().newInstance();for(Object tok:tokens)call(t,"setBounds",new Class<?>[]{Class.forName("android.window.WindowContainerToken"),Rect.class},tok,r);call(areas,"applyTransaction",new Class<?>[]{t.getClass()},t);}
 static void shadeBounds(Rect r)throws Exception{if(fitAreas==null)return;Object t=Class.forName("android.window.WindowContainerTransaction").getConstructor().newInstance();for(Object tok:shadeTokens)call(t,"setBounds",new Class<?>[]{Class.forName("android.window.WindowContainerToken"),Rect.class},tok,r);call(fitAreas,"applyTransaction",new Class<?>[]{t.getClass()},t);}
 static synchronized void cleanup(){if(done)return;done=true;
  try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){if(root!=null)call(t,"remove",new Class<?>[]{SurfaceControl.class},root);for(SurfaceControl s:fits)call(t,"setMatrix",new Class<?>[]{SurfaceControl.class,float.class,float.class,float.class,float.class},s,1f,0f,0f,1f);t.apply();}catch(Exception e){System.err.println(e);}
  try{shadeBounds(new Rect());bounds(new Rect());}catch(Exception e){System.err.println(e);}
  for(Object o:new Object[]{fitAreas,areas})if(o!=null)try{call(o,"unregisterOrganizer",new Class<?>[0]);}catch(Exception e){System.err.println(e);}
  for(SurfaceControl s:layers)try{s.release();}catch(Exception e){}System.out.println("SHADE_PROBE_RESTORED");
 }
 public static void main(String[] args)throws Exception{
  int seconds=Integer.parseInt(args[0]);if(seconds<1||seconds>60)throw new IllegalArgumentException();Looper.prepareMainLooper();
  Runtime.getRuntime().addShutdownHook(new Thread(ShadeFitProbe::cleanup));Thread watchdog=new Thread(()->{try{Thread.sleep((seconds+5)*1000L);}catch(Exception e){}cleanup();Runtime.getRuntime().halt(124);});watchdog.setDaemon(true);watchdog.start();
  try{
   java.lang.Process d=new ProcessBuilder("dumpsys","window","displays").start();BufferedReader in=new BufferedReader(new InputStreamReader(d.getInputStream()));String l;while((l=in.readLine())!=null)if((l.contains("* OneHanded:")||l.contains("* WindowedMagnification:"))&&l.contains("(organized)"))throw new IllegalStateException("Area occupied");d.waitFor();
   Class<?> org=Class.forName("android.window.DisplayAreaOrganizer");java.util.concurrent.Executor ex=Runnable::run;
   areas=org.getConstructor(java.util.concurrent.Executor.class).newInstance(ex);
   for(Object a:(List<?>)call(areas,"registerOrganizer",new Class<?>[]{int.class},4)){Object info=call(a,"getDisplayAreaInfo",new Class<?>[0]);layers.add((SurfaceControl)call(a,"getLeash",new Class<?>[0]));if(info.getClass().getField("displayId").getInt(info)==0)tokens.add(info.getClass().getField("token").get(info));}
   if(tokens.size()!=1)throw new IllegalStateException("Unexpected areas");bounds(new Rect(0,0,1574,2208));
   fitAreas=org.getConstructor(java.util.concurrent.Executor.class).newInstance(ex);
   for(Object a:(List<?>)call(fitAreas,"registerOrganizer",new Class<?>[]{int.class},3)){Object info=call(a,"getDisplayAreaInfo",new Class<?>[0]);SurfaceControl s=(SurfaceControl)call(a,"getLeash",new Class<?>[0]);layers.add(s);System.out.println("AREA "+info+" SURFACE "+s);if(info.getClass().getField("displayId").getInt(info)==0&&(s.toString().contains("OneHanded:17:17")||s.toString().contains("OneHanded:15:15"))){fits.add(s);if(s.toString().contains("OneHanded:17:17"))shadeTokens.add(info.getClass().getField("token").get(info));}}
   if(args.length>1&&args[1].equals("tall"))shadeBounds(new Rect(0,0,1768,(int)Math.ceil(2208*1768f/1574)));
   if(fits.size()!=2)throw new IllegalStateException("Unsupported shade/status grouping: "+fits.size());
   IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");wm=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
   root=layer("ReachPad shade fit probe","setContainerLayer");SurfaceControl black=layer("probe background","setColorLayer");
   try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
    call(t,"setLayerStack",new Class<?>[]{SurfaceControl.class,int.class},root,0);t.setLayer(root,2000000000);call(t,"show",new Class<?>[]{SurfaceControl.class},root);
    t.reparent(black,root).setLayer(black,0).setCrop(black,new Rect(0,0,1768,2208));call(t,"setColor",new Class<?>[]{SurfaceControl.class,float[].class},black,new float[]{0,0,0});call(t,"show",new Class<?>[]{SurfaceControl.class},black);
    for(SurfaceControl s:fits)call(t,"setMatrix",new Class<?>[]{SurfaceControl.class,float.class,float.class,float.class,float.class},s,1574f/1768,0f,0f,1574f/1768);
    for(int i=0;i<2;i++){SurfaceControl mirror=SurfaceControl.class.getConstructor().newInstance();layers.add(mirror);call(wm,"mirrorDisplay",new Class<?>[]{int.class,SurfaceControl.class},0,mirror);SurfaceControl viewport=layer("probe viewport "+i,"setContainerLayer");t.reparent(mirror,viewport);t.reparent(viewport,root).setLayer(viewport,i+1).setCrop(viewport,new Rect(i==0?0:787,0,i==0?787:1574,2208)).setPosition(viewport,i==0?0:194,0);call(t,"show",new Class<?>[]{SurfaceControl.class},mirror);call(t,"show",new Class<?>[]{SurfaceControl.class},viewport);}
    if(args.length>1&&args[1].equals("fill")){
     SurfaceControl shade=null;for(SurfaceControl f:fits)if(f.toString().contains("OneHanded:17:17"))shade=f;
     int bottom=(int)Math.floor(2208*1574f/1768), band=4;float stretch=(2208-bottom)/(float)band;
     for(int i=0;i<2;i++){SurfaceControl m=(SurfaceControl)SurfaceControl.class.getMethod("mirrorSurface",SurfaceControl.class).invoke(null,shade);layers.add(m);SurfaceControl v=layer("probe shade bottom "+i,"setContainerLayer");t.reparent(m,v);t.reparent(v,root).setLayer(v,20+i).setCrop(v,new Rect(i==0?0:884,2208-band,i==0?884:1768,2208));call(t,"setMatrix",new Class<?>[]{SurfaceControl.class,float.class,float.class,float.class,float.class},v,1574f/1768,0f,0f,stretch);t.setPosition(v,i==0?0:194,bottom-(2208-band)*stretch);call(t,"show",new Class<?>[]{SurfaceControl.class},m);call(t,"show",new Class<?>[]{SurfaceControl.class},v);}
    }
    t.apply();
   }
   System.out.println("SHADE_PROBE_READY");System.out.flush();Thread.sleep(seconds*1000L);
  }finally{cleanup();}System.exit(0);
 }
}
