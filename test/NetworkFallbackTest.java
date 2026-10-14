import java.net.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import nro.models.network.*;
import nro.models.interfaces.*;
public class NetworkFallbackTest {
    public static void main(String[] args)throws Exception{
        Network network=Network.gI();network.init();
        Field selector=Network.class.getDeclaredField("selector");selector.setAccessible(true);
        java.nio.channels.Selector original=(java.nio.channels.Selector)selector.get(network);
        if(original!=null)original.close();selector.set(network,null);
        CountDownLatch accepted=new CountDownLatch(1);
        network.setAcceptHandler(new ISessionAcceptHandler(){
            public void sessionInit(ISession session){accepted.countDown();}
            public void sessionDisconnect(ISession session){}
        }).setTypeSessioClone(Session.class).start(0);
        Field fallback=Network.class.getDeclaredField("fallbackSocket");fallback.setAccessible(true);
        ServerSocket server=(ServerSocket)fallback.get(network);
        try(Socket client=new Socket("127.0.0.1",server.getLocalPort())){
            if(!accepted.await(5,TimeUnit.SECONDS))throw new AssertionError("Connection not accepted");
            System.out.println("PASS: fallback accepts and initializes a session without Selector");
        }finally{network.close();}
        Field loop=Network.class.getDeclaredField("loopServer");loop.setAccessible(true);Thread thread=(Thread)loop.get(network);thread.join(2000);
        if(thread.isAlive())throw new AssertionError("Network thread did not stop");
        System.out.println("PASS: fallback network closes cleanly");
    }
}
