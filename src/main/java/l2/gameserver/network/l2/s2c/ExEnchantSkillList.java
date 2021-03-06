//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package l2.gameserver.network.l2.s2c;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import l2.gameserver.data.xml.holder.EnchantSkillHolder;
import l2.gameserver.model.Player;
import l2.gameserver.model.Skill;
import l2.gameserver.templates.SkillEnchant;

public class ExEnchantSkillList extends L2GameServerPacket {
  private final List<ExEnchantSkillList.SkillEnchantEntry> _skills = new ArrayList<>();

  public static ExEnchantSkillList packetFor(Player player) {
    Collection<Skill> playerSkills = player.getAllSkills();
    ExEnchantSkillList esl = new ExEnchantSkillList();
    Iterator var3 = playerSkills.iterator();

    while(true) {
      while(true) {
        int skillId;
        int currSkillLevel;
        int baseSkillLevel;
        Map skillEnchRoutes;
        do {
          do {
            do {
              if (!var3.hasNext()) {
                return esl;
              }

              Skill currSkill = (Skill)var3.next();
              skillId = currSkill.getId();
              currSkillLevel = currSkill.getLevel();
              baseSkillLevel = currSkill.getBaseLevel();
            } while(currSkillLevel < baseSkillLevel);

            skillEnchRoutes = EnchantSkillHolder.getInstance().getRoutesOf(skillId);
          } while(skillEnchRoutes == null);
        } while(skillEnchRoutes.isEmpty());

        SkillEnchant currSkillEnch = EnchantSkillHolder.getInstance().getSkillEnchant(skillId, currSkillLevel);
        if (currSkillLevel == baseSkillLevel) {
          Iterator var14 = skillEnchRoutes.values().iterator();

          while(var14.hasNext()) {
            Map<Integer, SkillEnchant> skillEnchLevels = (Map)var14.next();
            Iterator var16 = skillEnchLevels.values().iterator();

            while(var16.hasNext()) {
              SkillEnchant newSkillEnch = (SkillEnchant)var16.next();
              if (newSkillEnch.getEnchantLevel() == 1) {
                esl.addSkill(newSkillEnch.getSkillId(), newSkillEnch.getSkillLevel(), newSkillEnch.getSp(), newSkillEnch.getExp());
              }
            }
          }
        } else if (currSkillEnch != null) {
          Map<Integer, SkillEnchant> skillEnchLevels = (Map)skillEnchRoutes.get(currSkillEnch.getRouteId());
          int newSkillLevel = currSkillLevel + 1;
          SkillEnchant newSkillEnch = (SkillEnchant)skillEnchLevels.get(newSkillLevel);
          if (newSkillEnch != null) {
            esl.addSkill(newSkillEnch.getSkillId(), newSkillEnch.getSkillLevel(), newSkillEnch.getSp(), newSkillEnch.getExp());
          }
        }
      }
    }
  }

  public void addSkill(int id, int level, int sp, long exp) {
    this._skills.add(new ExEnchantSkillList.SkillEnchantEntry(id, level, sp, exp));
  }

  public ExEnchantSkillList() {
  }

  protected final void writeImpl() {
    this.writeEx(23);
    this.writeD(this._skills.size());
    Iterator var1 = this._skills.iterator();

    while(var1.hasNext()) {
      ExEnchantSkillList.SkillEnchantEntry see = (ExEnchantSkillList.SkillEnchantEntry)var1.next();
      see.write();
    }

  }

  class SkillEnchantEntry {
    private final int _skillId;
    private final int _skillLevel;
    private final int _neededSp;
    private final long _neededExp;

    public SkillEnchantEntry(int skillId, int skillLevel, int neededSp, long neededExp) {
      this._skillId = skillId;
      this._skillLevel = skillLevel;
      this._neededSp = neededSp;
      this._neededExp = neededExp;
    }

    private void write() {
      ExEnchantSkillList.this.writeD(this._skillId);
      ExEnchantSkillList.this.writeD(this._skillLevel);
      ExEnchantSkillList.this.writeD(this._neededSp);
      ExEnchantSkillList.this.writeQ(this._neededExp);
    }
  }
}
