package dev.foldpatch;

/** Two-finger motion stays in the input transform selected at ACTION_DOWN.
 * Do not jump across the physical gap mid-gesture: Android keeps the initial clone's transform. */
final class TouchpadScroll {
    float x,y;
    private float lastX,lastY,minX,maxX,maxY;
    void start(float pointerX,float pointerY,float centroidX,float centroidY,int left,int right,int width,int height){
        int gap=right-left;
        minX=pointerX>=right?gap:0;
        maxX=minX+width-gap-1;maxY=height-1;
        x=pointerX;y=pointerY;lastX=centroidX;lastY=centroidY;
    }
    void move(float centroidX,float centroidY){
        x=Math.max(minX,Math.min(maxX,x+centroidX-lastX));
        y=Math.max(0,Math.min(maxY,y+centroidY-lastY));
        lastX=centroidX;lastY=centroidY;
    }
}
