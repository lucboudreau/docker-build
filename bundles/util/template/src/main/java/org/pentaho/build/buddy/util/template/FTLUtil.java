package org.pentaho.build.buddy.util.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bryan on 3/7/16.
 */
public class FTLUtil {
    private final Configuration configuration;

    public FTLUtil(Class<?> clazz) {
        configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(clazz, "/");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public String render(String templatePath, String key, Object data) throws IOException {
        Map map = new HashMap();
        map.put(key, data);
        return render(templatePath, map);
    }

    public String render(String templatePath, Map data) throws IOException {
        Template template = configuration.getTemplate(templatePath);
        StringWriter stringWriter = new StringWriter();
        try {
            template.process(data, stringWriter);
        } catch (TemplateException e) {
            throw new IOException(e);
        } finally {
            stringWriter.close();
        }
        return stringWriter.toString();
    }
}
