package dev.foldpatch;

public final class ReflowPolicyTest {
    private static final String LEGACY="    * WindowedMagnification:0:31\n      * OneHanded:17:17\n      * OneHanded:15:15\n";
    private static final String COMBINED="    * WindowedMagnification:0:31 (organized)\n      * OneHanded:0:23\n        * Leaf:17:19\n        * Leaf:15:15\n        * AppZoomOut:2:14 (organized)\n          * ImeContainer\n          * DefaultTaskDisplayArea (organized)\n    * Leaf:24:25\n";
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    private static void reject(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Unsafe policy accepted");}
    public static void main(String[] args){
        check(ReflowPolicy.select(LEGACY)==ReflowPolicy.Path.LEGACY);
        check(ReflowPolicy.select(COMBINED)==ReflowPolicy.Path.COMBINED);
        // System-owned feature 4 is allowed only through the verified alternative tree.
        reject(()->ReflowPolicy.select(LEGACY.replace("0:31","0:31 (organized)")));
        reject(()->ReflowPolicy.select(COMBINED.replace("0:23","0:23 (organized)")));
        reject(()->ReflowPolicy.select(COMBINED+"    * OneHanded:32:32 (organized)\n"));
        reject(()->ReflowPolicy.select(COMBINED.replace("Leaf:17:19","Leaf:17:18")));
        reject(()->ReflowPolicy.select(COMBINED.replace("          * DefaultTaskDisplayArea","    * DefaultTaskDisplayArea")));
        reject(()->ReflowPolicy.select(COMBINED+COMBINED));
        reject(()->ReflowPolicy.select(""));
        ReflowPolicy.Saved old=ReflowPolicy.Saved.parse("1768,2208,1592");
        check(old.path==ReflowPolicy.Path.LEGACY&&old.usable==1592);
        for(ReflowPolicy.Path p:ReflowPolicy.Path.values()){
            ReflowPolicy.Saved saved=ReflowPolicy.Saved.parse(new ReflowPolicy.Saved(p,1768,2208,1592).encode());
            check(saved.path==p&&saved.width==1768&&saved.height==2208&&saved.usable==1592);
        }
        reject(()->ReflowPolicy.Saved.parse("v3,COMBINED,1768,2208,1592"));
        reject(()->ReflowPolicy.Saved.parse("v2,UNKNOWN,1768,2208,1592"));
        reject(()->ReflowPolicy.Saved.parse("1768,2208,1768"));
        reject(()->ReflowPolicy.Saved.parse("1768,2208,0"));
        reject(()->ReflowPolicy.Saved.parse(null));
        System.out.println("ReflowPolicyTest passed: structure, occupied areas, legacy and versioned recovery");
    }
}
