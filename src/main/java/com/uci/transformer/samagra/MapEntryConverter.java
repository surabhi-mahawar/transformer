package com.uci.transformer.samagra;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.lang.StringUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class MapEntryConverter implements Converter {

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext marshallingContext) {
        AbstractMap map = (AbstractMap) value;
        for (Object obj : map.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            writer.startNode(entry.getKey().toString());
            Object val = entry.getValue();
            if (val instanceof Map) {
                marshal(val, writer, marshallingContext);
            } else if (null != val) {
                writer.setValue(val.toString());
            }
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext unmarshallingContext) {
        Map<String, Object> map = new HashMap<>();

        while(reader.hasMoreChildren()) {
            reader.moveDown();

            String key = reader.getNodeName(); // nodeName aka element's name
            String value = reader.getValue().replaceAll("\\n|\\t", "");
            if (StringUtils.isBlank(value)) {
                map.put(key, unmarshal(reader, unmarshallingContext));
            } else {
                map.put(key, value);
            }

            reader.moveUp();
        }

        return map;
    }

    @Override
    public boolean canConvert(Class clazz) {
        return AbstractMap.class.isAssignableFrom(clazz);
    }
}