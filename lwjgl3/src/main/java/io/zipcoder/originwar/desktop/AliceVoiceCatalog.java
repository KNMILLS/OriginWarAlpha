package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.utils.JsonReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Content-hash binding prevents stale voice from speaking changed or unrelated copy. */
final class AliceVoiceCatalog {
    record Clip(String dialogueId,String textHash,String path,float seconds) {}
    private final Map<String,Clip> clips;
    AliceVoiceCatalog(String json) {
        Map<String,Clip> result=new HashMap<>();
        var root=new JsonReader().parse(json);
        if(root.getInt("version")!=1)throw new IllegalArgumentException("Unsupported voice index");
        for(var item=root.get("clips").child;item!=null;item=item.next) {
            var clip=new Clip(item.getString("dialogue_id"),item.getString("text_sha256"),
                    item.getString("path"),item.getFloat("duration_seconds"));
            if(!clip.dialogueId.matches("[a-z0-9-]{1,60}")||!clip.textHash.matches("[0-9a-f]{64}")
                    ||!clip.path.matches("audio/voice/[a-z0-9-]{1,70}\\.ogg")
                    ||!Float.isFinite(clip.seconds)||clip.seconds<=0||clip.seconds>180)
                throw new IllegalArgumentException("Invalid voice entry");
            if(result.put(clip.dialogueId+":"+clip.textHash,clip)!=null)
                throw new IllegalArgumentException("Duplicate voice entry");
        }
        clips=Map.copyOf(result);
    }
    Clip find(String id,String text) { return clips.get(id+":"+hash(text)); }
    int size() { return clips.size(); }
    static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
