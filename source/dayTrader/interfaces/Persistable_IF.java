package interfaces;

import org.hibernate.HibernateException;

public interface Persistable_IF {
  /* {src_lang=Java}*/

  /** 
   * Persist an object to the database.
   * @throws HibernateException if the object could not be persisted
   * @return the id of the persisted object 
   */
  public long insert() throws HibernateException;

  public void delete() throws HibernateException;
  
  public void update() throws HibernateException;

}