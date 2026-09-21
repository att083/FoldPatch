package dev.foldpatch;
import java.util.*;
public final class TrackpadGestureTest {
 static final class Recorder implements TrackpadGesture.Sink {
  List<Integer> actions=new ArrayList<>();float px,py,x,y,x2,y2;int holds;
  public void pointer(float x,float y){px=x;py=y;}
  public void hold(){holds++;}
  public void touch(int a,int count,float x,float y,float x2,float y2,long down,long time){actions.add(a);this.x=x;this.y=y;this.x2=x2;this.y2=y2;if(time<down)throw new AssertionError("time");}
 }
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static TrackpadGesture make(Recorder r){return new TrackpadGesture(r,1574,2208,0,1573,2,8,1100,1000);}
 public static void main(String[] args){
  Recorder r=new Recorder();TrackpadGesture g=make(r);
  g.event(0,1,300,500,0,0,100);g.event(2,1,340,520,0,0,120);g.event(1,1,340,520,0,0,140);
  check(r.actions.isEmpty()&&r.px==1180&&r.py==1040,"pointer movement must not click");
  g.event(0,1,300,500,0,0,1000);g.event(1,1,300,500,0,0,1050);g.event(0,1,300,500,0,0,1150);g.event(1,1,300,500,0,0,1200);
  check(r.actions.equals(Arrays.asList(0,1,0,1)),"double click");
  r=new Recorder();g=make(r);g.event(0,1,300,500,0,0,2000);g.hold(2350);g.event(2,1,200,530,0,0,2600);g.event(1,1,200,530,0,0,2700);
  check(r.actions.equals(Arrays.asList(0,2,1))&&r.x==900&&r.holds==1,"hold and drag");
  r=new Recorder();g=make(r);g.event(0,1,300,500,0,0,3000);g.event(5,2,280,500,320,500,3020);g.event(2,2,350,600,390,600,3050);g.event(6,2,350,600,390,600,3060);g.event(1,1,350,600,0,0,3080);
  check(r.actions.equals(Arrays.asList(0,2,1))&&r.x==1170&&r.y==1100,"diagonal scroll");
  r=new Recorder();g=make(r);g.event(0,1,250,500,0,0,4000);g.event(5,2,250,500,350,500,4020);g.event(2,2,200,500,400,500,4050);float expanded=r.x2-r.x;g.event(2,2,270,500,330,500,4070);check(r.x2-r.x<expanded,"pinch in and out");g.event(6,2,270,500,330,500,4090);g.event(1,1,270,500,0,0,4100);
  check(r.actions.equals(Arrays.asList(0,261,2,2,262,1)),"valid multi-touch lifecycle");
  r=new Recorder();g=make(r);g.event(0,1,300,500,0,0,5000);g.hold(5350);g.cancel(5500);g.event(1,1,300,500,0,0,5600);check(r.actions.equals(Arrays.asList(0,3)),"cancel releases press without click");
  r=new Recorder();g=make(r);g.event(0,1,300,500,0,0,6000);g.event(1,1,300,500,0,0,6050);g.event(0,1,300,500,0,0,6150);g.event(2,1,320,500,0,0,6200);g.event(1,1,320,500,0,0,6250);check(r.actions.equals(Arrays.asList(0,1,0,2,1)),"tap then press drag");
  System.out.println("Trackpad gestures passed: movement, click, double click, hold, drag, scroll, pinch, cancellation");
 }
}
