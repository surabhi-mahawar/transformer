package com.uci.transformer.odk.utilities;

import com.thoughtworks.xstream.XStream;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;

@Slf4j
@Builder
public class FormUpdation {
    String formPath;
    Document instanceData;
    private XStream magicApi;
    File formXml;
    static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    static Transformer transformer;
    static StringWriter writer = new StringWriter();
    static StreamResult sr = new StreamResult(writer);
    static FileInputStream fis;

    static {
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void init() throws ParserConfigurationException, IOException, SAXException, TransformerException {
        formXml = new File(formPath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.instanceData = builder.parse(formXml);
        this.instanceData.getDocumentElement().normalize();
    }

    public String getXML() throws TransformerException {
        DOMSource source = new DOMSource(this.instanceData);
        transformer.transform(source, sr);
        return writer.toString();
    }

    public InputStream getInputStream() throws TransformerException {
        DOMSource source = new DOMSource(this.instanceData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Result outputTarget = new StreamResult(outputStream);
        transformer.transform(source, outputTarget);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public void addSelectOneOptions(ArrayList<Item> options, String key){
        NodeList e =  this.instanceData.getElementsByTagName("select1");
        boolean found = false;
        Node selectElement = null;
        for(int i=0; i< e.getLength(); i++){
            if(e.item(i).getAttributes().getNamedItem("ref").getChildNodes().item(0).toString().contains(key)){
                found = true;
                selectElement = e.item(i);
                for (Item item: options){
                    Node itemNodeClone = selectElement.getFirstChild().getNextSibling().cloneNode(true);
                    itemNodeClone.getChildNodes().item(0).getFirstChild().setNodeValue(item.value + " " + item.label);
                    itemNodeClone.getChildNodes().item(1).getFirstChild().setNodeValue(item.value);
                    selectElement.appendChild(itemNodeClone);
                }
                // Delete the dummy one.
                selectElement.removeChild(selectElement.getFirstChild().getNextSibling());
                break;
            }
        }
    }
}
