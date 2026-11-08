package com.emberr.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val NOISE_TILE_SIZE = 256
private const val NOISE_SEED = 7
private const val GRAIN_SIZE_DP = 0.75f
private const val STRONGEST_TEXTURE_ALPHA = 0.144f
private val NoiseSpeckAlphas = listOf(0.25f, 0.5f, 0.75f, 1f)
private val NoiseSpeckColors = listOf(Color.Black, Color.White)

val LocalPaperTextureStrength = staticCompositionLocalOf { 0 }

fun interface PaperGrainMover {
    fun moveGrainBy(delta: Offset)
}

val LocalPaperGrainMover = staticCompositionLocalOf { PaperGrainMover { } }

enum class PaperTextureArea(val displayName: String) {
    WHOLE_APP("Whole App"),
    DAILY_NOTES("Daily Notes"),
    NOTES("Notes"),
    CANVAS("Canvas")
}

fun paperTextureAreasNamed(savedAreas: String): Set<PaperTextureArea> =
    savedAreas.split(",").mapNotNull { name -> PaperTextureArea.entries.firstOrNull { it.name == name } }.toSet()

fun paperTextureAreasSavedValue(areas: Set<PaperTextureArea>): String = areas.joinToString(",") { it.name }

fun isPaperTextureAreaChecked(areas: Set<PaperTextureArea>, area: PaperTextureArea): Boolean =
    PaperTextureArea.WHOLE_APP in areas || area in areas

fun paperTextureAreasWithToggled(areas: Set<PaperTextureArea>, tappedArea: PaperTextureArea): Set<PaperTextureArea> = when {
    tappedArea == PaperTextureArea.WHOLE_APP && PaperTextureArea.WHOLE_APP in areas -> emptySet()
    tappedArea == PaperTextureArea.WHOLE_APP -> setOf(PaperTextureArea.WHOLE_APP)
    PaperTextureArea.WHOLE_APP in areas -> PaperTextureArea.entries.toSet() - PaperTextureArea.WHOLE_APP - tappedArea
    tappedArea in areas -> areas - tappedArea
    else -> areas + tappedArea
}

fun paperTextureAreasLabel(areas: Set<PaperTextureArea>): String {
    if (PaperTextureArea.WHOLE_APP in areas) return PaperTextureArea.WHOLE_APP.displayName
    val checkedAreas = PaperTextureArea.entries.filter { it in areas }
    return if (checkedAreas.isEmpty()) "None" else checkedAreas.joinToString(", ") { it.displayName }
}

val LocalPaperTextureAreas = staticCompositionLocalOf { setOf(PaperTextureArea.WHOLE_APP) }

private val LocalInsideTexturedNote = staticCompositionLocalOf { false }

val LocalHoldsPaperGrainStill = staticCompositionLocalOf { false }

private class StillGrainRegion(val bounds: Rect, val shape: Shape, val drawsStillGrain: Boolean)

private class StillPaperGrainRegions {
    var boxCoordinates: LayoutCoordinates? = null
    private val regions = mutableStateMapOf<Any, StillGrainRegion>()
    private var cachedCoveredPath: Path? = null
    private var cachedStillGrainPath: Path? = null
    private var cachedOutlineDensity = 0f
    private var cachedOutlineLayoutDirection: LayoutDirection? = null

    fun place(regionKey: Any, coordinates: LayoutCoordinates, shape: Shape, drawsStillGrain: Boolean = true) {
        val box = boxCoordinates ?: return
        if (!box.isAttached || !coordinates.isAttached) return
        if (box.findRootCoordinates() != coordinates.findRootCoordinates()) {
            remove(regionKey)
            return
        }
        val bounds = box.localBoundingBoxOf(coordinates, clipBounds = false)
        val placedRegion = regions[regionKey]
        val isUnchanged = placedRegion != null && placedRegion.bounds == bounds &&
            placedRegion.shape == shape && placedRegion.drawsStillGrain == drawsStillGrain
        if (!isUnchanged) {
            regions[regionKey] = StillGrainRegion(bounds, shape, drawsStillGrain)
            forgetCachedPaths()
        }
    }

    fun remove(regionKey: Any) {
        if (regions.remove(regionKey) != null) forgetCachedPaths()
    }

    fun coveredPath(density: Density, layoutDirection: LayoutDirection): Path? {
        if (regions.isEmpty()) return null
        refreshCachedPaths(density, layoutDirection)
        return cachedCoveredPath
    }

    fun stillGrainPath(density: Density, layoutDirection: LayoutDirection): Path? {
        if (regions.isEmpty()) return null
        refreshCachedPaths(density, layoutDirection)
        return cachedStillGrainPath
    }

    private fun forgetCachedPaths() {
        cachedCoveredPath = null
        cachedStillGrainPath = null
    }

