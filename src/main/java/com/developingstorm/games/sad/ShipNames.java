package com.developingstorm.games.sad;

import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages ship names organized by class.
 * Each ship type has its own thematic name pool.
 */
public class ShipNames {

    private static ShipNames instance = new ShipNames();

    private final Map<Type, List<String>> unusedNames;
    private final Map<Type, List<String>> allNames;

    private ShipNames() {
        unusedNames = new HashMap<>();
        allNames = new HashMap<>();

        initializeDestroyerNames();
        initializeCruiserNames();
        initializeBattleshipNames();
        initializeCarrierNames();
        initializeSubmarineNames();
        initializeTransportNames();
    }

    private void initializeDestroyerNames() {
        List<String> names = Arrays.asList(
            // Famous destroyers and destroyer-themed names
            "Fletcher",
            "Gearing",
            "Sumner",
            "Benson",
            "Gleaves",
            "Gridley",
            "Mahan",
            "Sims",
            "Farragut",
            "Porter",
            "Johnston",
            "Hoel",
            "Heermann",
            "Samuel B. Roberts",
            "Laffey",
            "O'Bannon",
            "Nicholas",
            "Taylor",
            "Kidd",
            "Cassin Young",
            "The Sullivans",
            "Barton",
            "Monssen",
            "Aaron Ward",
            "Buchanan",
            "Duncan",
            "Cushing",
            "Preston",
            "Walke",
            "Benham",
            "Lang",
            "Sterett",
            "McCalla",
            "Jarvis",
            "Henley",
            "Patterson",
            "Ralph Talbot",
            "Blue",
            "Helm",
            "Mugford",
            "Bagley",
            "Selfridge",
            "McDougal",
            "Winslow",
            "Phelps",
            "Dewey",
            "Hull",
            "MacDonough",
            "Worden",
            "Dale",
            "Aylwin",
            "Monaghan",
            "Drayton"
        );
        allNames.put(Type.DESTROYER, names);
        unusedNames.put(Type.DESTROYER, new ArrayList<>(names));
    }

    private void initializeCruiserNames() {
        List<String> names = Arrays.asList(
            // Famous cruisers, often named after cities
            "Atlanta",
            "Baltimore",
            "Boston",
            "Brooklyn",
            "Chicago",
            "Cleveland",
            "Columbia",
            "Denver",
            "Helena",
            "Honolulu",
            "Houston",
            "Indianapolis",
            "Juneau",
            "Milwaukee",
            "Minneapolis",
            "Mobile",
            "Nashville",
            "New Orleans",
            "Oakland",
            "Pensacola",
            "Phoenix",
            "Pittsburgh",
            "Portland",
            "Quincy",
            "Raleigh",
            "Richmond",
            "St. Louis",
            "Salt Lake City",
            "San Diego",
            "San Francisco",
            "Savannah",
            "Springfield",
            "Topeka",
            "Tucson",
            "Wichita",
            "Astoria",
            "Biloxi",
            "Boise",
            "Concord",
            "Duluth",
            "Flint",
            "Little Rock",
            "Manchester",
            "Miami",
            "Montpelier",
            "Pasadena",
            "Providence",
            "Santa Fe",
            "Spokane",
            "Vincennes"
        );
        allNames.put(Type.CRUISER, names);
        unusedNames.put(Type.CRUISER, new ArrayList<>(names));
    }

    private void initializeBattleshipNames() {
        List<String> names = Arrays.asList(
            // Famous battleships, traditionally named after states
            "Iowa",
            "Missouri",
            "Wisconsin",
            "New Jersey",
            "Illinois",
            "Kentucky",
            "Montana",
            "Ohio",
            "Maine",
            "New Hampshire",
            "North Carolina",
            "Washington",
            "South Dakota",
            "Indiana",
            "Massachusetts",
            "Alabama",
            "Tennessee",
            "California",
            "Colorado",
            "Maryland",
            "West Virginia",
            "Nevada",
            "Oklahoma",
            "Pennsylvania",
            "Arizona",
            "New Mexico",
            "Mississippi",
            "Idaho",
            "New York",
            "Texas",
            "Arkansas",
            "Wyoming",
            "Florida",
            "Utah",
            "Delaware",
            "North Dakota",
            "Georgia",
            "Kansas",
            "Louisiana",
            "Minnesota",
            "Nebraska",
            "Rhode Island",
            "Vermont",
            "Virginia",
            "Connecticut",
            "Michigan",
            "South Carolina"
        );
        allNames.put(Type.BATTLESHIP, names);
        unusedNames.put(Type.BATTLESHIP, new ArrayList<>(names));
    }

    private void initializeCarrierNames() {
        List<String> names = Arrays.asList(
            // Famous carriers and carrier-themed names
            "Enterprise",
            "Yorktown",
            "Hornet",
            "Wasp",
            "Essex",
            "Lexington",
            "Saratoga",
            "Ranger",
            "Intrepid",
            "Bunker Hill",
            "Franklin",
            "Hancock",
            "Shangri-La",
            "Lake Champlain",
            "Bon Homme Richard",
            "Antietam",
            "Princeton",
            "Belleau Wood",
            "Cowpens",
            "Monterey",
            "Langley",
            "Cabot",
            "Bataan",
            "San Jacinto",
            "Independence",
            "Valley Forge",
            "Philippine Sea",
            "Kearsarge",
            "Tarawa",
            "Boxer",
            "Oriskany",
            "Constellation",
            "Kitty Hawk",
            "America",
            "John F. Kennedy",
            "Nimitz",
            "Eisenhower",
            "Vinson",
            "Roosevelt",
            "Lincoln",
            "Washington",
            "Stennis",
            "Truman",
            "Reagan",
            "Bush",
            "Midway",
            "Coral Sea",
            "Franklin D. Roosevelt",
            "Randolph",
            "Ticonderoga"
        );
        allNames.put(Type.CARRIER, names);
        unusedNames.put(Type.CARRIER, new ArrayList<>(names));
    }

