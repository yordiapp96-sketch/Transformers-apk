package com.example.game.pixel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * Generates and caches razor-sharp 16-bit pixel-art Bitmaps from character matrices.
 * Every sprite is drawn with nearest-neighbor filtering (isFilterBitmap = false)
 * so pixels stay 100% crisp at any screen resolution.
 */
object PixelSpriteBank {

    private val palette: Map<Char, Int> = mapOf(
        '.' to Color.TRANSPARENT,
        'k' to Color.rgb(9, 12, 21),
        'R' to Color.rgb(230, 43, 61),
        'r' to Color.rgb(154, 19, 36),
        'h' to Color.rgb(255, 118, 130),
        'B' to Color.rgb(31, 111, 235),
        'b' to Color.rgb(17, 62, 145),
        'S' to Color.rgb(209, 219, 232),
        's' to Color.rgb(100, 116, 139),
        'd' to Color.rgb(44, 56, 78),
        'D' to Color.rgb(24, 32, 48),
        'C' to Color.rgb(0, 229, 255),
        'c' to Color.rgb(180, 252, 255),
        'G' to Color.rgb(255, 193, 7),
        'g' to Color.rgb(180, 100, 12),
        'W' to Color.rgb(24, 30, 42),
        'w' to Color.rgb(78, 92, 114),
        'P' to Color.rgb(157, 78, 221),
        'p' to Color.rgb(91, 33, 182),
        'E' to Color.rgb(255, 42, 85),
        'e' to Color.rgb(255, 166, 188),
        'O' to Color.rgb(255, 122, 0),
        'Y' to Color.rgb(255, 238, 60),
    )

