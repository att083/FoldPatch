package dev.foldpatch;

/** Verified display-area shapes, independent of Android version numbers. */
final class ReflowPolicy {
    enum Path {
        LEGACY(4,"WindowedMagnification:"), COMBINED(3,"OneHanded:0:23");
        final int feature; final String area;
        Path(int feature,String area){this.feature=feature;this.area=area;}
    }
    static Path select(String dump) {
        if(combinedShape(dump)){requireFree(dump,3);return Path.COMBINED;}
        if(has(dump,"WindowedMagnification:")&&has(dump,"OneHanded:15:15")&&has(dump,"OneHanded:17:17")){
            requireFree(dump,4);requireFree(dump,3);return Path.LEGACY;
        }
        throw new IllegalStateException("지원하지 않는 화면 영역 구성이에요");
    }
    static boolean has(String dump,String name){return dump.contains("* "+name);}
    static boolean combinedShape(String dump){
        // Only accept the observed app + IME + status + shade subtree. A matching name
        // elsewhere or a vendor policy with different children is not sufficient.
        boolean inside=false,app=false,ime=false,status=false,shade=false;int indent=0,count=0;
        for(String line:dump.split("\n")){
            int star=line.indexOf("* ");
            if(star<0)continue;
            if(inside&&star<=indent)inside=false;
            if(line.substring(star).matches("\\* OneHanded:0:23(?: \\(organized\\))?\\s*")){
                inside=true;indent=star;count++;continue;
            }
            if(inside){app|=line.contains("* DefaultTaskDisplayArea");ime|=line.contains("* ImeContainer");status|=line.contains("* Leaf:15:15");shade|=line.contains("* Leaf:17:19");}
        }
        return count==1&&app&&ime&&status&&shade;
    }
    static void requireFree(String dump,int feature){
        String name=feature==3?"OneHanded:":"WindowedMagnification:";boolean found=false;
        for(String line:dump.split("\n"))if(line.contains("* "+name)){
            found=true;if(line.contains("(organized)"))throw new IllegalStateException("화면 영역을 다른 시스템 기능이 사용 중이에요");
        }
        if(!found)throw new IllegalStateException("지원하지 않는 화면 영역 구성이에요");
    }
    static final class Saved {
        final Path path;final int width,height,usable;
        Saved(Path path,int width,int height,int usable){
            if(width<=0||height<=0||usable<=0||usable>=width)throw new IllegalArgumentException("Invalid recovery dimensions");
            this.path=path;this.width=width;this.height=height;this.usable=usable;
        }
        String encode(){return "v2,"+path.name()+","+width+","+height+","+usable+"\n";}
        static Saved parse(String value){
            if(value==null)throw new IllegalArgumentException("Empty recovery record");
            String[] p=value.trim().split(",",-1);
            if(p.length==3)return new Saved(Path.LEGACY,Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]));
            if(p.length!=5||!p[0].equals("v2"))throw new IllegalArgumentException("Unknown recovery record");
            return new Saved(Path.valueOf(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]));
        }
    }
}