    private void initializeSubmarineNames() {
        List<String> names = Arrays.asList(
            // Famous submarines and submarine-themed names (fish, sea creatures)
            "Nautilus",
            "Gato",
            "Wahoo",
            "Tang",
            "Harder",
            "Flasher",
            "Barb",
            "Silversides",
            "Trigger",
            "Drum",
            "Tautog",
            "Cavalla",
            "Archerfish",
            "Batfish",
            "Cobia",
            "Rasher",
            "Seahorse",
            "Spadefish",
            "Swordfish",
            "Thresher",
            "Skipjack",
            "Sculpin",
            "Sargo",
            "Salmon",
            "Seal",
            "Shark",
            "Snapper",
            "Stingray",
            "Sturgeon",
            "Tarpon",
            "Tench",
            "Trepang",
            "Triton",
            "Trout",
            "Tunny",
            "Dolphin",
            "Marlin",
            "Pike",
            "Porpoise",
            "Grampus",
            "Pickerel",
            "Permit",
            "Plunger",
            "Pollack",
            "Pompano",
            "Scamp",
            "Scorpion",
            "Seadragon",
            "Seawolf",
            "Skate"
        );
        allNames.put(Type.SUBMARINE, names);
        unusedNames.put(Type.SUBMARINE, new ArrayList<>(names));
    }

    private void initializeTransportNames() {
        List<String> names = Arrays.asList(
            // Transport-themed names (bays, sounds, and geographic features)
            "Barnett",
            "McCawley",
            "Crescent City",
            "George F. Elliott",
            "Hunter Liggett",
            "President Jackson",
            "President Adams",
            "President Hayes",
            "Heywood",
            "Zeilin",
            "Neville",
            "Harris",
            "Fuller",
            "William P. Biddle",
            "Ormsby",
            "Doyen",
            "Feland",
            "Knox",
            "Calvert",
            "Leonard Wood",
            "Joseph T. Dickman",
            "Wakefield",
            "Mount Vernon",
            "West Point",
            "Chateau Thierry",
            "Tasker H. Bliss",
            "Thurston",
            "Monrovia",
            "Callaway",
            "Bountiful",
            "Hermitage",
            "Cavalier",
            "Electra",
            "Kenmore",
            "Woodrow Wilson",
            "Rochambeau",
            "Bolivar",
            "Leedstown",
            "Florence Nightingale",
            "Nightingale",
            "Storm King",
            "Comfort",
            "Solace",
            "Relief",
            "Haven",
            "Mercy",
            "Tranquility",
            "Repose",
            "Benevolence",
            "Consolation"
        );
        allNames.put(Type.TRANSPORT, names);
        unusedNames.put(Type.TRANSPORT, new ArrayList<>(names));
    }

    private String allocateName(Type shipType) {
        List<String> available = unusedNames.get(shipType);
        if (available == null || available.isEmpty()) {
            // Reset the pool if we've used all names
            available = new ArrayList<>(allNames.get(shipType));
            unusedNames.put(shipType, available);
        }

        if (available.isEmpty()) {
            // Fallback: generate a numbered name
            return shipType.getAbr() + "-" + RandomUtil.getInt(9999);
        }

        int index = RandomUtil.getInt(available.size());
        return available.remove(index);
    }

    private void releaseNameInternal(Type shipType, String name) {
        List<String> available = unusedNames.get(shipType);
        if (available != null && !available.contains(name)) {
            available.add(name);
        }
    }

    /**
     * Gets a name for a ship of the specified type.
     * @param shipType The type of ship
     * @return A unique name appropriate for that ship class
     */
    public static String getName(Type shipType) {
        if (!isShipType(shipType)) {
            return null;
        }
        return instance.allocateName(shipType);
    }

    /**
     * Returns a name back to the pool for reuse.
     * @param shipType The type of ship
     * @param name The name to release
     */
    public static void releaseName(Type shipType, String name) {
        if (!isShipType(shipType) || name == null) {
            return;
        }
        instance.releaseNameInternal(shipType, name);
    }

    /**
     * Checks if a type is a naval vessel.
     * @param type The unit type
     * @return true if the type is a ship
     */
    private static boolean isShipType(Type type) {
        return (
            type == Type.DESTROYER ||
            type == Type.CRUISER ||
            type == Type.BATTLESHIP ||
            type == Type.CARRIER ||
            type == Type.SUBMARINE ||
            type == Type.TRANSPORT
        );
    }

    /**
     * Resets all name pools to their initial state.
     * Useful for starting a new game.
     */
    public static void reset() {
        instance.unusedNames.clear();
        for (Map.Entry<
            Type,
            List<String>
        > entry : instance.allNames.entrySet()) {
            instance.unusedNames.put(
                entry.getKey(),
                new ArrayList<>(entry.getValue())
            );
        }
    }
}
