package com.example.game.pixel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.game.engine.GameEngine
import com.example.game.level.Sector01Level
import com.example.game.model.BossAction
import com.example.game.model.EnemyType
import com.example.game.model.LevelZone
import com.example.game.model.PickupType
import com.example.game.model.PlayerForm
import com.example.game.model.TileType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders the entire game world into a pixel-locked virtual framebuffer (virtualWidth x 240).
 * All world coordinates are rounded to integer pixels before drawing, guaranteeing zero subpixel shimmer.
 */
class PixelWorldRenderer {

    private val rectPaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        isAntiAlias = false
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private var frameBitmap: Bitmap? = null
    private var frameCanvas: Canvas? = null

    fun render(engine: GameEngine, virtualWidth: Int): Bitmap {
        val w = virtualWidth.coerceIn(320, 580)
        val h = GameEngine.VIRTUAL_HEIGHT
        if (frameBitmap == null || frameBitmap?.width != w || frameBitmap?.height != h) {
            frameBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            frameCanvas = Canvas(frameBitmap!!)
        }
        val canvas = frameCanvas!!

        val shakeX = if (engine.shakeTimer > 0f) {
            ((sin(engine.animClock * 95f) * engine.shakeIntensity)).roundToInt()
        } else 0
        val shakeY = if (engine.shakeTimer > 0f) {
            ((sin(engine.animClock * 120f + 1.2f) * engine.shakeIntensity * 0.7f)).roundToInt()
        } else 0
        val camX = engine.cameraX.roundToInt() + shakeX
        val camY = shakeY

        drawSkyAndParallax(canvas, w, h, camX, engine.animClock)
        drawWeather(canvas, engine)
        drawTilemap(canvas, engine, w, camX, camY)
        drawHintSigns(canvas, engine, camX, camY)
        drawMovingPlatforms(canvas, engine, camX, camY)
        drawLaserGates(canvas, engine, camX, camY)
        drawBarriersAndCheckpoints(canvas, engine, camX, camY)
        drawPickups(canvas, engine, camX, camY)
        drawEnemies(canvas, engine, camX, camY)
        drawBoss(canvas, engine, camX, camY)
        drawAfterimagesAndPlayer(canvas, engine, camX, camY)
        drawSlashArcs(canvas, engine, camX, camY)
        drawProjectilesAndParticles(canvas, engine, camX, camY)
        drawFloatingTexts(canvas, engine, camX, camY)

        return frameBitmap!!
    }

