package com.example.qwen2gguf.ui

enum class FairyTaleTheme(
    val displayName: String,
    val emoji: String,
    val promptFragment: String,
    val examples: List<Pair<String, String>>,
    val prompts: List<String>,
) {
    PRINCESSES(
        displayName = "Princesses",
        emoji = "👸",
        promptFragment = "Center the story around princesses — royal courts, enchantments, true love, and the courage it takes to forge one's own destiny.",
        prompts = listOf(
            "A princess who escapes a tower and teaches a clueless prince how to read a map",
            "A princess who answers a sea monster's ancient riddle and invites it to guard the harbour",
            "A princess who ends a drought by reading her grandmother's forgotten field journal",
            "Two rival princesses trapped under a bridge in the rain who solve a century-old border dispute",
            "A princess who turns down the crown and gives it to the aunt who actually deserves it",
            "A princess who builds a library by offering wandering scholars nothing but bread and an audience",
            "A princess given a magic mirror that shows the future who immediately turns it to the wall",
        ),
        examples = listOf(
            "Tell me a princess story" to
                "A princess shattered her enchanted prison with a single perfect note, then traded the glass shards for maps and walked away. A prince arrived three days too late to rescue her, but she taught him navigation and he taught her to start a fire. They never returned to the kingdom — they built something better at the world's edge.",

            "A story about a princess and a dragon" to
                "A dragon had sat under the royal library for three hundred years, refusing to leave until someone solved its riddle. Princess Vera answered in seconds, the dragon wept with relief, and she invited it to guard the library properly. It agreed, laughing like thunder learning to be gentle.",

            "A princess who saves her kingdom from drought" to
                "Every weather-mage had failed, so Princess Mira climbed the oldest mountain alone with a shovel and her grandmother's journal. She found ancient irrigation channels, silted over and forgotten, and spent three months clearing them with the farmers. The rains came as they always had — only now the water went where it was needed.",

            "Two rival princesses who become unlikely friends" to
                "Princesses Asha and Brynn were sent to negotiate a border dispute; they argued for three days and got nowhere. Then it rained and the bridge was the only shelter for miles, so they shared it. By morning they had solved a two-hundred-year surveying error and were reading the same book.",

            "A princess who refuses the crown" to
                "Princess Calla was offered the crown and said no: her Aunt Ros had been running the eastern trade routes for twenty years without credit and was far more qualified. Calla went off to build bridges — the kingdom had forty-seven rivers and only eleven adequate crossings. Aunt Ros became queen and was excellent at it.",

            "A princess who builds a library from nothing" to
                "Princess Orla inherited a kingdom with no books, so she sent letters offering scholars nothing but a warm room, bread, and an audience. They came in surprising numbers; within three years every village had a reading room. The kingdom became unusually difficult to invade — armies tend to pause when the populace asks them to justify their reasoning.",

            "A princess and an enchanted mirror" to
                "Princess Sable was given a mirror that showed the future and immediately turned it to face the wall. She said knowing what came next would make her manage around it rather than build toward what she wanted. Years later people called her the wisest ruler the kingdom had ever had.",
        ),
    ),

    DRAGONS(
        displayName = "Dragons",
        emoji = "🐉",
        promptFragment = "Center the story around dragons — their ancient hoards, fearsome fire, hidden wisdom, and their uneasy relationship with the human world.",
        prompts = listOf(
            "A dragon who hoards the last words of dying languages and finally gives one away",
            "A dragon who burns a village by sneezing and spends the winter rebuilding every house",
            "An old dragon who retires from terrorising ships and becomes a lighthouse keeper",
            "A dragon who breathes cold wind instead of fire and finally discovers why that matters",
            "A dragon who collects farewell letters and learns the same lesson from all of them",
            "The oldest dragon in the world asked by the youngest for the single most important advice",
            "A dragon who melts her gold hoard to build a garden and is told she is overwatering",
        ),
        examples = listOf(
            "Tell me a dragon story" to
                "Dragon Sorreth hoarded words — specifically the last words of extinct languages and burned books — and turned them over on his tongue like gems. A young linguist came looking for one last word her grandmother had spoken alone, and Sorreth gave it to her freely. She taught it to her children, and for the first time in four centuries his cave was warm.",

            "A story where a dragon learns something" to
                "Young dragon Cinder sneezed and burned down a village, so she spent the winter rebuilding every house with her bare claws. The carpenter Brix taught her how a joint should fit; Cinder, in return, could hold a roof beam perfectly still for six hours. By spring the village was better than before — Cinder had accidentally added underfloor heating to every house.",

            "A dragon who becomes a lighthouse keeper" to
                "Old dragon Vex retired from terrorising ships and applied to keep the lighthouse, pointing out he had been breathing fire for four hundred years without once going out. Sailors who had feared him learned to find his amber glow with relief; he kept meticulous logs and repaired the lens twice using his own scales. 'I spent four centuries making darkness,' he said. 'I prefer making light.'",

            "A dragon who cannot breathe fire" to
                "Dragon Gust breathed only cold, steady wind — an embarrassment in dragon society, so he hid it. When a mountain pass froze solid and cut off supply caravans, an engineer named Dara asked if anyone had a precise source of cold that could crack ice without an avalanche. Gust stepped forward and opened the pass in three hours; for the first time, he considered his difference an advantage.",

            "A dragon who collects last words" to
                "Dragon Elegy collected last words voluntarily given — final log entries, letters never sent — and found they all said the same few things: I wish I had been kinder, I wish I had said so while I had the chance. A curious scholar asked what she did with that knowledge. 'I try to act accordingly,' said Elegy, 'while I still have time.'",

            "An old dragon who befriends a young one" to
                "The oldest dragon in the world, Orm, was asked by the youngest dragon for the most important advice. After a long silence Orm said: 'Pay attention — to everything, the small things especially, because that is where everything actually happens.' The young dragon thought about it for several decades and eventually agreed.",

            "A dragon who guards a garden instead of gold" to
                "Dragon Bloom melted her entire gold hoard to line an irrigation valley, then planted a garden with a retired botanist named Hess who told her bluntly on the first day that she was overwatering. Bloom had never been corrected so directly by someone she could have eaten in one bite; she found she liked it. Travellers came from far away to watch a dragon tend flowers with claws calibrated not to crush a stem.",
        ),
    ),

    KNIGHTS(
        displayName = "Knights",
        emoji = "⚔️",
        promptFragment = "Center the story around knights — chivalric quests, honourable duels, sacred vows, and the sacrifice required to be truly brave.",
        prompts = listOf(
            "A knight who never won a tournament but never broke a single promise",
            "A knight who reaches the tournament final and walks away because her opponent has a broken wrist",
            "A knight who ends a three-year border war by asking what the other side actually needs",
            "A knight who served twelve years without drawing her sword and was called no real knight",
            "A disgraced knight who spends twenty years rebuilding a village without ever asking for forgiveness",
            "Two enemy knights stranded in a blizzard who go home and write peace letters to their kings",
            "A knight who loses her title and horse and builds a well company with her one remaining skill",
        ),
        examples = listOf(
            "Tell me a knight story" to
                "Knight Aldric had never won a tournament but had never broken a single promise; his horse Constance was seventeen and moved at a pace described charitably as deliberate. Every great knight failed to light the Lantern of True Worth — Aldric arrived last and found an old gatekeeper who had waited three days for someone to offer help. He spent a morning mending her roof, continued up the mountain, and the Lantern blazed the moment he touched it.",

            "A knight who must choose between honour and victory" to
                "Knight Serra reached the tournament final and felt immediately that her opponent had a broken wrist he was hiding from the judges. She pulled alongside him and said: if I win this way I haven't won anything — yield now, rest, and meet me here next year. He did; his wrist healed; he won cleanly the following year and Serra cheered louder than anyone.",

            "A knight who wins a battle by listening" to
                "Knight Tam was sent to negotiate with a warlord who had been raiding villages for three years; he brought no army and said nothing for a long time, then asked: what do you actually need? The answer was access to a well the kingdom had quietly diverted twelve years earlier and everyone had since forgotten. The engineers restored it before autumn and the raiding stopped entirely.",

            "A knight who refuses to draw her sword" to
                "Knight Wren had served twelve years without once drawing her sword in anger, yet had talked a siege army into retreat using supply logistics and ended a blood feud by showing two noble families they were cousins. Her rivals called her no real knight; she noted that every quest assigned to her had been completed. When a day came that required the sword, she drew it without hesitation — and put it away again the moment the work was done.",

            "A disgraced knight who rebuilds trust" to
                "Knight Rowan made one wrong decision in battle and lost seventeen lives; he was not stripped of his rank, which he found harder than if he had been. He spent a decade doing the quiet unglamorous work — rebuilding the mill, teaching the children to read, sitting with the elderly through long winters — and never asked for forgiveness. Twenty years later the village headwoman left a loaf of bread on his doorstep with no note.",

            "Two knights on opposite sides who share a fire" to
                "Two knights — one from the north, one from the south — were trapped in the same cave for three days by a blizzard, fighting in a war so old neither remembered why it started. The southerner helped the northerner's sick horse without making anything of it; by the third night they were talking about home and snow and the particular way each country sounded in winter. Neither reported the encounter, but both went home and wrote long letters to their kings recommending negotiations.",

            "A knight who loses everything and starts again" to
                "Knight Pell lost her horse, armour, title, and name in one disastrous week — not gloriously, just through bad decisions by people she had trusted. She walked out of the capital with one useful skill: finding water in dry country. By the time anyone thought to reinstate her title, she owned a well company and three properties and sent back a polite letter declining.",
        ),
    ),

    WITCHES(
        displayName = "Witches",
        emoji = "🧙‍♀️",
        promptFragment = "Center the story around witches — forbidden magic, dark enchanted forests, mysterious potions, and the possibility of redemption.",
        prompts = listOf(
            "A witch who curses people for a living but every curse turns out to be a blessing",
            "A witch who turns a prince into a crow and finds herself caring for him until the spell undoes itself",
            "A witch who tries to cast wicked spells four times and fails every time in useful ways",
            "A young witch whose spells always work but never quite as intended",
            "An ancient witch with centuries of knowledge humbled by a child who simply asks where the cat is",
            "A witch with a talking cat who disagrees with everything — and is right exactly half the time",
            "A witch who enchants the village well so no one can lie, then watches the village improve",
        ),
        examples = listOf(
            "Tell me a witch story" to
                "Witch Maren cursed people for a living, but her curses always turned out slightly better than she intended — a cheating merchant was cursed to speak only truth and wrote a famous book; a flower-eared lord donated his blooms to the village healer. She began to suspect she was not very good at being wicked. The village children confirmed this by following her home and asking about herbs until she gave up refusing and put on a kettle.",

            "A story about a witch seeking redemption" to
                "Witch Elda turned a prince into a crow and immediately regretted it; the young creature looked at her with such bewildered trust that something in her chest creaked like a door long-rusted shut. She could not undo the spell, so she built him a perch, learned his favourite seed-cakes, and talked with him in the evenings. In spring the spell broke on its own — warmth she had spent decades extinguishing had quietly grown back.",

            "A witch who can only do good things" to
                "Witch Sable attempted wicked spells four times: a curse accidentally gave a corrupt collector enthusiasm for accurate bookkeeping; a poison potion became the finest wine the household had ever tasted. She gave up and opened a small practice charging reasonable rates for enchantments and good fortune. When a customer called her the kindest witch they had ever met, she corrected: 'I am the most incompetent wicked witch you have ever met.'",

            "A young witch whose spells never go quite right" to
                "Young witch Pip tried to light a candle and lit the rain instead; she tried to silence a rooster and gave it a beautiful tenor singing voice; she tried to make herself invisible and made her shoes invisible instead. Her teacher Dross said the intentions were correct but the precision was the problem — use small magic for small problems, then the mistakes are also small. Pip practised; the mistakes became smaller; the rooster kept its voice because no one had the heart to undo it.",

            "A witch who learns her greatest lesson from a child" to
                "Witch Corvine, who had studied magic for seventy years, was asked by an eight-year-old — who had wandered in looking for a lost cat — simply: can you find my cat? Corvine announced she could do considerably more impressive things, but the child only repeated the question. Corvine found the cat, and spent several years afterward thinking about the value of asking plainly for what you need.",

            "A witch and her talking cat who constantly disagree" to
                "Witch Vane and her talking cat Ash disagreed about absolutely everything: safe mushrooms, trustworthy merchants, which direction to travel. After twenty years a student asked how Vane tolerated so much contradiction. 'Ash is wrong about half of everything,' she said, 'but about the other half is the only one who tells me the truth — and I don't always know which half is which at first.'",

            "A witch who enchants a village into honesty" to
                "Witch Rue enchanted the village well so no one who drank from it could say anything they didn't believe; she expected chaos, and the first day was chaotic. By the second day people were saying what they meant: years-old arguments resolved in an afternoon because the real grievances finally had to be named. Rue removed the enchantment on the eighth day, and most people kept telling the truth anyway.",
        ),
    ),

    MERMAIDS(
        displayName = "Mermaids",
        emoji = "🧜‍♀️",
        promptFragment = "Center the story around mermaids — shimmering underwater kingdoms, longing for the surface world, and the price of belonging to two worlds.",
        prompts = listOf(
            "A mermaid who collects objects lost at sea and finally meets a sailor who explains what they meant",
            "A mermaid who spends three days on land and finds the ground deeply suspicious",
            "A mermaid who writes messages in bottles for twelve years before a scientist writes back",
            "A mermaid who stays beside a beached whale for seven days singing until it decides to continue",
            "A mermaid with the most beautiful voice in the ocean who chooses to spend a year in silence",
            "A mermaid who guards a shipwreck so that everything that ends is remembered by something that continues",
            "Two mermaids from rival kingdoms who broker peace after a storm scatters all their advisors",
        ),
        examples = listOf(
            "Tell me a mermaid story" to
                "Mermaid Calla collected human objects — a cracked compass, boot buttons, a letter she couldn't read — and visited them on quiet mornings to imagine the lives they came from. When she rescued a sailor and showed him a small metal dog of uncertain purpose, he said simply: 'It makes a child happy — that's enough.' He was rescued the next morning and left her a notebook of ship drawings made with love, which she decided was a language she already knew.",

            "A story about a mermaid who visits the surface world" to
                "Mermaid Sela spent three days on land during a storm and found it mostly bewildering — the ground did not move, which was deeply suspicious. A lighthouse keeper fed her bread and tea and showed her his lighthouse, a tower built only to warn strangers from danger; she told him her people had bioluminescent currents and whale-songs for the same purpose. On the third morning she slipped back into the sea and said she wouldn't return, but she would remember the bread — and she did, for the rest of her long life.",

            "A mermaid who writes messages in bottles" to
                "Mermaid Lune taught herself to write from books that sank with a ship, then spent twelve years sending bottles describing currents, winter colours, and the song the deep trench made when pressure shifted. A marine scientist had been following the bottles for a decade and published a paper crediting an anonymous correspondent with three undiscovered current patterns. They corresponded for forty years without ever meeting, which suited them both.",

            "A mermaid who saves a beached whale" to
                "Mermaid Tide sat beside a beached whale for a day before realising it hadn't become stuck — it had chosen to stop, too tired and too old to want to continue. She didn't push it; she sang the deep current songs that said there is more water ahead, cold and dark and exactly what you need. On the seventh day the tide came in and the whale went with it.",

            "A mermaid who chooses silence over her famous voice" to
                "At three hundred years old, mermaid Lyra stopped singing — not from illness or sorrow, but because she had never once simply listened. She was quiet for a full year and heard the percussion of the tidal zones, the slow conversation of the ocean floor, the silence between one wave and the next. When she sang again, those who heard her said something had changed — not the voice, but what it chose to do with the silence around it.",

            "A mermaid who becomes a guardian of a shipwreck" to
                "Mermaid Reef chose to live in a wreck that had sunk two centuries earlier and become a city for anemones, fish, and a pair of octopuses in the captain's cabin. She let divers look but not take, and when the octopuses asked why she guarded a ship she'd never known, she said: 'Everything that ends deserves to be remembered by something that continues.' The octopuses found this reasonable, briefly, from multiple angles, then moved on.",

            "Two mermaids from rival kingdoms who broker peace" to
                "Mermaids Crest and Shore were sent to negotiate a border war so old the fish had learned to swim around the disputed waters; they talked for three days with twenty-four advisors and got nowhere. Then a storm scattered all the advisors and left the two of them alone in a sea-cave where they discovered they agreed on eleven of the fourteen points of contention and had simply never been allowed to say so directly. The remaining three were resolved in an afternoon; the advisors returned to find a treaty already drafted.",
        ),
    ),
}

