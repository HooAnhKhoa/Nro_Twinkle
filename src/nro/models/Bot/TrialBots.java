package nro.models.Bot;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import nro.models.server.ServerManager;
import nro.models.server.Maintenance;
import nro.models.utils.Logger;

/** Three-bot trial on one bounded loop. Configuration and status are local files, not player records. */
public final class TrialBots implements Runnable {
    private static final TrialBots INSTANCE=new TrialBots(Path.of(System.getProperty("nro.bot.trial.config","bot-trial.properties")),
            Path.of(System.getProperty("nro.bot.trial.status","data/trial-bots-status.json")));
    public static TrialBots gI(){return INSTANCE;}
    private final Path config,statusFile;
    private final List<TrialBot> bots=new ArrayList<>();
    private long nextConfig,nextStatus,lastError;
    private int desiredFarm=-1,desiredHunt=-1;
    TrialBots(Path config,Path statusFile){this.config=config;this.statusFile=statusFile;}
    public synchronized void configure(boolean enabled) throws IOException {
        Properties p=new Properties();p.setProperty("enabled",Boolean.toString(enabled));p.setProperty("farm","2");p.setProperty("hunt","1");
        Path full=config.toAbsolutePath();Files.createDirectories(full.getParent());Path temp=Files.createTempFile(full.getParent(),"bots-",".tmp");
        try(Writer w=Files.newBufferedWriter(temp)){p.store(w,"NRO trial: maximum 3 bots");}
        try{Files.move(temp,full,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,full,StandardCopyOption.REPLACE_EXISTING);}
        nextConfig=0;tick();
    }
    public synchronized void tick() {
        if(Maintenance.isRunning)return;
        long now=System.currentTimeMillis();
        if(now>=nextConfig){
            nextConfig=now+2000;
            Properties p=new Properties();
            try{
                if(Files.exists(config))try(Reader r=Files.newBufferedReader(config)){p.load(r);}
                boolean enabled=Boolean.parseBoolean(p.getProperty("enabled","false"));
                int farm=enabled?Integer.parseInt(p.getProperty("farm","2")):0;
                int hunt=enabled?Integer.parseInt(p.getProperty("hunt","1")):0;
                if(farm<0||hunt<0||farm+hunt>3)throw new IllegalArgumentException("Bot trial permits at most 3 bots");
                if(farm!=desiredFarm||hunt!=desiredHunt||bots.size()!=farm+hunt){
                    stop();
                    for(int i=1;i<=farm;i++)bots.add(new TrialBot(TrialBot.Mode.FARM,i));
                    for(int i=1;i<=hunt;i++)bots.add(new TrialBot(TrialBot.Mode.HUNT,i));
                    desiredFarm=farm;desiredHunt=hunt;
                }
            }catch(Exception e){if(now-lastError>30000){Logger.logException(TrialBots.class,e);lastError=now;}}
        }
        for(TrialBot bot:bots)try{bot.tick(now);}catch(Exception e){bot.errors++;if(now-lastError>30000){Logger.logException(TrialBots.class,e);lastError=now;}}
        if(now>=nextStatus){nextStatus=now+5000;try{
            Path full=statusFile.toAbsolutePath();Files.createDirectories(full.getParent());
            Files.writeString(full,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("updated",now,"count",bots.size(),"bots",bots.stream().map(TrialBot::status).toList())));
        }catch(IOException e){Logger.logException(TrialBots.class,e);}}
    }
    public synchronized void stop(){for(TrialBot b:bots)b.stop();bots.clear();}
    public synchronized List<TrialBot> snapshot(){return List.copyOf(bots);}
    public synchronized String summary(){StringBuilder s=new StringBuilder("Bot thử nghiệm: "+bots.size());for(TrialBot b:bots)s.append("\n").append(b.name).append(" · map ").append(b.zone==null?"chờ":b.zone.map.mapName+" khu "+b.zone.zoneId).append(" · ").append(b.activity);return s.toString();}
    @Override public void run(){try{while(ServerManager.isRunning){try{tick();Thread.sleep(250);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}catch(Exception e){Logger.logException(TrialBots.class,e);}}}finally{stop();}}
}
