## Ben Carroll's Robocode Bot: Feral

### Overview

Feral is a bot built to participate in a class melee competition, and is built of the AdvancedRobot class.

### Targeting

Feral uses a simple linear targeting algorithm to shoot at enemies predicted positions. To find and gather data about enemies, it locks its radar onto the closest bot to get data as fast as possible. Every 50 turns, it turns the radar 360 degrees to re-calculate the closest bot.

### Movement

Feral uses oscillator movement, and keeps itself perpendicular to its angle to the enemy. It chooses a random 

### Other Features
