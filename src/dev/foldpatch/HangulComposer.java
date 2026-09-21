package dev.foldpatch;

/** Stateful two-beolsik composition; no dictionary, telemetry, or stored typed text. */
final class HangulComposer {
    static final String INITIAL="ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    static final String VOWEL="ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
    static final String FINAL=" ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
    private int l=-1,v=-1,t=0;
    String text(){if(l>=0&&v>=0)return String.valueOf((char)(0xac00+(l*21+v)*28+t));if(l>=0)return ""+INITIAL.charAt(l);return v>=0?""+VOWEL.charAt(v):"";}
    void clear(){l=v=-1;t=0;}
    private static final String[] VPAIRS={"ㅗㅏㅘ","ㅗㅐㅙ","ㅗㅣㅚ","ㅜㅓㅝ","ㅜㅔㅞ","ㅜㅣㅟ","ㅡㅣㅢ"};
    private static final String[] TPAIRS={"ㄱㅅㄳ","ㄴㅈㄵ","ㄴㅎㄶ","ㄹㄱㄺ","ㄹㅁㄻ","ㄹㅂㄼ","ㄹㅅㄽ","ㄹㅌㄾ","ㄹㅍㄿ","ㄹㅎㅀ","ㅂㅅㅄ"};
    private static char combine(String[] pairs,char a,char b){for(String p:pairs)if(p.charAt(0)==a&&p.charAt(1)==b)return p.charAt(2);return 0;}
    private static String split(String[] pairs,char c){for(String p:pairs)if(p.charAt(2)==c)return p.substring(0,2);return ""+c;}
    /** Returns text to commit BEFORE the new composing fragment. */
    String add(char c){
        int nv=VOWEL.indexOf(c),nl=INITIAL.indexOf(c);String done="";
        if(nv>=0){
            if(v<0){v=nv;return "";}
            if(t>0){
                String parts=split(TPAIRS,FINAL.charAt(t));t=parts.length()==2?FINAL.indexOf(parts.charAt(0)):0;
                done=text();l=INITIAL.indexOf(parts.charAt(parts.length()-1));v=nv;t=0;return done;
            }
            char joint=combine(VPAIRS,VOWEL.charAt(v),c);
            if(joint!=0){v=VOWEL.indexOf(joint);return "";}
            done=text();clear();v=nv;return done;
        }
        if(nl>=0){
            if(l<0&&v<0){l=nl;return "";}
            int nt=FINAL.indexOf(c);
            if(l>=0&&v>=0&&nt>0){
                if(t==0){t=nt;return "";}
                char joint=combine(TPAIRS,FINAL.charAt(t),c);if(joint!=0){t=FINAL.indexOf(joint);return "";}
            }
            done=text();clear();l=nl;return done;
        }
        done=text()+c;clear();return done;
    }
    boolean backspace(){
        if(t>0){String parts=split(TPAIRS,FINAL.charAt(t));t=parts.length()==2?FINAL.indexOf(parts.charAt(0)):0;return true;}
        if(v>=0){String parts=split(VPAIRS,VOWEL.charAt(v));v=parts.length()==2?VOWEL.indexOf(parts.charAt(0)):-1;return true;}
        if(l>=0){l=-1;return true;}return false;
    }
}
