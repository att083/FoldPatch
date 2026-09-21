package dev.foldpatch;
public final class TouchpadScrollTest {
    private static void near(float expected,float actual){if(Math.abs(expected-actual)>.01f)throw new AssertionError(expected+" != "+actual);}
    public static void main(String[] args){
        TouchpadScroll s=new TouchpadScroll();
        s.start(1200,900,300,600,787,981,1768,2208);s.move(100,610);near(1000,s.x);near(910,s.y);
        s.move(-100,610);near(800,s.x);near(606,s.x-194); // Same right-clone input transform through the gap.
        s.move(-100,310);near(800,s.x);near(610,s.y);
        s.move(0,410);near(900,s.x);near(710,s.y); // Both axes and reversal.
        s.start(700,900,300,600,787,981,1768,2208);s.move(600,600);near(1000,s.x);near(900,s.y); // Left-clone stream does not gain the gap.
        s.move(10000,-10000);near(1573,s.x);near(0,s.y);
        s.start(1200,900,300,600,787,981,1768,2208);s.move(-10000,10000);near(194,s.x);near(2207,s.y);
        System.out.println("TouchpadScrollTest passed: horizontal, vertical, diagonal, reversal, fixed clone transforms and bounds");
    }
}
