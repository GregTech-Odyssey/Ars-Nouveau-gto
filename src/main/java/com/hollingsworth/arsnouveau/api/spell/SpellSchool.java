package com.hollingsworth.arsnouveau.api.spell;

import com.gto.fastcollection.fastutil.OpenCacheHashSet;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * A school that spell parts belong to.
 * NOTE: A spell being in a sub-school is also considered part of the parent school.
 */
public class SpellSchool {
    private final String id;
    private Set<SpellSchool> subSchools = new OpenCacheHashSet<>();
    private Set<AbstractSpellPart> spellParts = new OpenCacheHashSet<>();

    public SpellSchool(String id) {
        this.id = id;
    }

    public boolean isPartOfSchool(AbstractSpellPart part) {
        if (getSpellParts().contains(part))
            return true;
        for (SpellSchool spellSchool : getSubSchools()) {
            if (spellSchool.getSpellParts().contains(part))
                return true;
        }
        return false;
    }

    public boolean addSpellPart(AbstractSpellPart spellPart) {
        return getSpellParts().add(spellPart);
    }

    public Component getTextComponent() {
        return Component.translatable("ars_nouveau.school." + getId());
    }

    public String getId() {
        return id;
    }

    public Set<SpellSchool> getSubSchools() {
        return subSchools;
    }

    public void setSubSchools(Set<SpellSchool> subSchools) {
        this.subSchools = subSchools;
    }

    public SpellSchool withSubSchool(SpellSchool spellSchool) {
        this.getSubSchools().add(spellSchool);
        return this;
    }

    public Set<AbstractSpellPart> getSpellParts() {
        return spellParts;
    }

    public void setSpellParts(Set<AbstractSpellPart> spellParts) {
        this.spellParts = spellParts;
    }
}
