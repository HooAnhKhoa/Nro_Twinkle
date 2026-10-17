package nro.models.data;
import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
import nro.models.network.*;import nro.models.player.Player;import nro.models.server.Controller;
/** Real ready handler with a spy at the character-start boundary; no server listener or live account. */
public final class ClientReadyHandshakeTest {
 static int checks;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS: "+s);}
 static class Socket extends java.net.Socket {
  public SocketAddress getRemoteSocketAddress(){return new InetSocketAddress("127.0.0.1",1);}
  public InetAddress getInetAddress(){return InetAddress.getLoopbackAddress();}
  public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}
  public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
 }
 static class Session extends MySession {
  int packets,starts;boolean fail;final List<byte[]> catalog=new ArrayList<>();
  Session(){super(new Socket());player=new Player();player.isPlayer=true;player.id=123;}
  public void doSendMessage(Message m)throws Exception{byte[] b=m.getData();if(m.command==-28&&b.length>=3&&b[0]==8&&(b[2]==1||b[2]==2)){if(fail&&b[2]==2)throw new IOException("Expected QA catalog transmission failure");packets++;catalog.add(b.clone());}}
  @Override public boolean beginCharacterLoad(){if(super.beginCharacterLoad())starts++;return false;}
 }
 static void ready(Session s)throws Exception{Controller.gI().messageNotMap(s,new Message((byte)-28,new byte[]{13}));}
 public static void run()throws Exception{
  Session cached=new Session();ready(cached);
  check(cached.hasItemCatalog()&&cached.packets==3,"cached login receives complete bounded catalog once");
  check(cached.starts==0,"character does not start before replay acknowledgement");
  ready(cached);check(cached.starts==1&&cached.packets==3,"next ready acknowledges replay and starts character once");
  for(int i=0;i<100;i++)ready(cached);
  check(cached.starts==1&&cached.packets==3,"100 duplicate ready signals never restart loading or replay templates");
  boolean[] present=new boolean[2103];for(byte[] b:cached.catalog)ItemTemplatePacketTest.apply(Arrays.copyOfRange(b,1,b.length),present);
  check(java.util.stream.IntStream.range(0,present.length).allMatch(i->present[i]),"ready-handler catalog contains every item template");
  Session fresh=new Session();ItemData.updateItem(fresh);ready(fresh);ready(fresh);
  check(fresh.starts==1&&fresh.packets==3,"fresh version download is reused without an extra replay");
  Session failed=new Session();failed.fail=true;ready(failed);
  check(!failed.hasItemCatalog()&&failed.starts==0,"partial catalog cannot mark ready or start character");
  failed.fail=false;ready(failed);check(failed.hasItemCatalog()&&failed.starts==0,"successful retry still waits for its acknowledgement");
  ready(failed);check(failed.starts==1,"acknowledgement after retry starts character only once");
  MySession gate=new MySession(new Socket());check(!gate.beginCharacterLoad(),"gate blocks character before catalog sent");gate.markItemCatalogSent();
  var workers=Executors.newFixedThreadPool(4);try{List<Future<Boolean>> results=new ArrayList<>();for(int i=0;i<20;i++)results.add(workers.submit(gate::beginCharacterLoad));int starts=0;for(var f:results)if(f.get())starts++;check(starts==1,"concurrent duplicate ready has a single winner");}finally{workers.shutdownNow();}
  Session reconnect=new Session();ready(reconnect);check(reconnect.packets==3&&reconnect.starts==0,"new connection gets independent synchronization state");
  System.out.println("PASS: "+checks+" client-ready handshake checks");
 }
}