    private fun refreshCachedPaths(density: Density, layoutDirection: LayoutDirection) {
        val isCacheFresh = cachedCoveredPath != null && cachedOutlineDensity == density.density &&
            cachedOutlineLayoutDirection == layoutDirection
        if (isCacheFresh) return
        val coveredPath = Path()
        val stillGrainPath = Path()
        var hasStillGrain = false
        regions.values.forEach { region ->
            val regionOutline = region.shape.createOutline(region.bounds.size, layoutDirection, density)
            val regionPath = Path().apply { addOutline(regionOutline) }
            coveredPath.addPath(regionPath, region.bounds.topLeft)
            if (region.drawsStillGrain) {
                stillGrainPath.addPath(regionPath, region.bounds.topLeft)
                hasStillGrain = true
            }
        }
        cachedCoveredPath = coveredPath
        cachedStillGrainPath = if (hasStillGrain) stillGrainPath else null
        cachedOutlineDensity = density.density
        cachedOutlineLayoutDirection = layoutDirection
    }
}

private val LocalStillPaperGrainRegions = staticCompositionLocalOf<StillPaperGrainRegions?> { null }

@Composable
fun Modifier.stillPaperGrain(shape: Shape, holdsStill: Boolean = LocalHoldsPaperGrainStill.current): Modifier {
    val stillRegions = LocalStillPaperGrainRegions.current
    if (!holdsStill || stillRegions == null) return this
    val regionKey = remember { Any() }
    DisposableEffect(stillRegions, regionKey) {
        onDispose { stillRegions.remove(regionKey) }
    }
    return onGloballyPositioned { coordinates -> stillRegions.place(regionKey, coordinates, shape) }
}

@Composable
private fun paperTextureShownHere(): Boolean =
    PaperTextureArea.WHOLE_APP in LocalPaperTextureAreas.current || LocalInsideTexturedNote.current

private val paperNoiseBrushState = mutableStateOf<ShaderBrush?>(null)
private val paperNoiseTileLock = Mutex()

@Composable
private fun loadedPaperNoiseBrush(): ShaderBrush? {
    val paperNoiseBrush = paperNoiseBrushState.value
    LaunchedEffect(Unit) {
        paperNoiseTileLock.withLock {
            if (paperNoiseBrushState.value != null) return@withLock
            val noiseTile = withContext(Dispatchers.Default) { createPaperNoiseTile() }
            paperNoiseBrushState.value = ShaderBrush(ImageShader(noiseTile, TileMode.Repeated, TileMode.Repeated))
        }
    }
    return paperNoiseBrush
}

private fun createPaperNoiseTile(): ImageBitmap {
    val tile = ImageBitmap(NOISE_TILE_SIZE, NOISE_TILE_SIZE)
    val random = Random(NOISE_SEED)
    val speckLooks = NoiseSpeckColors.flatMap { speckColor ->
        NoiseSpeckAlphas.map { speckAlpha -> speckColor.copy(alpha = speckAlpha) }
    }
    val specksByLook = speckLooks.associateWith { mutableListOf<Offset>() }

    for (x in 0 until NOISE_TILE_SIZE) {
        for (y in 0 until NOISE_TILE_SIZE) {
            val speckLook = speckLooks[random.nextInt(speckLooks.size)]
            specksByLook.getValue(speckLook).add(Offset(x + 0.5f, y + 0.5f))
        }
    }

    val tileCanvas = Canvas(tile)
    specksByLook.forEach { (speckLook, speckPositions) ->
        val speckPaint = Paint().apply {
            color = speckLook
            strokeWidth = 1f
            strokeCap = StrokeCap.Square
            isAntiAlias = false
        }
        tileCanvas.drawPoints(PointMode.Points, speckPositions, speckPaint)
    }
    return tile
}

private fun grainScaleFor(density: Float): Float = (density * GRAIN_SIZE_DP).coerceAtLeast(1f)

private fun DrawScope.drawPaperGrain(
    paperNoiseBrush: ShaderBrush,
    grainScale: Float,
    tileSpan: Float,
    textureAlpha: Float
) {
    translate(left = -tileSpan, top = -tileSpan) {
        scale(grainScale, pivot = Offset.Zero) {
            drawRect(
                brush = paperNoiseBrush,
                size = Size(
                    (size.width + tileSpan * 2) / grainScale,
                    (size.height + tileSpan * 2) / grainScale
                ),
                alpha = textureAlpha
            )
        }
    }
}

