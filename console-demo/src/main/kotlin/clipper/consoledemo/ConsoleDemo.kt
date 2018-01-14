package clipper.consoledemo

import clipper.*
import java.awt.Desktop
import java.io.*
import java.util.*

//---------------------------------------------------------------------------
// SVGBuilder class
// a very simple class that creates an SVG image file
//---------------------------------------------------------------------------

typealias PolyInfoList = MutableList<SVGBuilder.PolyInfo>

class SVGBuilder {
    companion object {
        init {

        }
        private val SVG_XML_START: Array<String> = arrayOf(
                """<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.0//EN"
"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd">

<svg width="""",
                "\" height=\"",
                "\" viewBox=\"0 0 ",
                "\" version=\"1.0\" xmlns=\"http://www.w3.org/2000/svg\">\n\n"
        )
        private val POLY_END: Array<String> = arrayOf(
                "\"\n style=\"fill:",
                "; fill-opacity:",
                "; fill-rule:",
                "; stroke:",
                "; stroke-opacity:",
                "; stroke-width:",
                ";\"/>\n\n"
        )

        fun colorToHtml(clr: Long) =
                "#%06x".format(clr and 0xFFFFFF)

        fun getAlphaAsFrac(clr: Long) =
                (clr shr 24) / 255.0
    }

    class StyleInfo(
            val pft: PolyFillType,
            val brushClr: Long = 0xFFFFFFCC,
            val penClr: Long = 0xFF000000,
            val penWidth: Double = 0.8,
            val showCoords: Boolean = false
    )

    class PolyInfo(
            val paths: Paths,
            val si: StyleInfo
    )

    private val polyInfos: PolyInfoList = mutableListOf()

    fun addPaths(poly: Paths, style: StyleInfo) {
        if (poly.isEmpty()) return
        polyInfos += PolyInfo(poly, style)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun FileWriter.write(double: Double) = write("%.2f".format( Locale.ROOT, double))

    fun saveToFile(filename: String, scale: Double = 1.0, margin: Int = 10): Boolean {
        //calculate the bounding rect ...
        val scale = if (scale == 0.0) 1.0 else scale
        val margin = if (margin < 0) 0 else margin
        var i = 0
        var j = 0
        while (i < polyInfos.size) {
            j = 0
            val polyInfo = polyInfos[i]
            while (j < polyInfo.paths.size && polyInfo.paths[j].isEmpty()) j++
            if (j < polyInfo.paths.size) break
            i++
        }
        if (i == polyInfos.size) return false

        val first = polyInfos[i].paths[j][0]
        val rec = IntRect(left = first.x, right = first.x, top = first.y, bottom = first.y)
        while (i < polyInfos.size) {
            polyInfos[i].paths.forEach { path ->
                path.forEach { (x, y) ->
                    if (x < rec.left) rec.left = x
                    else if (x > rec.right) rec.right = x
                    if (y < rec.top) rec.top = y
                    else if (y > rec.bottom) rec.bottom = y
                }
            }
            i++
        }

        rec.left = (rec.left * scale).toLong()
        rec.top = (rec.top * scale).toLong()
        rec.right = (rec.right * scale).toLong()
        rec.bottom = (rec.bottom * scale).toLong()
        val offsetX = -rec.left + margin
        val offsetY = -rec.top + margin

        try {
            FileWriter(filename)
        } catch (e: IOException) {
            return false
        }.use { file ->
            with(file) {
                write(SVG_XML_START[0])
                write("${rec.right - rec.left + margin * 2}px")
                write(SVG_XML_START[1])
                write("${rec.bottom - rec.top + margin * 2}px")
                write(SVG_XML_START[2])
                write("${rec.right - rec.left + margin * 2} ")
                write("${rec.bottom - rec.top + margin * 2}")
                write(SVG_XML_START[3])

                polyInfos.forEach { polyInfo ->
                    write(" <path d=\"")
                    for (path in polyInfo.paths) {
                        val pathIter = path.iterator()
                        val firstPt = pathIter.next()
                        if (path.size < 3) continue
                        write(" M ")
                        write(firstPt.x * scale + offsetX)
                        write(" ")
                        write(firstPt.y * scale + offsetY)
                        pathIter.forEach { (x, y) ->
                            write(" L ")
                            write(x * scale + offsetX)
                            write(" ")
                            write(y * scale + offsetY)
                        }
                        write(" z")
                    }
                    write(POLY_END[0])
                    write(colorToHtml(polyInfo.si.brushClr))
                    write(POLY_END[1])
                    write(getAlphaAsFrac(polyInfo.si.brushClr))
                    write(POLY_END[2])
                    write(if (polyInfo.si.pft == PolyFillType.EvenOdd) "evenodd" else "nonzero")
                    write(POLY_END[3])
                    write(colorToHtml(polyInfo.si.penClr))
                    write(POLY_END[4])
                    write(getAlphaAsFrac(polyInfo.si.penClr))
                    write(POLY_END[5])
                    write(polyInfo.si.penWidth)
                    write(POLY_END[6])
                    if (polyInfo.si.showCoords) {
                        write("<g font-family=\"Verdana\" font-size=\"11\" fill=\"black\">\n\n")
                        for (path in polyInfo.paths) {
                            if (path.size < 3) continue
                            path.forEach { (x, y) ->
                                val textX = (x * scale + offsetX).toInt()
                                val textY = (y * scale + offsetY).toInt()
                                write("<text x=\"$textX\" y=\"$textY\">$x,$y</text>\n\n")
                            }
                        }
                        write("</g>\n")
                    }
                }
                write("</svg>\n")
            }
        }
        return true
    }
}  // SVGBuilder

//------------------------------------------------------------------------------
// Miscellaneous function ...
//------------------------------------------------------------------------------

fun saveToFile(filename: String, ppg: Paths, scale: Double = 1.0, decimalPlaces: Long = 0): Boolean {
    val decimalPlaces = if (decimalPlaces > 8) 8 else decimalPlaces
    val format = "%.${decimalPlaces}f, %.${decimalPlaces}f,\n"
    try {
        FileWriter(filename)
    } catch (e: IOException) {
        return false
    }.use { file ->
        with(file) {
            ppg.forEach { pg ->
                pg.forEach { (x, y) ->
                    write(format.format(x / scale, y / scale))
                }
                write("\n")
            }
        }
    }
    return true
}

fun loadFromFile(ppg: Paths, filename: String, scale: Double): Boolean {
    //file format assumes:
    //  1. path coordinates (x,y) are comma separated (+/- spaces) and
    //  each coordinate is on a separate line
    //  2. each path is separated by one or more blank lines
    ppg.clear()
    var pg: Path = pathOf()
    try {
        BufferedReader(FileReader(filename))
    } catch (e: IOException) {
        return false
    }.use { file ->
        var line: String = ""
        val doublePattern = """([0-9]*(?:\.[0-9]+)?)"""
        val pattern = """\s*$doublePattern\s*,\s*$doublePattern\s*,?\s*""".toPattern()
        while (file.readLine()?.also { line = it } != null) {
            if (line == "") {
                //ie blank lines => flag start of next polygon
                if (pg.isNotEmpty()) {
                    ppg += pg
                    pg = pathOf()
                }
                continue
            }
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                val x = matcher.group(1).toDouble() * scale
                val y = matcher.group(2).toDouble() * scale
                pg.add(IntPoint(x.toLong(), y.toLong()))
            }
        }
        if (pg.isNotEmpty()) ppg += pg
    }
    return true
}

