package nro.models.event.trungthu2026;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One immutable configuration snapshot per process; invalid settings fail startup. */
public final class TrungThuConfig {
    private static final Properties VALUES=read(Path.of("config/trung-thu-2026.properties"));
    private static Properties read(Path path){
        Properties values=new Properties();
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){values.load(reader);}
        catch(IOException error){throw new IllegalStateException("Không đọc được cấu hình Trung Thu",error);}
        return values;
    }
    public static String text(String key){String value=VALUES.getProperty(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Thiếu cấu hình "+key);return value.trim();}
    public static int number(String key){int value=Integer.parseInt(text(key));if(value<0)throw new IllegalArgumentException(key);return value;}
    public static int item(String key){return number("item."+key);}
    public static int[] numbers(String key){return Arrays.stream(text(key).split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();}
    public static boolean enabled(){return nro.models.event.EventManager.enabled("trung_thu");}
    public static void validate(){
        if(!"manual".equals(text("control.mode")))throw new IllegalArgumentException("Đợt thử Trung Thu chỉ hỗ trợ bật/tắt thủ công");
        for(String key:VALUES.stringPropertyNames()){
            if(key.endsWith("-percent")&&number(key)>100)throw new IllegalArgumentException("Tỷ lệ ngoài 0–100: "+key);
            if(key.startsWith("item."))for(int id:numbers(key)){
                var template=nro.models.services.ItemService.gI().getTemplate(id);
                if(template==null||template.id!=id)throw new IllegalArgumentException("Sai template "+key+"="+id);
            }
        }
        for(String key:List.of("tails","carrots","lantern","normal-box","vegetarian-box"))for(var entry:TrungThuRewards.table(key)){
            if(!Set.of("gold","gems","ruby","stones","stars").contains(entry.kind()))item(entry.kind());
        }
        TrungThuRewards.petDays(new Random(0));
    }
    private TrungThuConfig(){}
}
