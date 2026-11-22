package com.example.game.level

import com.example.game.model.BreakableBarrier
import com.example.game.model.Checkpoint
import com.example.game.model.CrumblePlatform
import com.example.game.model.Enemy
import com.example.game.model.EnemyType
import com.example.game.model.LaserGate
import com.example.game.model.MovingPlatform
import com.example.game.model.Pickup
import com.example.game.model.PickupType
import com.example.game.model.TileType
import com.example.game.model.WorldHintSign

/**
 * Handcrafted Sector 01: Energon Viaduct (304 x 15 tiles, 16px per tile = 4864 x 240 virtual world).
 * Designed around seamless switching between Bipedal Mech (vertical platforming, double-jump, melee/aimed shots)
 * and Cyber-Rig Vehicle (low clearance laser slides, high speed, nitro ramming through barricades).
 */
class Sector01Level {
    companion object {
        const val TILE_SIZE = 16
        const val WIDTH_TILES = 304
        const val HEIGHT_TILES = 15
        const val WORLD_WIDTH = WIDTH_TILES * TILE_SIZE
        const val WORLD_HEIGHT = HEIGHT_TILES * TILE_SIZE
        const val BOSS_TRIGGER_X = 256f * TILE_SIZE
        const val ARENA_LEFT_X = 250f * TILE_SIZE
        const val ARENA_RIGHT_X = 296f * TILE_SIZE
    }

    val tiles: Array<Array<TileType>> = Array(HEIGHT_TILES) { Array(WIDTH_TILES) { TileType.EMPTY } }
    val movingPlatforms = mutableListOf<MovingPlatform>()
    val crumblePlatforms = mutableListOf<CrumblePlatform>()
    val laserGates = mutableListOf<LaserGate>()
    val barriers = mutableListOf<BreakableBarrier>()
    val enemies = mutableListOf<Enemy>()
    val pickups = mutableListOf<Pickup>()
    val checkpoints = mutableListOf<Checkpoint>()
    val hintSigns = mutableListOf<WorldHintSign>()

    init {
        buildLevel()
    }

    fun getTile(tx: Int, ty: Int): TileType {
        if (tx < 0 || tx >= WIDTH_TILES) return TileType.GIRDER
        if (ty < 0) return TileType.EMPTY
        if (ty >= HEIGHT_TILES) return TileType.EMPTY
        return tiles[ty][tx]
    }

    private fun fillRect(x0: Int, y0: Int, x1: Int, y1: Int, type: TileType) {
        for (y in y0..y1) {
            for (x in x0..x1) {
                if (x in 0 until WIDTH_TILES && y in 0 until HEIGHT_TILES) {
                    tiles[y][x] = type
                }
            }
        }
    }

    private fun deckColumn(x0: Int, x1: Int, topY: Int) {
        fillRect(x0, topY, x1, topY, TileType.DECK)
        if (topY + 1 < HEIGHT_TILES) {
            fillRect(x0, topY + 1, x1, HEIGHT_TILES - 1, TileType.GIRDER)
        }
    }

    private fun platformSpan(x0: Int, x1: Int, y: Int) {
        fillRect(x0, y, x1, y, TileType.PLATFORM)
    }

