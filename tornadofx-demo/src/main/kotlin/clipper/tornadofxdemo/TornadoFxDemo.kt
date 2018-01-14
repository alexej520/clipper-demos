package clipper.tornadofxdemo

import javafx.scene.Group
import tornadofx.*
import clipper.*
import javafx.event.EventHandler
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.FillRule

class TornadoFxDemo : App(MainView::class)

class MainView : View() {
    lateinit var polygonGroup: Group
    val model = ClipperDemo()

    override val root = borderpane {
        title = "Clipper Kotlin Demo"
        left = vbox {
            titledpane("Boolean Op:", collapsible = false) {
                vbox(spacing = 8.0) {
                    togglegroup {
                        radiobutton("Intersect", this) {
                            selectToggle(this)
                            action {
                                model.clipType = ClipType.Intersection
                                drawBitmap()
                            }
                        }
                        radiobutton("Union", this) {
                            action {
                                model.clipType = ClipType.Union
                                drawBitmap()
                            }
                        }
                        radiobutton("Difference", this) {
                            action {
                                model.clipType = ClipType.Difference
                                drawBitmap()
                            }
                        }
                        radiobutton("XOR", this) {
                            action {
                                model.clipType = ClipType.Xor
                                drawBitmap()
                            }
                        }
                        radiobutton("None", this) {
                            action {
                                model.clipType = null
                                drawBitmap()
                            }
                        }
                    }
                }
            }
            titledpane("Options:", collapsible = false) {
                vbox(spacing = 8.0) {
                    togglegroup {
                        radiobutton("EvenOdd", this) {
                            selectToggle(this)
                            action {
                                model.polyFillType = PolyFillType.EvenOdd
                                drawBitmap()
                            }
                        }
                        radiobutton("NonZero", this) {
                            action {
                                model.polyFillType = PolyFillType.NonZero
                                drawBitmap()
                            }
                        }
                    }
                    label("Vertex Count:")
                    spinner(3, 400, 50, 1, true){
                        valueProperty().addListener {_, _, newVal ->
                            model.vertexCount = newVal
                            model.generate()
                            drawBitmap()
                        }
                    }
                    label("Offset:")
                    spinner(-10.0, 10.0, 0.0, 1.0, true) {
                        valueProperty().addListener { _, _, newVal ->
                            model.offset = newVal
                            drawBitmap()
                        }
                    }
                }
            }
            titledpane("Sample", collapsible = false) {
                vbox(spacing = 8.0) {
                    togglegroup {
                        radiobutton("One", this) {
                            selectToggle(this)
                            //TODO
                        }
                        radiobutton("Two", this) {
                            //TODO
                        }
                    }
                }
            }
            button("New Sample") {
                useMaxWidth = true
                action {
                    model.generate()
                    drawBitmap()
                }
            }.vboxConstraints { margin = insets(8.0) }
            button("Save as SVG File") {
                useMaxWidth = true
                //TODO
            }.vboxConstraints { margin = insets(8.0) }
        }
        center = pane {
            prefWidth = 500.0
            layoutBoundsProperty().addListener { _, _, newVal ->
                model.width = newVal.width.toInt()
                model.height = newVal.height.toInt()
                model.generate()
                model.execute()
                drawBitmap()
            }
            group {
                polygonGroup = this
            }
        }
    }

    private fun drawBitmap() {
        model.execute()
        polygonGroup.replaceWith(group {
            polygonGroup = this
            clipperPaths(model.subject, model.scale.toFloat(),
                    fillColor = Color.rgb(0xDD, 0xDD, 0xF0, 127.0 / 255.0),
                    strokeColor = Color.rgb(0xC3, 0xC9, 0xCF, 196.0 / 255.0),
                    fillRuleType = model.polyFillType)
            clipperPaths(model.clip, model.scale.toFloat(),
                    fillColor = Color.rgb(0xFF, 0xE0, 0xE0, 127.0 / 255.0),
                    strokeColor = Color.rgb(0xF9, 0xBE, 0xA6, 196.0 / 255.0),
                    fillRuleType = model.polyFillType)
            //It really shouldn't matter what PolyFillType is used for solution
            //polygons because none of the solution polygons overlap.
            //However, PolyFillType.NonZero will show any orientation errors where
            //holes will be stroked (outlined) correctly but filled incorrectly  ...
            clipperPaths(model.solution, model.scale.toFloat(),
                    fillColor = Color.rgb(0x66, 0xEF, 0x7F, 127.0 / 255.0),
                    strokeColor = Color.rgb(0, 0x33, 0, 1.0),
                    fillRuleType = PolyFillType.NonZero)
        })
    }

    init {
        root.onScroll = EventHandler { event ->
            model.offset += event.deltaY
            model.offset = maxOf(-10.0, minOf(model.offset, 10.0))
        }
    }
}

fun Group.clipperPaths(paths: Paths, scale: Float, fillColor: Paint, strokeColor: Paint, fillRuleType: PolyFillType) {
    val fillRuleType = when (fillRuleType) {
        PolyFillType.EvenOdd -> FillRule.EVEN_ODD
        PolyFillType.NonZero -> FillRule.NON_ZERO
        else -> throw IllegalStateException()
    }
    path {
        fillRule = fillRuleType
        stroke = strokeColor
        fill = fillColor
        paths.forEach { path ->
            if (path.isNotEmpty()) {
                val iterator = path.iterator()
                iterator.next().let { (x, y) -> moveTo(x / scale, y / scale) }
                iterator.forEach { (x, y) -> lineTo(x / scale, y / scale) }
                closepath()
            }
        }
    }
}

class ClipperDemo {
    var width = 400
    var height = 400
    var scale = 100.0
    var offset = 0.0
    var clipType: ClipType? = ClipType.Intersection
    var polyFillType = PolyFillType.EvenOdd
    val subject = Paths()
    val clip = Paths()
    val solution = Paths()
    fun generate() {
        generateRandomPolygon(vertexCount, width, height, scale, subject)
        generateRandomPolygon(vertexCount, width, height, scale, clip)
    }

    fun execute() {
        clipType?.let { clipType ->
            Clipper().apply {
                addPaths(subject, PolyType.Subject, true)
                addPaths(clip, PolyType.Clip, true)
            }.execute(clipType, solution, polyFillType, polyFillType)
        } ?: solution.clear()
        if (offset != 0.0) {
            ClipperOffset().apply {
                addPaths(solution, JoinType.Round, EndType.ClosedPolygon)
            }.execute(solution, offset * scale)
        }
    }

    var vertexCount: Int = 50
}

fun generateRandomPolygon(count: Int, width: Int, height: Int, scale: Double, paths: Paths) {
    val q = 10
    val l = 10
    val t = 10
    val r = (width - 20) / q * q
    val b = (height - 10) / q * q

    paths.clear()
    paths += Path(count) { generateRandomPoint(l, t, r, b, scale) }
}

fun generateRandomPoint(l: Int, t: Int, r: Int, b: Int, scale: Double): IntPoint {
    val q = 10
    return IntPoint(
            x = (Math.random() * r).toLong() / q * q + l + 10,
            y = (Math.random() * b).toLong() / q * q + t + 10).apply {
        x = (x * scale).toLong()
        y = (y * scale).toLong()
    }
}