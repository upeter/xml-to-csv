package org.up.tools

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.File


class MyArgs(parser: ArgParser) {

    val dest by parser.storing(
            "-o", "--out",
            help = "destination path of type tsv")
    { File(this) }
            .default(File("./out.tsv"))

    val filenamecolumn by parser.storing(
            "-c", "--fileColumnName",
            help = "column name add with the name of each source file that is processed")
            .default { "HOTEL_NAME" }

    val xpath by parser.storing(
            "-x", "--xpath",
            help = "xpath expression for selecting nodes that are transformed to TSV")
            .default("//G_ROOM")

    val sources by parser.positionalList(
            name = "SOURCES",
            sizeRange = 1..Int.MAX_VALUE,
            help = "source xml files or directory containing xml files") { File(this) }

}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::MyArgs).run {
        println("Welcome to XML2CSV converter")
        println("I'm going to convert the xml files from: ${sources.map { it.absolutePath }.joinToString(", ")} to: ${dest} with XPath: ${xpath} and additional column containing each file name: ${filenamecolumn}")
        XML2CSVConverter().process(dest, xpath, filenamecolumn, * sources.toTypedArray())
    }

}

