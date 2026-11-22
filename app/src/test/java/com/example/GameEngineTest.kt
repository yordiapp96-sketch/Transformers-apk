package com.example

import com.example.audio.RetroAudioEngine
import com.example.game.engine.GameEngine
import com.example.game.level.Sector01Level
import com.example.game.model.BossAction
import com.example.game.model.EnemyType
import com.example.game.model.GamePhase
import com.example.game.model.InputState
import com.example.game.model.LevelZone
import com.example.game.model.PlayerForm
import com.example.game.model.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun createEngine(): GameEngine {
        val audio = RetroAudioEngine().apply { isMuted = true }
        return GameEngine(audio = audio).apply { startNewRun() }
    }

    @Test
    fun transform_switchesBetweenMechAndVehicleWithMomentum() {
        val engine = createEngine()
        assertEquals(PlayerForm.MECH, engine.form)

        // Trigger transform to Vehicle (0.18s = 11 frames at 60Hz)
        engine.stepFrame(1f / 60f, InputState(transformJustPressed = true), 426)
        repeat(10) {
            engine.stepFrame(1f / 60f, InputState(), 426)
        }
        assertEquals(PlayerForm.VEHICLE, engine.form)
        assertEquals(GameEngine.VEH_H, engine.playerHeight, 0.01f)
        assertTrue(engine.playerVx >= 150f)
    }

    @Test
    fun vehicleMode_passesSafelyUnderLowLaserGate() {
        val engine = createEngine()
        // Transform to vehicle and position just before laser gate at tile 20
        engine.stepFrame(1f / 60f, InputState(transformJustPressed = true), 426)
        repeat(15) { engine.stepFrame(1f / 60f, InputState(), 426) }
        assertEquals(PlayerForm.VEHICLE, engine.form)

        engine.playerX = 18.5f * Sector01Level.TILE_SIZE
        engine.playerY = 12f * Sector01Level.TILE_SIZE - GameEngine.VEH_H
        val initialHp = engine.hp

        // Drive past tile 20 laser gate
        repeat(35) {
            engine.stepFrame(1f / 60f, InputState(right = true), 426)
        }
        assertTrue(engine.playerX > 21f * Sector01Level.TILE_SIZE)
        assertEquals(initialHp, engine.hp)
    }

    @Test
    fun lowCeiling_preventsStandingUpInMaintenanceTunnel() {
        val engine = createEngine()
        engine.stepFrame(1f / 60f, InputState(transformJustPressed = true), 426)
        repeat(15) { engine.stepFrame(1f / 60f, InputState(), 426) }
        assertEquals(PlayerForm.VEHICLE, engine.form)

        // Place inside Zone 2 low maintenance tunnel (tiles 143..155, floor at y=11*16, ceiling at y=8..9)
        engine.playerX = 144f * Sector01Level.TILE_SIZE
        engine.playerY = 11f * Sector01Level.TILE_SIZE - GameEngine.VEH_H
        engine.onGround = true

        // Try transforming back to Mech under low ceiling -> blocked!
        engine.stepFrame(1f / 60f, InputState(transformJustPressed = true), 426)
        repeat(15) { engine.stepFrame(1f / 60f, InputState(), 426) }
        assertEquals(PlayerForm.VEHICLE, engine.form)
    }

    @Test
    fun mechDoubleJump_triggersMidAir() {
        val engine = createEngine()
        engine.stepFrame(1f / 60f, InputState(jumpJustPressed = true, jumpHeld = true), 426)
        assertTrue(engine.playerVy < -200f)

        // Hold jump for 15 frames to reach apex, then trigger double jump mid-air
        repeat(15) {
            engine.stepFrame(1f / 60f, InputState(jumpHeld = true), 426)
        }
        assertFalse(engine.onGround)
        engine.stepFrame(1f / 60f, InputState(jumpJustPressed = true, jumpHeld = true), 426)
        assertTrue(engine.playerVy < -200f)
        assertFalse(engine.doubleJumpAvailable)
    }

    @Test
    fun vehicleBoost_smashesThroughBreakableBarrier() {
        val engine = createEngine()
        engine.stepFrame(1f / 60f, InputState(transformJustPressed = true), 426)
        repeat(15) { engine.stepFrame(1f / 60f, InputState(), 426) }

        val barrier = engine.level.barriers.first()
        assertFalse(barrier.destroyed)
        engine.playerX = barrier.x - GameEngine.VEH_W - 8f
        engine.playerY = 12f * Sector01Level.TILE_SIZE - GameEngine.VEH_H

        // Activate Nitro Boost and drive into barrier
        engine.stepFrame(1f / 60f, InputState(boostJustPressed = true, right = true), 426)
        repeat(20) {
            engine.stepFrame(1f / 60f, InputState(right = true), 426)
        }
        assertTrue(barrier.destroyed)
        assertTrue(engine.score >= 150)
    }

    @Test
    fun bossEncounter_locksArenaAndVictoryTriggersOnDefeat() {
        val engine = createEngine()
        engine.playerX = Sector01Level.BOSS_TRIGGER_X + 10f
        engine.playerY = 12f * Sector01Level.TILE_SIZE - GameEngine.MECH_H
        engine.stepFrame(1f / 60f, InputState(), 426)

        assertTrue(engine.arenaLocked)
        assertEquals(LevelZone.BOSS_ARENA, engine.currentZone)
        assertEquals(BossAction.INTRO_DROP, engine.boss.action)

        // Simulate landing & defeating boss
        repeat(45) { engine.stepFrame(1f / 60f, InputState(), 426) }
        engine.boss.hp = 1
        engine.playerX = engine.boss.x - 20f
        engine.playerY = engine.boss.y + 10f
        engine.facingRight = true
        engine.stepFrame(1f / 60f, InputState(fireJustPressed = true), 426)

        assertEquals(BossAction.DYING, engine.boss.action)
        // Advance through dying explosion sequence & victory delay
        repeat(180) { engine.stepFrame(1f / 60f, InputState(), 426) }
        assertEquals(GamePhase.VICTORY, engine.phase)
        assertTrue(engine.computeRank() in listOf("S", "A", "B", "C"))
    }

    @Test
    fun mechChargedBeam_piercesAndBreaksHeavyShield() {
        val engine = createEngine()
        val trooper = engine.level.enemies.first { it.type == EnemyType.HEAVY_TROOPER && it.shielded }
        // Position Mech facing left/right toward the trooper from safe distance
        engine.playerX = trooper.x - 70f
        engine.playerY = trooper.y + trooper.height - GameEngine.MECH_H
        engine.facingRight = true
        trooper.facingRight = false

        // Hold fire for >= 0.45s (30 frames at 60Hz) to charge plasma cannon
        repeat(30) {
            engine.stepFrame(1f / 60f, InputState(fireHeld = true), 426)
        }
        assertTrue(engine.chargeTimer >= GameEngine.CHARGE_THRESHOLD)

        // Release fire to shoot piercing charged beam
        engine.stepFrame(1f / 60f, InputState(fireHeld = false), 426)
        assertTrue(engine.projectiles.any { it.isChargedBeam && it.piercing })

        // Step until beam hits trooper's front shield and shatters it
        repeat(15) {
            engine.stepFrame(1f / 60f, InputState(), 426)
        }
        assertFalse(trooper.shielded)
    }

    @Test
    fun energonBladeParry_deflectsEnemyBoltAndAwardsBonus() {
        val engine = createEngine()
        engine.playerX = 96f
        engine.playerY = 12f * Sector01Level.TILE_SIZE - GameEngine.MECH_H
        engine.facingRight = true

        // Place an incoming enemy projectile inside the Mech slash hitbox
        val bolt = Projectile(
            x = engine.playerX + GameEngine.MECH_W + 10f,
            y = engine.playerY + 10f,
            vx = -120f,
            vy = 0f,
            fromPlayer = false,
            damage = 1,
            color = 0xFFFF2A55.toInt(),
        )
        engine.projectiles += bolt
        val initialScore = engine.score

        engine.stepFrame(1f / 60f, InputState(fireJustPressed = true), 426)

        assertTrue(bolt.fromPlayer)
        assertTrue(bolt.reflected)
        assertTrue(bolt.vx > 0f)
        assertEquals(initialScore + 200, engine.score)
    }

    @Test
    fun bossPhase2Overclock_triggersAtHalfHp() {
        val engine = createEngine()
        engine.playerX = Sector01Level.BOSS_TRIGGER_X + 10f
        engine.playerY = 12f * Sector01Level.TILE_SIZE - GameEngine.MECH_H
        engine.stepFrame(1f / 60f, InputState(), 426)
        repeat(45) { engine.stepFrame(1f / 60f, InputState(), 426) }

        assertFalse(engine.bossOverclockTriggered)
        engine.boss.hp = (engine.boss.maxHp / 2) + 1
        engine.playerX = engine.boss.x - 20f
        engine.playerY = engine.boss.y + 10f
        engine.facingRight = true

        engine.stepFrame(1f / 60f, InputState(fireJustPressed = true), 426)

        assertTrue(engine.bossOverclockTriggered)
        assertTrue(engine.overclockBannerTimer > 0f)
    }
}
