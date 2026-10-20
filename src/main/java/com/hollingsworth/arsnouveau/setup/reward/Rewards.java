package com.hollingsworth.arsnouveau.setup.reward;

import net.minecraft.world.item.DyeColor;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Rewards {
    public static List<ContributorStarby> starbuncles = new ArrayList<>();
    public static List<UUID> CONTRIBUTORS = new ArrayList<>();

    public static class ContributorStarby {
        public String name;
        public String adopter;
        public String color;
        public String bio;

        public ContributorStarby(String name, String adopter, String color, String bio) {
            this.name = name;
            this.adopter = adopter;
            this.color = color;
            this.bio = bio;

            if (!FMLEnvironment.production) {
                if (name == null) {
                    throw new RuntimeException("Name is null");
                }
                if (adopter == null) {
                    throw new RuntimeException("Adopter is null");
                }
                if (color == null) {
                    throw new RuntimeException("Color is null");
                }
                if (bio == null) {
                    throw new RuntimeException("Bio is null");
                }
                boolean foundColor = Arrays.stream(DyeColor.values()).anyMatch(dye -> dye.getName().equals(color));
                if (!foundColor) {
                    throw new RuntimeException("Color is not a valid dye color");
                }
            }
        }
    }
}
