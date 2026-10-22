package ovh.litapp.keepscore.helper

import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/**
 * XML utilities.
 *
 * @author nolan
 */
object XmlHelper {

    @JvmStatic
    fun prettyPrint(unformattedXml: String): String =
        try {
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            // initialize StreamResult with File object to save to file
            val result = StreamResult(StringWriter())
            val source = StreamSource(StringReader(unformattedXml))
            transformer.transform(source, result)
            result.writer.toString()
        } catch (t: Throwable) {
            // should never happen
            throw RuntimeException(t)
        }

    @JvmStatic
    @Throws(XmlPullParserException::class)
    fun loadData(xmlData: String): XmlPullParser {
        val parserFactory = XmlPullParserFactory.newInstance()
        val parser = parserFactory.newPullParser()
        parser.setInput(StringReader(xmlData))

        return parser
    }
}
