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
    public static void main(String[] args)throws Exception{
        try{
            try(var c=LocalManager.getConnection()){check("nro_teamobi_qa_20261017_v3".equals(c.getCatalog()),"isolated canonical QA database");}
            Manager.gI();
            List<Message> messages=ItemData.itemTemplateMessages();
            check(messages.size()>=3,"oversized legacy append split into multiple packets");
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
            check(DataGame.vsItem>=14,"client with broken version 12 forced to reload");
            var first=Manager.ITEM_TEMPLATES.get(0);String original=first.description;
            try{first.description="x".repeat(60000);boolean rejected=false;try{ItemData.itemTemplateMessages();}catch(IOException expected){rejected=true;}check(rejected,"single oversized item rejected before sending partial templates");}
            finally{first.description=original;}
            System.out.println("PASS: "+checks+" packet checks; max payload "+max+" bytes");
        }finally{LocalManager.close();}
        System.exit(0);
    }
}