    private fun drawSkyAndParallax(canvas: Canvas, w: Int, h: Int, camX: Int, clock: Float) {
        // Deep cyber-night gradient bands (quantized 16-bit style sky!)
        val bands = intArrayOf(
            Color.rgb(6, 10, 22),
            Color.rgb(9, 15, 34),
            Color.rgb(12, 21, 46),
            Color.rgb(16, 28, 60),
            Color.rgb(20, 34, 72),
        )
        val bandH = h / bands.size + 1
        for (i in bands.indices) {
            rectPaint.color = bands[i]
            canvas.drawRect(0f, (i * bandH).toFloat(), w.toFloat(), ((i + 1) * bandH).toFloat(), rectPaint)
        }

        // Distant stars
        for (i in 0 until 28) {
            val sx = ((i * 73 + 19 - (camX * 0.04f).toInt()) % w + w) % w
            val sy = (i * 37 + 7) % 105
            val twinkle = ((clock * 2.5f + i) % 3f) > 0.35f
            if (twinkle) {
                rectPaint.color = if (i % 4 == 0) Color.rgb(0, 229, 255) else Color.rgb(185, 210, 255)
                canvas.drawRect(sx.toFloat(), sy.toFloat(), (sx + 1).toFloat(), (sy + 1).toFloat(), rectPaint)
            }
        }

        // Distant Cybertron ringed moon
        val moonX = w - 92 - ((camX * 0.03f).toInt() % 40)
        val moonY = 38
        rectPaint.color = Color.rgb(28, 48, 92)
        canvas.drawCircle(moonX.toFloat(), moonY.toFloat(), 22f, rectPaint)
        rectPaint.color = Color.rgb(42, 74, 134)
        canvas.drawCircle((moonX - 3).toFloat(), (moonY - 3).toFloat(), 17f, rectPaint)
        linePaint.color = Color.rgb(0, 180, 230)
        canvas.drawLine((moonX - 34).toFloat(), (moonY + 8).toFloat(), (moonX + 34).toFloat(), (moonY - 8).toFloat(), linePaint)

        // Parallax Layer 1: Towering Cybertronian Spires (0.18x)
        val spireOffset = (camX * 0.18f).toInt()
        val spireSpacing = 54
        val firstSpire = spireOffset / spireSpacing - 1
        for (idx in firstSpire..(firstSpire + w / spireSpacing + 3)) {
            val sx = idx * spireSpacing - spireOffset
            val spireH = 75 + ((idx * 37) and 63)
            val spireW = 28 + ((idx * 13) and 15)
            val topY = h - 32 - spireH
            rectPaint.color = Color.rgb(13, 21, 44)
            canvas.drawRect(sx.toFloat(), topY.toFloat(), (sx + spireW).toFloat(), (h - 32).toFloat(), rectPaint)
            // Glowing vertical circuit strips
            rectPaint.color = if (idx % 2 == 0) Color.rgb(0, 110, 165) else Color.rgb(75, 42, 135)
            canvas.drawRect((sx + 5).toFloat(), (topY + 8).toFloat(), (sx + 7).toFloat(), (h - 40).toFloat(), rectPaint)
            // Window dots
            rectPaint.color = Color.rgb(0, 195, 240)
            for (wy in (topY + 10) until (h - 46) step 10) {
                if ((wy + idx) % 3 != 0) {
                    canvas.drawRect((sx + spireW - 8).toFloat(), wy.toFloat(), (sx + spireW - 5).toFloat(), (wy + 2).toFloat(), rectPaint)
                }
            }
        }

        // Parallax Layer 2: Midground Viaduct Arches (0.42x)
        val archOffset = (camX * 0.42f).toInt()
        val archSpacing = 96
        val firstArch = archOffset / archSpacing - 1
        for (idx in firstArch..(firstArch + w / archSpacing + 3)) {
            val ax = idx * archSpacing - archOffset
            rectPaint.color = Color.rgb(18, 28, 56)
            canvas.drawRect(ax.toFloat(), (h - 68).toFloat(), (ax + 12).toFloat(), h.toFloat(), rectPaint)
            canvas.drawRect(ax.toFloat(), (h - 72).toFloat(), (ax + archSpacing).toFloat(), (h - 66).toFloat(), rectPaint)
            rectPaint.color = Color.rgb(0, 145, 195)
            canvas.drawRect(ax.toFloat(), (h - 73).toFloat(), (ax + archSpacing).toFloat(), (h - 72).toFloat(), rectPaint)
        }
    }