    val nearestPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    private fun build(rows: List<String>): Bitmap {
        val h = rows.size
        val w = rows.maxOf { it.length }
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val row = rows[y]
            for (x in 0 until w) {
                val ch = if (x < row.length) row[x] else '.'
                pixels[y * w + x] = palette[ch] ?: Color.TRANSPARENT
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun flipX(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val inPx = IntArray(w * h)
        val outPx = IntArray(w * h)
        src.getPixels(inPx, 0, w, 0, 0, w, h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                outPx[y * w + (w - 1 - x)] = inPx[y * w + x]
            }
        }
        return Bitmap.createBitmap(outPx, w, h, Bitmap.Config.ARGB_8888)
    }

    class SpritePair(val right: Bitmap) {
        val left: Bitmap = flipX(right)
        fun get(facingRight: Boolean): Bitmap = if (facingRight) right else left
    }

    // ==================== HERO MECH (24x28) ====================
    private val mechIdle0Right = build(
        listOf(
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBk.kBBk.kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            "..krrRRRRRRSSSRRRRRrrk..",
            ".kSSk.kRRRSSSSRRRk.kBBk.",
            ".kBBk.krrGGGGGGrrk.kBBk.",
            ".kBBk..kddSSSSddk..kSSkk",
            ".kssk..kssssssssk..kBBk.",
            "..kk...kBBk..kBBk...kk..",
            ".......kBBk..kBBk.......",
            "......kBWBk..kBWBk......",
            "......kBWWk..kBWWk......",
            "......kBWBk..kBWBk......",
            "......kBBBk..kBBBk......",
            "......kbbbk..kbbbk......",
            ".....kSSSSk..kSSSSk.....",
            ".....kddddk..kddddk.....",
            ".....kkkkkk..kkkkkk.....",
            "........................",
            "........................",
        )
    )

    private val mechIdle1Right = build(
        listOf(
            "........................",
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBk.kBBk.kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            "..krrRRRRRRSSSRRRRRrrk..",
            ".kSSk.kRRRSSSSRRRk.kBBk.",
            ".kBBk.krrGGGGGGrrk.kBBk.",
            ".kBBk..kddSSSSddk..kSSkk",
            ".kssk..kssssssssk..kBBk.",
            "..kk...kBBk..kBBk...kk..",
            "......kBWBk..kBWBk......",
            "......kBWWk..kBWWk......",
            "......kBWBk..kBWBk......",
            "......kBBBk..kBBBk......",
            "......kbbbk..kbbbk......",
            ".....kSSSSk..kSSSSk.....",
            ".....kddddk..kddddk.....",
            ".....kkkkkk..kkkkkk.....",
            "........................",
            "........................",
        )
    )

    private val mechRun0Right = build(
        listOf(
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            ".kBBkRRRRRRSSSRRRRRrk...",
            ".kBBkkSSkRRRSSSSRRkBBk..",
            "..kk.kBBkkrrGGGrrkkSSkk.",
            ".....kssk.kddSSddk.kBBk.",
            "......kk..kssssssk..kk..",
            ".........kBBk..kBBk.....",
            "........kBWBk...kBWBk...",
            ".......kBWWk.....kBWWk..",
            ".......kBBk.......kBBk..",
            "......kbbk........kbbk..",
            ".....kSSSk........kSSSk.",
            ".....kdddk........kdddk.",
            ".....kkkkk........kkkkk.",
            "........................",
            "........................",
            "........................",
            "........................",
            "........................",
        )
    )

    private val mechRun1Right = build(
        listOf(
            "........................",
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            "..krrRRRRRRSSSRRRRRrrk..",
            "...kSSk.kRRRSSSSRRk.kBBk",
            "...kBBk.krrGGGGGGrrkkSSk",
            "....kk...kddSSSSddk..kk.",
            ".........kssssssssk.....",
            "........kBBk...kBBk.....",
            ".......kBWBk...kBWBk....",
            ".......kBWWk...kBWWk....",
            "........kBBk....kBBk....",
            "........kbbk...kSSSk....",
            ".......kSSSk...kdddk....",
            ".......kdddk...kkkkk....",
            ".......kkkkk............",
            "........................",
            "........................",
            "........................",
            "........................",
        )
    )

    private val mechRun2Right = build(
        listOf(
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            "..krrRRRRRRSSSRRRRRrk...",
            "....kSSkRRRRSSSSRRkBBk..",
            "....kBBkkrrGGGGGGrrSSkk.",
            ".....kk..kddSSSSddkkBBk.",
            ".........kssssssssk.kk..",
            ".........kBBk..kBBk.....",
            "........kBWBk..kBWBk....",
            "........kBWWk..kBWWk....",
            "........kBBBk..kBBBk....",
            "........kbbbk..kbbbk....",
            "........kSSSk..kSSSk....",
            "........kdddk..kdddk....",
            "........kkkkk..kkkkk....",
            "........................",
            "........................",
            "........................",
            "........................",
            "........................",
        )
    )

    private val mechRun3Right = build(
        listOf(
            "........................",
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            "...krrRRRRRSSSRRRRRBBk..",
            "..kBBk.kRRRSSSSRRRkSSk..",
            "..kSSkk.krrGGGGGGrrkk...",
            "...kk....kddSSSSddk.....",
            ".........kssssssssk.....",
            "........kBBk...kBBk.....",
            ".......kBWBk....kBWBk...",
            "......kBWWk......kBWWk..",
            "......kBBk........kBBk..",
            ".....kSSSk........kbbk..",
            ".....kdddk.......kSSSk..",
            ".....kkkkk.......kdddk..",
            ".................kkkkk..",
            "........................",
            "........................",
            "........................",
            "........................",
        )
    )

    private val mechJumpRight = build(
        listOf(
            ".......k........k.......",
            "......kBk..kk..kBk......",
            "......kBkkBSSBkkBk......",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            ".kBBkRRRRRRSSSRRRRRkBBk.",
            ".kSSk.kRRRSSSSRRRk.kSSk.",
            "..kk..krrGGGGGGrrk..kk..",
            ".......kddSSSSddk.......",
            ".......kssssssssk.......",
            "......kBBk....kBBk......",
            ".....kBWBk....kBWBk.....",
            ".....kBWWk....kBWWk.....",
            "......kBBk.....kBBk.....",
            ".....kSSSk.....kSSSk....",
            ".....kdddk.....kdddk....",
            ".....kCCdk.....kCCdk....",
            ".....kCOdk.....kCOdk....",
            "......kOk.......kOk.....",
            ".......k.........k......",
            "........................",
            "........................",
            "........................",
        )
    )

    private val mechFallRight = build(
        listOf(
            ".kSSk..k........k..kSSk.",
            ".kBBk.kBk..kk..kBk.kBBk.",
            "..kk..kBkkBSSBkkBk..kk..",
            "......kBkBCCCCBkBk......",
            "......kBkkSSSSkkBk......",
            ".......kkkSSSSkkk.......",
            "....kkkRRRRRRRRRRkkk....",
            "...khhRRRRGGGGGRRRRhk...",
            "..kRRRCCcRGGGGGRcCCRRk..",
            "..kRRRCCCRRSSSSRCCCRRk..",
            "..krrRRRRRRSSSRRRRRrrk..",
            "......kRRRSSSSRRRk......",
            "......krrGGGGGGrrk......",
            ".......kddSSSSddk.......",
            ".......kssssssssk.......",
            "......kBBk....kBBk......",
            "......kBWBk..kBWBk......",
            "......kBWWk..kBWWk......",
            "......kBBBk..kBBBk......",
            "......kbbbk..kbbbk......",
            ".....kSSSSk..kSSSSk.....",
            ".....kddddk..kddddk.....",
            ".....kkkkkk..kkkkkk.....",
            "........................",
            "........................",
            "........................",
            "........................",
            "........................",
        )
    )

    // ==================== TRANSFORM SEQUENCE (28x28) ====================
    private val trans0Right = build(
        listOf(
            "............................",
            "............................",
            ".........k........k.........",
            "........kBk..kk..kBk........",
            "........kBkkBSSBkkBk........",
            "......kkkBkBCCCCBkBkkk......",
            "....kkRRRRkkSSSSkkRRRRkk....",
            "...khhRRRRGGGGGGGGRRRRhhk...",
            "..kRRRCCcRRGGGGGGGRRcCCRRk..",
            "..kRRRCCCRRRSSSSRRRRCCCRRk..",
            ".kBBkrrRRRRRRSSRRRRRRrrkBBk.",
            ".kSSk..kRRRRSSSSRRRRk..kSSk.",
            "..kk...krrGGGGGGGGrrk...kk..",
            ".......kddSSSSSSSSddk.......",
            "......kssssssssssssssk......",
            ".....kBBWBk......kBWBBk.....",
            ".....kBWWBk......kBWWBk.....",
            ".....kBBBBk......kBBBBk.....",
            ".....kSSSSk......kSSSSk.....",
            ".....kddddk......kddddk.....",
            ".....kkkkkk......kkkkkk.....",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
        )
    )

    private val trans1Right = build(
        listOf(
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "........kkkkkkkkkkkk........",
            "......kkRRRRGGGGGRRRRkk.....",
            "....kkhhRRRRGGGGGRRRRhhkk...",
            "...kRRRRCCcRRSSSSRRcCCRRRk..",
            "..kBBkRRCCCRRSSSSRRCCCRRkBk.",
            "..kSSkrrRRRRRSSSSRRRRRrrkSk.",
            "...kkkddSSSSSSSSSSSSSSddkkk.",
            "....kkWWssssssssssssssWWkk..",
            "...kWWWWkBBBBk..kBBBBkWWWWk.",
            "...kWwWWkBWWBk..kBWWBkWWwWk.",
            "...kWWWWkBBBBk..kBBBBkWWWWk.",
            "....kkkkkSSSSk..kSSSSkkkkk..",
            "........kkkkkk..kkkkkk......",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
        )
    )

    private val trans2Right = build(
        listOf(
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            ".........kkkkkkk............",
            ".......kkSSSkkCCk...........",
            ".....kkRRRRRRRRRRkkkk.......",
            "...kkhhRRRRCCCCcRRRRRkkk....",
            "..kSSkRRRRRCCCCCRRRRRhhRk...",
            "..kSSkBBBBBBBBBBBBBBBBCCRk..",
            "..kddkBBWWWWBBBBBBWWWWBGGk..",
            "...kkkBWWWWWWBBBBWWWWWWBkk..",
            ".....kWWwWwWWkkkkWWwWwWWk...",
            ".....kWWWWWWk....kWWWWWWk...",
            "......kkkkkk......kkkkkk....",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
            "............................",
        )
    )

    // ==================== HERO VEHICLE MODE (32x18) ====================
    private val veh0Right = build(
        listOf(
            "..........kkkkkk................",
            ".........kSSSSCCkk..............",
            "........kkSSkkddSSk.............",
            "......kkRRRRRRRRRRkkkk..........",
            "....kkhhRRRRRRRRRRRRRRkkk.......",
            "...kSSkRRRRRCCCCCCcRRRRRhk......",
            "..kSSkRRRRRRCCCCCCCRRRRRRRk.....",
            "..kSSkBBBBBBBBBBBBBBBBBBCCRk....",
            ".kddkBBBBGGGGGGGGGGGGGBBGGSk....",
            ".kBBkBBBBBBBBBBBBBBBBBBBBSSk....",
            ".kdkBWkkkWBbbbbbbbbbBWkkkWBk....",
            "..kWWWWWWkBdddddddddBWwwWWWk....",
            "..kWwSSwWkBkkkkkkkkkBWwSSwWk....",
            "..kWWSSWWk..........kWWSSWWk....",
            "..kWWWWWWk..........kWWWWWWk....",
            "...kkkkkk............kkkkkk.....",
            "................................",
            "................................",
        )
    )

    private val veh1Right = build(
        listOf(
            "..........kkkkkk................",
            ".........kSSSSCCkk..............",
            "........kkSSkkddSSk.............",
            "......kkRRRRRRRRRRkkkk..........",
            "....kkhhRRRRRRRRRRRRRRkkk.......",
            "...kSSkRRRRRCCCCCCcRRRRRhk......",
            "..kSSkRRRRRRCCCCCCCRRRRRRRk.....",
            "..kSSkBBBBBBBBBBBBBBBBBBCCRk....",
            ".kddkBBBBGGGGGGGGGGGGGBBGGSk....",
            ".kBBkBBBBBBBBBBBBBBBBBBBBSSk....",
            ".kdkBWkkkWBbbbbbbbbbBWkkkWBk....",
            "..kWwwWWWkBdddddddddBWWWWWWk....",
            "..kWWSSwWkBkkkkkkkkkBWWSSwWk....",
            "..kWwSSWWk..........kWwSSWWk....",
            "..kWWWWWWk..........kWWWWWWk....",
            "...kkkkkk............kkkkkk.....",
            "................................",
            "................................",
        )
    )

    private val vehBoostRight = build(
        listOf(
            "..........kkkkkk................",
            ".........kSSSSCCkk..............",
            "........kkSSkkddSSk.............",
            "......kkRRRRRRRRRRkkkk..........",
            "....kkhhRRRRRRRRRRRRRRkkk.......",
            "..kCkSSkRRRRCCCCCCcRRRRRhk......",
            ".kCOkSSkRRRRCCCCCCCRRRRRRRk.....",
            "kCCOkSSkBBBBBBBBBBBBBBBBCCRk....",
            "kcckddkBBBBGGGGGGGGGGGGBBGGSk...",
            "kCOkBBkBBBBBBBBBBBBBBBBBBSSk....",
            ".kdkBWkkkWBbbbbbbbbbBWkkkWBk....",
            "..kWWWWWWkBdddddddddBWwwWWWk....",
            "..kWwSSwWkBkkkkkkkkkBWwSSwWk....",
            "..kWWSSWWk..........kWWSSWWk....",
            "..kWWWWWWk..........kWWWWWWk....",
            "...kkkkkk............kkkkkk.....",
            "................................",
            "................................",
        )
    )

    // ==================== ENEMIES ====================
    // Recon Hover Drone (20x18)
    private val drone0Right = build(
        listOf(
            ".....kkkkkkkkkk.....",
            "...kkPPPPPPPPPPkk...",
            "..kPPpddddddddpPPk..",
            ".kPPpdEEEEEEEEdpPPk.",
            ".kpppdEeEEEEeEdpppk.",
            "kdddddEEEEEEEEdddddk",
            "kssPPPPddddddddPPssk",
            ".kppPPddddddddddppk.",
            "..kppkkPPPPPPPPkkpk.",
            "...kk..kddddddk..kk.",
            ".......kCCkkCCk.....",
            ".......kOk..kOk.....",
            "........k....k......",
            "....................",
        )
    )

    private val drone1Right = build(
        listOf(
            "....................",
            ".....kkkkkkkkkk.....",
            "...kkPPPPPPPPPPkk...",
            "..kPPpddddddddpPPk..",
            ".kPPpdEEEEEEEEdpPPk.",
            ".kpppdEeEEEEeEdpppk.",
            "kdddddEEEEEEEEdddddk",
            "kssPPPPddddddddPPssk",
            ".kppPPddddddddddppk.",
            "..kppkkPPPPPPPPkkpk.",
            "...kk..kddddddk..kk.",
            ".......kCOkkOCk.....",
            "........k....k......",
            "....................",
        )
    )

    // Heavy Decepticon Trooper (22x26)
    private val trooper0Right = build(
        listOf(
            ".....k........k.......",
            "....kPk..kk..kPk......",
            "....kPkkPddPkkPk......",
            "....kPkPEEEEPkPk......",
            ".....kkPddddPkk.......",
            "..kkkPPPPPPPPPPkkk....",
            ".kPPPPddddddddPPPPk...",
            ".kppPPpPPPPPPpPPppk...",
            ".kddPPdSSddSSdPPddk...",
            ".kPPk.kppPPppk.kSSk...",
            ".kPPk.kddddddk.kPPkkkk",
            ".kppk.kssssssk.kEEEEEk",
            "..kk..kPPk.kPPk.kkkkkk",
            ".....kPPP..kPPPk......",
            ".....kppk..kpppk......",
            ".....kddk..kdddk......",
            "....kPPPk..kPPPk......",
            "....kdddk..kdddk......",
            "....kkkkk..kkkkk......",
            "......................",
        )
    )

    private val trooper1Right = build(
        listOf(
            "......................",
            ".....k........k.......",
            "....kPk..kk..kPk......",
            "....kPkkPddPkkPk......",
            "....kPkPEEEEPkPk......",
            ".....kkPddddPkk.......",
            "..kkkPPPPPPPPPPkkk....",
            ".kPPPPddddddddPPPPk...",
            ".kppPPpPPPPPPpPPppk...",
            ".kddPPdSSddSSdPPddk...",
            ".kPPk.kppPPppk.kSSk...",
            ".kPPk.kddddddk.kPPkkkk",
            ".kppk.kssssssk.kEEEEEk",
            "..kk..kPPk.kPPk.kkkkkk",
            "....kPPPk...kPPPk.....",
            "....kppk.....kppk.....",
            "....kddk.....kddk.....",
            "...kPPPk.....kPPPk....",
            "...kdddk.....kdddk....",
            "...kkkkk.....kkkkk....",
        )
    )

    // Laser Turret (18x16)
    private val turretRight = build(
        listOf(
            "......kkkkkk......",
            "....kkPPPPPPkk....",
            "...kPPddddddPPk...",
            "..kPPdEEEEEEEdPPk.",
            "..kppdEeEEEEEkkkkk",
            "..kddddddddddSSSSk",
            "...kPPPPPPPPPPkkkk",
            "..kssssssssssssk..",
            ".kddddddddddddddk.",
            "kSSSSSSSSSSSSSSSSk",
            "kddddddddddddddddk",
            "kkkkkkkkkkkkkkkkkk",
        )
    )

    // Seeker Jet (26x14)
    private val seekerRight = build(
        listOf(
            "........kkkk..............",
            ".......kPPPPkk............",
            "......kPPppPPPkkk.........",
            "....kkPPpEEEEpPPPkkkk.....",
            "..kkPPPPPEeeEPPPPPPPPkkk..",
            "kCCkkPPPPPPPPPPPPSSSSSSSk.",
            "kOOkkppppPPPPPPPPppppppkk.",
            "..kkddddppppppppddddddkk..",
            ".....kkkddddddddkkkkkk....",
            "........kPPppPPk..........",
            ".........kkkkkk...........",
        )
    )

    // ==================== BOSS: MEGATRONUS MK-IV (42x46) ====================
    private val bossIdle0Right = build(
        listOf(
            ".............kk............kk.............",
            "............kPPk...kkkk...kPPk............",
            "...........kPPPkk.kPPPPk.kkPPPk...........",
            "...........kPPPPPkkSSSSkkPPPPPk...........",
            "...........kPPddPPSEEEESPddPPPk...........",
            "............kkddPPSEeeESPddPkk............",
            ".............kddPPSSSSSSPPddk.............",
            "......kkkkkkkPPPPPPPPPPPPPPPPkkkkkkk......",
            "....kkPPPPPPPPpddddddddddddpPPPPPPPPkk....",
            "...kPPPppPPPPPdSSSEEEEEESSSdPPPPPppPPPk...",
            "..kPPPpddpPPPPdSSSEeEEeESSSdPPPPpddpPPPk..",
            "..kPPpddddpPPPdSSSEEEEEESSSdPPPpddddpPPk..",
            "..kppddddddPPPPddddddddddddPPPPddddddppk..",
            ".kSSSk.kPPpPPPPPPPPppppPPPPPPPPpPPk.kSSSk.",
            ".kPPPk.kppPPPPppppppppppppppPPPPppk.kPPPk.",
            ".kPPPk..kddddPPPPPPPPPPPPPPPPddddk..kPPPk.",
            ".kdddk..kSSSSddddddddddddddddSSSSk..kdddk.",
            ".kPPPk..kppppSSSSSSSSSSSSSSSSppppk..kPPPk.",
            ".kPPPk...kPPPPPPk........kPPPPPPk...kPPPk.",
            "kSSSSSk..kPPPPPPk........kPPPPPPk..kSSSSSk",
            "kEEEEk...kppPPppk........kppPPppk...kddddk",
            "kEEEEk...kddddddk........kddddddk...kSSSk.",
            "kSSSSSk..kPPPPPPk........kPPPPPPk....kkk..",
            ".kkkkk..kPPWWWWPPk......kPPWWWWPPk........",
            "........kPPWWWWPPk......kPPWWWWPPk........",
            "........kppWWWWppk......kppWWWWppk........",
            "........kddddddddk......kddddddddk........",
            ".......kPPPPPPPPPPk....kPPPPPPPPPPk.......",
            ".......kSSSSSSSSSSk....kSSSSSSSSSSk.......",
            ".......kddddddddddk....kddddddddddk.......",
            ".......kkkkkkkkkkkk....kkkkkkkkkkkk.......",
        )
    )

    private val bossIdle1Right = build(
        listOf(
            "..........................................",
            ".............kk............kk.............",
            "............kPPk...kkkk...kPPk............",
            "...........kPPPkk.kPPPPk.kkPPPk...........",
            "...........kPPPPPkkSSSSkkPPPPPk...........",
            "...........kPPddPPSEEEESPddPPPk...........",
            "............kkddPPSEeeESPddPkk............",
            ".............kddPPSSSSSSPPddk.............",
            "......kkkkkkkPPPPPPPPPPPPPPPPkkkkkkk......",
            "....kkPPPPPPPPpddddddddddddpPPPPPPPPkk....",
            "...kPPPppPPPPPdSSSEEEEEESSSdPPPPPppPPPk...",
            "..kPPPpddpPPPPdSSSEeEEeESSSdPPPPpddpPPPk..",
            "..kPPpddddpPPPdSSSEEEEEESSSdPPPpddddpPPk..",
            "..kppddddddPPPPddddddddddddPPPPddddddppk..",
            ".kSSSk.kPPpPPPPPPPPppppPPPPPPPPpPPk.kSSSk.",
            ".kPPPk.kppPPPPppppppppppppppPPPPppk.kPPPk.",
            ".kPPPk..kddddPPPPPPPPPPPPPPPPddddk..kPPPk.",
            ".kdddk..kSSSSddddddddddddddddSSSSk..kdddk.",
            ".kPPPk..kppppSSSSSSSSSSSSSSSSppppk..kPPPk.",
            ".kPPPk...kPPPPPPk........kPPPPPPk...kPPPk.",
            "kSSSSSk..kPPPPPPk........kPPPPPPk..kSSSSSk",
            "kEeEEk...kppPPppk........kppPPppk...kddddk",
            "kEeEEk...kddddddk........kddddddk...kSSSk.",
            "kSSSSSk..kPPPPPPk........kPPPPPPk....kkk..",
            ".kkkkk..kPPWWWWPPk......kPPWWWWPPk........",
            "........kPPWWWWPPk......kPPWWWWPPk........",
            "........kppWWWWppk......kppWWWWppk........",
            "........kddddddddk......kddddddddk........",
            ".......kPPPPPPPPPPk....kPPPPPPPPPPk.......",
            ".......kSSSSSSSSSSk....kSSSSSSSSSSk.......",
            ".......kddddddddddk....kddddddddddk.......",
        )
    )

    private val bossChargeRight = build(
        listOf(
            "..........................................",
            "..........................................",
            ".............kk............kk.............",
            "............kPPk...kkkk...kPPk............",
            "...........kPPPkk.kPPPPk.kkPPPk...........",
            "...........kPPddPPSEEEESPddPPPk...........",
            "......kkkkkkkPPPPPPSSSSSSPPPPPPkkkkkkk....",
            "....kkPPPPPPPPpddddddddddddpPPPPPPPPkk....",
            "..kEkkPPPppPPPdSSSEEEEEESSSdPPPPPppPPPk...",
            ".kEEkkPPPpddpPdSSSEeEEeESSSdPPPPpddpPPPk..",
            "kEeEkkppddddddPddddddddddddPPPPddddddppk..",
            ".kEEkkSSSkPPpPPPPPPppppPPPPPPPPpPPkSSSSk..",
            "..kEk.kPPPkppPPPPppppppppPPPPppkPPkEEEEk..",
            "......kPPPkddddPPPPPPPPPPPPddddkPPkEEEEk..",
            ".......kddkSSSSddddddddddddSSSSkddkSSSSk..",
            "........kkkPPPPPPk......kPPPPPPkkkkkkkk...",
            ".........kPPWWWWPPk....kPPWWWWPPk.........",
            "........kPPWWWWPPk......kPPWWWWPPk........",
            "........kddddddddk......kddddddddk........",
            ".......kPPPPPPPPPPk....kPPPPPPPPPPk.......",
            ".......kSSSSSSSSSSk....kSSSSSSSSSSk.......",
            ".......kddddddddddk....kddddddddddk.......",
            ".......kkkkkkkkkkkk....kkkkkkkkkkkk.......",
        )
    )

    // ==================== PICKUPS & PROPS ====================
    val energonCube0 = build(
        listOf(
            "....kkkkkk....",
            "..kkCCCCCCkk..",
            ".kCCCccccCCCk.",
            "kCCCccCCccCCCk",
            "kCCcCCCCCCcCCk",
            "kCCcCCccCCcCCk",
            "kCCcCCccCCcCCk",
            "kCCcCCCCCCcCCk",
            "kCCCccCCccCCCk",
            ".kCCCccccCCCk.",
            "..kkCCCCCCkk..",
            "....kkkkkk....",
        )
    )

    val energonCube1 = build(
        listOf(
            "....kkkkkk....",
            "..kkccCCcckk..",
            ".kcCCcCCcCCck.",
            "kCCcCCCCCCcCCk",
            "kCCcCCccCCcCCk",
            "kCCcCccccCcCCk",
            "kCCcCccccCcCCk",
            "kCCcCCccCCcCCk",
            "kCCcCCCCCCcCCk",
            ".kcCCcCCcCCck.",
            "..kkccCCcckk..",
            "....kkkkkk....",
        )
    )

    val matrixRelic0 = build(
        listOf(
            "......kkkk......",
            "....kkGGGGkk....",
            "..kkGGGccGGGkk..",
            ".kGGggkCCkggGGk.",
            "kGGgkCCCCCCkgGGk",
            "kGgkCCccccCCkgGk",
            "kGgkCCcCCcCCkgGk",
            "kGGgkCCCCCCkgGGk",
            ".kGGggkCCkggGGk.",
            "..kkGGGccGGGkk..",
            "....kkGGGGkk....",
            "......kkkk......",
        )
    )

    val matrixRelic1 = build(
        listOf(
            "......kkkk......",
            "....kkYYYYkk....",
            "..kkYYYccYYYkk..",
            ".kYYGGkCCkGGYYk.",
            "kYYGkCCCCCCkGYYk",
            "kYGkCCcCCcCCkGYk",
            "kYGkCCcCCcCCkGYk",
            "kYYGkCCCCCCkGYYk",
            ".kYYGGkCCkGGYYk.",
            "..kkYYYccYYYkk..",
            "....kkYYYYkk....",
            "......kkkk......",
        )
    )

    val checkpointOff = build(
        listOf(
            "......kkkk......",
            ".....kssssk.....",
            "....kssddssk....",
            "....ksddddsk....",
            ".....kssssk.....",
            "......kddk......",
            "......kddk......",
            "......kddk......",
            "......kddk......",
            "......kddk......",
            ".....kSSSSk.....",
            "....kddddddk....",
            "...kSSSSSSSSk...",
            "...kkkkkkkkkk...",
        )
    )

    val checkpointOn = build(
        listOf(
            "......kkkk......",
            ".....kCCCCk.....",
            "....kCCccCCk....",
            "....kCCCCCCk....",
            ".....kCCCCk.....",
            "......kCCk......",
            "......kddk......",
            "......kCCk......",
            "......kddk......",
            "......kCCk......",
            ".....kSSSSk.....",
            "....kddddddk....",
            "...kSSSSSSSSk...",
            "...kkkkkkkkkk...",
        )
    )

    val barrierTile = build(
        listOf(
            "kGGGGkkkkkkGGGGk",
            "kggGGkOOOOkgGGGk",
            "kddddkddddkddddk",
            "kSSSSkYYYYkSSSSk",
            "kddddkOOOOkkdddk",
            "kGGGGkkkkkkGGGGk",
            "kddddkSSSSkddddk",
            "kOOddkddddkddOOk",
            "kYYYYkGGGGkYYYYk",
            "kddddkddddkddddk",
            "kSSSSkOOOOkkSSSk",
            "kddddkYYYYkddddk",
            "kGGGGkkkkkkGGGGk",
            "kddddkddddkddddk",
            "kSSSSkSSSSkSSSSk",
            "kkkkkkkkkkkkkkkk",
        )
    )

    // ==================== 16x16 TILESET ====================
    val tileDeck = build(
        listOf(
            "CCCCCCCCCCCCCCCC",
            "SSSSSSSSSSSSSSSS",
            "dddddddddddddddd",
            "dDDDDdDDDDdDDDDd",
            "dDssDdDssDdDssDd",
            "dddddddddddddddd",
            "DDkDDDDDDkDDDDDD",
            "DDkDDssDDkDDssDD",
            "DDkDDDDDDkDDDDDD",
            "dddddddddddddddd",
            "DDDDkDDDDDDDDkDD",
            "DDDDkDDssDDDDkDD",
            "DDDDDDDDDDDDDDDD",
            "dDDDDdDDDDdDDDDd",
            "dddddddddddddddd",
            "kkkkkkkkkkkkkkkk",
        )
    )

    val tileGirder = build(
        listOf(
            "kkkkkkkkkkkkkkkk",
            "kddddddddddddddk",
            "kdDDDDDDDDDDDDdk",
            "kdDDssDDDDssDDdk",
            "kdDDDDDDDDDDDDdk",
            "kddddkddddkddddk",
            "kdDDDkDDDDkDDDdk",
            "kdDDkDDDDDDkDDdk",
            "kdDkDDDDDDDDkDdk",
            "kddddddddddddddk",
            "kdDDkDDDDDDkDDdk",
            "kdDDDkDDDDkDDDdk",
            "kdDDDDkDDkDDDDdk",
            "kdDDssDDDDssDDdk",
            "kddddddddddddddk",
            "kkkkkkkkkkkkkkkk",
        )
    )

    val tilePlatform = build(
        listOf(
            "CCCCCCCCCCCCCCCC",
            "SSSSSSSSSSSSSSSS",
            "kddkddkddkddkddk",
            "ksskkkksskkkkssk",
            "kkkkkkkkkkkkkkkk",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
        )
    )

    val tileBoost0 = build(
        listOf(
            "YYCCYYCCYYCCYYCC",
            "kCCkkCCkkCCkkCCk",
            "kOOCCkOOCCkOOCCk",
            "kddOOCCddOOCCddk",
            "kOOCCkOOCCkOOCCk",
            "kCCkkCCkkCCkkCCk",
            "dddddddddddddddd",
            "kDDDDdDDDDdDDDDk",
            "kddddddddddddddk",
            "kDDDDDDDDDDDDDDk",
            "kddddddddddddddk",
            "kDDDDdDDDDdDDDDk",
            "kddddddddddddddk",
            "kDDDDDDDDDDDDDDk",
            "kddddddddddddddk",
            "kkkkkkkkkkkkkkkk",
        )
    )

    val tileBoost1 = build(
        listOf(
            "CCYYCCYYCCYYCCYY",
            "kkCCkkCCkkCCkkCC",
            "CCkOOCCkOOCCkOOk",
            "OOCCddOOCCddOOCk",
            "CCkOOCCkOOCCkOOk",
            "kkCCkkCCkkCCkkCC",
            "dddddddddddddddd",
            "kDDDDdDDDDdDDDDk",
            "kddddddddddddddk",
            "kDDDDDDDDDDDDDDk",
            "kddddddddddddddk",
            "kDDDDdDDDDdDDDDk",
            "kddddddddddddddk",
            "kDDDDDDDDDDDDDDk",
            "kddddddddddddddk",
            "kkkkkkkkkkkkkkkk",
        )
    )

    val tileCrumble = build(
        listOf(
            "OOOOOOOOOOOOOOOO",
            "SSSSSSSSSSSSSSSS",
            "kddkDDDDkddkDDdk",
            "kddDkDDkDddkDddk",
            "kkkkDDDDkkkkDDDk",
            "....kkkkkkkk....",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
        )
    )

    val tilePlasma0 = build(
        listOf(
            "..CC....cc....CC",
            "cCCCCccCCCCccCCC",
            "CCCCCCCCCCCCCCCC",
            "CCBBCCcCCcCCBBCC",
            "BBBBCCCCCCCCBBBB",
            "BBbBBBCCCCBBBbBB",
            "BBBBBBBBBBBBBBBB",
            "bBBBBbBBBBbBBBBb",
            "BBBBBBBBBBBBBBBB",
            "BBbBBBBBBbBBBBBB",
            "BBBBBBBBBBBBBBBB",
            "bBBBBBBBBBBBBBBb",
            "BBBBBBBBBBBBBBBB",
            "BBBBbBBBBBBBBbBB",
            "BBBBBBBBBBBBBBBB",
            "bBBBBBBBBBBBBBBb",
        )
    )

    val tilePlasma1 = build(
        listOf(
            "cc....CC....cc..",
            "CCCccCCCCccCCCCc",
            "CCCCCCCCCCCCCCCC",
            "CCcCCBBCCBBCCcCC",
            "CCCCBBBBBBBBCCCC",
            "BBCCCCBBBBCCCCBB",
            "BBBBBBBBBBBBBBBB",
            "BBBBbBBBBbBBBBbB",
            "BBBBBBBBBBBBBBBB",
            "bBBBBBBbBBBBBBbB",
            "BBBBBBBBBBBBBBBB",
            "BBBBbBBBBBBBBbBB",
            "BBBBBBBBBBBBBBBB",
            "bBBBBBBBBBBBBBBb",
            "BBBBBBBBBBBBBBBB",
            "BBBBbBBBBBBBBbBB",
        )
    )

    // Pre-flipped sprite pairs
    val mechIdle = listOf(SpritePair(mechIdle0Right), SpritePair(mechIdle1Right))
    val mechRun = listOf(
        SpritePair(mechRun0Right),
        SpritePair(mechRun1Right),
        SpritePair(mechRun2Right),
        SpritePair(mechRun3Right),
    )
    val mechJump = SpritePair(mechJumpRight)
    val mechFall = SpritePair(mechFallRight)
    val transFrames = listOf(
        SpritePair(trans0Right),
        SpritePair(trans1Right),
        SpritePair(trans2Right),
    )
    val vehDrive = listOf(SpritePair(veh0Right), SpritePair(veh1Right))
    val vehBoost = SpritePair(vehBoostRight)

    val droneFrames = listOf(SpritePair(drone0Right), SpritePair(drone1Right))
    val trooperFrames = listOf(SpritePair(trooper0Right), SpritePair(trooper1Right))
    val turretSprite = SpritePair(turretRight)
    val seekerSprite = SpritePair(seekerRight)

    val bossIdle = listOf(SpritePair(bossIdle0Right), SpritePair(bossIdle1Right))
    val bossCharge = SpritePair(bossChargeRight)

    private val dstRect = Rect()
    private val srcRect = Rect()

    fun drawSprite(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Int,
        y: Int,
        scale: Int = 1,
    ) {
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        dstRect.set(x, y, x + bitmap.width * scale, y + bitmap.height * scale)
        canvas.drawBitmap(bitmap, srcRect, dstRect, nearestPaint)
    }

    // ==================== 3x5 PIXEL FONT FOR FLOATING POPUPS ====================
    private val font3x5: Map<Char, IntArray> = mapOf(
        '0' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        '3' to intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '6' to intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        '7' to intArrayOf(0b111, 0b001, 0b001, 0b010, 0b010),
        '8' to intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        '9' to intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111),
        '+' to intArrayOf(0b000, 0b010, 0b111, 0b010, 0b000),
        '-' to intArrayOf(0b000, 0b000, 0b111, 0b000, 0b000),
        '!' to intArrayOf(0b010, 0b010, 0b010, 0b000, 0b010),
        'A' to intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101),
        'B' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b110),
        'C' to intArrayOf(0b111, 0b100, 0b100, 0b100, 0b111),
        'D' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b110),
        'E' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b111),
        'F' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b100),
        'G' to intArrayOf(0b111, 0b100, 0b101, 0b101, 0b111),
        'H' to intArrayOf(0b101, 0b101, 0b111, 0b101, 0b101),
        'I' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b111),
        'K' to intArrayOf(0b101, 0b101, 0b110, 0b101, 0b101),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'M' to intArrayOf(0b101, 0b111, 0b111, 0b101, 0b101),
        'N' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b101),
        'O' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        'P' to intArrayOf(0b111, 0b101, 0b111, 0b100, 0b100),
        'R' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b101),
        'S' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        'T' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b010),
        'U' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b111),
        'V' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b010),
        'X' to intArrayOf(0b101, 0b101, 0b010, 0b101, 0b101),
        ' ' to intArrayOf(0b000, 0b000, 0b000, 0b000, 0b000),
    )

    private val textPaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    fun drawPixelText(
        canvas: Canvas,
        text: String,
        x: Int,
        y: Int,
        color: Int,
        shadowColor: Int = Color.rgb(9, 12, 21),
    ) {
        val upper = text.uppercase()
        var cursorX = x
        // Shadow pass first
        textPaint.color = shadowColor
        for (ch in upper) {
            val glyph = font3x5[ch]
            if (glyph != null) {
                for (row in 0 until 5) {
                    val bits = glyph[row]
                    for (col in 0 until 3) {
                        if ((bits shr (2 - col)) and 1 == 1) {
                            canvas.drawRect(
                                (cursorX + col + 1).toFloat(),
                                (y + row + 1).toFloat(),
                                (cursorX + col + 2).toFloat(),
                                (y + row + 2).toFloat(),
                                textPaint,
                            )
                        }
                    }
                }
            }
            cursorX += 4
        }
        // Foreground pass
        cursorX = x
        textPaint.color = color
        for (ch in upper) {
            val glyph = font3x5[ch]
            if (glyph != null) {
                for (row in 0 until 5) {
                    val bits = glyph[row]
                    for (col in 0 until 3) {
                        if ((bits shr (2 - col)) and 1 == 1) {
                            canvas.drawRect(
                                (cursorX + col).toFloat(),
                                (y + row).toFloat(),
                                (cursorX + col + 1).toFloat(),
                                (y + row + 1).toFloat(),
                                textPaint,
                            )
                        }
                    }
                }
            }
            cursorX += 4
        }
    }
}