    private fun buildLevel() {
        // Left boundary wall
        fillRect(0, 0, 0, HEIGHT_TILES - 1, TileType.GIRDER)

        // ============================================================
        // ZONE 1: CYBER-HIGHWAY VIADUCT APPROACH (tiles 1..78)
        // ============================================================
        deckColumn(1, 38, 12)
        // Overhead structure supporting the first Low Laser Gate (clearance = 32px, beam hangs down to 176.8px)
        // Vehicle (179..192) slides right under! Mech (168..192) hits the laser beam!
        fillRect(18, 0, 26, 9, TileType.GIRDER)
        laserGates += LaserGate(
            x = 20f * TILE_SIZE,
            topY = 10f * TILE_SIZE,
            bottomY = 11.05f * TILE_SIZE,
            alwaysOn = true,
        )
        hintSigns += WorldHintSign(14f * TILE_SIZE, 9.2f * TILE_SIZE, "SHIFT VEHICLE")

        // Speed Boost Pad into Destructible Energon Barricade!
        fillRect(29, 12, 31, 12, TileType.BOOST_PAD)
        hintSigns += WorldHintSign(28f * TILE_SIZE, 9.2f * TILE_SIZE, "BOOST RAM!")
        barriers += BreakableBarrier(id = 1, x = 35f * TILE_SIZE, y = 10f * TILE_SIZE)

        // Gap 1 + Overpass with Hidden Matrix Relic #1
        fillRect(39, 14, 41, 14, TileType.PLASMA_HAZARD)
        deckColumn(42, 60, 12)
        // Cargo block step-up for double jump to upper overpass
        fillRect(45, 10, 47, 11, TileType.GIRDER)
        fillRect(45, 10, 47, 10, TileType.DECK)
        platformSpan(48, 55, 6)
        platformSpan(57, 61, 8)
        pickups += Pickup(1, PickupType.MATRIX_RELIC, 51.5f * TILE_SIZE, 4.5f * TILE_SIZE, bobOffset = 0f)

        // Second lower highway span with enemies
        deckColumn(61, 78, 11)
        platformSpan(65, 70, 7)
        pickups += Pickup(2, PickupType.ENERGON_CUBE, 67.5f * TILE_SIZE, 5.5f * TILE_SIZE, bobOffset = 0.8f)

        enemies += Enemy(1, EnemyType.RECON_DRONE, 32f * TILE_SIZE, 8f * TILE_SIZE, 32f * TILE_SIZE, 8f * TILE_SIZE, width = 18f, height = 14f, hp = 3, maxHp = 3)
        enemies += Enemy(2, EnemyType.RECON_DRONE, 52f * TILE_SIZE, 9f * TILE_SIZE, 52f * TILE_SIZE, 9f * TILE_SIZE, width = 18f, height = 14f, hp = 3, maxHp = 3)
        enemies += Enemy(3, EnemyType.HEAVY_TROOPER, 68f * TILE_SIZE, 9f * TILE_SIZE, 68f * TILE_SIZE, 9f * TILE_SIZE, width = 18f, height = 22f, hp = 6, maxHp = 6, shielded = true)

        // ============================================================
        // ZONE 2: ENERGON REFINERY PLATFORMS (tiles 79..164)
        // ============================================================
        deckColumn(79, 89, 11)
        checkpoints += Checkpoint(1, 81f * TILE_SIZE, 11f * TILE_SIZE - 14f)
        pickups += Pickup(3, PickupType.ENERGON_CUBE, 85f * TILE_SIZE, 9.5f * TILE_SIZE, bobOffset = 1.5f)

        // Plasma Vat Floor (tiles 90..139) with Hover-Lifts & Refinery Platforms
        fillRect(90, 14, 139, 14, TileType.PLASMA_HAZARD)

        // Horizontal Hover-Lift 1
        movingPlatforms += MovingPlatform(
            id = 1,
            startX = 91f * TILE_SIZE,
            startY = 10.5f * TILE_SIZE,
            width = 40f,
            moveX = 48f,
            moveY = 0f,
            speed = 1.4f,
        )
        // Mid Refinery Island + Laser Turret
        deckColumn(99, 108, 10)
        platformSpan(101, 106, 6)
        enemies += Enemy(4, EnemyType.LASER_TURRET, 104f * TILE_SIZE, 10f * TILE_SIZE - 12f, 104f * TILE_SIZE, 10f * TILE_SIZE - 12f, width = 16f, height = 12f, hp = 4, maxHp = 4)
        enemies += Enemy(5, EnemyType.RECON_DRONE, 103f * TILE_SIZE, 4.2f * TILE_SIZE, 103f * TILE_SIZE, 4.2f * TILE_SIZE, width = 18f, height = 14f, hp = 3, maxHp = 3)

        // Vertical Hover-Lift 2
        movingPlatforms += MovingPlatform(
            id = 2,
            startX = 111f * TILE_SIZE,
            startY = 10.5f * TILE_SIZE,
            width = 36f,
            moveX = 0f,
            moveY = -56f,
            speed = 1.6f,
        )

        // Upper Refinery Catwalk + Crumbling Grates
        platformSpan(116, 123, 7)
        pickups += Pickup(4, PickupType.ENERGON_CUBE, 119f * TILE_SIZE, 5.5f * TILE_SIZE, bobOffset = 0.4f)

        for (tx in 125..137 step 3) {
            crumblePlatforms += CrumblePlatform(tileX = tx, tileY = 8)
            crumblePlatforms += CrumblePlatform(tileX = tx + 1, tileY = 8)
        }
        enemies += Enemy(6, EnemyType.RECON_DRONE, 131f * TILE_SIZE, 5f * TILE_SIZE, 131f * TILE_SIZE, 5f * TILE_SIZE, width = 18f, height = 14f, hp = 3, maxHp = 3)

        // Maintenance Deck with Hidden Matrix Relic #2 behind Barricade
        deckColumn(140, 164, 11)
        // Upper route
        platformSpan(143, 155, 7)
        // Lower maintenance tunnel (Vehicle slide under ceiling + break barrier!)
        fillRect(142, 8, 156, 9, TileType.GIRDER)
        barriers += BreakableBarrier(id = 2, x = 146f * TILE_SIZE, y = 9f * TILE_SIZE)
        pickups += Pickup(5, PickupType.MATRIX_RELIC, 152f * TILE_SIZE, 9.8f * TILE_SIZE, bobOffset = 2.1f)

        enemies += Enemy(7, EnemyType.HEAVY_TROOPER, 149f * TILE_SIZE, 5f * TILE_SIZE, 149f * TILE_SIZE, 5f * TILE_SIZE, width = 18f, height = 22f, hp = 6, maxHp = 6, shielded = true)
        enemies += Enemy(8, EnemyType.LASER_TURRET, 159f * TILE_SIZE, 11f * TILE_SIZE - 12f, 159f * TILE_SIZE, 11f * TILE_SIZE - 12f, width = 16f, height = 12f, hp = 4, maxHp = 4)

        // ============================================================
        // ZONE 3: PLASMA REACTOR GAUNTLET (tiles 165..247)
        // ============================================================
        deckColumn(165, 184, 11)
        checkpoints += Checkpoint(2, 166.5f * TILE_SIZE, 11f * TILE_SIZE - 14f)

        // Laser Gate + Boost Pad Ramp combo!
        fillRect(172, 0, 178, 8, TileType.GIRDER)
        laserGates += LaserGate(x = 174f * TILE_SIZE, topY = 9f * TILE_SIZE, bottomY = 10.05f * TILE_SIZE)
        fillRect(180, 11, 183, 11, TileType.BOOST_PAD)
        hintSigns += WorldHintSign(179f * TILE_SIZE, 8.2f * TILE_SIZE, "JUMP + SHIFT")

        // Reactor Abyss with Hover-Lifts & Crumble Bridges + Seeker Jets!
        fillRect(185, 14, 232, 14, TileType.PLASMA_HAZARD)
        deckColumn(190, 200, 10)
        barriers += BreakableBarrier(id = 3, x = 194f * TILE_SIZE, y = 8f * TILE_SIZE)
        enemies += Enemy(9, EnemyType.SEEKER_JET, 198f * TILE_SIZE, 3.8f * TILE_SIZE, 198f * TILE_SIZE, 3.8f * TILE_SIZE, width = 24f, height = 12f, hp = 4, maxHp = 4)

        movingPlatforms += MovingPlatform(
            id = 3,
            startX = 202f * TILE_SIZE,
            startY = 10f * TILE_SIZE,
            width = 40f,
            moveX = 52f,
            moveY = -16f,
            speed = 1.5f,
        )

        deckColumn(210, 224, 9)
        platformSpan(214, 222, 5)
        // Hidden Matrix Relic #3 high above reactor spine!
        pickups += Pickup(6, PickupType.MATRIX_RELIC, 218f * TILE_SIZE, 3.2f * TILE_SIZE, bobOffset = 1.0f)
        enemies += Enemy(10, EnemyType.HEAVY_TROOPER, 217f * TILE_SIZE, 7f * TILE_SIZE, 217f * TILE_SIZE, 7f * TILE_SIZE, width = 18f, height = 22f, hp = 7, maxHp = 7, shielded = true)
        enemies += Enemy(11, EnemyType.SEEKER_JET, 226f * TILE_SIZE, 4.2f * TILE_SIZE, 226f * TILE_SIZE, 4.2f * TILE_SIZE, width = 24f, height = 12f, hp = 4, maxHp = 4)

        for (tx in 226..230 step 2) {
            crumblePlatforms += CrumblePlatform(tileX = tx, tileY = 9)
        }

        // Pre-Boss Supply Deck
        deckColumn(233, 249, 11)
        pickups += Pickup(7, PickupType.ENERGON_CUBE, 240f * TILE_SIZE, 9.5f * TILE_SIZE, bobOffset = 0.2f)
        pickups += Pickup(8, PickupType.ENERGON_CUBE, 243f * TILE_SIZE, 9.5f * TILE_SIZE, bobOffset = 1.1f)
        enemies += Enemy(12, EnemyType.RECON_DRONE, 238f * TILE_SIZE, 6.5f * TILE_SIZE, 238f * TILE_SIZE, 6.5f * TILE_SIZE, width = 18f, height = 14f, hp = 3, maxHp = 3)

        // ============================================================
        // ZONE 4: BOSS ARENA — MEGATRONUS MK-IV (tiles 250..298)
        // ============================================================
        deckColumn(250, 298, 12)
        // Side tactical catwalks for vertical dodging & Mech combat
        platformSpan(255, 261, 8)
        platformSpan(286, 292, 8)
        // Right boundary wall
        fillRect(298, 0, 303, HEIGHT_TILES - 1, TileType.GIRDER)
    }
}
