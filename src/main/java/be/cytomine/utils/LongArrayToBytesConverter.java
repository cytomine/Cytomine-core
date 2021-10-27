package be.cytomine.utils;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.*;

@Converter
public class LongArrayToBytesConverter implements AttributeConverter<Long[],byte[]>{
    @Override
    public byte[] convertToDatabaseColumn(Long[] attribute) {
        if (attribute==null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(attribute);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("cannot serialize", e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    @Override
    public Long[] convertToEntityAttribute(byte[] dbData) {
        if (dbData==null) {
            return null;
        }
        try (ByteArrayInputStream b = new ByteArrayInputStream(dbData)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                Long[] data = (Long[]) o.readObject();
                return data;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("cannot deserialize", e);
        }

    }
}
