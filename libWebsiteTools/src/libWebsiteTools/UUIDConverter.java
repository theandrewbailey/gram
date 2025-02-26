package libWebsiteTools;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;
import org.postgresql.util.PGobject;

/**
 * Because Eclipselink doesn't quite support UUIDs yet.
 *
 * @author alpha
 */
@Converter(autoApply = true)
public class UUIDConverter implements AttributeConverter<UUID, Object> {

    @Override
    public Object convertToDatabaseColumn(UUID uuid) {
        PostgresUuid object = new PostgresUuid();
        object.setType("uuid");
        try {
            if (uuid == null) {
                object.setValue(null);
            } else {
                object.setValue(uuid.toString());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error when creating Postgres uuid", e);
        }
        return object;
    }

    @Override
    public UUID convertToEntityAttribute(Object dbData) {
        if (dbData instanceof String) {
            return UUID.fromString(dbData.toString());
        } else {
            return (UUID) dbData;
        }
    }
}

class PostgresUuid extends PGobject implements Comparable<Object> {

    private static final long serialVersionUID = 1L;

    @Override
    public int compareTo(Object arg0) {
        return 0;
    }
}
