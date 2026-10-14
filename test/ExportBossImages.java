import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.google.gson.GsonBuilder;
import nro.models.boss.*;

/** Extract only visual metadata; never construct Bosses or start the game/database. */
public class ExportBossImages {
    static final Map<String,Set<Integer>> names=new TreeMap<>();
    static final Map<String,Set<Integer>> locations=new TreeMap<>();
    static void name(String name,int head){names.computeIfAbsent(name.trim().toLowerCase(Locale.ROOT),k->new HashSet<>()).add(head);}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);Map<String,Integer> byKey=new TreeMap<>(),byClass=new HashMap<>();Map<String,String> sources=new HashMap<>();
        for(Field f:BossesData.class.getFields())if(f.getType()==BossData.class){BossData d=(BossData)f.get(null);name(d.getName(),d.getOutfit()[0]);String location=d.getName().trim().toLowerCase(Locale.ROOT)+"|"+Arrays.toString(d.getMapJoin()).replace(" ","");locations.computeIfAbsent(location,k->new HashSet<>()).add((int)d.getOutfit()[0]);}
        try(java.util.stream.Stream<Path> files=Files.walk(root.resolve("src/nro/models/boss"))){for(Path file:(Iterable<Path>)files.filter(p->p.toString().endsWith(".java"))::iterator){String text=Files.readString(file);Matcher pkg=Pattern.compile("package\\s+([\\w.]+)").matcher(text);if(!pkg.find())continue;String cls=pkg.group(1)+"."+file.getFileName().toString().replace(".java","");sources.put(cls,text);Matcher call=Pattern.compile("super\\s*\\(([\\s\\S]*?)\\);",Pattern.MULTILINE).matcher(text);if(!call.find())continue;String body=call.group(1);Integer head=null;
            Matcher reference=Pattern.compile("BossesData\\.(\\w+)").matcher(body);if(reference.find())try{head=((BossData)BossesData.class.getField(reference.group(1)).get(null)).getOutfit()[0]+0;}catch(NoSuchFieldException ignored){}
            if(head==null){Matcher inline=Pattern.compile("new\\s+short\\[\\]\\s*\\{\\s*(\\d+)").matcher(body);if(inline.find())head=Integer.parseInt(inline.group(1));}
            if(head!=null){byClass.put(cls,head);byKey.put(cls,head);Matcher inlineName=Pattern.compile("new\\s+BossData\\s*\\(\\s*\"([^\"]+)\"").matcher(body);if(inlineName.find())name(inlineName.group(1),head);}
        }}
        for(Map.Entry<String,String> source:sources.entrySet()){if(!source.getKey().contains("Boss_Manager."))continue;String manager=source.getKey().substring(source.getKey().lastIndexOf('.')+1);Map<String,String> imports=new HashMap<>();Matcher imp=Pattern.compile("import\\s+([\\w.]+);" ).matcher(source.getValue());while(imp.find()){String value=imp.group(1);imports.put(value.substring(value.lastIndexOf('.')+1),value);}
            Matcher cases=Pattern.compile("case\\s+BossID\\.(\\w+)\\s*->\\s*new\\s+(\\w+)").matcher(source.getValue());while(cases.find()){String className=imports.get(cases.group(2));Integer head=byClass.get(className);if(head!=null)try{int id=BossID.class.getField(cases.group(1)).getInt(null);byKey.put(manager+":"+id,head);}catch(NoSuchFieldException ignored){}}
        }
        Map<String,Integer> byName=new TreeMap<>();for(Map.Entry<String,Set<Integer>> entry:names.entrySet())if(entry.getValue().size()==1)byName.put(entry.getKey(),entry.getValue().iterator().next());
        Map<String,Integer> byNameMap=new TreeMap<>();for(Map.Entry<String,Set<Integer>> entry:locations.entrySet())if(entry.getValue().size()==1)byNameMap.put(entry.getKey(),entry.getValue().iterator().next());
        Map<String,Object> output=new LinkedHashMap<>();output.put("by_key",byKey);output.put("by_name_map",byNameMap);output.put("by_name",byName);Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(output));System.out.println("Exported "+byKey.size()+" Boss mappings and "+byName.size()+" unambiguous visual names.");
    }
}
