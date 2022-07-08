package org.ml.table.output.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.ml.table.Table;
import org.ml.table.render.RenderingContext;
import org.ml.tools.PropertyManager;
import org.ml.tools.logging.LoggerFactory;
import org.ml.tools.velocity.VelocityConfig;
import static org.ml.tools.velocity.VelocityConfig.RequiredKey;
import static org.ml.tools.velocity.VelocityConfig.OptionalKey;

/**
 * A simple default writer to create output of a table via Velocity without
 * having to implement any code separately. A simple default template is
 * provided which can be used directly.
 *
 * @author osboxes
 */
public class VelocityWriter {

    private final static Logger LOGGER = LoggerFactory.getLogger(VelocityWriter.class.getName());
    private PropertyManager propertyManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy | HH:mm");

    /**
     * These are the keys injected into the VelocityContext
     */
    private enum PrivateContextKey {
        renderingContext, table, tables, date
    }

    /**
     *
     */
    public enum ContextKey {
        title
    }

    /**
     * These are additional context keys supported by the default templates
     */
    public enum OptionalContextKey {
        /**
         * Path to a CSS file to reference in the header
         */
        cssFileReference, cssFileInclude;
    }

    /**
     *
     */
    public VelocityWriter() {

    }

    /**
     * All properties submitted here will be added to the VelocityContext
     *
     * @param propertyManager
     */
    public VelocityWriter(PropertyManager propertyManager) {
        if (propertyManager == null) {
            throw new NullPointerException("propertyManager may not be null");
        }
        this.propertyManager = propertyManager;
    }

    /**
     * Write a single table
     *
     * @param table
     * @param fileName
     * @throws Exception
     */
    public void write(Table table, String fileName) throws Exception {
        if (table == null) {
            throw new NullPointerException("table may not be null");
        }
        write(null, table, fileName);
    }

    /**
     * Write multiple tables into a single file
     *
     * @param tables The keys are used as headers for the tables
     * @param fileName
     * @throws Exception
     */
    public void write(Map<String, Table> tables, String fileName) throws Exception {
        if (tables == null) {
            throw new NullPointerException("tables may not be null");
        }
        write(tables, null, fileName);
    }

    /**
     * Generic helper
     *
     * @param tables
     * @param table
     * @param fileName
     * @throws Exception
     */
    private void write(Map<String, Table> tables, Table table, String fileName) throws Exception {
        if (fileName == null) {
            throw new NullPointerException("fileName may not be null");
        }

        PropertyManager velocityPropertyManager = new PropertyManager();

        //.... A separate template file name can be specified which is then used instead of the default template contained in the package
        if (propertyManager.containsProperty(RequiredKey.templateName)) {
            velocityPropertyManager.setProperty(RequiredKey.templateName, propertyManager.getProperty(RequiredKey.templateName));
            if (propertyManager.containsProperty(OptionalKey.templateDirectory)) {
                velocityPropertyManager.setProperty(OptionalKey.templateDirectory, propertyManager.getProperty(OptionalKey.templateDirectory));
            } else {
                throw new UnsupportedOperationException("If a template file is specified, a template directory also needs ot be provided");
            }
        } else {
            if (table != null) {
                velocityPropertyManager.setProperty(RequiredKey.templateName, "velocity/table.vm");
            } else {
                velocityPropertyManager.setProperty(RequiredKey.templateName, "velocity/tables.vm");
            }
        }

        VelocityConfig velocityConfig = new VelocityConfig(velocityPropertyManager);
        Template template = velocityConfig.getTemplate();
        VelocityContext context = new VelocityContext();
        context.put(PrivateContextKey.renderingContext.toString(), RenderingContext.VELOCITY);
        if (table != null) {
            context.put(PrivateContextKey.table.toString(), table);
        } else {
            context.put(PrivateContextKey.tables.toString(), tables);
        }
        for (String key : propertyManager.getProperties().keySet()) {
            context.put(key, propertyManager.getProperty(key));
        }
        context.put(PrivateContextKey.date.toString(), formatter.format(ZonedDateTime.now()));

        BufferedWriter writer;
        File file = new File(fileName);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        LOGGER.log(Level.INFO, "Writing output file {0}", fileName);
        if (file.exists()) {
            file.delete();
        }
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8));
        template.merge(context, writer);
        writer.flush();
        writer.close();
    }

}
