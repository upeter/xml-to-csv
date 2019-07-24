package org.up.tools

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.PrintStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import java.io.InputStreamReader
import java.io.FileInputStream

fun List<Any>.pad(size: Int, padValue: Any): List<Any> {
    val elsToAdd = size - this.size
    fun recurse(els: Int, list: List<Any>): List<Any> {
        return if (els <= 0) list else recurse(els - 1, list + padValue)
    }
    return recurse(elsToAdd, this)
}


fun <E> List<List<E>>.transpose(): List<List<E>> {
    if (isEmpty()) return this

    val width = first().size
    if (any { it.size != width }) {
        throw IllegalArgumentException("All nested lists must have the same size, but sizes were ${map { it.size }}")
    }

    return (0 until width).map { col ->
        (0 until size).map { row -> this[row][col] }
    }
}

fun Node.toXml(): String {
    val transfac = TransformerFactory.newInstance()
    val trans = transfac.newTransformer()
    trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    trans.setOutputProperty(OutputKeys.INDENT, "yes")
    val sw = StringWriter()
    val result = StreamResult(sw)
    val source = DOMSource(this)
    trans.transform(source, result)
    return sw.toString()
}

fun Node?.childTextNodes(): List<Pair<String, String>> {
    fun recurse(nl: NodeList?, res: List<Pair<String, String>>): List<Pair<String, String>> {
        return if (nl != null) {
            (0..(nl.length - 1)).toList().flatMap {
                val node: Node? = nl.item(it)
                if (node != null)
                    when {
                        node.nodeType == Node.ELEMENT_NODE && node.firstChild?.nodeType == Node.TEXT_NODE -> res + (node.getNodeName() to node.firstChild.textContent)
                        node.nodeType == Node.ELEMENT_NODE && node.firstChild == null -> res + (node.getNodeName() to "")
                        else -> (res union recurse(node.childNodes, res)).toList()
                    } else res
            }
        } else res
    }
    return if (this != null) recurse(this.childNodes, emptyList()) else emptyList()
}


class Tsv(val writer: PrintStream) {

    val Separator = "\t"
    private var csv = mutableMapOf<String, List<Any>>()

    fun put(key: String, value: Any) {
        csv.put(key, csv.getOrDefault(key, emptyList()) + value)
    }

    fun pad() {
        val longest = csv.values.map { it.size }.max() ?: 0
        csv = csv.map { (key, values) -> key to values.pad(longest, "") }.toMap().toMutableMap()
    }

    fun write(): Int {
        try {
            writer.println(csv.keys.joinToString(Separator))
            val transposed = csv.values.toList().transpose()
            transposed.forEach { row -> writer.println(row.joinToString(Separator)) }
            return transposed.size
        } finally {
            writer.close()
        }
    }

    companion object {
        operator fun invoke(file: File) = Tsv(PrintStream(file))
    }
}

class XML2CSVConverter {
    private val builderFactory = DocumentBuilderFactory.newInstance()
    private val builder = builderFactory.newDocumentBuilder()

    private fun loadFile(file: File):InputSource {
        val inputStream = FileInputStream(file)
        val reader = InputStreamReader(inputStream, "UTF-8")
        val source = InputSource(reader)
        source.encoding = "UTF-8"
        return source
    }
    internal fun load(vararg files: File): List<Pair<File, Document>> {
        return files.flatMap {
            when {
                it.isFile && it.absolutePath.endsWith(".xml") -> listOf(it to builder.parse(loadFile(it)))
                it.isDirectory && it.listFiles()
                        .any { it.absolutePath.endsWith(".xml") } -> it.listFiles()
                        .filter { it.absolutePath.endsWith(".xml") }
                        .map { it as File to builder.parse(loadFile(it)) }
                else -> {
                    println("WARNING: ${it} has or is not an XML file")
                    emptyList()
                }
            }
        }
    }


    internal fun filterNodes(file: File, xmlDocs: Document, xpath: String): Pair<File, NodeList> {
        val xPath = XPathFactory.newInstance().newXPath()
        val nodeList = xPath.compile(xpath).evaluate(xmlDocs, XPathConstants.NODESET) as NodeList
        return file to nodeList
    }


    fun process(out: File, xpath: String, fileNameColumn: String, vararg xmlFiles: File) {
        try {
            val docs = load(* xmlFiles)
            require(docs.isNotEmpty(), { "No xml files found in ${xmlFiles.map { it.absolutePath }}" })
            val xPathResult = docs.map { (f, xml) -> filterNodes(f, xml, xpath) }
            val tsv = Tsv(out)
            xPathResult.forEach { (f, nl) ->
                println("Processing: ${f.absolutePath}")
                (0..(nl.length - 1)).toList().forEach {
                    tsv.put(fileNameColumn, f.nameWithoutExtension)
                    nl.item(it).childTextNodes().forEach { (key, value) -> tsv.put(key, value) }
                    tsv.pad()
                }
            }
            val records = tsv.write()
            println("Successfully written $records records to ${out.absolutePath}")
        } catch (ex: Exception) {
            println("ERROR: Processing failed due to: ${ex.message}")
        }


    }
}