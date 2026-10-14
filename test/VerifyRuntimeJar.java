import java.nio.file.*;
import java.net.*;
import java.util.zip.*;
public class VerifyRuntimeJar {
    public static void main(String[] args)throws Exception {
        Path jar=Path.of(args[0]);int count=0;
        try(ZipFile zip=new ZipFile(jar.toFile());URLClassLoader loader=new URLClassLoader(new URL[]{jar.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
            if(zip.getEntry("nro/models/consts/ConstIgnoreName.class")==null)throw new AssertionError("Missing ConstIgnoreName");
            for(java.util.Enumeration<? extends ZipEntry> entries=zip.entries();entries.hasMoreElements();){String name=entries.nextElement().getName();if(name.startsWith("nro/")&&name.endsWith(".class")){Class.forName(name.substring(0,name.length()-6).replace('/','.'),false,loader);count++;}}
        }
        System.out.println("PASS: ConstIgnoreName exists; "+count+" application classes load from packaged JAR without starting the game.");
    }
}
