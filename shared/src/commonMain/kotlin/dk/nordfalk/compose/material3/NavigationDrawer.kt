/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// FORKIGO de androidx.compose.material3.NavigationDrawer (Compose Material3
// 1.13.0-alpha01) en la pakon dk.nordfalk.compose.material3 — kun la ORIGINAJ
// nomoj, por ke la diferenco kontraŭ la originalo restu kiel eble plej malgranda.
//
// Diferencoj de la originalo (ĉiuj ceteraj linioj estas kiel eble plej laŭvortaj;
// ĉiu diferenco estas markita per "FORK-DIFF"):
//   1. `DrawerState.anchoredDraggableState` estas PUBLIKA (tie `internal`) —
//      la sola API-diferenco. Ĝi ebligas al la tro-rula gesto sur la hejmaj
//      vicoj (ui/TirkestaTiro.kt) movi la tirkeston kun la fingro.
//   2. MotionSchemeKeyTokens, ScrimTokens, NavigationDrawerTokens kaj la internaj
//      `Strings`-rimedoj de material3 ne estas atingeblaj: la animacioj uzas
//      `tween(256)` (la implicita `AnchoredDraggableDefaultAnimationSpec`), la
//      tekstoj (paneTitle, skermo-priskribo) estas fiksitaj esperantaj kaj la
//      tirkest-pecoj de `NavigationDrawerTokens` venas tra la publikaj
//      `DrawerDefaults` (samaj valoroj).
//   3. La prognoza-reena maŝino (DrawerPredictiveBackState/Handler, interna)
//      mankas; reen fermas la malfermitan tirkeston per NavigationBackHandler
//      (nav3), en ModalDrawerSheet — sama loko kiel la originalo.
//   4. La internaj `fastMap`/`fastForEach`/`fastMaxOfOrNull` estas anstataŭigitaj
//      per `map`/`forEach`/`maxOfOrNull`.
//   5. Ne-kopiitaj partoj de la originala dosiero (ne uzataj de ĉi tiu apo):
//      Dismissible/Permanent-tirkesto, NavigationDrawerItem kaj DrawerDefaults
//      (tiuj restas de material3 mem).

package dk.nordfalk.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dk.nordfalk.compose.material3.internal.FloatProducer
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * State of the [ModalNavigationDrawer] and [DismissibleNavigationDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@Stable
public class DrawerState(
    initialValue: DrawerValue,
    internal val confirmStateChange: (DrawerValue) -> Boolean = { true },
) {

    internal var anchoredDraggableMotionSpec: FiniteAnimationSpec<Float> =
        AnchoredDraggableDefaultAnimationSpec

    // FORK-DIFF: `public` (origine `internal`) — la sola API-diferenco de ĉi tiu
    // forkigo. Ebligas al la tro-rula gesto (ui/TirkestaTiro.kt) puŝi deltojn
    // rekte en la staton (dispatchRawDelta) kaj solvi ĝin (settle).
    @Suppress("Deprecation")
    public val anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            snapAnimationSpec = anchoredDraggableMotionSpec,
            decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
            confirmValueChange = confirmStateChange,
            positionalThreshold = { distance: Float -> distance * DrawerPositionalThreshold },
            velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
        )

    /** Whether the drawer is open. */
    public val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /** Whether the drawer is closed. */
    public val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer currently
     * in. If a swipe or an animation is in progress, this corresponds the state drawer was in
     * before the swipe or animation started.
     */
    public val currentValue: DrawerValue
        get() {
            return anchoredDraggableState.settledValue
        }

    /** Whether the state is currently animating. */
    public val isAnimationRunning: Boolean
        get() {
            return anchoredDraggableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is interrupted
     *
     * @return the reason the open animation ended
     */
    public suspend fun open(): Unit =
        animateTo(targetValue = DrawerValue.Open, animationSpec = openDrawerMotionSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is interrupted
     *
     * @return the reason the close animation ended
     */
    public suspend fun close(): Unit =
        animateTo(targetValue = DrawerValue.Closed, animationSpec = closeDrawerMotionSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @Deprecated(
        message =
            "This method has been replaced by the open and close methods. The animation " +
                "spec is now an implementation detail of ModalDrawer."
    )
    public suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        animateTo(targetValue = targetValue, animationSpec = anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    public suspend fun snapTo(targetValue: DrawerValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    public val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    @Deprecated(
        message =
            "Please access the offset through currentOffset, which returns the value " +
                "directly instead of wrapping it in a state object.",
        replaceWith = ReplaceWith("currentOffset"),
    )
    public val offset: State<Float> =
        object : State<Float> {
            override val value: Float
                get() = anchoredDraggableState.offset
        }

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    public val currentOffset: Float
        get() = anchoredDraggableState.offset

    internal var density: Density? by mutableStateOf(null)

    // FORK-DIFF: `snap()` (tuj) kiel en la originalo; ModalNavigationDrawer
    // anstataŭigas ilin per AnchoredDraggableDefaultAnimationSpec ĉar
    // MotionSchemeKeyTokens (tween-bazigita) estas internal de material3.
    internal var openDrawerMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var closeDrawerMotionSpec: FiniteAnimationSpec<Float> = snap()

    private fun requireDensity() =
        requireNotNull(density) {
            "The density on DrawerState ($this) was not set. Did you use DrawerState" +
                " with the ModalNavigationDrawer or DismissibleNavigationDrawer composables?"
        }

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    private suspend fun animateTo(
        targetValue: DrawerValue,
        animationSpec: AnimationSpec<Float>,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }

    public companion object {
        /** The default [Saver] implementation for [DrawerState]. */
        public fun Saver(
            confirmStateChange: (DrawerValue) -> Boolean
        ): Saver<DrawerState, DrawerValue> =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) },
            )
    }
}

/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
public fun rememberDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true },
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}

