package io.zipcoder.originwar.desktop;
import io.zipcoder.originwar.model.GameState;
import java.io.*;
import java.util.concurrent.*;
/** Single writer, immutable pending snapshots, explicit failure/retry and nonblocking render polling. */
public final class SaveCoordinator implements AutoCloseable{
 public enum Status{READY,WRITING,FAILED}
 public enum Operation{BEGIN,SAVE,LOAD}
 private final SaveStore store;private final ExecutorService executor=Executors.newSingleThreadExecutor(r->new Thread(r,"originwar-profile-writer"));
 private Future<GameState> future;private GameState pending,loaded;private Operation operation;private Status status=Status.READY;private String error="";private long savedRevision=-1;
 public SaveCoordinator(SaveStore store){this.store=store;}
 public synchronized Status status(){poll();return status;}
 public synchronized String error(){poll();return error;}
 public synchronized Operation operation(){return operation;}
 public synchronized long savedRevision(){return savedRevision;}
 public synchronized boolean begin(GameState s){return start(Operation.BEGIN,s);}
 public synchronized boolean save(GameState s){if(status==Status.READY&&s.revision<=savedRevision)return true;return start(Operation.SAVE,s);}
 public synchronized boolean load(){return start(Operation.LOAD,null);}
 private boolean start(Operation op,GameState s){poll();if(status!=Status.READY)return false;operation=op;pending=s==null?null:s.copy();launch();return true;}
 private void launch(){status=Status.WRITING;error="";GameState captured=pending;Operation op=operation;future=executor.submit(()->{switch(op){case BEGIN->store.begin(captured);case SAVE->store.save(captured);case LOAD->{return store.load();}}return null;});}
 public synchronized void poll(){
  if(status!=Status.WRITING||future==null||!future.isDone())return;
  try{loaded=future.get();savedRevision=operation==Operation.LOAD?loaded.revision:pending.revision;status=Status.READY;future=null;pending=null;}
  catch(InterruptedException e){Thread.currentThread().interrupt();status=Status.FAILED;error="Snapshot completion interrupted.";}
  catch(ExecutionException e){status=Status.FAILED;error=e.getCause().getMessage();}
 }
 public synchronized boolean retry(){poll();if(status!=Status.FAILED)return false;launch();return true;}
 public synchronized boolean dismissFailedLoad(){poll();if(status!=Status.FAILED||operation!=Operation.LOAD)return false;status=Status.READY;future=null;pending=null;loaded=null;error="";return true;}
 public synchronized GameState takeLoaded(){poll();var s=loaded;loaded=null;return s==null?null:s.copy();}
 public void close()throws IOException{executor.shutdown();try{if(!executor.awaitTermination(10,TimeUnit.SECONDS))throw new IOException("Snapshot writer did not stop.");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}poll();if(status==Status.FAILED)throw new IOException(error);}
}
