package com.lsaegis.eoms.web;
import java.net.URI;
/** Platform-independent chooser boundaries, also exercised by the JVM regression. */
public final class ImageSelectionPolicy {
 private ImageSelectionPolicy() { }
 public static boolean sameOrigin(String current, String expected) {
  try {
   URI a=URI.create(current), b=URI.create(expected);
   if(a.getUserInfo()!=null || b.getUserInfo()!=null || b.getHost()==null) return false;
   if(!"http".equalsIgnoreCase(b.getScheme()) && !"https".equalsIgnoreCase(b.getScheme())) return false;
   return b.getScheme().equalsIgnoreCase(a.getScheme()) && b.getHost().equalsIgnoreCase(a.getHost()) && port(a)==port(b);
  }catch(RuntimeException ex){return false;}
 }
 private static int port(URI u){return u.getPort()>=0?u.getPort():"https".equalsIgnoreCase(u.getScheme())?443:80;}
 public static int selectionLimit(String value, boolean multiple){
  if(!multiple)return 1;
  try{return Math.max(0,Math.min(9,Integer.parseInt(value)));}catch(RuntimeException ex){return 9;}
 }
 public static boolean supportedHeader(byte[] b){
  if(b==null)return false;
  if(b.length>=3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255)return true;
  int[] png={137,80,78,71,13,10,26,10};if(b.length<png.length)return false;
  for(int i=0;i<png.length;i++)if((b[i]&255)!=png[i])return false;return true;
 }
}
