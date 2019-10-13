//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package l2.gameserver.data.xml.holder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import l2.commons.data.xml.AbstractHolder;
import l2.gameserver.model.GameObject;
import l2.gameserver.model.entity.Reflection;
import l2.gameserver.model.entity.residence.Residence;
import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.TreeIntObjectMap;

public final class ResidenceHolder extends AbstractHolder {
  private static ResidenceHolder _instance = new ResidenceHolder();
  private IntObjectMap<Residence> _residences = new TreeIntObjectMap();
  private Map<Class, List<Residence>> _fastResidencesByType = new HashMap(4);

  public static ResidenceHolder getInstance() {
    return _instance;
  }

  private ResidenceHolder() {
  }

  public void addResidence(Residence r) {
    this._residences.put(r.getId(), r);
  }

  public <R extends Residence> R getResidence(int id) {
    return (Residence)this._residences.get(id);
  }

  public <R extends Residence> R getResidence(Class<R> type, int id) {
    Residence r = this.getResidence(id);
    return r != null && r.getClass() == type ? r : null;
  }

  public <R extends Residence> List<R> getResidenceList(Class<R> t) {
    return (List)this._fastResidencesByType.get(t);
  }

  public Collection<Residence> getResidences() {
    return this._residences.values();
  }

  public <R extends Residence> R getResidenceByObject(Class<? extends Residence> type, GameObject object) {
    return this.getResidenceByCoord(type, object.getX(), object.getY(), object.getZ(), object.getReflection());
  }

  public <R extends Residence> R getResidenceByCoord(Class<R> type, int x, int y, int z, Reflection ref) {
    Collection<Residence> residences = type == null ? this.getResidences() : this.getResidenceList(type);
    Iterator var7 = ((Collection)residences).iterator();

    Residence residence;
    do {
      if (!var7.hasNext()) {
        return null;
      }

      residence = (Residence)var7.next();
    } while(!residence.checkIfInZone(x, y, z, ref));

    return residence;
  }

  public <R extends Residence> R findNearestResidence(Class<R> clazz, int x, int y, int z, Reflection ref, int offset) {
    Residence residence = this.getResidenceByCoord(clazz, x, y, z, ref);
    if (residence == null) {
      double closestDistance = (double)offset;
      Iterator var12 = this.getResidenceList(clazz).iterator();

      while(var12.hasNext()) {
        Residence r = (Residence)var12.next();
        double distance = r.getZone().findDistanceToZone(x, y, z, false);
        if (closestDistance > distance) {
          closestDistance = distance;
          residence = r;
        }
      }
    }

    return residence;
  }

  public void callInit() {
    Iterator var1 = this.getResidences().iterator();

    while(var1.hasNext()) {
      Residence r = (Residence)var1.next();
      r.init();
    }

  }

  private void buildFastLook() {
    Residence residence;
    Object list;
    for(Iterator var1 = this._residences.values().iterator(); var1.hasNext(); ((List)list).add(residence)) {
      residence = (Residence)var1.next();
      list = (List)this._fastResidencesByType.get(residence.getClass());
      if (list == null) {
        this._fastResidencesByType.put(residence.getClass(), list = new ArrayList());
      }
    }

  }

  public void log() {
    this.buildFastLook();
    this.info("total size: " + this._residences.size());
    Iterator var1 = this._fastResidencesByType.entrySet().iterator();

    while(var1.hasNext()) {
      Entry<Class, List<Residence>> entry = (Entry)var1.next();
      this.info(" - load " + ((List)entry.getValue()).size() + " " + ((Class)entry.getKey()).getSimpleName().toLowerCase() + "(s).");
    }

  }

  public int size() {
    return 0;
  }

  public void clear() {
    this._residences.clear();
    this._fastResidencesByType.clear();
  }
}
