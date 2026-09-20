package dev.reachpad;
public final class HangulComposerTest {
 static void equal(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
 static String compose(String keys){HangulComposer c=new HangulComposer();String s="";for(char k:keys.toCharArray())s+=c.add(k);return s+c.text();}
 public static void main(String[] args){
  equal("한글",compose("ㅎㅏㄴㄱㅡㄹ"));
  equal("안녕하세요",compose("ㅇㅏㄴㄴㅕㅇㅎㅏㅅㅔㅇㅛ"));
  equal("읽어",compose("ㅇㅣㄹㄱㅇㅓ"));equal("일거",compose("ㅇㅣㄹㄱㅓ"));
  equal("과일",compose("ㄱㅗㅏㅇㅣㄹ"));equal("괜찮아",compose("ㄱㅗㅐㄴㅊㅏㄴㅎㅇㅏ"));
  equal("값이",compose("ㄱㅏㅂㅅㅇㅣ"));equal("갑시",compose("ㄱㅏㅂㅅㅣ"));
  equal("까",compose("ㄲㅏ"));equal("ㅏ",compose("ㅏ"));
  HangulComposer c=new HangulComposer();for(char k:"ㄱㅗㅏㄴ".toCharArray())c.add(k);
  equal("관",c.text());c.backspace();equal("과",c.text());c.backspace();equal("고",c.text());c.backspace();equal("ㄱ",c.text());c.backspace();equal("",c.text());if(c.backspace())throw new AssertionError();
  System.out.println("Hangul composition and deletion checks passed");
 }
}
