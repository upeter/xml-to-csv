package org.up.tools

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.File
import java.io.PrintStream


class Xml2CsvConverterTest {

    val xmlLoader = XML2CSVConverter()

    @Test
    fun shouldConvertXml() {
        val f = File(this.javaClass.getResource("/").file)
        val xmlFiles = xmlLoader.load(f)
        val res = xmlFiles.map { (f, xml) -> xmlLoader.filterNodes(f, xml, "//G_ROOM") }
        assertEquals(2, res.size)
        val writer = PrintStream(System.out)
        val csv = Tsv(writer)
        res.forEach { (f, nl) ->

            (0..(nl.length - 1)).toList().forEach {
                csv.put("HOTEL", f.name)
                nl.item(it).childTextNodes().forEach { (key, value) -> csv.put(key, value) }
                csv.pad()
            }

        }
        csv.write()
    }

}