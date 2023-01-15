package db.challenge.entity;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Table {
    private String type;
    private List<Column> columns;

    /**
     * Append table tag into root tag
     * 
     * @param doc
     * @param superElement
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public void appendXmlChild(Document doc, Element superElement)
            throws ParserConfigurationException, TransformerException {
        Element element = doc.createElement("generator");

        element.setAttribute("type", type);
        for (Column column : columns) {
            column.appendXmlChild(doc, element);
        }

        superElement.appendChild(element);
    }
}
