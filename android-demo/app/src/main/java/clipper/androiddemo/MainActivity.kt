package clipper.androiddemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import clipper.*
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView

class MainActivity : AppCompatActivity() {
    val model = ClipperDemo().apply {
        generate()
        execute()
    }
    //var bitmapBuffer = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    //var intBuffer = intArrayOf()

    val clipTypes = ClipType.values().map { "$it" } + "None"
    val polyFillTypes = listOf(PolyFillType.EvenOdd, PolyFillType.NonZero)

    val subjectStrokeColor = Color.argb(196, 0xC3, 0xC9, 0xCF)
    val subjectFillColor = Color.argb(127, 0xDD, 0xDD, 0xF0)
    val clipStrokeColor = Color.argb(196, 0xF9, 0xBE, 0xA6)
    val clipFillColor = Color.argb(127, 0xFF, 0xE0, 0xE0)
    val solutionStrokeColor = Color.argb(0xFF, 0, 0x33, 0)
    val solutionFillColor = Color.argb(127, 0x66, 0xEF, 0x7F)
    lateinit var drawView: DrawView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootView = verticalLayout {
            spinner {
                adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, clipTypes)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>, p1: View?, pos: Int, id: Long) {
                        model.clipType = clipTypes[id.toInt()].let {
                            if (it == "None") null
                            else ClipType.valueOf(it)
                        }
                        model.execute()
                        drawView.invalidate()
                    }
                }
            }
            spinner {
                adapter = ArrayAdapter<PolyFillType>(context, android.R.layout.simple_spinner_item, polyFillTypes)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                        model.polyFillType = polyFillTypes[id.toInt()]
                        model.execute()
                        drawView.invalidate()
                    }
                }
            }
            linearLayout {
                textView("Vertex Count:")
                ankoView({ MySeekBar(it) }, 0) {
                    progressToString = Int::toString
                    stringToProgress = String::toIntOrNull
                    inputType = InputType.TYPE_CLASS_NUMBER
                    min = 3
                    max = 400
                    value = 50
                    onValueChangedListener = { newVal ->
                        model.vertexCount = newVal
                        model.generate()
                        model.execute()
                        drawView.invalidate()
                    }
                }.lparams(matchParent, wrapContent)
            }
            linearLayout {
                textView("Offset:")
                ankoView({ MySeekBar(it) }, 0) {
                    progressToString = { value -> "%.1f".format(value.toDouble()) }
                    stringToProgress = { string -> string.toDoubleOrNull()?.toInt() }
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    min = -10
                    max = 10
                    value = 0
                    onValueChangedListener = { newVal ->
                        model.offset = newVal.toDouble()
                        model.execute()
                        drawView.invalidate()
                    }
                }.lparams(matchParent, wrapContent)
            }
            ankoView({ DrawView(it) }, 0) {
                drawView = this
                onDrawListener = { canvas ->
                    canvas.drawRGB(0xff, 0xff, 0xff)
                    canvas.drawClipperPaths(model.subject, model.scale.toFloat(), subjectFillColor, subjectStrokeColor, model.polyFillType)
                    canvas.drawClipperPaths(model.clip, model.scale.toFloat(), clipFillColor, clipStrokeColor, model.polyFillType)
                    //It really shouldn't matter what PolyFillType is used for solution
                    //polygons because none of the solution polygons overlap.
                    //However, PolyFillType.NonZero will show any orientation errors where
                    //holes will be stroked (outlined) correctly but filled incorrectly  ...
                    canvas.drawClipperPaths(model.solution, model.scale.toFloat(), solutionFillColor, solutionStrokeColor, PolyFillType.NonZero)//, bitmapBuffer = bitmapBuffer, intBuffer = intBuffer)
                }
                onSizeChangedListener = { w, h, oldw, oldh ->
                    model.apply {
                        width = w
                        height = h
                        generate()
                        execute()
                    }
                    /*if (bitmapBuffer.allocationByteCount > w * h * 4) {
                        bitmapBuffer.reconfigure(w, h, Bitmap.Config.ARGB_8888)
                    } else {
                        bitmapBuffer.recycle()
                        bitmapBuffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        intBuffer = IntArray(w * h)
                    }*/
                }
            }
        }
        setContentView(rootView)


    }
}

