package com.coc.builderwidget.data

import com.coc.builderwidget.model.UpgradeCategory

object CoCItemNames {
    private val knownNames = mapOf(
        // Home Village Buildings (1000000+)
        1000000 to "Army Camp",
        1000001 to "Town Hall",
        1000002 to "Elixir Collector",
        1000003 to "Elixir Storage",
        1000004 to "Gold Mine",
        1000005 to "Gold Storage",
        1000006 to "Barracks",
        1000007 to "Laboratory",
        1000008 to "Cannon",
        1000009 to "Archer Tower",
        1000010 to "Wall",
        1000011 to "Wizard Tower",
        1000012 to "Air Defense",
        1000013 to "Mortar",
        1000014 to "Clan Castle",
        1000015 to "Builder's Hut",
        1000019 to "Hidden Tesla",
        1000020 to "Spell Factory",
        1000021 to "X-Bow",
        1000023 to "Dark Elixir Drill",
        1000024 to "Dark Elixir Storage",
        1000026 to "Dark Barracks",
        1000027 to "Inferno Tower",
        1000028 to "Air Sweeper",
        1000029 to "Dark Spell Factory",
        1000031 to "Eagle Artillery",
        1000032 to "Bomb Tower",
        1000059 to "Workshop",
        1000064 to "Pet House",
        1000067 to "Scattershot",
        1000068 to "Builder's Hut",
        1000070 to "Blacksmith",
        1000071 to "Hero Hall",
        1000072 to "Spell Tower",
        1000077 to "Monolith",
        1000084 to "Multi-Archer Tower",
        1000085 to "Ricochet Cannon",
        1000089 to "Firespitter",
        1000090 to "Multi-Gear Tower",

        // Traps (12000000+)
        12000000 to "Bomb",
        12000001 to "Spring Trap",
        12000002 to "Giant Bomb",
        12000005 to "Air Bomb",
        12000006 to "Seeking Air Mine",
        12000008 to "Skeleton Trap",
        12000016 to "Tornado Trap",
        12000020 to "Giga Bomb",

        // Heroes (28000000+)
        28000000 to "Barbarian King",
        28000001 to "Archer Queen",
        28000002 to "Grand Warden",
        28000003 to "Battle Machine",
        28000004 to "Royal Champion",
        28000005 to "Battle Copter",
        28000006 to "Minion Prince",

        // Pets (73000000+)
        73000000 to "L.A.S.S.I",
        73000001 to "Electro Owl",
        73000002 to "Mighty Yak",
        73000003 to "Unicorn",
        73000004 to "Frosty",
        73000007 to "Diggy",
        73000008 to "Poison Lizard",
        73000009 to "Phoenix",
        73000010 to "Spirit Fox",
        73000011 to "Angry Jelly",
        73000016 to "Sneezy"
    )

    fun resolve(id: Int, category: UpgradeCategory): String {
        return knownNames[id] ?: when (category) {
            UpgradeCategory.BUILDER -> "Bangunan #$id"
            UpgradeCategory.LAB -> "Riset Lab #$id"
            UpgradeCategory.PET -> "Pet #$id"
            UpgradeCategory.BUILDER_BASE -> "Builder Base #$id"
        }
    }
}
