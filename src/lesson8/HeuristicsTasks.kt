@file:Suppress("UNUSED_PARAMETER", "unused")

package lesson8

import lesson6.Graph
import lesson6.Path
import lesson7.knapsack.Fill
import lesson7.knapsack.Item
import kotlin.math.pow
import kotlin.random.Random

// Примечание: в этом уроке достаточно решить одну задачу

/**
 * Решить задачу о ранце (см. урок 6) любым эвристическим методом
 *
 * Очень сложная
 *
 * load - общая вместимость ранца, items - список предметов
 *
 * Используйте parameters для передачи дополнительных параметров алгоритма
 * (не забудьте изменить тесты так, чтобы они передавали эти параметры)
 */
fun fillKnapsackHeuristics(load: Int, items: List<Item>, vararg parameters: Any): Fill {
    TODO()
}

/**
 * Решить задачу коммивояжёра (см. урок 5) методом колонии муравьёв
 * или любым другим эвристическим методом, кроме генетического и имитации отжига
 * (этими двумя методами задача уже решена в под-пакетах annealing & genetic).
 *
 * Очень сложная
 *
 * Граф передаётся через получатель метода
 *
 * Используйте parameters для передачи дополнительных параметров алгоритма
 * (не забудьте изменить тесты так, чтобы они передавали эти параметры)
 */
/* При реализации алгоритма использовалась данная публикация
M. Dorigo and L. M. Gambardella, "Ant colony system: a cooperative learning approach to the traveling salesman problem,"
in IEEE Transactions on Evolutionary Computation, vol. 1, no. 1, pp. 53-66, April 1997, doi: 10.1109/4235.585892.
 */

fun defaultPheromoneCalculation(graph: Graph): Double? {
    var length = 0.0
    var currentVertex = graph.vertices.first()
    val visitedVertices: MutableList<Graph.Vertex> = mutableListOf()
    while (visitedVertices.size < graph.vertices.size - 1) {
        val nextVertex =
            graph.getNeighbors(currentVertex).filter { !visitedVertices.contains(it) }.minBy {
                graph.getConnection(it, currentVertex)!!.weight
            } ?: return null
        length += graph.getConnection(currentVertex, nextVertex)!!.weight
        visitedVertices.add(currentVertex)
        currentVertex = nextVertex
    }
    return 1 / (length * graph.vertices.size)
}

// Время О(antNumber * iterationNumber * кол-во вершин графа)
// Память О(antNumber * (кол-во вершин графа + кол-во рёбер графа))

fun Graph.findVoyagingPathHeuristics(
    iterationNumber: Int,
    alpha: Double = 0.1,
    beta: Double = 2.0,
    antNumber: Int = 10,
    randomFactor: Double = 0.9,
    ro: Double = 0.1
): Path {
    var count = 0
    var bestPathChanged = false
    val startingCity = vertices.first()
    val defaultPheromone = defaultPheromoneCalculation(this) ?: 1.0
    val edgesPheromoneValue = mutableMapOf<Graph.Edge, Double>()

    class Ant(
        val graph: Graph,
        //val edgesPheromoneValue: MutableMap<Graph.Edge, Double>,
        val visitedCities: MutableSet<Graph.Vertex> = mutableSetOf(),
        val visitedEdges: MutableSet<Graph.Edge> = mutableSetOf(),
        var hasTraversedCorrectly: Boolean = true
    ) {
        fun clear() {
            visitedCities.clear()
            visitedEdges.clear()
            hasTraversedCorrectly = true
        }

        fun chooseNextCity(currentCity: Graph.Vertex): Graph.Vertex? {
            visitedCities.add(currentCity)
            val q = Random.nextDouble(0.0, 1.0)
            val probabilities = calculateProbabilities(currentCity)
            if (probabilities.isEmpty()) return null
            if (q > randomFactor) {
                if (probabilities.size == 1) return probabilities.keys.first()
                val rand = Random.nextDouble(0.0, 1.0)
                val probSorted = probabilities.toList().sortedBy { (_, value) -> value }.toMap()
                probSorted.forEach {
                    if (rand < it.value) return it.key
                }
            } else {
                return probabilities.maxBy { it.value }!!.key
            }
            return null

        }

        private fun calculateProbabilities(city: Graph.Vertex): Map<Graph.Vertex, Double> {
            var overallImportance = 0.0
            val probabilities = mutableMapOf<Graph.Vertex, Double>()
            for (neighbour in graph.getNeighbors(visitedCities.last())) {
                if (!visitedCities.contains(neighbour)) {
                    val edge = graph.getConnection(city, neighbour)
                    val currentImportance = edgesPheromoneValue[edge]?.times((1.0 / edge!!.weight).pow(beta))
                    if (currentImportance != null) {
                        overallImportance += currentImportance
                        probabilities[neighbour] = currentImportance
                    }
                }
            }
            if (probabilities.isEmpty()) return mapOf()
            var check = 0.0
            for (currentCity in probabilities) {
                val probability = currentCity.value / overallImportance
                check += probability
                currentCity.setValue(probability)
            }
            return probabilities
        }


        fun getPath(): Path? {
            var result = Path(visitedCities.first())
            for (vertex in visitedCities.minus(visitedCities.first())) {
                if (graph.getConnection(result.vertices.last(), vertex) == null)
                    return null
                result = Path(result, graph, vertex)
            }
            return Path(result, graph, visitedCities.first())
        }


    }




    edges.forEach { edgesPheromoneValue[it] = defaultPheromone }
    val ants = List(antNumber) { Ant(this) }
    var bestPath: Path? = null

    for (i in 0..iterationNumber) {
        for (ant in ants) {
            var currentCity = startingCity

            while (ant.visitedCities.count() < this.vertices.count()) {
                val nextCity = ant.chooseNextCity(currentCity)
                    ?: break // Он в тупике???
                val currentEdge = this.getConnection(currentCity, nextCity)
                if (currentEdge != null) {
                    val newPheromoneValue = (1.0 - ro) * edgesPheromoneValue[currentEdge]!! + defaultPheromone * ro
                    edgesPheromoneValue[currentEdge] = newPheromoneValue
                }
                currentEdge?.let { ant.visitedEdges.add(it) }
                currentCity = nextCity
            }
            val finalEdge = this.getConnection(startingCity, currentCity)
            if (finalEdge == null || ant.visitedCities.count() != this.vertices.count())
                ant.hasTraversedCorrectly = false
            else
                ant.visitedEdges.add(finalEdge)

        }
        val minPath =
            ants.filter { it.hasTraversedCorrectly }.minBy { it.getPath()?.length ?: Int.MAX_VALUE }?.getPath()
        if (bestPath == null || minPath?.length ?: Int.MAX_VALUE < bestPath.length) {
            bestPath = minPath
            bestPathChanged = true
        }

        for ((key, value) in edgesPheromoneValue) {
            edgesPheromoneValue[key] = value * (1.0 - ro)
        }
        count++
        if (bestPath != null) {
            for (currentCity in bestPath.vertices.minus(startingCity)) {
                var previousCity = startingCity
                if (this.getConnection(previousCity, currentCity) == null) {
                    break
                }
                edgesPheromoneValue[this.getConnection(previousCity, currentCity)!!] =
                    edgesPheromoneValue[this.getConnection(
                        previousCity,
                        currentCity
                    )]!! +
                            if (bestPathChanged) alpha * (1.0 / bestPath.length) else {
                                bestPathChanged = false
                                0.0
                            }
                previousCity = currentCity
            }
        }
        ants.forEach { it.clear() }
    }
    println(bestPath)
    return bestPath!!

}



