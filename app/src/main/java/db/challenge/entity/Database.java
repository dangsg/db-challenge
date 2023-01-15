package db.challenge.entity;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Database {
    private List<Table> tables;

    /**
     * Generate schema XML tag
     * 
     * @return
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public Document toXmlDocument() throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("setup");

        doc.appendChild(rootElement);
        for (Table table : tables) {
            table.appendXmlChild(doc, rootElement);
        }

        return doc;
    }
}