    private fun drawTilemap(canvas: Canvas, engine: GameEngine, viewW: Int, camX: Int, camY: Int) {
        val level = engine.level
        val startTx = (camX / 16 - 1).coerceAtLeast(0)
        val endTx = ((camX + viewW) / 16 + 1).coerceAtMost(Sector01Level.WIDTH_TILES - 1)
        val boostAnim = ((engine.animClock * 8f).toInt() % 2) == 0
        val plasmaAnim = ((engine.animClock * 4f).toInt() % 2) == 0

        for (ty in 0 until Sector01Level.HEIGHT_TILES) {
            val py = ty * 16 - camY
            for (tx in startTx..endTx) {
                val px = tx * 16 - camX
                val tile = level.getTile(tx, ty)
                val bmp = when (tile) {
                    TileType.EMPTY -> null
                    TileType.DECK -> PixelSpriteBank.tileDeck
                    TileType.GIRDER -> PixelSpriteBank.tileGirder
                    TileType.PLATFORM -> PixelSpriteBank.tilePlatform
                    TileType.BOOST_PAD -> if (boostAnim) PixelSpriteBank.tileBoost0 else PixelSpriteBank.tileBoost1
                    TileType.PLASMA_HAZARD -> if (plasmaAnim) PixelSpriteBank.tilePlasma0 else PixelSpriteBank.tilePlasma1
                }
                if (bmp != null) {
                    PixelSpriteBank.drawSprite(canvas, bmp, px, py)
                }
            }
        }

        // Crumbling platforms
        for (cp in level.crumblePlatforms) {
            if (cp.state == 2) continue
            val jitter = if (cp.state == 1 && ((engine.animClock * 40f).toInt() % 2 == 0)) 1 else 0
            val px = cp.tileX * 16 - camX + jitter
            val py = cp.tileY * 16 - camY
            if (px > -16 && px < viewW + 16) {
                PixelSpriteBank.drawSprite(canvas, PixelSpriteBank.tileCrumble, px, py)
            }
        }

        // Arena Energy Walls when boss fight locked
        if (engine.arenaLocked) {
            val leftWallX = Sector01Level.ARENA_LEFT_X.roundToInt() - camX
            val rightWallX = Sector01Level.ARENA_RIGHT_X.roundToInt() - camX
            val pulse = ((sin(engine.animClock * 12f) + 1f) * 0.5f)
            rectPaint.color = if (pulse > 0.5f) Color.rgb(255, 42, 85) else Color.rgb(180, 20, 60)
            canvas.drawRect(leftWallX.toFloat(), 0f, (leftWallX + 3).toFloat(), (12 * 16 - camY).toFloat(), rectPaint)
            canvas.drawRect((rightWallX - 3).toFloat(), 0f, rightWallX.toFloat(), (12 * 16 - camY).toFloat(), rectPaint)
        }
    }

    private fun drawHintSigns(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        for (sign in engine.level.hintSigns) {
            val sx = sign.x.roundToInt() - camX
            val sy = sign.y.roundToInt() - camY
            val textW = sign.text.length * 4 + 6
            rectPaint.color = Color.argb(190, 9, 14, 28)
            canvas.drawRect((sx - 3).toFloat(), (sy - 3).toFloat(), (sx + textW).toFloat(), (sy + 8).toFloat(), rectPaint)
            rectPaint.color = Color.rgb(0, 229, 255)
            canvas.drawRect((sx - 3).toFloat(), (sy - 3).toFloat(), (sx + textW).toFloat(), (sy - 2).toFloat(), rectPaint)
            PixelSpriteBank.drawPixelText(canvas, sign.text, sx, sy, Color.rgb(0, 229, 255))
        }
    }

    private fun drawMovingPlatforms(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        for (mp in engine.level.movingPlatforms) {
            val px = mp.x.roundToInt() - camX
            val py = mp.y.roundToInt() - camY
            val w = mp.width.roundToInt()
            // Top energy strip + dark steel hull
            rectPaint.color = Color.rgb(20, 28, 46)
            canvas.drawRect(px.toFloat(), py.toFloat(), (px + w).toFloat(), (py + 6).toFloat(), rectPaint)
            rectPaint.color = Color.rgb(0, 229, 255)
            canvas.drawRect(px.toFloat(), py.toFloat(), (px + w).toFloat(), (py + 2).toFloat(), rectPaint)
            // Under-thruster flames
            val flicker = ((engine.animClock * 16f).toInt() % 2)
            rectPaint.color = Color.rgb(0, 180, 255)
            canvas.drawRect((px + 4).toFloat(), (py + 6).toFloat(), (px + 9).toFloat(), (py + 8 + flicker).toFloat(), rectPaint)
            canvas.drawRect((px + w - 9).toFloat(), (py + 6).toFloat(), (px + w - 4).toFloat(), (py + 8 + flicker).toFloat(), rectPaint)
        }
    }