private class GrainScrollFollower(
    private val tileSpan: Float,
    private val parentKeptStill: GrainScrollFollower?
) : NestedScrollConnection, PaperGrainMover {
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set
    private var scrollAlreadyFollowedInside = Offset.Zero

    override fun moveGrainBy(delta: Offset) {
        offsetX = (offsetX + delta.x).mod(tileSpan)
        offsetY = (offsetY + delta.y).mod(tileSpan)
    }

    fun skipScrollFollowedInside(delta: Offset) {
        scrollAlreadyFollowedInside += delta
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        moveGrainBy(consumed - scrollAlreadyFollowedInside)
        scrollAlreadyFollowedInside = Offset.Zero
        parentKeptStill?.skipScrollFollowedInside(consumed)
        return Offset.Zero
    }
}

@Composable
fun PaperTextureBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    PaperTextureLayerBox(
        modifier = modifier,
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
        isShown = paperTextureShownHere(),
        content = content
    )
}

@Composable
fun SeparatePaperGrainBox(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val parentRegions = LocalStillPaperGrainRegions.current
    val regionKey = remember { Any() }
    DisposableEffect(parentRegions, regionKey) {
        onDispose { parentRegions?.remove(regionKey) }
    }
    val regionModifier = if (parentRegions == null) {
        Modifier
    } else {
        Modifier.onGloballyPositioned { coordinates ->
            parentRegions.place(regionKey, coordinates, shape, drawsStillGrain = false)
        }
    }
    PaperTextureLayerBox(
        modifier = modifier.then(regionModifier),
        contentAlignment = Alignment.TopStart,
        propagateMinConstraints = false,
        isShown = parentRegions != null,
        keepsParentGrainStill = true,
        content = content
    )
}

@Composable
fun NotePaperTextureBox(
    area: PaperTextureArea,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val paperTextureAreas = LocalPaperTextureAreas.current
    val showsOnThisPageOnly = PaperTextureArea.WHOLE_APP !in paperTextureAreas && area in paperTextureAreas
    val isInsideTexturedNote = LocalInsideTexturedNote.current
    CompositionLocalProvider(LocalInsideTexturedNote provides (showsOnThisPageOnly || isInsideTexturedNote)) {
        PaperTextureLayerBox(
            modifier = modifier,
            contentAlignment = contentAlignment,
            propagateMinConstraints = false,
            isShown = showsOnThisPageOnly && !isInsideTexturedNote,
            content = content
        )
    }
}

@Composable
private fun PaperTextureLayerBox(
    modifier: Modifier,
    contentAlignment: Alignment,
    propagateMinConstraints: Boolean,
    isShown: Boolean,
    keepsParentGrainStill: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val textureStrength = if (isShown) LocalPaperTextureStrength.current.coerceAtMost(FULL_STRENGTH_PERCENT) else 0
    val density = LocalDensity.current.density
    val grainScale = grainScaleFor(density)
    val tileSpan = NOISE_TILE_SIZE * grainScale
    val drawsGrain = textureStrength > 0
    val parentGrainMover = LocalPaperGrainMover.current
    val parentFollowerKeptStill = if (keepsParentGrainStill) parentGrainMover as? GrainScrollFollower else null
    val grainScrollFollower = remember(tileSpan, parentFollowerKeptStill) {
        GrainScrollFollower(tileSpan, parentFollowerKeptStill)
    }
    val stillRegions = remember { StillPaperGrainRegions() }
    val parentStillRegions = LocalStillPaperGrainRegions.current
    val grainTrackingModifier = if (drawsGrain) {
        Modifier
            .nestedScroll(grainScrollFollower)
            .onPlaced { coordinates -> stillRegions.boxCoordinates = coordinates }
    } else {
        Modifier
    }

    Box(
        modifier = modifier.then(grainTrackingModifier),
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints
    ) {
        CompositionLocalProvider(
            LocalPaperGrainMover provides (if (drawsGrain) grainScrollFollower else parentGrainMover),
            LocalStillPaperGrainRegions provides (if (drawsGrain) stillRegions else parentStillRegions)
        ) {
            content()
        }
        val paperNoiseBrush = if (drawsGrain) loadedPaperNoiseBrush() else null
        if (paperNoiseBrush != null) {
            val textureAlpha = textureStrength / FULL_STRENGTH_PERCENT.toFloat() * STRONGEST_TEXTURE_ALPHA
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clipToBounds()
                    .drawWithContent {
                        val coveredPath = stillRegions.coveredPath(this, layoutDirection)
                        if (coveredPath == null) {
                            drawContent()
                        } else {
                            clipPath(coveredPath, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
                        }
                    }
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = grainScrollFollower.offsetX
                            translationY = grainScrollFollower.offsetY
                        }
                        .drawBehind { drawPaperGrain(paperNoiseBrush, grainScale, tileSpan, textureAlpha) }
                )
            }
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val stillPath = stillRegions.stillGrainPath(this, layoutDirection) ?: return@drawBehind
                        clipPath(stillPath) { drawPaperGrain(paperNoiseBrush, grainScale, tileSpan, textureAlpha) }
                    }
            )
        }
    }
}
