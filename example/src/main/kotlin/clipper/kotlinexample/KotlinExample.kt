package clipper.kotlinexample

import clipper.*

fun main(args: Array<String>) {
    val printASCII = true
    val clipType = ClipType.Union
    val subject = pathOf(
            IntPoint(0, 0),
            IntPoint(10, 0),
            IntPoint(10, 10),
            IntPoint(0, 10)
    )
    val clip = pathOf(
            IntPoint(5, -1),
            IntPoint(11, 5),
            IntPoint(5, 11),
            IntPoint(-1, 5)
    )
    val solution = Paths()

    Clipper().apply {
        addPath(subject, PolyType.Subject, true)
        addPath(clip, PolyType.Clip, true)
    }.execute(clipType, solution, PolyFillType.EvenOdd).let { success ->
        if (!success) {
            println("error")
        } else {
            if (!printASCII) {
                solution.forEachIndexed { i, path ->
                    println("polygon: " + i)
                    for (pt in path) {
                        println(pt)
                    }
                    println()
                }
            } else {
                with(solution.bound){
                    if (solution.size != 1
                            || Math.abs(right - left) > 80
                            || Math.abs(top - bottom) > 30
                            || solution[0].size > 99
                            || subject.size > 99
                            || clip.size > 99) {
                        println("success, but solution, clip or subject is too big or has more than 1 polygon or empty")
                    } else {
                        println("\n----------------------------------------")
                        println("Subject")
                        printPolygon(subject, left, right, top, bottom)
                        println("\n----------------------------------------")
                        println("Clip")
                        printPolygon(clip, left, right, top, bottom)
                        println("\n----------------------------------------")
                        println(clipType)
                        printPolygon(solution[0], left, right, top, bottom)
                    }
                }
            }
        }
    }
}

private fun printPolygon(polygon: List<IntPoint>, left: Long, right: Long, top: Long, bottom: Long): Boolean {
    if (polygon.size > 99) {
        return false
    }
    val points = HashMap<IntPoint, Int>()
    for (i in polygon.indices) {
        points.put(polygon[i], i)
    }
    val polygon = polygon.sortedWith(Comparator{ o1, o2 ->
        when {
            o1.y > o2.y -> -1
            o1.y < o2.y -> 1
            else -> o1.x.compareTo(o2.x)
        }
    })
    var prev = IntPoint(0, bottom - top)
    polygon.forEach{pt ->
        val newLines = prev.y - pt.y
        var newLine = false
        (0 until newLines).forEach{
            println()
            newLine = true
        }
        val spaces = if (newLine) pt.x - left + 1 else pt.x - prev.x
        (0 until spaces - 1).forEach{
            print("  ")
        }
        if (newLines > 0 || spaces > 1) {
            val position = points[pt]!!
            print(if (Math.abs(position) < 10) " " + position else position)
        }
        prev = pt
    }
    println()
    return true
}