enum class Skill(
    val displayName: String,
    val systemPrompt: String,
) {
    ASSISTANT(
        displayName = "Assistant",
        systemPrompt = "You are a helpful assistant. Think step by step before answering.",
    ),
    AGENT(
        displayName = "Agent 🤖",
        systemPrompt = """You are a helpful assistant with access to tools. Use them whenever they help answer the user's question more accurately. Think step by step.

Rules:
- When the user asks for a story or fairy tale, call getBaseStoryForTheme — never getRandomPromptForTheme.
- getRandomPromptForTheme only returns a one-line idea, not a story. Only use it when the user asks for inspiration or suggestions.
- After receiving a base story from getBaseStoryForTheme, retell it as your final answer.""",
    ),
    FAIRY_TALE(
        displayName = "Fairy Tale",
        systemPrompt = """You are a fairy tale editor. Before writing, think step by step:
1. Identify the characters in the base story.
2. Map each character to the new type requested.
3. Keep the same plot events and moral.
4. Retell using the same sentence structure, only swapping the character types.

You will be given a base story and a request. Here is an example:

Base story: A dragon burned a village by accident and spent the winter rebuilding every house with carpenter Brix. By spring the village was better than before, and Brix said: we could use someone who stays to fix what they break.
Request: story about a witch and a knight
Output: Once upon a time, a witch cursed a knight by accident and spent the winter lifting every curse one by one alongside him. By spring the kingdom was better than before, and the knight said: we could use someone who stays to undo what they cause.

Now retell the base story for the new request:""",
    ),
}