fun makeRandomPoly(edgeCount: Int, width: Int, height: Int, poly: Paths) {
    poly.clear()
    poly += Path(edgeCount) {
        IntPoint(
                x = (Math.random() * width).toLong(),
                y = (Math.random() * height).toLong()
        )
    }
}

fun main(args: Array<String>) {
    val argc = args.size
    if (argc > 0) {
        val arg1 = args[0]
        if (arg1.equals("-b", ignoreCase = true)
                || arg1.equals("--benchmark", ignoreCase = true)) {
            //do a benchmark test that creates a subject and a clip polygon both with
            //100 vertices randomly placed in a 400 * 400 space. Then perform an
            //intersection operation based on even-odd filling. Repeat all this X times.
            val loopCnt = args.getOrNull(1)?.toLong().takeIf { it != 0L } ?: 1000L
            print("\nPerforming $loopCnt random intersection operations ... ")
            var errorCnt = 0
            val subject = Paths()
            val clip = Paths()
            val solution = Paths()
            val clipper = Clipper()
            val timeStart = System.currentTimeMillis()
            (1L..loopCnt).forEach {
                makeRandomPoly(100, 400, 400, subject)
                makeRandomPoly(100, 400, 400, clip)
                clipper.apply {
                    clear()
                    addPaths(subject, PolyType.Subject, true)
                    addPaths(clip, PolyType.Clip, true)
                }
                        .execute(ClipType.Intersection, solution, PolyFillType.EvenOdd, PolyFillType.EvenOdd)
                        .let { if (!it) errorCnt++ }
            }
            val timeElapsed = (System.currentTimeMillis() - timeStart) / 1e3
            print("\nFinished in $timeElapsed secs with $errorCnt errors.\n\n")
            //let's save the very last result ...
            saveToFile("Subject.txt", subject)
            saveToFile("Clip.txt", clip)
            saveToFile("Solution.txt", solution)

            //and see the final clipping op as an image too ...
            SVGBuilder().apply {
                addPaths(subject, SVGBuilder.StyleInfo(
                        pft = PolyFillType.EvenOdd,
                        brushClr = 0x1200009C,
                        penClr = 0xCCD3D3DA,
                        penWidth = 0.8
                ))
                addPaths(clip, SVGBuilder.StyleInfo(
                        pft = PolyFillType.EvenOdd,
                        brushClr = 0x129C0000,
                        penClr = 0xCCFFA07A,
                        penWidth = 0.8
                ))
                addPaths(solution, SVGBuilder.StyleInfo(
                        pft = PolyFillType.NonZero,
                        brushClr = 0x6080ff9C,
                        penClr = 0xFF003300,
                        penWidth = 0.8
                ))
            }.saveToFile("solution.svg")
            System.exit(0)
        }
    }
    if (argc < 2) {
        print("""
Usage:
  clipper_console_demo S_FILE C_FILE CT [S_FILL C_FILL] [PRECISION] [SVG_SCALE]
or
  clipper_console_demo --benchmark [LOOP_COUNT]

Legend: [optional parameters in square braces]; {comments in curly braces}

Parameters:
  S_FILE & C_FILE are the subject and clip input files (see format below)
  CT: cliptype, either INTERSECTION or UNION or DIFFERENCE or XOR
  SUBJECT_FILL & CLIP_FILL: either EVENODD or NONZERO. Default: NONZERO
  PRECISION (in decimal places) for input data. Default = 0
  SVG_SCALE: scale of the output svg image. Default = 1.0
  LOOP_COUNT is the number of random clipping operations. Default = 1000


File format for input and output files:
  X, Y[,] {first vertex of first path}
  X, Y[,] {next vertex of first path}
  {etc.}
  X, Y[,] {last vertex of first path}
  {blank line(s) between paths}
  X, Y[,] {first vertex of second path}
  X, Y[,] {next vertex of second path}
  {etc.}

Examples:
  clipper_console_demo "subj.txt" "clip.txt" INTERSECTION EVENODD EVENODD
  clipper_console_demo --benchmark 1000""")
        System.exit(1)
    }
    val scaleLog10 = args.getOrNull(5)?.toDouble() ?: 0.0
    val scale = Math.pow(10.0, scaleLog10)
    val svgScale = args.getOrNull(6)?.toDouble() ?: 1.0 / scale
    val subject = Paths()
    val clip = Paths()
    if (!loadFromFile(subject, args[0], scale)) {
        System.err.print("\nCan't open the file ${args[0]} or the file format is invalid.\n")
        System.exit(1)
    }
    if (!loadFromFile(clip, args[1], scale)) {
        System.err.print("\nCan't open the file ${args[1]} or the file format is invalid.\n")
        System.exit(1)
    }

    val clipType = when (args.getOrNull(2)?.toUpperCase()) {
        "XOR" -> ClipType.Xor
        "UNION" -> ClipType.Union
        "DIFFERENCE" -> ClipType.Difference
        else -> ClipType.Intersection
    }

    val subjPft = args.getOrNull(3).let {
        if (it.equals("EVENODD", ignoreCase = true)) PolyFillType.EvenOdd else PolyFillType.NonZero
    }
    val clipPft = args.getOrNull(4).let {
        if (it.equals("EVENODD", ignoreCase = true)) PolyFillType.EvenOdd else PolyFillType.NonZero
    }

    val solution = Paths()

    Clipper().apply {
        addPaths(subject, PolyType.Subject, true)
        addPaths(clip, PolyType.Clip, true)
    }
            .execute(clipType, solution, subjPft, clipPft)
            .let {
                if (!it) {
                    print("$clipType failed!\n\n")
                    System.exit(1)
                }
            }
    print("\nFinished!\n\n")
    saveToFile("solution.txt", solution, scale)

    //let's see the result too ...
    SVGBuilder().apply {
        addPaths(subject, SVGBuilder.StyleInfo(
                pft = subjPft,
                brushClr = 0x1200009C,
                penClr = 0xCCD3D3DA,
                penWidth = 0.8
        ))
        addPaths(clip, SVGBuilder.StyleInfo(
                pft = clipPft,
                brushClr = 0x129C0000,
                penClr = 0xCCFFA07A,
                penWidth = 0.8
        ))
        addPaths(solution, SVGBuilder.StyleInfo(
                pft = PolyFillType.NonZero,
                brushClr = 0x6080ff9C,
                penClr = 0xFF003300,
                penWidth = 0.8
        ))
    }.saveToFile("solution.svg", svgScale)

    //finally, show the svg image in the default viewing application
    Desktop.getDesktop().open(File("solution.svg"))
    System.exit(0)
}