    private fun drawLaserGates(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        val flicker = ((engine.animClock * 24f).toInt() % 2) == 0
        for (gate in engine.level.laserGates) {
            val gx = gate.x.roundToInt() - camX
            val top = gate.topY.roundToInt() - camY
            val bot = gate.bottomY.roundToInt() - camY
            rectPaint.color = if (flicker) Color.rgb(255, 42, 85) else Color.rgb(255, 160, 180)
            canvas.drawRect(gx.toFloat(), top.toFloat(), (gx + 3).toFloat(), bot.toFloat(), rectPaint)
            // Emitter cap
            rectPaint.color = Color.rgb(70, 82, 105)
            canvas.drawRect((gx - 2).toFloat(), (top - 3).toFloat(), (gx + 5).toFloat(), top.toFloat(), rectPaint)
        }
    }

    private fun drawBarriersAndCheckpoints(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        for (b in engine.level.barriers) {
            if (b.destroyed) continue
            val bx = b.x.roundToInt() - camX
            val by = b.y.roundToInt() - camY
            PixelSpriteBank.drawSprite(canvas, PixelSpriteBank.barrierTile, bx, by)
            PixelSpriteBank.drawSprite(canvas, PixelSpriteBank.barrierTile, bx, by + 16)
            if (b.hitFlash > 0f) {
                rectPaint.color = Color.argb(140, 255, 255, 255)
                canvas.drawRect(bx.toFloat(), by.toFloat(), (bx + 16).toFloat(), (by + 32).toFloat(), rectPaint)
            }
        }

        for (cp in engine.level.checkpoints) {
            val cx = cp.x.roundToInt() - camX
            val cy = cp.y.roundToInt() - camY
            val bmp = if (cp.activated) PixelSpriteBank.checkpointOn else PixelSpriteBank.checkpointOff
            PixelSpriteBank.drawSprite(canvas, bmp, cx, cy)
            if (cp.activated) {
                val beamAlpha = (60 + (sin(engine.animClock * 6f) * 30f)).toInt().coerceIn(20, 110)
                rectPaint.color = Color.argb(beamAlpha, 0, 229, 255)
                canvas.drawRect((cx + 5).toFloat(), 0f, (cx + 11).toFloat(), cy.toFloat(), rectPaint)
            }
        }
    }

    private fun drawPickups(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        val frame = ((engine.animClock * 6f).toInt() % 2)
        for (p in engine.level.pickups) {
            if (p.collected) continue
            val bob = (sin(engine.animClock * 4.5f + p.bobOffset) * 2.5f).roundToInt()
            val px = p.x.roundToInt() - camX
            val py = p.y.roundToInt() - camY + bob
            val bmp = when (p.type) {
                PickupType.ENERGON_CUBE -> if (frame == 0) PixelSpriteBank.energonCube0 else PixelSpriteBank.energonCube1
                PickupType.MATRIX_RELIC -> if (frame == 0) PixelSpriteBank.matrixRelic0 else PixelSpriteBank.matrixRelic1
            }
            PixelSpriteBank.drawSprite(canvas, bmp, px, py)
        }
    }

