package io.zipcoder.originwar.desktop;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
/** Held before any profile reads or writes. OS releases the lock after a crash. */
public final class ProfileLease implements AutoCloseable{
 private final FileChannel channel;private final FileLock lock;
 private ProfileLease(FileChannel c,FileLock l){channel=c;lock=l;}
 public static ProfileLease acquire(Path profile)throws IOException{
  Path p=profile.toAbsolutePath().normalize();Files.createDirectories(p);var c=FileChannel.open(p.resolve("profile.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  try{var l=c.tryLock();if(l==null)throw new IOException("This profile is already open in another Origin War instance.");return new ProfileLease(c,l);}
  catch(OverlappingFileLockException e){c.close();throw new IOException("This profile is already open in another Origin War instance.",e);}
  catch(IOException|RuntimeException e){c.close();throw e;}
 }
 public void close()throws IOException{try{lock.release();}finally{channel.close();}}
}
