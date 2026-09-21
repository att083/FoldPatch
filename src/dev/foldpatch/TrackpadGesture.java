package dev.foldpatch;

/** Gesture recognition in one logical screen space. No Android or window dependencies. */
final class TrackpadGesture {
    interface Sink {void pointer(float x,float y);void touch(int action,int count,float x,float y,float x2,float y2,long down,long time);void hold();}
    private final Sink sink;private final float width,height,min,max,speed,slop;
    float x,y;private float fx,fy,startX,startY,cx,cy,span,vx,vy,anchorX,anchorY,tx,ty,pinchRadius;
    private long started,streamDown,lastTap=-10000;private boolean moved,down,two,ignore;
    private int mode; // 0 pending/moving, 1 drag, 2 classify, 3 scroll, 4 pinch/rotate
    TrackpadGesture(Sink s,float width,float height,float min,float max,float speed,float slop,float x,float y){sink=s;this.width=width;this.height=height;this.min=min;this.max=max;this.speed=speed;this.slop=slop;this.x=x;this.y=y;}
    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    private void emit(int action,int count,long now){sink.touch(action,count,tx,ty,anchorX,anchorY,streamDown,now);}
    private void begin(long now){tx=x;ty=y;streamDown=now;down=true;emit(0,1,now);}
    void cancel(long now){if(down)emit(3,mode==4?2:1,now);down=false;mode=0;ignore=true;lastTap=-10000;}
    void hold(long now){if(!ignore&&!two&&!moved&&mode==0&&now-started>=350){mode=1;begin(now);sink.hold();}}
    void event(int action,int count,float a,float b,float c,float d,long now){
        if(action==0){if(down)cancel(now);ignore=false;two=false;moved=false;mode=0;started=now;fx=startX=a;fy=startY=b;
            if(now-lastTap<=300){mode=1;begin(now);lastTap=-10000;}return;}
        if(ignore)return;
        if(action==3||count>2){cancel(now);return;}
        if(action==5&&count==2){if(down)emit(3,1,now);down=false;two=true;mode=2;moved=true;lastTap=-10000;
            cx=(a+c)/2;cy=(b+d)/2;vx=c-a;vy=d-b;span=Math.max(1,(float)Math.hypot(vx,vy));anchorX=x;anchorY=y;return;}
        if(action==2){
            if(two&&count==2){float ncx=(a+c)/2,ncy=(b+d)/2,nvx=c-a,nvy=d-b,nspan=Math.max(1,(float)Math.hypot(nvx,nvy));
                float delta=(float)Math.hypot(ncx-cx,ncy-cy),spread=Math.abs(nspan-span);
                if(mode==2){
                    if(Math.max(delta,spread)<slop)return;
                    if(spread>slop&&spread>delta*.7f){
                        mode=4;float room=Math.min(Math.min(x,width-1-x),Math.min(y,height-1-y));
                        pinchRadius=Math.min(span/2,room*.6f);
                        if(pinchRadius<3){ignore=true;return;}
                        float ux=vx/span,uy=vy/span;tx=x-ux*pinchRadius;ty=y-uy*pinchRadius;anchorX=x+ux*pinchRadius;anchorY=y+uy*pinchRadius;
                        streamDown=now;down=true;emit(0,1,now);emit(261,2,now);
                    }else {mode=3;begin(now);}
                }
                if(mode==3){tx=clamp(x+ncx-cx,0,width-1);ty=clamp(y+ncy-cy,0,height-1);emit(2,1,now);}
                else if(mode==4){float r=pinchRadius*nspan/span,ux=nvx/nspan,uy=nvy/nspan;
                    float room=Math.min(Math.min(x,width-1-x)/Math.max(.001f,Math.abs(ux)),Math.min(y,height-1-y)/Math.max(.001f,Math.abs(uy)));
                    r=clamp(r,2,room);tx=x-ux*r;ty=y-uy*r;anchorX=x+ux*r;anchorY=y+uy*r;emit(2,2,now);}
            }else if(!two){
                if(Math.hypot(a-startX,b-startY)>slop)moved=true;
                x=clamp(x+(a-fx)*speed,min,max);y=clamp(y+(b-fy)*speed,0,height-1);fx=a;fy=b;sink.pointer(x,y);
                if(mode==1){tx=x;ty=y;emit(2,1,now);}
            }return;
        }
        if(action==6&&two){if(down){if(mode==4){emit(262,2,now);emit(1,1,now);}else emit(1,1,now);}down=false;ignore=true;return;}
        if(action==1){if(down){emit(1,1,now);down=false;}else if(!two&&!moved){begin(now);emit(1,1,now);down=false;lastTap=now;}ignore=true;}
    }
}
