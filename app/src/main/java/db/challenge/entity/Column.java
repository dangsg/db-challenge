package db.challenge.entity;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Column {
    private String attribute;
    private String name;
    private String type;
    private String selector;

    /**
     * Append column tag into table tag
     * 
     * @param doc
     * @param parentElement
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public void appendXmlChild(Document doc, Element parentElement)
            throws ParserConfigurationException, TransformerException {
        Element element = doc.createElement(attribute);

        element.setAttribute("name", name);
        element.setAttribute("type", type);
        element.setAttribute("selector", selector);

        parentElement.appendChild(element);
    }
}
