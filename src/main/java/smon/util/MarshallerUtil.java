package smon.util;

import smon.dto.ShopDefinitionList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.OutputStream;


/** Reads and writes the shop import/export XML (UTF-8). */
public class MarshallerUtil {

    public static void marshall(ShopDefinitionList shops, OutputStream out) throws JAXBException {
        Marshaller marshaller = context().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(shops, out);
    }

    public static ShopDefinitionList unmarshall(File file) throws JAXBException {
        return (ShopDefinitionList) context().createUnmarshaller().unmarshal(file);
    }

    /** Built once: creating a JAXBContext is slow, and the context is thread-safe. */
    private static volatile JAXBContext context;

    private static JAXBContext context() throws JAXBException {
        if (context == null) {
            context = JAXBContext.newInstance(ShopDefinitionList.class);
        }
        return context;
    }
}
