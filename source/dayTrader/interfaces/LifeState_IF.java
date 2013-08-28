package interfaces;

public interface LifeState_IF {
  /* {src_lang=Java}*/


  public void initialize();

  public abstract void run();

  public abstract void sleep();

  public abstract void terminate();

  public abstract void wakeup();

}