/**
 * [Material Design navigation drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim. They
 * are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 */
@Composable
public fun ModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // FORK-DIFF: "Naviga menuo" (origine getString(Strings.NavigationMenu))
    val navigationMenu = "Naviga menuo"
    val density = LocalDensity.current
    var anchorsInitialized by remember { mutableStateOf(false) }
    var minValue by remember(density) { mutableFloatStateOf(0f) }
    val maxValue = 0f
    val focusRequester = remember { FocusRequester() }

    // FORK-DIFF: MotionSchemeKeyTokens.DefaultSpatial/FastEffects (internal de
    // material3) → AnchoredDraggableDefaultAnimationSpec (la MotionScheme-fallback)
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> = AnchoredDraggableDefaultAnimationSpec
    val openMotion: FiniteAnimationSpec<Float> = AnchoredDraggableDefaultAnimationSpec
    val closeMotion: FiniteAnimationSpec<Float> = AnchoredDraggableDefaultAnimationSpec

    SideEffect {
        drawerState.density = density
        drawerState.openDrawerMotionSpec = openMotion
        drawerState.closeDrawerMotionSpec = closeMotion
        drawerState.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            // Keyboard focus should go to first element of the drawer when it opens
            focusRequester.requestFocus()
        }
    }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = drawerState.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                reverseDirection = isRtl,
            )
    ) {
        Box { content() }
        val onDismissRequest = {
            if (gesturesEnabled && drawerState.confirmStateChange(DrawerValue.Closed)) {
                scope.launch { drawerState.close() }
            }
        }
        Scrim(
            // FORK-DIFF: "Fermu la navigan menuon" (origine getString(Strings.CloseDrawer))
            contentDescription = "Fermu la navigan menuon",
            onClick = if (drawerState.isOpen) onDismissRequest else null,
            alpha = { calculateFraction(minValue, maxValue, drawerState.requireOffset()) },
            color = scrimColor,
        )
        Layout(
            content = drawerContent,
            modifier =
                Modifier.offset {
                        drawerState.currentOffset.let { offset ->
                            val offsetX =
                                when {
                                    !offset.isNaN() -> offset.roundToInt()
                                    // If offset is NaN, set offset based on open/closed state
                                    drawerState.isOpen -> 0
                                    else -> -DrawerDefaults.MaximumDrawerWidth.roundToPx()
                                }
                            IntOffset(offsetX, 0)
                        }
                    }
                    .semantics {
                        paneTitle = navigationMenu
                        if (drawerState.isOpen) {
                            dismiss {
                                if (drawerState.confirmStateChange(DrawerValue.Closed)) {
                                    scope.launch { drawerState.close() }
                                }
                                true
                            }
                        }
                    }
                    .onKeyEvent {
                        // Drawer should close via escape key.
                        if (
                            drawerState.isOpen &&
                                it.type == KeyEventType.KeyUp &&
                                it.key == Key.Escape
                        ) {
                            scope.launch { drawerState.close() }
                            return@onKeyEvent true
                        }
                        return@onKeyEvent false
                    }
                    .focusRequester(focusRequester),
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            // FORK-DIFF: fastMap (internal de material3) → map
            val placeables = measurables.map { it.measure(looseConstraints) }
            val width = placeables.maxOfOrNull { it.width } ?: 0
            val height = placeables.maxOfOrNull { it.height } ?: 0

            layout(width, height) {
                val currentClosedAnchor =
                    drawerState.anchoredDraggableState.anchors.positionOf(DrawerValue.Closed)
                val calculatedClosedAnchor = -width.toFloat()

                if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
                    if (!anchorsInitialized) {
                        anchorsInitialized = true
                    }
                    minValue = calculatedClosedAnchor
                    drawerState.anchoredDraggableState.updateAnchors(
                        DraggableAnchors {
                            DrawerValue.Closed at minValue
                            DrawerValue.Open at maxValue
                        }
                    )
                }
                val isDrawerVisible =
                    calculateFraction(minValue, maxValue, drawerState.requireOffset()) != 0f
                if (isDrawerVisible) {
                    // Only place the drawer when it's visible so that keyboard focus doesn't
                    // navigate to an offscreen element.
                    // FORK-DIFF: fastForEach (internal de material3) → forEach
                    placeables.forEach { it.placeRelative(0, 0) }
                }
            }
        }
    }
}

