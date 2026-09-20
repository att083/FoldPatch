package dev.reachpad;
public final class PointerGeometryTest {
    static void near(float got,float expected){if(Math.abs(got-expected)>.01f)throw new AssertionError(got+" != "+expected);}
    public static void main(String[] args){
        near(PointerGeometry.move(790,10,796,972,1768,true),976);
        near(PointerGeometry.move(976,-10,796,972,1768,true),790);
        near(PointerGeometry.move(0,-500,796,972,1768,true),0);
        near(PointerGeometry.move(1767,500,796,972,1768,true),1767);
        near(PointerGeometry.move(900,0,796,972,1768,true),972);
        near(PointerGeometry.move(1000,-500,796,972,1768,false),972);
        near(PointerGeometry.move(650,200,700,1060,1768,true),1210);
        near(PointerGeometry.speed(Float.NaN),1);near(PointerGeometry.speed(-2),.5f);near(PointerGeometry.speed(9),3);
        near(PointerGeometry.move(100,50*PointerGeometry.speed(2),796,972,1768,true),200);
        for(int x=0;x<1592;x++){float physical=x<796?x:x+176;float next=PointerGeometry.move(physical,1,796,972,1768,true);if(next>=796&&next<972)throw new AssertionError("cursor in gap");}
        // An intact screen has one continuous coordinate space at the center.
        near(PointerGeometry.moveTarget(880,10,884,884,1768,2),890);
        near(PointerGeometry.moveTarget(890,-10,884,884,1768,2),880);
        near(PointerGeometry.moveTarget(900,-100,884,884,1768,1),884);
        near(PointerGeometry.moveTarget(880,100,884,884,1768,0),883);
        near(PointerGeometry.moveTarget(0,-10,884,884,1768,2),0);
        near(PointerGeometry.moveTarget(1767,10,884,884,1768,2),1767);
        for(int x=0;x<1768;x++)near(PointerGeometry.moveTarget(x,0,884,884,1768,2),x);
        System.out.println("PointerGeometryTest passed: bidirectional seam, bounds, asymmetric regions, speed, invalid settings");
    }
}