    private fun drawEnemies(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        val frameIdx = ((engine.animClock * 7f).toInt() % 2)
        for (e in engine.level.enemies) {
            if (!e.alive) continue
            val ex = e.x.roundToInt() - camX
            val ey = e.y.roundToInt() - camY

            // Seeker Jet red targeting laser telegraph!
            if (e.type == EnemyType.SEEKER_JET && e.telegraphTimer > 0f) {
                linePaint.color = Color.rgb(255, 42, 85)
                canvas.drawLine(
                    (ex + e.width * 0.5f),
                    (ey + e.height * 0.5f),
                    (e.diveTargetX - camX),
                    (e.diveTargetY - camY),
                    linePaint,
                )
            }

            val bmp = when (e.type) {
                EnemyType.RECON_DRONE -> PixelSpriteBank.droneFrames[frameIdx].get(e.facingRight)
                EnemyType.HEAVY_TROOPER -> {
                    val walkIdx = if (abs(e.x - e.homeX) > 1f) frameIdx else 0
                    PixelSpriteBank.trooperFrames[walkIdx].get(e.facingRight)
                }
                EnemyType.LASER_TURRET -> PixelSpriteBank.turretSprite.get(e.facingRight)
                EnemyType.SEEKER_JET -> PixelSpriteBank.seekerSprite.get(e.facingRight)
            }

            val drawX = ex - (bmp.width - e.width.roundToInt()) / 2
            val drawY = ey - (bmp.height - e.height.roundToInt())
            PixelSpriteBank.drawSprite(canvas, bmp, drawX, drawY)

            // Shield indicator on Heavy Trooper front
            if (e.shielded) {
                val shieldX = if (e.facingRight) ex + e.width.roundToInt() + 1 else ex - 3
                rectPaint.color = Color.rgb(0, 229, 255)
                canvas.drawRect(shieldX.toFloat(), (ey + 2).toFloat(), (shieldX + 2).toFloat(), (ey + e.height.roundToInt() - 2).toFloat(), rectPaint)
            }

            if (e.hitFlash > 0f) {
                rectPaint.color = Color.argb(165, 255, 255, 255)
                canvas.drawRect(ex.toFloat(), ey.toFloat(), (ex + e.width).toFloat(), (ey + e.height).toFloat(), rectPaint)
            }
        }
    }

    private fun drawBoss(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        val b = engine.boss
        if (b.action == BossAction.HIDDEN || b.action == BossAction.DEAD) return

        val bx = b.x.roundToInt() - camX
        val by = b.y.roundToInt() - camY

        // Telegraph laser sight before Fusion Volley or Jet Charge!
        if (b.action == BossAction.TELEGRAPH_VOLLEY || b.action == BossAction.TELEGRAPH_CHARGE) {
            val flicker = ((engine.animClock * 30f).toInt() % 2) == 0
            if (flicker) {
                linePaint.color = Color.rgb(255, 42, 85)
                val targetX = if (b.facingRight) (bx + 280).toFloat() else (bx - 280).toFloat()
                canvas.drawLine((bx + b.width * 0.5f), (by + 20f), targetX, (by + 20f), linePaint)
            }
        }

        val frameIdx = ((engine.animClock * 5f).toInt() % 2)
        val bmp = if (b.action == BossAction.JET_CHARGE || b.action == BossAction.TELEGRAPH_CHARGE) {
            PixelSpriteBank.bossCharge.get(b.facingRight)
        } else {
            PixelSpriteBank.bossIdle[frameIdx].get(b.facingRight)
        }
        val drawX = bx - (bmp.width - b.width.roundToInt()) / 2
        val drawY = by - (bmp.height - b.height.roundToInt())
        PixelSpriteBank.drawSprite(canvas, bmp, drawX, drawY)

        // Boss Overclock aura (<50% HP)
        if (engine.bossOverclockTriggered && b.action != BossAction.DYING) {
            for (k in 0 until 4) {
                val angle = engine.animClock * 6f + k * 1.57f
                val sx = bx + b.width * 0.5f + cos(angle) * 20f
                val sy = by + b.height * 0.5f + sin(angle) * 22f
                rectPaint.color = if (k % 2 == 0) Color.rgb(255, 42, 85) else Color.rgb(180, 90, 255)
                canvas.drawRect(sx, sy, sx + 2f, sy + 2f, rectPaint)
            }
        }

        if (b.hitFlash > 0f) {
            rectPaint.color = Color.argb(170, 255, 255, 255)
            canvas.drawRect(bx.toFloat(), by.toFloat(), (bx + b.width).toFloat(), (by + b.height).toFloat(), rectPaint)
        }
    }

