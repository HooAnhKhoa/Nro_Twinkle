package nro.models.data;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import nro.models.server.Manager;
import nro.models.network.*;
import nro.models.interfaces.ISession;
public class ItemTemplatePacketTest {
    static int checks;
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);checks++;System.out.println("PASS: "+message);}
    static void apply(byte[] cached,boolean[] present)throws Exception{
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(cached))){in.readUnsignedByte();int mode=in.readUnsignedByte(),start=0,end;
            if(mode==1){Arrays.fill(present,false);end=in.readUnsignedShort();}else if(mode==2){start=in.readUnsignedShort();end=in.readUnsignedShort();}else throw new AssertionError("Unexpected cache mode");
            for(int i=start;i<end;i++){in.readByte();in.readByte();in.readUTF();in.readUTF();in.readByte();in.readInt();in.readShort();in.readShort();in.readBoolean();present[i]=true;}
            if(in.available()!=0)throw new AssertionError("Cached packet has trailing bytes");
        }
    }
    public static void main(String[] args)throws Exception{
        try{
            try(var c=LocalManager.getConnection()){check("nro_teamobi_qa_20261017_v3".equals(c.getCatalog()),"isolated canonical QA database");}
            Manager.gI();
            List<Message> messages=ItemData.itemTemplateMessages();
            check(messages.size()>=3,"oversized legacy append split into multiple packets");
            List<byte[]> savedPayloads=new ArrayList<>();for(Message m:messages)savedPayloads.add(m.getData().clone());
            int next=0,total=0,max=0;
            ISession plain=(ISession)Proxy.newProxyInstance(ISession.class.getClassLoader(),new Class[]{ISession.class},(p,m,a)->m.getName().equals("sentKey")?false:null);
            for(Message msg:messages){
                byte[] payload=msg.getData();max=Math.max(max,payload.length);
                check(payload.length<=60000,"actual item payload fits two-byte frame: "+payload.length);
                ByteArrayOutputStream wire=new ByteArrayOutputStream();
                new MessageSendCollect().doSendMessage(plain,new DataOutputStream(wire),msg);
                DataInputStream framed=new DataInputStream(new ByteArrayInputStream(wire.toByteArray()));
                check(framed.readByte()==-28&&framed.readUnsignedShort()==payload.length,"real sender frame length matches all payload bytes");
                check(Arrays.equals(framed.readAllBytes(),payload),"real sender payload has no overflow or extra bytes");
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload));
                check(in.readUnsignedByte()==8&&in.readByte()==DataGame.vsItem,"native item command and refreshed cache version");
                int mode=in.readUnsignedByte(),start=0,end;
                if(mode==1){check(next==0,"reset packet first");end=in.readUnsignedShort();}
                else{check(mode==2,"subsequent packets use native append");start=in.readUnsignedShort();end=in.readUnsignedShort();}
                check(start==next&&end>start&&end<=Manager.ITEM_TEMPLATES.size(),"contiguous client append range "+start+".."+end);
                for(int i=start;i<end;i++){
                    var item=Manager.ITEM_TEMPLATES.get(i);
                    if(in.readByte()!=item.type||in.readByte()!=item.gender||!in.readUTF().equals(item.name)||!in.readUTF().equals(item.description)||in.readByte()!=item.level||in.readInt()!=item.strRequire||in.readShort()!=item.iconID||in.readShort()!=item.part||in.readBoolean()!=item.isUpToUp)throw new AssertionError("Template altered "+i);
                    total++;
                }
                check(in.available()==0,"client decoder consumes packet exactly");
                next=end;
            }
            check(total==2103&&next==2103,"all 2103 items reconstructed exactly without gaps or duplicates");
            check(DataGame.vsItem>=16,"incomplete disk cache version 15 forced to refresh");
            Map<Integer,byte[]> disk=new HashMap<>();
            for(byte[] payload:savedPayloads)disk.put((int)payload[2],Arrays.copyOfRange(payload,1,payload.length));
            boolean[] cached=new boolean[2103];apply(disk.get(1),cached);apply(disk.get(2),cached);
            check(!cached[1089]&&cached[1632],"two-slot client cache reproduces missing Inosuke even while Himmel exists");
            check(java.util.stream.IntStream.range(0,cached.length).filter(i->cached[i]).count()<2103,"disk reload alone does not reconstruct all templates");
            for(byte[] payload:savedPayloads)apply(Arrays.copyOfRange(payload,1,payload.length),cached);
            check(java.util.stream.IntStream.range(0,cached.length).allMatch(i->cached[i]),"ready-phase replay repairs every cached hole before character parsing");
            check(cached[1089]&&cached[1632]&&cached[2078],"Inosuke, Himmel and new event boxes all available after replay");
            if(java.nio.file.Files.exists(java.nio.file.Path.of("client-NRitem1"))){boolean[] actual=new boolean[2103];apply(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("client-NRitem1")),actual);apply(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("client-NRitem2")),actual);check(!actual[1089]&&actual[1632],"read-only snapshot of actual Unity cache reproduces reported gap");for(byte[] payload:savedPayloads)apply(Arrays.copyOfRange(payload,1,payload.length),actual);check(java.util.stream.IntStream.range(0,actual.length).allMatch(i->actual[i]),"same replay repairs actual client cache snapshot completely");}
            ClientReadyHandshakeTest.run();
            var first=Manager.ITEM_TEMPLATES.get(0);String original=first.description;
            try{first.description="x".repeat(60000);boolean rejected=false;try{ItemData.itemTemplateMessages();}catch(IOException expected){rejected=true;}check(rejected,"single oversized item rejected before sending partial templates");}
            finally{first.description=original;}
            System.out.println("PASS: "+checks+" packet checks; max payload "+max+" bytes");
        }finally{LocalManager.close();}
        System.exit(0);
    }
}