fun Canvas.drawClipperPaths(paths: Paths, scale: Float, fillColor: Int, strokeColor: Int, fillRuleType: PolyFillType) {
    val fillRuleType = when (fillRuleType) {
        PolyFillType.EvenOdd -> android.graphics.Path.FillType.EVEN_ODD
        PolyFillType.NonZero -> android.graphics.Path.FillType.WINDING
        else -> throw IllegalStateException()
    }
    val androidPath = android.graphics.Path().apply {
        fillType = fillRuleType
        paths.forEach { path ->
            if (path.isNotEmpty()) {
                val iterator = path.iterator()
                iterator.next().let { (x, y) -> moveTo(x / scale, y / scale) }
                iterator.forEach { (x, y) -> lineTo(x / scale, y / scale) }
                close()
            }
        }
    }
    val paint = Paint()
    drawPath(androidPath, paint.apply {
        color = fillColor
        style = Paint.Style.FILL
    })
    drawPath(androidPath, paint.apply {
        color = strokeColor
        style = Paint.Style.STROKE
    })
}

fun Canvas.drawClipperPolyTree(paths: PolyTree, scale: Float, fillColor: Int, strokeColor: Int, fillRuleType: PolyFillType, bitmapBuffer: Bitmap, intBuffer: IntArray) {
    val fillRuleType = when (fillRuleType) {
        PolyFillType.EvenOdd -> android.graphics.Path.FillType.EVEN_ODD
        PolyFillType.NonZero -> android.graphics.Path.FillType.WINDING
        else -> throw IllegalStateException()
    }

    bitmapBuffer.eraseColor(Color.TRANSPARENT)
    val tmpCanvas = Canvas(bitmapBuffer)
    val paint = Paint()

    fun fillPaths(nodes: List<PolyNode>) {
        if (nodes.isNotEmpty()) {
            val androidPath = android.graphics.Path().apply { fillType = fillRuleType }
            val isHole = nodes.first().isHole
            nodes.forEach { node ->
                if (node.contour.isNotEmpty()) {
                    androidPath.apply {
                        val iterator = node.contour.iterator()
                        iterator.next().let { (x, y) -> moveTo(x / scale, y / scale) }
                        iterator.forEach { (x, y) -> lineTo(x / scale, y / scale) }
                        close()
                    }
                }
            }
            tmpCanvas.drawPath(androidPath, paint.apply {
                color = if (isHole) Color.WHITE else fillColor
                style = Paint.Style.FILL
            })
            tmpCanvas.drawPath(androidPath, paint.apply {
                color = strokeColor
                style = Paint.Style.STROKE
            })
            if (isHole) {
                bitmapBuffer.getPixels(intBuffer, 0, bitmapBuffer.width, 0, 0, bitmapBuffer.width, bitmapBuffer.height)
                intBuffer.forEachIndexed { i, pixel ->
                    if (pixel == Color.WHITE) {
                        intBuffer[i] = Color.TRANSPARENT
                    }
                }
                bitmapBuffer.setPixels(intBuffer, 0, bitmapBuffer.width, 0, 0, bitmapBuffer.width, bitmapBuffer.height)
            }
            fillPaths(nodes.flatMap { it.childs })
        }
    }

    fillPaths(paths.childs)
    drawBitmap(bitmapBuffer, 0f, 0f, null)
}

class DrawView(context: Context) : View(context) {
    var onDrawListener: ((canvas: Canvas) -> Unit)? = null
    var onSizeChangedListener: ((w: Int, h: Int, oldw: Int, oldh: Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        onDrawListener?.invoke(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        onSizeChangedListener?.invoke(w, h, oldw, oldh)
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

class MySeekBar(context: Context) : LinearLayout(context) {
    var progressToString: ((Int) -> String)? = null
    var stringToProgress: ((String) -> Int?)? = null
    var onValueChangedListener: ((newVal: Int) -> Unit)? = null
    var min: Int = 0
        set(value) {
            seekBar.max = seekBar.max - field + value
            field = value
        }
    var max: Int
        get() = seekBar.max + min
        set(value) {
            seekBar.max = value - min
        }
    var value: Int
        get() = seekBar.progress + min
        set(value) {
            seekBar.progress = value - min
        }
    var inputType: Int
        get() = editText.inputType
        set(value) {
            editText.inputType = value
        }
    private lateinit var editText: EditText
    private lateinit var seekBar: SeekBar
    private val onSeekBarChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
            editText.setText(progressToString?.let { it(value) } ?: value.toString())
            onValueChangedListener?.invoke(value)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }
    private val onEditorDone = { textView: TextView, actionId: Int, _: KeyEvent? ->
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
            val oldValue = value
            value = stringToProgress?.let { it(textView.text.toString()) } ?: value
            if (oldValue == value) {
                textView.text = progressToString?.let { it(value) } ?: value.toString()
            }
            true
        } else {
            false
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        editText {
            editText = this
            setOnEditorActionListener(onEditorDone)
            setEms(3)
        }.layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent)
        seekBar {
            seekBar = this
            setOnSeekBarChangeListener(onSeekBarChanged)
        }.layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        baselineAlignedChildIndex = 0
    }
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