    private fun drawAfterimagesAndPlayer(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        // Phase-dash / Nitro afterimages
        for (af in engine.afterimages) {
            val ax = af.x.roundToInt() - camX
            val ay = af.y.roundToInt() - camY
            val alpha = (af.life / 0.16f * 130f).toInt().coerceIn(20, 130)
            rectPaint.color = Color.argb(alpha, 0, 229, 255)
            val w = if (af.isVehicle) 24 else 14
            val h = if (af.isVehicle) 13 else 24
            canvas.drawRect(ax.toFloat(), ay.toFloat(), (ax + w).toFloat(), (ay + h).toFloat(), rectPaint)
        }

        // Invulnerability blink
        if (engine.invulnTimer > 0f && ((engine.animClock * 24f).toInt() % 2 == 0)) {
            return
        }

        val px = engine.playerX.roundToInt() - camX
        val py = engine.playerY.roundToInt() - camY

        val bmp: Bitmap = if (engine.transformTimer > 0f) {
            val progress = 1f - (engine.transformTimer / 0.18f)
            val idx = if (engine.targetForm == PlayerForm.VEHICLE) {
                (progress * 3f).toInt().coerceIn(0, 2)
            } else {
                (2 - (progress * 3f).toInt()).coerceIn(0, 2)
            }
            PixelSpriteBank.transFrames[idx].get(engine.facingRight)
        } else if (engine.form == PlayerForm.VEHICLE) {
            if (engine.boostTimer > 0f) {
                PixelSpriteBank.vehBoost.get(engine.facingRight)
            } else {
                val tireIdx = ((engine.animClock * (abs(engine.playerVx) / 14f + 4f)).toInt() % 2)
                PixelSpriteBank.vehDrive[tireIdx].get(engine.facingRight)
            }
        } else {
            if (!engine.onGround) {
                if (engine.playerVy < 15f) {
                    PixelSpriteBank.mechJump.get(engine.facingRight)
                } else {
                    PixelSpriteBank.mechFall.get(engine.facingRight)
                }
            } else if (abs(engine.playerVx) > 12f) {
                val runIdx = ((engine.runAnimTimer * 7.5f).toInt() % PixelSpriteBank.mechRun.size)
                PixelSpriteBank.mechRun[runIdx].get(engine.facingRight)
            } else {
                val idleIdx = ((engine.animClock * 2.5f).toInt() % 2)
                PixelSpriteBank.mechIdle[idleIdx].get(engine.facingRight)
            }
        }

        val suspOffset = if (engine.form == PlayerForm.VEHICLE && engine.suspensionCompression > 0.2f) 1 else 0
        val drawX = px - (bmp.width - engine.playerWidth.roundToInt()) / 2
        val drawY = py - (bmp.height - engine.playerHeight.roundToInt()) + suspOffset + (if (engine.transformTimer > 0f) 0 else 2)
        PixelSpriteBank.drawSprite(canvas, bmp, drawX, drawY)

        // Mech Cannon Charge Aura
        if (engine.form == PlayerForm.MECH && engine.chargeTimer > 0.12f) {
            val ready = engine.chargeTimer >= GameEngine.CHARGE_THRESHOLD
            val auraColor = if (ready) Color.rgb(255, 193, 7) else Color.rgb(0, 229, 255)
            val cx = px + engine.playerWidth * 0.5f
            val cy = py + engine.playerHeight * 0.45f
            val radius = if (ready) 13f else 10f
            for (k in 0 until 4) {
                val angle = engine.animClock * (if (ready) 14f else 8f) + k * 1.57f
                val sx = (cx + cos(angle) * radius).roundToInt()
                val sy = (cy + sin(angle) * radius).roundToInt()
                rectPaint.color = auraColor
                canvas.drawRect(sx.toFloat(), sy.toFloat(), (sx + 2).toFloat(), (sy + 2).toFloat(), rectPaint)
            }
        }
    }

