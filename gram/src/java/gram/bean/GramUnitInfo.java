package gram.bean;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.security.Exceptionevent;
import libWebsiteTools.security.Honeypot;
import gram.bean.database.Article;
import gram.bean.database.Comment;
import gram.bean.database.Section;
import libWebsiteTools.UUIDConverter;

public class GramUnitInfo implements PersistenceUnitInfo {

    private final String n;
    private final DataSource d;

    public GramUnitInfo(String name, DataSource ds) {
        this.n = name;
        this.d = ds;
    }

    @Override
    public String getPersistenceUnitName() {
        return n;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return "org.eclipse.persistence.jpa.PersistenceProvider";
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    @Override
    public DataSource getJtaDataSource() {
        return null;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return d;
    }

    @Override
    public List<String> getMappingFileNames() {
        return List.of();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return List.of(getPersistenceUnitRootUrl());
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        try {
            // give it a jar of something, I don't care, excludeUnlistedClasses() is true and getManagedClassNames() returns the entities its trying to find
            URL purl = Localization.class.getProtectionDomain().getCodeSource().getLocation();
            return purl;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<String> getManagedClassNames() {
        return List.of(UUIDConverter.class.getName(),
                Exceptionevent.class.getName(),
                Honeypot.class.getName(),
                Fileupload.class.getName(),
                Localization.class.getName(),
                Article.class.getName(),
                Section.class.getName(),
                Comment.class.getName());
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return true;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.ENABLE_SELECTIVE;
    }

    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();
        p.put("jakarta.persistence.schema-generation.database.action", "create");
        p.put("jakarta.persistence.schema-generation.create-source", "script");
        p.put("jakarta.persistence.schema-generation.create-script-source", "gramsetup.sql");
        p.put("hibernate.hbm2ddl.import_files_sql_extractor", "org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor");
        return p;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "2.0";
    }

    @Override
    public ClassLoader getClassLoader() {
        // use the current class's loader to prevent ClassCaseExceptions
        return this.getClass().getClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer ct) {
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}
