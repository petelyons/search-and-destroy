package com.developingstorm.games.sad;

import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages fictional continent names for the game world.
 * These are made-up names not based on Earth geography.
 */
public class ContinentNames {

    private static ContinentNames instance = new ContinentNames();

    private final List<String> allNames;
    private final List<String> unusedNames;

    private ContinentNames() {
        allNames = Arrays.asList(
            // Fictional continent names with varied linguistic styles
            "Valdoria",
            "Thalassar",
            "Kyrenth",
            "Eldavon",
            "Norvandis",
            "Azumar",
            "Drakholm",
            "Celestia",
            "Mythrendor",
            "Zephyria",
            "Avaloria",
            "Brynthia",
            "Calathis",
            "Dwendor",
            "Eryndor",
            "Frostmere",
            "Galendor",
            "Havenmoor",
            "Ironvale",
            "Jyrnath",
            "Kaldros",
            "Lumaria",
            "Mystral",
            "Netherwyn",
            "Orandel",
            "Pyralis",
            "Quendor",
            "Ryvonia",
            "Sylvandor",
            "Telvaris",
            "Umbrath",
            "Vorthane",
            "Wynderyl",
            "Xyloria",
            "Ytharn",
            "Zephros",
            "Aethermoor",
            "Brakmoor",
            "Cindral",
            "Duskmere",
            "Ebonreach",
            "Faerun",
            "Grimvale",
            "Hallownest",
            "Icewyn",
            "Jadestone",
            "Khymar",
            "Lorendel",
            "Mordania",
            "Nyxmoor",
            "Obsidian",
            "Peloria",
            "Quarenthia",
            "Raventhorn",
            "Stormhaven",
            "Thornwick",
            "Ulthar",
            "Velmora",
            "Westmarch",
            "Xenthia",
            "Yolrand",
            "Zenithia",
            "Argentor",
            "Blazewood",
            "Crystalia",
            "Darnoth",
            "Emberfell",
            "Frostwing",
            "Goldmere",
            "Hollowmoor",
            "Ironshore",
            "Jademarch",
            "Krythos",
            "Lunaris",
            "Mirstone",
            "Nightvale",
            "Onyxmoor",
            "Prismara",
            "Quillmoor",
            "Runestone",
            "Shadowmere",
            "Titanreach",
            "Ulmoria",
            "Verdantia",
            "Windfall",
            "Xalvador",
            "Yarrowmoor",
            "Zanthir",
            "Ashenfold",
            "Brinmore",
            "Copperhill",
            "Deepmoor",
            "Everfrost",
            "Flameheart",
            "Glimmerdale",
            "Highgarden",
            "Ivoryreach",
            "Jasperwind",
            "Kingsmoor",
            "Lighthollow"
        );

        unusedNames = new ArrayList<>(allNames);
    }

    private String allocateName() {
        if (unusedNames.isEmpty()) {
            // Reset pool if exhausted
            unusedNames.addAll(allNames);
        }

        int index = RandomUtil.getInt(unusedNames.size());
        return unusedNames.remove(index);
    }

    private void releaseNameInternal(String name) {
        if (name != null && !unusedNames.contains(name)) {
            unusedNames.add(name);
        }
    }

    /**
     * Gets a unique continent name.
     * @return A fictional continent name
     */
    public static String getName() {
        return instance.allocateName();
    }

    /**
     * Returns a name back to the pool for reuse.
     * @param name The name to release
     */
    public static void releaseName(String name) {
        instance.releaseNameInternal(name);
    }

    /**
     * Resets the name pool to its initial state.
     * Call this when starting a new game.
     */
    public static void reset() {
        instance.unusedNames.clear();
        instance.unusedNames.addAll(instance.allNames);
    }
}
