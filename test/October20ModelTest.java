package nro.models.event.october20;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.google.gson.*;
import nro.models.data.LocalManager;
public class October20ModelTest {
 static int checks;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   ok("nro_october20_top_20261018".equals(c.getCatalog()),"isolated model QA guard");
   JsonObject plan=new JsonParser().parse(Files.readString(Path.of("model-plan.json"))).getAsJsonObject();
   for(JsonElement value:plan.getAsJsonArray("models")){
    JsonObject model=value.getAsJsonObject();int item=model.get("item").getAsInt();
    for(JsonElement pv:model.getAsJsonArray("parts")){
     JsonObject part=pv.getAsJsonObject();int id=part.get("id").getAsInt();JsonArray original=new JsonParser().parse(part.get("original").getAsString()).getAsJsonArray();
     try(PreparedStatement q=c.prepareStatement("SELECT DATA,TYPE FROM part WHERE id=?")){q.setInt(1,id);try(ResultSet r=q.executeQuery()){ok(r.next(),"part exists");JsonArray frames=new JsonParser().parse(r.getString(1)).getAsJsonArray();ok(frames.size()==original.size(),"native frame count unchanged");ok(r.getString(1).equals(part.get("proposed").getAsString()),"only staged QA part used");
      for(JsonElement frame:frames){JsonArray row=frame.getAsJsonArray();int image=row.get(0).getAsInt();ok(image>=0&&image<32767&&Math.abs(row.get(1).getAsInt())<128&&Math.abs(row.get(2).getAsInt())<128,"native short image and byte offsets valid");if(image==2955)continue;
       for(int zoom=1;zoom<=4;zoom++){Path file=Path.of("data/icon/x"+zoom,image+".png");var png=ImageIO.read(file.toFile());ok(png!=null&&png.getWidth()==96*zoom&&png.getHeight()==80*zoom,"model PNG decodes at correct zoom");int visible=0;for(int y=0;y<png.getHeight();y+=zoom)for(int x=0;x<png.getWidth();x+=zoom)if((png.getRGB(x,y)>>>24)>64)visible++;ok(visible>100,"body frame is visible");ok((png.getRGB(0,0)>>>24)<16,"corner remains transparent");}
      }
     }}
     try(PreparedStatement q=c.prepareStatement("SELECT DATA FROM srcnrofree.part WHERE id=?")){q.setInt(1,id);try(ResultSet r=q.executeQuery()){ok(r.next()&&r.getString(1).equals(part.get("original").getAsString()),"production part remains unchanged before approval");}}
    }
    for(int zoom=1;zoom<=4;zoom++){var png=ImageIO.read(Path.of("data/icon/x"+zoom,model.get("icon").getAsInt()+".png").toFile());ok(png!=null&&png.getWidth()==32*zoom&&png.getHeight()==32*zoom,"costume portrait valid");}
    System.out.println("PASS: model "+item+" six-part/native-image checks remain isolated");
   }
   for(int id:new int[]{17740,17710})for(int zoom=1;zoom<=4;zoom++){var png=ImageIO.read(Path.of("data/icon/x"+zoom,id+".png").toFile());ok(png!=null&&png.getWidth()>0&&png.getHeight()>0,"new box icons decode");}
   System.out.println("PASS: "+checks+" model frame/PNG/scope checks; visual client fit still needs review");
  }finally{LocalManager.close();}
 }
}
