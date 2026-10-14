import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import com.google.gson.Gson;
import nro.models.boss.*;
public class ExportBossFixtures {
    public static void main(String[] args)throws Exception {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(Field f:BossesData.class.getFields())if(f.getType()==BossData.class){BossData d=(BossData)f.get(null);Map<String,Object> r=new LinkedHashMap<>();r.put("name",d.getName());r.put("hp",d.getHp()[0]);r.put("damage",Math.max(0,d.getDame()));r.put("rest",Math.max(0,d.getSecondsRest()));r.put("head",d.getOutfit()[0]);r.put("maps",Arrays.toString(d.getMapJoin()));rows.add(r);if(rows.size()==89)break;}
        Files.writeString(Path.of(args[0]),new Gson().toJson(rows));System.out.println("Exported "+rows.size()+" actual BossData templates for isolated UI fixtures.");
    }
}