/**
 * Content inside of a modal navigation drawer.
 *
 * This version of [ModalDrawerSheet] does not handle back by default. For automatic
 * back handling, use the [ModalDrawerSheet] that accepts `drawerState` as a param.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a modal navigation drawer
 */
@Composable
public fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerContainerColor: Color = DrawerDefaults.modalContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerSheet(
        windowInsets = windowInsets,
        modifier = modifier,
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

/**
 * Content inside of a modal navigation drawer.
 *
 * This version of [ModalDrawerSheet] requires a [drawerState] to be provided and will handle
 * back by default for all Android versions.
 *
 * @param drawerState state of the drawer
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a modal navigation drawer
 */
@Composable
public fun ModalDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerContainerColor: Color = DrawerDefaults.modalContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    // FORK-DIFF: la prognoza-reena traktado de material3 (DrawerPredictiveBackHandler,
    // internal) anstataŭigita per NavigationBackHandler (nav3) — reen fermas la
    // malfermitan tirkeston; sama loko kiel en la originalo.
    val backStato = rememberNavigationEventState(NavigationEventInfo.None)
    val scope = rememberCoroutineScope()
    NavigationBackHandler(
        state = backStato,
        isBackEnabled = drawerState.isOpen,
        onBackCompleted = { scope.launch { drawerState.close() } },
    )
    DrawerSheet(
        windowInsets = windowInsets,
        modifier = modifier,
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        drawerOffset = { drawerState.anchoredDraggableState.offset },
        content = content,
    )
}

// FORK-DIFF: sen la drawerPredictiveBackState-parametro de la originalo
// (la prognoza-reena maŝino ne estas kopiita).
@Composable
internal fun DrawerSheet(
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.PermanentDrawerElevation,
    drawerOffset: FloatProducer = FloatProducer { 0F },
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val maxWidth = DrawerDefaults.MaximumDrawerWidth
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(
        modifier =
            modifier
                .sizeIn(minWidth = MinimumDrawerWidth, maxWidth = maxWidth)
                // Scale up the Surface horizontally in case the drawer offset it greater than zero.
                // This is done to avoid showing a gap when the drawer opens and bounces when it's
                // applied with a bouncy motion. Note that the content inside the Surface is scaled
                // back down to maintain its aspect ratio (see below).
                .horizontalScaleUp(
                    drawerOffset = drawerOffset,
                    drawerWidth = maxWidthPx,
                    isRtl = isRtl,
                )
                .fillMaxHeight(),
        shape = drawerShape,
        color = drawerContainerColor,
        contentColor = drawerContentColor,
        tonalElevation = drawerTonalElevation,
    ) {
        Column(
            Modifier.sizeIn(minWidth = MinimumDrawerWidth, maxWidth = maxWidth)
                // Scale the content down in case the drawer offset is greater than one. The
                // wrapping Surface is scaled up, so this is done to maintain the content's aspect
                // ratio.
                .horizontalScaleDown(
                    drawerOffset = drawerOffset,
                    drawerWidth = maxWidthPx,
                    isRtl = isRtl,
                )
                .windowInsetsPadding(windowInsets),
            content = content,
        )
    }
}

/**
 * A [Modifier] that scales up the drawing layer on the X axis in case the [drawerOffset] is greater
 * than zero. The scaling will ensure that there is no visible gap between the drawer and the edge
 * of the screen in case the drawer bounces when it opens due to a more expressive motion setting.
 *
 * A [horizontalScaleDown] should be applied to the content of the drawer to maintain the content
 * aspect ratio as the container scales up.
 *
 * @see horizontalScaleDown
 */
private fun Modifier.horizontalScaleUp(
    drawerOffset: FloatProducer,
    drawerWidth: Float,
    isRtl: Boolean,
) = graphicsLayer {
    val offset = drawerOffset()
    scaleX = if (offset > 0f) 1f + offset / drawerWidth else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0.5f)
}

/**
 * A [Modifier] that scales down the drawing layer on the X axis in case the [drawerOffset] is
 * greater than zero. This modifier should be applied to the content inside a component that was
 * scaled up with a [horizontalScaleUp] modifier. It will ensure that the content maintains its
 * aspect ratio as the container scales up.
 *
 * @see horizontalScaleUp
 */
private fun Modifier.horizontalScaleDown(
    drawerOffset: FloatProducer,
    drawerWidth: Float,
    isRtl: Boolean,
) = graphicsLayer {
    val offset = drawerOffset()
    scaleX = if (offset > 0f) 1 / (1f + offset / drawerWidth) else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0f)
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

private val DrawerPositionalThreshold = 0.5f
private val DrawerVelocityThreshold
    get() = 400.dp
private val MinimumDrawerWidth
    get() = 240.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnchoredDraggableDefaultAnimationSpec = TweenSpec<Float>(durationMillis = 256)
