package com.example.game.model

enum class GamePhase {
    TITLE,
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER,
}

enum class PlayerForm {
    MECH,
    VEHICLE,
}

enum class LevelZone(val label: String) {
    VIADUCT_APPROACH("ZONE 1 // VIADUCT APPROACH"),
    REFINERY_PLATFORMS("ZONE 2 // ENERGON REFINERY"),
    REACTOR_GAUNTLET("ZONE 3 // REACTOR GAUNTLET"),
    BOSS_ARENA("ALERT // MEGATRONUS MK-IV"),
}

data class InputState(
    val left: Boolean = false,
    val right: Boolean = false,
    val up: Boolean = false,
    val down: Boolean = false,
    val jumpHeld: Boolean = false,
    val jumpJustPressed: Boolean = false,
    val fireHeld: Boolean = false,
    val fireJustPressed: Boolean = false,
    val transformJustPressed: Boolean = false,
    val boostJustPressed: Boolean = false,
)

data class HudState(
    val phase: GamePhase = GamePhase.TITLE,
    val hp: Int = 10,
    val maxHp: Int = 10,
    val energon: Float = 100f,
    val maxEnergon: Float = 100f,
    val form: PlayerForm = PlayerForm.MECH,
    val isTransforming: Boolean = false,
    val chargeRatio: Float = 0f,
    val score: Int = 0,
    val combo: Int = 0,
    val comboTimer: Float = 0f,
    val elapsedSeconds: Float = 0f,
    val relicsCollected: Int = 0,
    val totalRelics: Int = 3,
    val zone: LevelZone = LevelZone.VIADUCT_APPROACH,
    val zoneBannerTimer: Float = 0f,
    val bossActive: Boolean = false,
    val bossOverclock: Boolean = false,
    val overclockBannerTimer: Float = 0f,
    val bossHp: Int = 0,
    val bossMaxHp: Int = 120,
    val bossName: String = "MEGATRONUS MK-IV",
    val progressPercent: Float = 0f,
    val damageTakenCount: Int = 0,
    val enemiesDefeated: Int = 0,
    val rank: String = "B",
    val hintText: String? = null,
)

enum class TileType {
    EMPTY,
    DECK,
    GIRDER,
    PLATFORM,      // One-way pass-through platform
    BOOST_PAD,     // Speed boost deck
    PLASMA_HAZARD, // Damaging energon plasma vat
}

data class MovingPlatform(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val width: Float,
    val height: Float = 6f,
    val moveX: Float,
    val moveY: Float,
    val speed: Float,
    var phase: Float = 0f,
    var x: Float = startX,
    var y: Float = startY,
    var prevX: Float = startX,
    var prevY: Float = startY,
)

data class CrumblePlatform(
    val tileX: Int,
    val tileY: Int,
    var state: Int = 0, // 0=solid, 1=shaking, 2=gone
    var timer: Float = 0f,
)

data class LaserGate(
    val x: Float,
    val topY: Float,
    val bottomY: Float, // Vehicle (13px tall) fits underneath; Mech (24px tall) hits it!
    val cycleOffset: Float = 0f,
    val alwaysOn: Boolean = true,
)

data class BreakableBarrier(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float = 16f,
    val height: Float = 32f,
    var hp: Int = 6,
    var hitFlash: Float = 0f,
    var destroyed: Boolean = false,
)

enum class EnemyType {
    RECON_DRONE,
    HEAVY_TROOPER,
    LASER_TURRET,
    SEEKER_JET,
}

data class Enemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    val homeX: Float,
    val homeY: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val width: Float,
    val height: Float,
    var hp: Int,
    val maxHp: Int,
    var facingRight: Boolean = false,
    var aiTimer: Float = 0f,
    var fireCooldown: Float = 1.2f,
    var hitFlash: Float = 0f,
    var shielded: Boolean = false,
    var alive: Boolean = true,
    var telegraphTimer: Float = 0f,
    var diveTargetX: Float = 0f,
    var diveTargetY: Float = 0f,
)

enum class BossAction {
    HIDDEN,
    INTRO_DROP,
    TRACK_PLAYER,
    TELEGRAPH_VOLLEY,
    FUSION_VOLLEY,
    TELEGRAPH_CHARGE,
    JET_CHARGE,
    AERIAL_BARRAGE,
    DYING,
    DEAD,
}

data class BossState(
    var x: Float = 285f * 16f,
    var y: Float = -48f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val width: Float = 32f,
    val height: Float = 42f,
    var hp: Int = 120,
    val maxHp: Int = 120,
    var facingRight: Boolean = false,
    var action: BossAction = BossAction.HIDDEN,
    var timer: Float = 0f,
    var volleyCount: Int = 0,
    var hitFlash: Float = 0f,
    var chargeTargetX: Float = 0f,
    var dyingExplosionTimer: Float = 0f,
)

enum class PickupType {
    ENERGON_CUBE,
    MATRIX_RELIC,
}

data class Pickup(
    val id: Int,
    val type: PickupType,
    val x: Float,
    val y: Float,
    var collected: Boolean = false,
    val bobOffset: Float = 0f,
)

data class Checkpoint(
    val id: Int,
    val x: Float,
    val y: Float,
    var activated: Boolean = false,
)

data class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var fromPlayer: Boolean,
    val damage: Int,
    var color: Int,
    val radius: Float = 3f,
    var life: Float = 2.0f,
    val piercing: Boolean = false,
    val isRocket: Boolean = false,
    val isChargedBeam: Boolean = false,
    var reflected: Boolean = false,
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Int,
    val size: Int = 2,
    val gravity: Float = 0f,
)

data class WeatherParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val length: Int,
    val color: Int,
)

data class Afterimage(
    val x: Float,
    val y: Float,
    val facingRight: Boolean,
    val isVehicle: Boolean,
    var life: Float,
)

data class FloatingText(
    val text: String,
    var x: Float,
    var y: Float,
    var vy: Float = -24f,
    var life: Float = 0.75f,
    val color: Int,
)

data class SlashArc(
    val x: Float,
    val y: Float,
    val facingRight: Boolean,
    var life: Float = 0.14f,
)

data class WorldHintSign(
    val x: Float,
    val y: Float,
    val text: String,
)