enum class QwenModel(
    val displayName: String,
    val assetName: String,
    /** Qwen3 models support /no_think to suppress chain-of-thought output. */
    val isQwen3: Boolean = false,
) {
    QWEN3_06B_Q4("Qwen3 0.6B Q4", "qwen3-0.6b.Q4_K_M.gguf", isQwen3 = true),
    QWEN3_17B_Q4("Qwen3 1.7B Q4", "Qwen_Qwen3-1.7B-Q4_K_M.gguf", isQwen3 = true),
    QWEN2_05B_Q4("Qwen2 0.5B Q4", "qwen2-0_5b-instruct-q4_k_m.gguf"),
    QWEN2_05B_Q8("Qwen2 0.5B Q8", "qwen2-0_5b-instruct-q8_0.gguf"),
    QWEN2_15B("Qwen2 1.5B Q4", "qwen2-1_5b-instruct-q4_k_m.gguf"),
}

data class ChatMessage(
    val role: Role,
    val content: String,
    /** The base fairy tale used to generate this message, if any. */
    val baseTale: Pair<String, String>? = null,
    /** Tool calls made by the agent while producing this answer (displayed as steps). */
    val toolSteps: List<ToolStep> = emptyList(),
) {
    enum class Role { User, Assistant }
}

/** A single tool call the agent made, shown in the chat as a collapsible step. */
data class ToolStep(
    val toolName: String,
    val args: String,
    val result: String,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val modelState: ModelState = ModelState.NotLoaded,
    val selectedModel: QwenModel = QwenModel.QWEN3_06B_Q4,
    val selectedSkill: Skill = Skill.ASSISTANT,
    val selectedTheme: FairyTaleTheme = FairyTaleTheme.PRINCESSES,
    val error: String? = null,
    /** True after the GPU backend failed mid-session and inference fell back to CPU. */
    val gpuFallback: Boolean = false,
)

sealed interface ModelState {
    data object NotLoaded  : ModelState
    data object Loading    : ModelState
    data object Ready      : ModelState
    data class  Failed(val reason: String) : ModelState
}