    private fun drawWeather(canvas: Canvas, engine: GameEngine) {
        val hotZone = engine.currentZone == LevelZone.REACTOR_GAUNTLET || engine.currentZone == LevelZone.BOSS_ARENA
        for (wp in engine.weatherParticles) {
            val wx = wp.x.roundToInt()
            val wy = wp.y.roundToInt()
            if (hotZone) {
                rectPaint.color = if ((wx + wy) % 2 == 0) Color.rgb(255, 130, 30) else Color.rgb(255, 70, 90)
                canvas.drawRect(wx.toFloat(), wy.toFloat(), (wx + 2).toFloat(), (wy + 2).toFloat(), rectPaint)
            } else {
                linePaint.color = wp.color
                canvas.drawLine(wx.toFloat(), wy.toFloat(), (wx - 1).toFloat(), (wy + wp.length).toFloat(), linePaint)
            }
        }
    }

    private fun drawSlashArcs(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        for (sl in engine.slashArcs) {
            val sx = sl.x.roundToInt() - camX
            val sy = sl.y.roundToInt() - camY
            val progress = 1f - (sl.life / 0.14f)
            rectPaint.color = Color.rgb(0, 229, 255)
            val arcX = if (sl.facingRight) sx + (progress * 10).roundToInt() else sx + 14 - (progress * 10).roundToInt()
            canvas.drawRect(arcX.toFloat(), (sy + 2).toFloat(), (arcX + 6).toFloat(), (sy + 18).toFloat(), rectPaint)
            rectPaint.color = Color.rgb(220, 254, 255)
            canvas.drawRect((arcX + 1).toFloat(), (sy + 4).toFloat(), (arcX + 4).toFloat(), (sy + 16).toFloat(), rectPaint)
        }
    }

    private fun drawProjectilesAndParticles(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        for (p in engine.projectiles) {
            val px = p.x.roundToInt() - camX
            val py = p.y.roundToInt() - camY
            if (p.isChargedBeam) {
                rectPaint.color = Color.rgb(255, 193, 7)
                canvas.drawRect((px - 9).toFloat(), (py - 4).toFloat(), (px + 9).toFloat(), (py + 4).toFloat(), rectPaint)
                rectPaint.color = Color.rgb(0, 229, 255)
                canvas.drawRect((px - 7).toFloat(), (py - 2).toFloat(), (px + 7).toFloat(), (py + 2).toFloat(), rectPaint)
                rectPaint.color = Color.WHITE
                canvas.drawRect((px - 5).toFloat(), (py - 1).toFloat(), (px + 5).toFloat(), (py + 1).toFloat(), rectPaint)
            } else {
                val r = p.radius.roundToInt()
                rectPaint.color = p.color
                canvas.drawRect((px - r).toFloat(), (py - r).toFloat(), (px + r).toFloat(), (py + r).toFloat(), rectPaint)
                rectPaint.color = Color.WHITE
                canvas.drawRect((px - 1).toFloat(), (py - 1).toFloat(), (px + 1).toFloat(), (py + 1).toFloat(), rectPaint)
            }
        }

        for (pt in engine.particles) {
            val px = pt.x.roundToInt() - camX
            val py = pt.y.roundToInt() - camY
            rectPaint.color = pt.color
            canvas.drawRect(px.toFloat(), py.toFloat(), (px + pt.size).toFloat(), (py + pt.size).toFloat(), rectPaint)
        }
    }

    private fun drawFloatingTexts(canvas: Canvas, engine: GameEngine, camX: Int, camY: Int) {
        for (ft in engine.floatingTexts) {
            val fx = ft.x.roundToInt() - camX
            val fy = ft.y.roundToInt() - camY
            PixelSpriteBank.drawPixelText(canvas, ft.text, fx, fy, ft.color)
        }
    }
}
