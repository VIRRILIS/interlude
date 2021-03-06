//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package l2.gameserver.stats.conditions;

import l2.gameserver.stats.Env;

public class ConditionPlayerMinLevel extends Condition {
  private final int _level;

  public ConditionPlayerMinLevel(int level) {
    this._level = level;
  }

  protected boolean testImpl(Env env) {
    return env.character.getLevel() >= this._level;
  }
}
