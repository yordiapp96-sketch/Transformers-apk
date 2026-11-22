package com.example.game.engine

import com.example.audio.RetroAudioEngine
import com.example.audio.RetroAudioEngine.Sfx
import com.example.game.level.Sector01Level
import com.example.game.model.Afterimage
import com.example.game.model.BossAction
import com.example.game.model.BossState
import com.example.game.model.Enemy
import com.example.game.model.EnemyType
import com.example.game.model.FloatingText
import com.example.game.model.GamePhase
import com.example.game.model.HudState
import com.example.game.model.InputState
import com.example.game.model.LevelZone
import com.example.game.model.MovingPlatform
import com.example.game.model.Particle
import com.example.game.model.PickupType
import com.example.game.model.PlayerForm
import com.example.game.model.Projectile
import com.example.game.model.SlashArc
import com.example.game.model.TileType
import com.example.game.model.WeatherParticle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic 60Hz fixed-timestep game simulation for Sector 01: Energon Viaduct.
 */
class GameEngine(
    private val audio: RetroAudioEngine,
    private val onHapticEvent: (HapticKind) -> Unit = {},
) {
    enum class HapticKind { LIGHT, MEDIUM, HEAVY }

    companion object {
        const val FIXED_DT = 1f / 60f
        const val VIRTUAL_HEIGHT = 240
        const val MECH_W = 14f
        const val MECH_H = 24f
        const val VEH_W = 24f
        const val VEH_H = 13f
        const val CHARGE_THRESHOLD = 0.45f

        private fun rgb(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    var level = Sector01Level()
        private set

    var phase: GamePhase = GamePhase.TITLE
        private set

    // Player physics & combat state
    var playerX = 48f
    var playerY = 12f * Sector01Level.TILE_SIZE - MECH_H
    var playerVx = 0f
    var playerVy = 0f
    var facingRight = true
    var form = PlayerForm.MECH
    var onGround = false
    var doubleJumpAvailable = true
    var coyoteTimer = 0f
    var jumpBufferTimer = 0f

    var transformTimer = 0f
    var targetForm = PlayerForm.MECH
    var boostTimer = 0f
    var fireCooldown = 0f
    var slashCooldown = 0f
    var chargeTimer = 0f
    var chargeReadyNotified = false
    var invulnTimer = 0f
    var animClock = 0f
    var runAnimTimer = 0f
    var suspensionCompression = 0f

    var hp = 10
    val maxHp = 10
    var energon = 100f
    val maxEnergon = 100f
    var score = 0
    var combo = 0
    var comboTimer = 0f
    var elapsedSeconds = 0f
    var relicsCollected = 0
    var damageTakenCount = 0
    var enemiesDefeated = 0
    var currentZone = LevelZone.VIADUCT_APPROACH
    var zoneBannerTimer = 2.6f
    var overclockBannerTimer = 0f
    var bossOverclockTriggered = false
    var activeHintText: String? = null
    var hintTimer = 0f

    var respawnX = 48f
    var respawnY = 12f * Sector01Level.TILE_SIZE - MECH_H
    var lastSafeX = 48f
    var lastSafeY = 12f * Sector01Level.TILE_SIZE - MECH_H

    // Camera & Juice
    var cameraX = 0f
    var shakeTimer = 0f
    var shakeIntensity = 0f
    var hitStopTimer = 0f
    var arenaLocked = false
    var victoryDelayTimer = 0f

    var boss = BossState()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<Particle>()
    val weatherParticles = mutableListOf<WeatherParticle>()
    val afterimages = mutableListOf<Afterimage>()
    val floatingTexts = mutableListOf<FloatingText>()
    val slashArcs = mutableListOf<SlashArc>()

    private var accumulator = 0f
    private val rng = Random(2026)

    val playerWidth: Float get() = if (form == PlayerForm.VEHICLE) VEH_W else MECH_W
    val playerHeight: Float get() = if (form == PlayerForm.VEHICLE) VEH_H else MECH_H

    fun startNewRun() {
        level = Sector01Level()
        phase = GamePhase.PLAYING
        playerX = 48f
        playerY = 12f * Sector01Level.TILE_SIZE - MECH_H
        playerVx = 0f
        playerVy = 0f
        facingRight = true
        form = PlayerForm.MECH
        targetForm = PlayerForm.MECH
        onGround = true
        doubleJumpAvailable = true
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        transformTimer = 0f
        boostTimer = 0f
        fireCooldown = 0f
        slashCooldown = 0f
        chargeTimer = 0f
        chargeReadyNotified = false
        invulnTimer = 0f
        animClock = 0f
        runAnimTimer = 0f
        suspensionCompression = 0f

        hp = maxHp
        energon = maxEnergon
        score = 0
        combo = 0
        comboTimer = 0f
        elapsedSeconds = 0f
        relicsCollected = 0
        damageTakenCount = 0
        enemiesDefeated = 0
        currentZone = LevelZone.VIADUCT_APPROACH
        zoneBannerTimer = 2.8f
        overclockBannerTimer = 0f
        bossOverclockTriggered = false
        activeHintText = "HOLD FIRE TO CHARGE BEAM • SLASH TO PARRY BOLTS"
        hintTimer = 4.5f

        respawnX = playerX
        respawnY = playerY
        lastSafeX = playerX
        lastSafeY = playerY

        cameraX = 0f
        shakeTimer = 0f
        shakeIntensity = 0f
        hitStopTimer = 0f
        arenaLocked = false
        victoryDelayTimer = 0f

        boss = BossState()
        projectiles.clear()
        particles.clear()
        weatherParticles.clear()
        afterimages.clear()
        floatingTexts.clear()
        slashArcs.clear()
        accumulator = 0f
        initWeather(426)

        // Drop-in thruster puff
        spawnBurst(playerX + 7f, playerY + 20f, rgb(0, 229, 255), 14, 90f)
    }

    fun setPaused(paused: Boolean) {
        if (paused && phase == GamePhase.PLAYING) {
            phase = GamePhase.PAUSED
        } else if (!paused && phase == GamePhase.PAUSED) {
            phase = GamePhase.PLAYING
        }
    }

    fun returnToTitle() {
        phase = GamePhase.TITLE
    }

    fun stepFrame(deltaSeconds: Float, input: InputState, virtualWidth: Int) {
        if (phase != GamePhase.PLAYING) return
        val clamped = deltaSeconds.coerceIn(0f, 0.05f)
        accumulator += clamped
        var usedOneShot = false
        while (accumulator >= FIXED_DT) {
            val stepInput = if (!usedOneShot) {
                usedOneShot = true
                input
            } else {
                input.copy(
                    jumpJustPressed = false,
                    fireJustPressed = false,
                    transformJustPressed = false,
                    boostJustPressed = false,
                )
            }
            fixedUpdate(FIXED_DT, stepInput, virtualWidth)
            accumulator -= FIXED_DT
        }
    }

    private fun fixedUpdate(dt: Float, input: InputState, virtualWidth: Int) {
        animClock += dt
        if (shakeTimer > 0f) shakeTimer = max(0f, shakeTimer - dt)
        if (zoneBannerTimer > 0f) zoneBannerTimer = max(0f, zoneBannerTimer - dt)
        if (overclockBannerTimer > 0f) overclockBannerTimer = max(0f, overclockBannerTimer - dt)
        if (hintTimer > 0f) {
            hintTimer = max(0f, hintTimer - dt)
            if (hintTimer <= 0f) activeHintText = null
        }

        // Hit-stop freezes world simulation briefly for heavy impact juice
        if (hitStopTimer > 0f) {
            hitStopTimer = max(0f, hitStopTimer - dt)
            updateParticlesOnly(dt)
            return
        }

        elapsedSeconds += dt
        if (invulnTimer > 0f) invulnTimer = max(0f, invulnTimer - dt)
        if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)
        if (slashCooldown > 0f) slashCooldown = max(0f, slashCooldown - dt)
        if (suspensionCompression > 0f) suspensionCompression = max(0f, suspensionCompression - dt * 6f)

        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) combo = 0
        }

        // Passive Energon recharge
        energon = min(maxEnergon, energon + 14f * dt)

        updateMovingPlatforms(dt)
        updateCrumblePlatforms(dt)
        updatePlayer(dt, input)
        updateBarriers(dt)
        updateLaserGates()
        updateCheckpointsAndPickups()
        updateEnemies(dt)
        updateBoss(dt)
        updateProjectiles(dt, virtualWidth)
        updateParticlesOnly(dt)
        updateZonesAndCamera(dt, virtualWidth)
        updateWeather(dt, virtualWidth)

        if (victoryDelayTimer > 0f) {
            victoryDelayTimer -= dt
            if (victoryDelayTimer <= 0f) {
                phase = GamePhase.VICTORY
                audio.play(Sfx.VICTORY)
            }
        }
    }

    private fun updateMovingPlatforms(dt: Float) {
        for (mp in level.movingPlatforms) {
            mp.prevX = mp.x
            mp.prevY = mp.y
            mp.phase += dt * mp.speed
            val factor = (1f - cos(mp.phase)) * 0.5f
            mp.x = mp.startX + mp.moveX * factor
            mp.y = mp.startY + mp.moveY * factor
        }
    }

    private fun updateCrumblePlatforms(dt: Float) {
        for (cp in level.crumblePlatforms) {
            if (cp.state == 1) {
                cp.timer -= dt
                if (cp.timer <= 0f) {
                    cp.state = 2
                    cp.timer = 2.4f
                    spawnBurst(
                        cp.tileX * 16f + 8f,
                        cp.tileY * 16f + 4f,
                        rgb(255, 140, 0),
                        6,
                        55f,
                    )
                }
            } else if (cp.state == 2) {
                cp.timer -= dt
                if (cp.timer <= 0f) {
                    cp.state = 0
                }
            }
        }
    }

    private fun updatePlayer(dt: Float, input: InputState) {
        // Transformation input
        if (input.transformJustPressed && transformTimer <= 0f) {
            if (form == PlayerForm.MECH) {
                startTransform(PlayerForm.VEHICLE)
            } else {
                if (canStandUpAsMech()) {
                    startTransform(PlayerForm.MECH)
                } else {
                    addFloatingText("LOW CEILING!", playerX + 2f, playerY - 8f, rgb(255, 193, 7))
                }
            }
        }

        if (transformTimer > 0f) {
            transformTimer -= dt
            if (transformTimer <= 0f) {
                val oldH = playerHeight
                form = targetForm
                val newH = playerHeight
                playerY += (oldH - newH)
                if (form == PlayerForm.VEHICLE) {
                    // Preserve or boost forward speed when dropping into cyber-rig!
                    val dir = if (facingRight) 1f else -1f
                    if (abs(playerVx) < 165f) playerVx = dir * 165f
                }
            }
        }

        // Boost / Overdrive input
        if (input.boostJustPressed && boostTimer <= 0f && energon >= 30f && transformTimer <= 0f) {
            energon -= 30f
            boostTimer = if (form == PlayerForm.VEHICLE) 0.38f else 0.22f
            val dir = if (facingRight) 1f else -1f
            playerVx = dir * if (form == PlayerForm.VEHICLE) 315f else 285f
            if (form == PlayerForm.MECH) playerVy = min(playerVy, 0f)
            audio.play(Sfx.BOOST)
            onHapticEvent(HapticKind.LIGHT)
            triggerShake(0.12f, 2f)
            addFloatingText(
                if (form == PlayerForm.VEHICLE) "NITRO RAM!" else "PHASE DASH!",
                playerX,
                playerY - 10f,
                rgb(0, 229, 255),
            )
            spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight * 0.5f, rgb(0, 229, 255), 10, 110f)
        }

        if (boostTimer > 0f) {
            boostTimer -= dt
            val dir = if (facingRight) 1f else -1f
            playerVx = dir * if (form == PlayerForm.VEHICLE) 305f else 270f
            if (form == PlayerForm.MECH) playerVy = 0f
            if ((animClock * 60f).toInt() % 3 == 0) {
                afterimages += Afterimage(playerX, playerY, facingRight, form == PlayerForm.VEHICLE, 0.16f)
            }
            if (form == PlayerForm.VEHICLE) {
                spawnParticle(
                    x = playerX + (if (facingRight) -2f else playerWidth + 2f),
                    y = playerY + playerHeight - 4f,
                    vx = -dir * (60f + rng.nextFloat() * 50f),
                    vy = (rng.nextFloat() - 0.5f) * 28f,
                    life = 0.20f,
                    color = if (rng.nextBoolean()) rgb(255, 130, 0) else rgb(0, 229, 255),
                    size = 2,
                )
            }
        } else {
            // Horizontal movement
            val moveDir = (if (input.right) 1f else 0f) - (if (input.left) 1f else 0f)
            if (moveDir != 0f) {
                facingRight = moveDir > 0f
            }

            val maxSpeed = if (form == PlayerForm.VEHICLE) 215f else 128f
            val accel = if (form == PlayerForm.VEHICLE) {
                if (onGround) 720f else 480f
            } else {
                if (onGround) 980f else 720f
            }
            val decel = if (form == PlayerForm.VEHICLE) {
                if (onGround) 520f else 220f
            } else {
                if (onGround) 1350f else 360f
            }

            if (moveDir != 0f) {
                // Extra snap when reversing direction + Vehicle drift sparks!
                val turning = (moveDir > 0f && playerVx < 0f) || (moveDir < 0f && playerVx > 0f)
                if (turning && form == PlayerForm.VEHICLE && onGround && abs(playerVx) > 105f && (animClock * 60f).toInt() % 2 == 0) {
                    spawnParticle(
                        x = playerX + playerWidth * 0.5f,
                        y = playerY + playerHeight - 1f,
                        vx = playerVx * 0.35f + (rng.nextFloat() - 0.5f) * 40f,
                        vy = -35f - rng.nextFloat() * 30f,
                        life = 0.18f,
                        color = if (rng.nextBoolean()) rgb(255, 193, 7) else rgb(220, 230, 245),
                        size = 2,
                        gravity = 160f,
                    )
                }
                val stepAccel = accel * (if (turning) 1.8f else 1.0f) * dt
                playerVx = (playerVx + moveDir * stepAccel).coerceIn(-maxSpeed, maxSpeed)
            } else {
                if (abs(playerVx) <= decel * dt) {
                    playerVx = 0f
                } else {
                    playerVx -= kotlin.math.sign(playerVx) * decel * dt
                }
            }
        }

        // Check boost pads underneath
        if (onGround) {
            val centerTileX = ((playerX + playerWidth * 0.5f) / 16f).toInt()
            val footTileY = ((playerY + playerHeight + 1f) / 16f).toInt()
            if (level.getTile(centerTileX, footTileY) == TileType.BOOST_PAD) {
                playerVx = max(playerVx, 275f)
                facingRight = true
                if ((animClock * 60f).toInt() % 4 == 0) {
                    spawnParticle(playerX, playerY + playerHeight - 2f, -90f, -25f, 0.25f, rgb(0, 229, 255), 2)
                }
            }
        }

        // Coyote & Jump Buffer
        if (onGround) {
            coyoteTimer = 0.11f
            doubleJumpAvailable = true
        } else if (coyoteTimer > 0f) {
            coyoteTimer = max(0f, coyoteTimer - dt)
        }

        if (input.jumpJustPressed) {
            jumpBufferTimer = 0.12f
        } else if (jumpBufferTimer > 0f) {
            jumpBufferTimer = max(0f, jumpBufferTimer - dt)
        }

        // Execute Jump / Drop-through / Double Jump
        if (jumpBufferTimer > 0f) {
            if (input.down && onGround && isStandingOnOneWayPlatform()) {
                // Drop cleanly through one-way catwalk
                playerY += 3f
                onGround = false
                coyoteTimer = 0f
                jumpBufferTimer = 0f
            } else if (coyoteTimer > 0f) {
                playerVy = if (form == PlayerForm.VEHICLE) -205f else -272f
                onGround = false
                coyoteTimer = 0f
                jumpBufferTimer = 0f
                audio.play(Sfx.JUMP)
                spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight, rgb(180, 240, 255), 5, 45f)
            } else if (form == PlayerForm.MECH && doubleJumpAvailable) {
                doubleJumpAvailable = false
                playerVy = -242f
                jumpBufferTimer = 0f
                audio.play(Sfx.DOUBLE_JUMP)
                spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight, rgb(0, 229, 255), 10, 75f)
            }
        }

        // Variable jump height cut when button released early
        if (!input.jumpHeld && playerVy < -90f) {
            playerVy *= 0.58f
        }

        // Gravity (faster fall for snappy platforming)
        if (boostTimer <= 0f || form == PlayerForm.VEHICLE) {
            val grav = if (playerVy < 0f) 760f else 1040f
            playerVy = min(380f, playerVy + grav * dt)
        }

        // Move X and resolve solid tile + barrier collisions
        val prevY = playerY
        playerX += playerVx * dt
        resolveHorizontalCollisions()

        // Clamp within Boss Arena if locked
        if (arenaLocked) {
            playerX = playerX.coerceIn(Sector01Level.ARENA_LEFT_X + 2f, Sector01Level.ARENA_RIGHT_X - playerWidth - 2f)
        } else {
            playerX = playerX.coerceIn(16f, Sector01Level.WORLD_WIDTH - 32f)
        }

        // Move Y and resolve floor/ceiling/platforms
        val wasOnGround = onGround
        playerY += playerVy * dt
        onGround = false
        resolveVerticalCollisions(prevY)
        if (!wasOnGround && onGround && form == PlayerForm.VEHICLE) {
            suspensionCompression = 1f
            spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight, rgb(148, 163, 184), 4, 35f)
        }

        if (abs(playerVx) > 15f && onGround) {
            runAnimTimer += dt * (abs(playerVx) / 110f)
        }

        // Combat: Charge Shot (Mech Hold+Release), Energon Blade Slash/Parry, or Rapid Fire
        if (transformTimer <= 0f) {
            if (form == PlayerForm.MECH && input.fireHeld) {
                chargeTimer = min(0.55f, chargeTimer + dt)
                if (chargeTimer >= CHARGE_THRESHOLD && !chargeReadyNotified) {
                    chargeReadyNotified = true
                    audio.play(Sfx.CHARGE_READY)
                    onHapticEvent(HapticKind.LIGHT)
                }
            } else if (form == PlayerForm.MECH && !input.fireHeld && chargeTimer > 0f) {
                if (chargeTimer >= CHARGE_THRESHOLD) {
                    fireChargedBeam(input)
                }
                chargeTimer = 0f
                chargeReadyNotified = false
            } else if (form != PlayerForm.MECH) {
                chargeTimer = 0f
                chargeReadyNotified = false
            }

            if (input.fireJustPressed || input.fireHeld) {
                handlePlayerAttack(input)
            }
        }

        // Plasma Vat Hazard Check
        checkPlasmaHazard()
    }

    private fun startTransform(newForm: PlayerForm) {
        targetForm = newForm
        transformTimer = 0.18f
        boostTimer = 0f
        audio.play(if (newForm == PlayerForm.VEHICLE) Sfx.TRANSFORM_TO_VEHICLE else Sfx.TRANSFORM_TO_MECH)
        onHapticEvent(HapticKind.MEDIUM)
        spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight * 0.5f, rgb(0, 229, 255), 14, 95f)
        spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight * 0.5f, rgb(255, 193, 7), 8, 70f)
    }

    private fun canStandUpAsMech(): Boolean {
        val wDiff = (MECH_W - VEH_W) * 0.5f
        val testX = playerX - wDiff
        val newTop = playerY + VEH_H - MECH_H
        val leftTile = (testX / 16f).toInt()
        val rightTile = ((testX + MECH_W - 0.5f) / 16f).toInt()
        val topTile = (newTop / 16f).toInt()
        val botTile = ((playerY + VEH_H - 1f) / 16f).toInt()
        for (ty in topTile..botTile) {
            for (tx in leftTile..rightTile) {
                val t = level.getTile(tx, ty)
                if (t == TileType.DECK || t == TileType.GIRDER || t == TileType.BOOST_PAD) {
                    return false
                }
            }
        }
        return true
    }

    private fun isStandingOnOneWayPlatform(): Boolean {
        val leftTile = (playerX / 16f).toInt()
        val rightTile = ((playerX + playerWidth - 0.5f) / 16f).toInt()
        val footTileY = ((playerY + playerHeight + 2f) / 16f).toInt()
        for (tx in leftTile..rightTile) {
            if (level.getTile(tx, footTileY) == TileType.PLATFORM) return true
        }
        return false
    }

    private fun resolveHorizontalCollisions() {
        val w = playerWidth
        val h = playerHeight
        val topTile = ((playerY + 1f) / 16f).toInt()
        val botTile = ((playerY + h - 1f) / 16f).toInt()

        if (playerVx > 0f) {
            val rightTile = ((playerX + w) / 16f).toInt()
            for (ty in topTile..botTile) {
                val t = level.getTile(rightTile, ty)
                if (isSolidTile(t)) {
                    playerX = rightTile * 16f - w - 0.01f
                    playerVx = 0f
                    break
                }
            }
        } else if (playerVx < 0f) {
            val leftTile = (playerX / 16f).toInt()
            for (ty in topTile..botTile) {
                val t = level.getTile(leftTile, ty)
                if (isSolidTile(t)) {
                    playerX = (leftTile + 1) * 16f + 0.01f
                    playerVx = 0f
                    break
                }
            }
        }

        // Breakable barriers
        for (b in level.barriers) {
            if (b.destroyed) continue
            if (rectsOverlap(playerX, playerY, w, h, b.x, b.y, b.width, b.height)) {
                if (form == PlayerForm.VEHICLE && (boostTimer > 0f || abs(playerVx) >= 220f)) {
                    // Smash straight through with Vehicle Ram!
                    destroyBarrier(b)
                } else {
                    if (playerX + w * 0.5f < b.x + b.width * 0.5f) {
                        playerX = b.x - w - 0.01f
                    } else {
                        playerX = b.x + b.width + 0.01f
                    }
                    playerVx = 0f
                }
            }
        }
    }

    private fun resolveVerticalCollisions(prevY: Float) {
        val w = playerWidth
        val h = playerHeight
        val leftTile = ((playerX + 1f) / 16f).toInt()
        val rightTile = ((playerX + w - 1f) / 16f).toInt()

        if (playerVy >= 0f) {
            val botTile = ((playerY + h) / 16f).toInt()
            val prevBot = prevY + h
            for (tx in leftTile..rightTile) {
                val t = level.getTile(tx, botTile)
                val tileTop = botTile * 16f
                if (isSolidTile(t)) {
                    playerY = tileTop - h
                    playerVy = 0f
                    onGround = true
                    recordSafeSpot(t)
                    break
                } else if (t == TileType.PLATFORM && prevBot <= tileTop + 4f) {
                    playerY = tileTop - h
                    playerVy = 0f
                    onGround = true
                    break
                }
            }

            // Crumble platforms
            if (!onGround) {
                for (cp in level.crumblePlatforms) {
                    if (cp.state == 2) continue
                    val px = cp.tileX * 16f
                    val py = cp.tileY * 16f
                    if (playerX + w > px + 1f && playerX < px + 15f &&
                        prevBot <= py + 4f && playerY + h >= py
                    ) {
                        playerY = py - h
                        playerVy = 0f
                        onGround = true
                        if (cp.state == 0) {
                            cp.state = 1
                            cp.timer = 0.48f
                        }
                        break
                    }
                }
            }

            // Moving hover-lifts
            if (!onGround) {
                for (mp in level.movingPlatforms) {
                    if (playerX + w > mp.x + 1f && playerX < mp.x + mp.width - 1f &&
                        prevBot <= mp.prevY + 5f && playerY + h >= mp.y
                    ) {
                        playerY = mp.y - h
                        playerVy = 0f
                        onGround = true
                        playerX += (mp.x - mp.prevX)
                        break
                    }
                }
            }
        } else if (playerVy < 0f) {
            val topTile = (playerY / 16f).toInt()
            for (tx in leftTile..rightTile) {
                val t = level.getTile(tx, topTile)
                if (isSolidTile(t)) {
                    playerY = (topTile + 1) * 16f + 0.01f
                    playerVy = 0f
                    break
                }
            }
        }
    }

    private fun recordSafeSpot(tile: TileType) {
        if (tile == TileType.DECK || tile == TileType.BOOST_PAD) {
            lastSafeX = playerX
            lastSafeY = playerY
        }
    }

    private fun isSolidTile(t: TileType): Boolean =
        t == TileType.DECK || t == TileType.GIRDER || t == TileType.BOOST_PAD

    private fun fireChargedBeam(input: InputState) {
        val dir = if (facingRight) 1f else -1f
        val speed = 475f
        val muzzleX = if (facingRight) playerX + playerWidth + 3f else playerX - 6f
        val muzzleY = playerY + 10f
        val vx = if (input.up) dir * speed * 0.84f else dir * speed
        val vy = if (input.up) -speed * 0.54f else 0f
        projectiles += Projectile(
            x = muzzleX,
            y = muzzleY,
            vx = vx,
            vy = vy,
            fromPlayer = true,
            damage = 4,
            color = rgb(255, 193, 7),
            radius = 5f,
            life = 1.6f,
            piercing = true,
            isChargedBeam = true,
        )
        fireCooldown = 0.22f
        audio.play(Sfx.CHARGE_BEAM)
        onHapticEvent(HapticKind.MEDIUM)
        triggerShake(0.14f, 2.5f)
        spawnBurst(muzzleX, muzzleY, rgb(255, 193, 7), 10, 95f)
    }

    private fun handlePlayerAttack(input: InputState) {
        val dir = if (facingRight) 1f else -1f
        // Check if enemy, barrier, or incoming bolt is in close melee range for Mech Energon Blade Slash / Parry!
        if (form == PlayerForm.MECH && input.fireJustPressed && slashCooldown <= 0f) {
            val slashBoxX = if (facingRight) playerX + playerWidth - 2f else playerX - 32f
            val slashBoxY = playerY - 4f
            val slashBoxW = 34f
            val slashBoxH = 30f
            var hitMelee = false
            var parriedBolt = false

            // Parry / deflect enemy bolts!
            for (proj in projectiles) {
                if (!proj.fromPlayer && pointInRect(proj.x, proj.y, slashBoxX, slashBoxY, slashBoxW, slashBoxH)) {
                    proj.fromPlayer = true
                    proj.reflected = true
                    proj.vx = if (abs(proj.vx) > 20f) -proj.vx * 1.8f else dir * 380f
                    proj.vy = -proj.vy * 0.8f
                    proj.color = rgb(255, 220, 60)
                    proj.life = 2.0f
                    parriedBolt = true
                    hitMelee = true
                }
            }

            for (e in level.enemies) {
                if (e.alive && rectsOverlap(slashBoxX, slashBoxY, slashBoxW, slashBoxH, e.x, e.y, e.width, e.height)) {
                    damageEnemy(e, 3, fromBehind = true, label = "SLASH!")
                    hitMelee = true
                }
            }
            for (b in level.barriers) {
                if (!b.destroyed && rectsOverlap(slashBoxX, slashBoxY, slashBoxW, slashBoxH, b.x, b.y, b.width, b.height)) {
                    damageBarrier(b, 3)
                    hitMelee = true
                }
            }
            if (boss.action != BossAction.HIDDEN && boss.action != BossAction.DYING && boss.action != BossAction.DEAD) {
                if (rectsOverlap(slashBoxX, slashBoxY, slashBoxW, slashBoxH, boss.x, boss.y, boss.width, boss.height)) {
                    damageBoss(4, "BLADE!")
                    hitMelee = true
                }
            }

            if (hitMelee) {
                slashCooldown = 0.26f
                fireCooldown = 0.20f
                slashArcs += SlashArc(
                    x = if (facingRight) playerX + playerWidth else playerX - 22f,
                    y = playerY + 2f,
                    facingRight = facingRight,
                )
                if (parriedBolt) {
                    score += 200
                    audio.play(Sfx.PARRY)
                    onHapticEvent(HapticKind.MEDIUM)
                    addFloatingText("PARRY! +200", playerX - 8f, playerY - 10f, rgb(255, 220, 60))
                } else {
                    audio.play(Sfx.SLASH)
                }
                hitStopTimer = 0.042f
                return
            }
        }

        if (fireCooldown > 0f) return

        if (form == PlayerForm.MECH) {
            fireCooldown = 0.16f
            val speed = 410f
            val muzzleX = if (facingRight) playerX + playerWidth + 2f else playerX - 4f
            val muzzleY = playerY + 10f
            val vx: Float
            val vy: Float
            if (input.up) {
                vx = dir * speed * 0.82f
                vy = -speed * 0.57f
            } else {
                vx = dir * speed
                vy = 0f
            }
            projectiles += Projectile(
                x = muzzleX,
                y = muzzleY,
                vx = vx,
                vy = vy,
                fromPlayer = true,
                damage = 1,
                color = rgb(0, 229, 255),
                radius = 3f,
            )
            audio.play(Sfx.SHOOT)
            spawnParticle(muzzleX, muzzleY, dir * 50f, (rng.nextFloat() - 0.5f) * 30f, 0.12f, rgb(255, 238, 60), 2)
        } else {
            // Vehicle Mode: Rapid Twin Autocannon!
            fireCooldown = 0.11f
            val muzzleX = if (facingRight) playerX + playerWidth + 1f else playerX - 3f
            val muzzleY = playerY + 2f
            projectiles += Projectile(
                x = muzzleX,
                y = muzzleY,
                vx = dir * 450f,
                vy = -12f,
                fromPlayer = true,
                damage = 1,
                color = rgb(255, 193, 7),
                radius = 2.5f,
            )
            audio.play(Sfx.SHOOT)
        }
    }

    private fun updateBarriers(dt: Float) {
        for (b in level.barriers) {
            if (b.hitFlash > 0f) b.hitFlash = max(0f, b.hitFlash - dt)
        }
    }

    private fun damageBarrier(b: com.example.game.model.BreakableBarrier, dmg: Int) {
        if (b.destroyed) return
        b.hp -= dmg
        b.hitFlash = 0.1f
        audio.play(Sfx.HIT)
        spawnBurst(b.x + b.width * 0.5f, b.y + b.height * 0.5f, rgb(255, 193, 7), 6, 75f)
        if (b.hp <= 0) {
            destroyBarrier(b)
        }
    }

    private fun destroyBarrier(b: com.example.game.model.BreakableBarrier) {
        b.destroyed = true
        score += 150
        audio.play(Sfx.EXPLOSION)
        onHapticEvent(HapticKind.HEAVY)
        triggerShake(0.20f, 4f)
        hitStopTimer = 0.045f
        addFloatingText("SMASH +150", b.x - 8f, b.y - 6f, rgb(255, 193, 7))
        spawnBurst(b.x + 8f, b.y + 16f, rgb(255, 140, 0), 18, 130f)
        spawnBurst(b.x + 8f, b.y + 16f, rgb(200, 210, 225), 10, 100f)
    }

    private fun updateLaserGates() {
        for (gate in level.laserGates) {
            if (rectsOverlap(playerX, playerY, playerWidth, playerHeight, gate.x - 2f, gate.topY, 6f, gate.bottomY - gate.topY)) {
                if (boostTimer > 0f && form == PlayerForm.MECH) continue // Phase dash passes laser!
                damagePlayer(2, knockDir = if (playerX < gate.x) -1f else 1f)
            }
        }
    }

    private fun checkPlasmaHazard() {
        val leftTile = ((playerX + 2f) / 16f).toInt()
        val rightTile = ((playerX + playerWidth - 2f) / 16f).toInt()
        val footTile = ((playerY + playerHeight - 1f) / 16f).toInt()
        var hitPlasma = playerY > VIRTUAL_HEIGHT + 24f
        if (!hitPlasma) {
            for (tx in leftTile..rightTile) {
                if (level.getTile(tx, footTile) == TileType.PLASMA_HAZARD) {
                    hitPlasma = true
                    break
                }
            }
        }
        if (hitPlasma) {
            damagePlayer(2, knockDir = 0f)
            if (phase == GamePhase.PLAYING) {
                playerX = respawnX.takeIf { lastSafeX < respawnX } ?: lastSafeX
                playerY = min(respawnY, lastSafeY) - 6f
                playerVx = 0f
                playerVy = 0f
                invulnTimer = 1.4f
                spawnBurst(playerX + 8f, playerY + 10f, rgb(0, 229, 255), 12, 85f)
            }
        }
    }

    private fun updateCheckpointsAndPickups() {
        for (cp in level.checkpoints) {
            if (!cp.activated && playerX + playerWidth >= cp.x - 4f) {
                cp.activated = true
                respawnX = cp.x
                respawnY = cp.y - playerHeight
                hp = min(maxHp, hp + 3)
                energon = maxEnergon
                score += 250
                audio.play(Sfx.CHECKPOINT)
                addFloatingText("CHECKPOINT +3HP", cp.x - 20f, cp.y - 20f, rgb(0, 229, 255))
                spawnBurst(cp.x + 8f, cp.y, rgb(0, 229, 255), 16, 95f)
            }
        }

        for (p in level.pickups) {
            if (p.collected) continue
            if (rectsOverlap(playerX, playerY, playerWidth, playerHeight, p.x - 2f, p.y - 2f, 16f, 16f)) {
                p.collected = true
                when (p.type) {
                    PickupType.ENERGON_CUBE -> {
                        hp = min(maxHp, hp + 2)
                        energon = min(maxEnergon, energon + 45f)
                        score += 100
                        audio.play(Sfx.PICKUP)
                        addFloatingText("+2 HP +ENERGON", p.x - 16f, p.y - 8f, rgb(0, 229, 255))
                        spawnBurst(p.x + 6f, p.y + 6f, rgb(0, 229, 255), 12, 80f)
                    }
                    PickupType.MATRIX_RELIC -> {
                        relicsCollected++
                        score += 750
                        hp = maxHp
                        energon = maxEnergon
                        audio.play(Sfx.RELIC)
                        onHapticEvent(HapticKind.MEDIUM)
                        addFloatingText("MATRIX RELIC! +750", p.x - 24f, p.y - 10f, rgb(255, 193, 7))
                        spawnBurst(p.x + 8f, p.y + 8f, rgb(255, 193, 7), 20, 110f)
                    }
                }
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        for (e in level.enemies) {
            if (!e.alive) continue
            if (e.hitFlash > 0f) e.hitFlash = max(0f, e.hitFlash - dt)

            val distToPlayer = abs((playerX + playerWidth * 0.5f) - (e.x + e.width * 0.5f))
            if (distToPlayer > 360f) continue

            e.aiTimer += dt
            e.fireCooldown -= dt

            when (e.type) {
                EnemyType.RECON_DRONE -> {
                    e.x = e.homeX + sin(e.aiTimer * 1.8f) * 34f
                    e.y = e.homeY + cos(e.aiTimer * 3.1f) * 8f
                    e.facingRight = playerX > e.x
                    if (e.fireCooldown <= 0f && distToPlayer < 210f) {
                        e.fireCooldown = 1.9f
                        fireEnemyBolt(e.x + e.width * 0.5f, e.y + e.height * 0.6f, 165f)
                    }
                }
                EnemyType.HEAVY_TROOPER -> {
                    e.facingRight = playerX > e.x
                    val step = if (e.facingRight) 22f else -22f
                    if (abs(playerX - e.x) in 42f..170f) {
                        val nextX = e.x + step * dt
                        if (abs(nextX - e.homeX) < 38f) {
                            e.x = nextX
                        }
                    }
                    if (e.fireCooldown <= 0f && distToPlayer < 220f) {
                        e.fireCooldown = 1.65f
                        val dir = if (e.facingRight) 1f else -1f
                        projectiles += Projectile(
                            x = e.x + (if (e.facingRight) e.width else 0f),
                            y = e.y + 10f,
                            vx = dir * 195f,
                            vy = 0f,
                            fromPlayer = false,
                            damage = 1,
                            color = rgb(255, 42, 85),
                        )
                    }
                }
                EnemyType.LASER_TURRET -> {
                    e.facingRight = playerX > e.x
                    if (e.fireCooldown <= 0f && distToPlayer < 230f) {
                        e.fireCooldown = 2.1f
                        val dir = if (e.facingRight) 1f else -1f
                        projectiles += Projectile(
                            x = e.x + (if (e.facingRight) e.width else 0f),
                            y = e.y + 4f,
                            vx = dir * 225f,
                            vy = 0f,
                            fromPlayer = false,
                            damage = 2,
                            color = rgb(255, 42, 85),
                            radius = 3.5f,
                        )
                    }
                }
                EnemyType.SEEKER_JET -> {
                    if (e.telegraphTimer > 0f) {
                        e.telegraphTimer -= dt
                        if (e.telegraphTimer <= 0f) {
                            // Launch fast diagonal dive!
                            val dx = e.diveTargetX - e.x
                            val dy = e.diveTargetY - e.y
                            val len = max(1f, hypot(dx, dy))
                            e.vx = (dx / len) * 235f
                            e.vy = (dy / len) * 235f
                        }
                    } else if (e.vx != 0f || e.vy != 0f) {
                        e.x += e.vx * dt
                        e.y += e.vy * dt
                        if (e.y > VIRTUAL_HEIGHT + 20f || e.x < cameraX - 60f) {
                            e.x = e.homeX
                            e.y = e.homeY
                            e.vx = 0f
                            e.vy = 0f
                            e.fireCooldown = 2.2f
                        }
                    } else {
                        e.y = e.homeY + sin(e.aiTimer * 2.4f) * 6f
                        e.facingRight = playerX > e.x
                        if (e.fireCooldown <= 0f && distToPlayer < 230f) {
                            e.telegraphTimer = 0.65f
                            e.diveTargetX = playerX + playerWidth * 0.5f
                            e.diveTargetY = playerY + playerHeight * 0.5f
                            e.fireCooldown = 3.0f
                        }
                    }
                }
            }

            // Player contact vs Vehicle Nitro Ram!
            if (rectsOverlap(playerX, playerY, playerWidth, playerHeight, e.x, e.y, e.width, e.height)) {
                if (form == PlayerForm.VEHICLE && boostTimer > 0f) {
                    damageEnemy(e, 10, fromBehind = true, label = "RAM!")
                    hitStopTimer = 0.04f
                    triggerShake(0.16f, 3.5f)
                } else if (boostTimer > 0f && form == PlayerForm.MECH) {
                    // Phase dash passes cleanly without taking damage
                } else {
                    damagePlayer(1, knockDir = if (playerX < e.x) -1f else 1f)
                }
            }
        }
    }

    private fun fireEnemyBolt(ox: Float, oy: Float, speed: Float) {
        val tx = playerX + playerWidth * 0.5f - ox
        val ty = playerY + playerHeight * 0.5f - oy
        val len = max(1f, hypot(tx, ty))
        projectiles += Projectile(
            x = ox,
            y = oy,
            vx = (tx / len) * speed,
            vy = (ty / len) * speed,
            fromPlayer = false,
            damage = 1,
            color = rgb(255, 42, 85),
        )
    }

    private fun damageEnemy(e: Enemy, dmg: Int, fromBehind: Boolean, label: String? = null) {
        if (!e.alive) return
        e.hp -= dmg
        e.hitFlash = 0.12f
        if (e.hp <= 0) {
            e.alive = false
            enemiesDefeated++
            combo++
            comboTimer = 3.2f
            val pts = (100 + e.maxHp * 25) * min(4, max(1, combo))
            score += pts
            energon = min(maxEnergon, energon + 15f)
            audio.play(Sfx.EXPLOSION)
            onHapticEvent(HapticKind.MEDIUM)
            triggerShake(0.14f, 2.5f)
            addFloatingText(
                if (label != null) "$label +$pts" else "+$pts",
                e.x,
                e.y - 6f,
                rgb(255, 238, 60),
            )
            spawnBurst(e.x + e.width * 0.5f, e.y + e.height * 0.5f, rgb(255, 122, 0), 16, 120f)
            spawnBurst(e.x + e.width * 0.5f, e.y + e.height * 0.5f, rgb(157, 78, 221), 10, 90f)
        } else {
            audio.play(Sfx.HIT)
            spawnBurst(e.x + e.width * 0.5f, e.y + e.height * 0.5f, rgb(255, 193, 7), 5, 65f)
        }
    }

    private fun updateBoss(dt: Float) {
        if (boss.action == BossAction.HIDDEN) {
            if (playerX >= Sector01Level.BOSS_TRIGGER_X) {
                arenaLocked = true
                boss.action = BossAction.INTRO_DROP
                boss.x = 285f * 16f
                boss.y = -48f
                boss.vy = 180f
                boss.timer = 1.4f
                currentZone = LevelZone.BOSS_ARENA
                zoneBannerTimer = 2.8f
                activeHintText = "DEFEAT MEGATRONUS MK-IV! DODGE & COUNTER!"
                hintTimer = 4.0f
                audio.play(Sfx.BOSS_ALERT)
            }
            return
        }

        if (boss.hitFlash > 0f) boss.hitFlash = max(0f, boss.hitFlash - dt)
        val floorY = 12f * 16f - boss.height

        when (boss.action) {
            BossAction.INTRO_DROP -> {
                boss.y += boss.vy * dt
                if (boss.y >= floorY) {
                    boss.y = floorY
                    boss.vy = 0f
                    triggerShake(0.35f, 5f)
                    onHapticEvent(HapticKind.HEAVY)
                    spawnBurst(boss.x + boss.width * 0.5f, boss.y + boss.height, rgb(157, 78, 221), 20, 130f)
                    boss.action = BossAction.TRACK_PLAYER
                    boss.timer = 1.0f
                }
            }
            BossAction.TRACK_PLAYER -> {
                boss.facingRight = playerX > boss.x
                val dir = if (boss.facingRight) 1f else -1f
                val dist = abs(playerX - boss.x)
                if (dist > 85f) {
                    boss.x = (boss.x + dir * 48f * dt).coerceIn(
                        Sector01Level.ARENA_LEFT_X + 16f,
                        Sector01Level.ARENA_RIGHT_X - boss.width - 16f,
                    )
                }
                boss.timer -= dt
                if (boss.timer <= 0f) {
                    val phase2 = boss.hp <= boss.maxHp / 2
                    val speedScale = if (phase2) 0.78f else 1.0f
                    val roll = rng.nextInt(if (phase2) 3 else 2)
                    when (roll) {
                        0 -> {
                            boss.action = BossAction.TELEGRAPH_VOLLEY
                            boss.timer = 0.45f * speedScale
                            boss.volleyCount = if (phase2) 4 else 3
                        }
                        1 -> {
                            boss.action = BossAction.TELEGRAPH_CHARGE
                            boss.timer = 0.55f * speedScale
                            boss.chargeTargetX = if (playerX < boss.x) {
                                Sector01Level.ARENA_LEFT_X + 18f
                            } else {
                                Sector01Level.ARENA_RIGHT_X - boss.width - 18f
                            }
                        }
                        else -> {
                            boss.action = BossAction.AERIAL_BARRAGE
                            boss.timer = 1.25f
                        }
                    }
                }
            }
            BossAction.TELEGRAPH_VOLLEY -> {
                boss.facingRight = playerX > boss.x
                boss.timer -= dt
                if (boss.timer <= 0f) {
                    boss.action = BossAction.FUSION_VOLLEY
                    boss.timer = 0.05f
                }
            }
            BossAction.FUSION_VOLLEY -> {
                boss.timer -= dt
                if (boss.timer <= 0f) {
                    val dir = if (boss.facingRight) 1f else -1f
                    // Alternate high/low cannon height so player can jump OR transform-slide under!
                    val highShot = boss.volleyCount % 2 == 0
                    val shotY = if (highShot) boss.y + 16f else boss.y + boss.height - 10f
                    val boltSpeed = if (bossOverclockTriggered) 265f else 235f
                    projectiles += Projectile(
                        x = boss.x + (if (boss.facingRight) boss.width else -4f),
                        y = shotY,
                        vx = dir * boltSpeed,
                        vy = 0f,
                        fromPlayer = false,
                        damage = 2,
                        color = rgb(255, 42, 85),
                        radius = 4f,
                    )
                    audio.play(Sfx.SHOOT)
                    boss.volleyCount--
                    if (boss.volleyCount <= 0) {
                        boss.action = BossAction.TRACK_PLAYER
                        boss.timer = if (bossOverclockTriggered) 0.85f else 1.15f
                    } else {
                        boss.timer = if (bossOverclockTriggered) 0.27f else 0.34f
                    }
                }
            }
            BossAction.TELEGRAPH_CHARGE -> {
                boss.facingRight = boss.chargeTargetX > boss.x
                boss.timer -= dt
                if (boss.timer <= 0f) {
                    boss.action = BossAction.JET_CHARGE
                    val dir = if (boss.facingRight) 1f else -1f
                    boss.vx = dir * 265f
                    audio.play(Sfx.BOOST)
                }
            }
            BossAction.JET_CHARGE -> {
                boss.x += boss.vx * dt
                if ((animClock * 60f).toInt() % 2 == 0) {
                    spawnParticle(
                        boss.x + boss.width * 0.5f,
                        boss.y + boss.height - 4f,
                        -boss.vx * 0.3f,
                        -25f,
                        0.22f,
                        rgb(255, 42, 85),
                        3,
                    )
                }
                val hitWall = (boss.vx < 0f && boss.x <= Sector01Level.ARENA_LEFT_X + 14f) ||
                    (boss.vx > 0f && boss.x >= Sector01Level.ARENA_RIGHT_X - boss.width - 14f)
                if (hitWall) {
                    boss.x = boss.x.coerceIn(
                        Sector01Level.ARENA_LEFT_X + 14f,
                        Sector01Level.ARENA_RIGHT_X - boss.width - 14f,
                    )
                    boss.vx = 0f
                    triggerShake(0.22f, 4f)
                    boss.action = BossAction.TRACK_PLAYER
                    boss.timer = 1.1f
                }
            }
            BossAction.AERIAL_BARRAGE -> {
                boss.timer -= dt
                if ((boss.timer * 60f).toInt() % 14 == 0) {
                    val dropX = Sector01Level.ARENA_LEFT_X + 36f + rng.nextFloat() * (Sector01Level.ARENA_RIGHT_X - Sector01Level.ARENA_LEFT_X - 72f)
                    projectiles += Projectile(
                        x = dropX,
                        y = 8f,
                        vx = 0f,
                        vy = 165f,
                        fromPlayer = false,
                        damage = 2,
                        color = rgb(255, 122, 0),
                        radius = 4f,
                        isRocket = true,
                    )
                }
                if (boss.timer <= 0f) {
                    boss.action = BossAction.TRACK_PLAYER
                    boss.timer = 1.1f
                }
            }
            BossAction.DYING -> {
                boss.timer -= dt
                boss.dyingExplosionTimer -= dt
                if (boss.dyingExplosionTimer <= 0f) {
                    boss.dyingExplosionTimer = 0.14f
                    val ex = boss.x + rng.nextFloat() * boss.width
                    val ey = boss.y + rng.nextFloat() * boss.height
                    spawnBurst(ex, ey, rgb(255, 140, 0), 14, 120f)
                    spawnBurst(ex, ey, rgb(0, 229, 255), 8, 90f)
                    audio.play(Sfx.EXPLOSION)
                    triggerShake(0.18f, 3.5f)
                }
                if (boss.timer <= 0f) {
                    boss.action = BossAction.DEAD
                    score += 2500
                    victoryDelayTimer = 0.9f
                }
            }
            BossAction.HIDDEN, BossAction.DEAD -> Unit
        }

        // Boss contact damage
        if (boss.action != BossAction.DYING && boss.action != BossAction.DEAD) {
            if (rectsOverlap(playerX, playerY, playerWidth, playerHeight, boss.x + 3f, boss.y + 4f, boss.width - 6f, boss.height - 4f)) {
                if (boostTimer > 0f && form == PlayerForm.MECH) {
                    // Phase dash avoids contact
                } else {
                    damagePlayer(2, knockDir = if (playerX < boss.x) -1f else 1f)
                }
            }
        }
    }

    private fun damageBoss(dmg: Int, label: String? = null) {
        if (boss.action == BossAction.HIDDEN || boss.action == BossAction.DYING || boss.action == BossAction.DEAD) return
        boss.hp = max(0, boss.hp - dmg)
        boss.hitFlash = 0.08f
        score += 40
        audio.play(Sfx.HIT)
        if (label != null) {
            addFloatingText(label, boss.x + 6f, boss.y - 6f, rgb(0, 229, 255))
        }
        spawnBurst(boss.x + boss.width * 0.5f, boss.y + boss.height * 0.4f, rgb(255, 193, 7), 5, 70f)
        if (boss.hp in 1..(boss.maxHp / 2) && !bossOverclockTriggered) {
            bossOverclockTriggered = true
            overclockBannerTimer = 2.8f
            audio.play(Sfx.BOSS_ALERT)
            onHapticEvent(HapticKind.HEAVY)
            triggerShake(0.28f, 4f)
            spawnBurst(boss.x + boss.width * 0.5f, boss.y + boss.height * 0.5f, rgb(157, 78, 221), 22, 130f)
            addFloatingText("OVERCLOCK!", boss.x - 4f, boss.y - 12f, rgb(255, 42, 85))
        }
        if (boss.hp <= 0) {
            boss.action = BossAction.DYING
            boss.timer = 1.6f
            boss.dyingExplosionTimer = 0f
            projectiles.removeAll { !it.fromPlayer }
            hitStopTimer = 0.08f
            onHapticEvent(HapticKind.HEAVY)
            addFloatingText("BOSS DESTROYED!", boss.x - 10f, boss.y - 12f, rgb(255, 193, 7))
        }
    }

    private fun updateProjectiles(dt: Float, virtualWidth: Int) {
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt

            // Remove out-of-bounds or expired
            if (p.life <= 0f || p.x < cameraX - 40f || p.x > cameraX + virtualWidth + 60f || p.y < -20f || p.y > VIRTUAL_HEIGHT + 20f) {
                iter.remove()
                continue
            }

            // Tile hit
            val tx = (p.x / 16f).toInt()
            val ty = (p.y / 16f).toInt()
            if (isSolidTile(level.getTile(tx, ty))) {
                spawnBurst(p.x, p.y, p.color, 4, 45f)
                iter.remove()
                continue
            }

            if (p.fromPlayer) {
                var consumed = false
                for (b in level.barriers) {
                    if (!b.destroyed && pointInRect(p.x, p.y, b.x, b.y, b.width, b.height)) {
                        damageBarrier(b, p.damage)
                        consumed = !p.piercing
                        break
                    }
                }
                if (consumed) {
                    iter.remove()
                    continue
                }

                for (e in level.enemies) {
                    if (e.alive && pointInRect(p.x, p.y, e.x - 2f, e.y - 2f, e.width + 4f, e.height + 4f)) {
                        if (p.piercing && e.hitFlash > 0.01f) continue
                        val shotFromRight = p.vx < 0f
                        val hitFrontShield = e.shielded && ((e.facingRight && shotFromRight) || (!e.facingRight && !shotFromRight))
                        if (hitFrontShield && !p.piercing && !p.reflected) {
                            audio.play(Sfx.HIT)
                            addFloatingText("SHIELD!", e.x - 4f, e.y - 8f, rgb(200, 220, 240))
                            spawnBurst(p.x, p.y, rgb(200, 220, 240), 5, 65f)
                            consumed = true
                        } else {
                            if (hitFrontShield) {
                                e.shielded = false
                                addFloatingText("SHIELD BREAK!", e.x - 10f, e.y - 10f, rgb(255, 193, 7))
                                hitStopTimer = 0.035f
                            }
                            damageEnemy(
                                e,
                                p.damage,
                                fromBehind = true,
                                label = if (p.reflected) "PARRY" else if (p.isChargedBeam) "BEAM" else null,
                            )
                            consumed = !p.piercing
                        }
                        if (consumed) break
                    }
                }
                if (consumed) {
                    iter.remove()
                    continue
                }

                if (boss.action != BossAction.HIDDEN && boss.action != BossAction.DYING && boss.action != BossAction.DEAD) {
                    if (pointInRect(p.x, p.y, boss.x, boss.y, boss.width, boss.height)) {
                        if (!(p.piercing && boss.hitFlash > 0.01f)) {
                            damageBoss(p.damage, if (p.reflected) "PARRY!" else if (p.isChargedBeam) "BEAM!" else null)
                        }
                        if (!p.piercing) {
                            iter.remove()
                            continue
                        }
                    }
                }
            } else {
                // Enemy projectile vs player
                if (pointInRect(p.x, p.y, playerX, playerY, playerWidth, playerHeight)) {
                    if (boostTimer > 0f && form == PlayerForm.MECH) {
                        // Phase dash dodges!
                        continue
                    }
                    damagePlayer(p.damage, knockDir = if (p.vx >= 0f) 1f else -1f)
                    spawnBurst(p.x, p.y, p.color, 6, 70f)
                    iter.remove()
                }
            }
        }
    }

    private fun damagePlayer(amount: Int, knockDir: Float) {
        if (invulnTimer > 0f || phase != GamePhase.PLAYING) return
        hp = max(0, hp - amount)
        damageTakenCount++
        combo = 0
        invulnTimer = 1.15f
        hitStopTimer = 0.045f
        triggerShake(0.24f, 4.5f)
        audio.play(Sfx.DAMAGE)
        onHapticEvent(HapticKind.HEAVY)
        addFloatingText("-$amount HP", playerX, playerY - 10f, rgb(255, 42, 85))
        spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight * 0.5f, rgb(255, 42, 85), 14, 110f)

        if (knockDir != 0f) {
            playerVx = knockDir * 135f
            playerVy = -140f
        }

        if (hp <= 0) {
            phase = GamePhase.GAME_OVER
            spawnBurst(playerX + playerWidth * 0.5f, playerY + playerHeight * 0.5f, rgb(255, 122, 0), 26, 150f)
            audio.play(Sfx.EXPLOSION)
        }
    }

    private fun updateParticlesOnly(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val pt = pIter.next()
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.vy += pt.gravity * dt
            pt.life -= dt
            if (pt.life <= 0f) pIter.remove()
        }
        val aIter = afterimages.iterator()
        while (aIter.hasNext()) {
            val af = aIter.next()
            af.life -= dt
            if (af.life <= 0f) aIter.remove()
        }
        val fIter = floatingTexts.iterator()
        while (fIter.hasNext()) {
            val ft = fIter.next()
            ft.y += ft.vy * dt
            ft.life -= dt
            if (ft.life <= 0f) fIter.remove()
        }
        val sIter = slashArcs.iterator()
        while (sIter.hasNext()) {
            val sl = sIter.next()
            sl.life -= dt
            if (sl.life <= 0f) sIter.remove()
        }
    }

    private fun updateZonesAndCamera(dt: Float, virtualWidth: Int) {
        val newZone = when {
            boss.action != BossAction.HIDDEN -> LevelZone.BOSS_ARENA
            playerX >= 165f * 16f -> LevelZone.REACTOR_GAUNTLET
            playerX >= 79f * 16f -> LevelZone.REFINERY_PLATFORMS
            else -> LevelZone.VIADUCT_APPROACH
        }
        if (newZone != currentZone) {
            currentZone = newZone
            zoneBannerTimer = 2.6f
        }

        val lookAhead = if (facingRight) 54f else -36f
        val targetCam = if (arenaLocked) {
            (Sector01Level.ARENA_LEFT_X + (Sector01Level.ARENA_RIGHT_X - Sector01Level.ARENA_LEFT_X - virtualWidth) * 0.5f)
                .coerceIn(Sector01Level.ARENA_LEFT_X, max(Sector01Level.ARENA_LEFT_X, Sector01Level.ARENA_RIGHT_X - virtualWidth))
        } else {
            (playerX + lookAhead - virtualWidth * 0.38f)
                .coerceIn(0f, max(0f, (Sector01Level.WORLD_WIDTH - virtualWidth).toFloat()))
        }
        cameraX += (targetCam - cameraX) * min(1f, dt * 8.5f)
    }

    private fun initWeather(virtualWidth: Int) {
        weatherParticles.clear()
        repeat(28) {
            weatherParticles += WeatherParticle(
                x = rng.nextFloat() * virtualWidth,
                y = rng.nextFloat() * VIRTUAL_HEIGHT,
                vx = -38f - rng.nextFloat() * 24f,
                vy = 145f + rng.nextFloat() * 65f,
                length = if (rng.nextBoolean()) 4 else 3,
                color = if (rng.nextFloat() > 0.3f) rgb(0, 175, 225) else rgb(110, 225, 255),
            )
        }
    }

    private fun updateWeather(dt: Float, virtualWidth: Int) {
        if (weatherParticles.isEmpty()) initWeather(virtualWidth)
        val hotZone = currentZone == LevelZone.REACTOR_GAUNTLET || currentZone == LevelZone.BOSS_ARENA
        for (i in weatherParticles.indices) {
            val wp = weatherParticles[i]
            val speedY = if (hotZone) -32f - (i % 5) * 8f else wp.vy
            val speedX = if (hotZone) -18f - (i % 3) * 10f else wp.vx
            wp.x += speedX * dt
            wp.y += speedY * dt
            if (wp.x < 0f) wp.x += virtualWidth
            if (wp.x > virtualWidth) wp.x -= virtualWidth
            if (wp.y > VIRTUAL_HEIGHT) wp.y -= VIRTUAL_HEIGHT
            if (wp.y < 0f) wp.y += VIRTUAL_HEIGHT
        }
    }

    fun computeRank(): String {
        var pts = 0
        if (elapsedSeconds <= 115f) pts += 3 else if (elapsedSeconds <= 155f) pts += 2 else pts += 1
        if (damageTakenCount <= 2) pts += 3 else if (damageTakenCount <= 6) pts += 2 else pts += 1
        pts += relicsCollected
        return when {
            pts >= 8 -> "S"
            pts >= 6 -> "A"
            pts >= 4 -> "B"
            else -> "C"
        }
    }

    fun toHudState(): HudState = HudState(
        phase = phase,
        hp = hp,
        maxHp = maxHp,
        energon = energon,
        maxEnergon = maxEnergon,
        form = form,
        isTransforming = transformTimer > 0f,
        chargeRatio = (chargeTimer / CHARGE_THRESHOLD).coerceIn(0f, 1f),
        score = score,
        combo = combo,
        comboTimer = comboTimer,
        elapsedSeconds = elapsedSeconds,
        relicsCollected = relicsCollected,
        totalRelics = 3,
        zone = currentZone,
        zoneBannerTimer = zoneBannerTimer,
        bossActive = boss.action != BossAction.HIDDEN && boss.action != BossAction.DEAD,
        bossOverclock = bossOverclockTriggered,
        overclockBannerTimer = overclockBannerTimer,
        bossHp = boss.hp,
        bossMaxHp = boss.maxHp,
        progressPercent = (playerX / (Sector01Level.BOSS_TRIGGER_X)).coerceIn(0f, 1f),
        damageTakenCount = damageTakenCount,
        enemiesDefeated = enemiesDefeated,
        rank = computeRank(),
        hintText = activeHintText,
    )

    private fun spawnBurst(cx: Float, cy: Float, color: Int, count: Int, speed: Float) {
        repeat(count) {
            val angle = rng.nextFloat() * (2f * Math.PI.toFloat())
            val spd = speed * (0.35f + rng.nextFloat() * 0.75f)
            spawnParticle(
                x = cx,
                y = cy,
                vx = cos(angle) * spd,
                vy = sin(angle) * spd,
                life = 0.22f + rng.nextFloat() * 0.25f,
                color = color,
                size = if (rng.nextFloat() > 0.65f) 3 else 2,
                gravity = 160f,
            )
        }
    }

    private fun spawnParticle(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        life: Float,
        color: Int,
        size: Int,
        gravity: Float = 0f,
    ) {
        if (particles.size > 160) return
        particles += Particle(x, y, vx, vy, life, life, color, size, gravity)
    }

    private fun addFloatingText(text: String, x: Float, y: Float, color: Int) {
        floatingTexts += FloatingText(text, x, y, color = color)
    }

    private fun triggerShake(duration: Float, intensity: Float) {
        shakeTimer = max(shakeTimer, duration)
        shakeIntensity = max(shakeIntensity, intensity)
    }

    private fun rectsOverlap(
        ax: Float,
        ay: Float,
        aw: Float,
        ah: Float,
        bx: Float,
        by: Float,
        bw: Float,
        bh: Float,
    ): Boolean = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    private fun pointInRect(px: Float, py: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean =
        px >= rx && px <= rx + rw && py >= ry && py <= ry + rh
}
