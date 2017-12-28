package com.fang.home;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源注册<br/>
 * 启动动态数据源请在启动类中（如SpringBootSampleApplication）
 * 添加 @Import(DynamicDataSourceRegister.class)
 *
 * @作者 wzx
 * @日期 2017-1-12
 */
public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceRegister.class);
    private ConversionService conversionService = new DefaultConversionService();
    private PropertyValues dataSourcePropertyValues;
    // 如配置文件中未指定数据源类型，使用该默认值
    private static final Object DATASOURCE_TYPE_DEFAULT = "com.alibaba.druid.pool.DruidDataSource";
    // 数据源
    private DataSource defaultDataSource;
    private Map<String, DataSource> customDataSources = new HashMap<String, DataSource>();
    private JSONObject loadDBSets;


    /**
     * 加载多数据源配置
     */
    @Override
    public void setEnvironment(Environment environment) {
        loadDBSetting(environment);
        initDefaultDataSource(environment);
        initCustomDataSources(environment);
    }

    /**
     * 加载数据源的设置
     */
    public void loadDBSetting(Environment env) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "configer.");
        String filepath = propertyResolver.getProperty("filePath");
        String truefilepath = System.getProperty("user.dir")+File.separator+filepath;
        System.out.println("load filepath: "+truefilepath);
        logger.info("load filepath: "+truefilepath);
        logger.info("System.getProperty user.dir ; "+System.getProperty("user.dir"));
        System.getProperty("user.dir");
        String content = readFileByLines(truefilepath);
        if (content != null) {
            JSONObject json = JSONObject.parseObject(content);
            loadDBSets = json;
            logger.info("load file"+content);
        }
    }

    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public static String readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer content = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
//                System.out.println("line " + line + ": " + tempString);
                content.append(tempString);
                line++;
            }
            reader.close();
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }


    /**
     * 初始化主数据源
     *
     * @author SHANHY
     * @create 2016年1月24日
     */
    private void initDefaultDataSource(Environment env) {
        // 赋值加载的默认数据源
        if (loadDBSets!=null) {
//            logger.info("load file");
            JSONObject defaultDBSet = (JSONObject) loadDBSets.get("default");
            DBModel model = new DBModel(defaultDBSet);
            Map<String, Object> dsNewMap = new HashMap<>();
            dsNewMap.put("type", model.getType());
            dsNewMap.put("driver-class-name", model.getDriverclassname());
            dsNewMap.put("url", model.getUrl());
            dsNewMap.put("username", model.getUsername());
            dsNewMap.put("password", model.getPassword());
            defaultDataSource = buildDataSource(dsNewMap);
            customDataSources.put("default", defaultDataSource);//默认数据源放到动态数据源里
            dataBinder(defaultDataSource, env);
        }else {
            // 修改之前的
            System.out.println("load properties");
//            logger.info("load properties");
            RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "spring.datasource.");
            Map<String, Object> dsMap = new HashMap<>();
            dsMap.put("type", propertyResolver.getProperty("type"));
            dsMap.put("driver-class-name", propertyResolver.getProperty("driver-class-name"));
            dsMap.put("url", propertyResolver.getProperty("url"));
            dsMap.put("username", propertyResolver.getProperty("username"));
            dsMap.put("password", propertyResolver.getProperty("password"));

            defaultDataSource = buildDataSource(dsMap);
            customDataSources.put("default", defaultDataSource);//默认数据源放到动态数据源里
            dataBinder(defaultDataSource, env);
        }
    }

    /**
     * 为DataSource绑定更多数据
     *
     * @param dataSource
     * @param env
     */
    private void dataBinder(DataSource dataSource, Environment env) {
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(dataSource);
        //dataBinder.setValidator(new LocalValidatorFactory().run(this.applicationContext));
        dataBinder.setConversionService(conversionService);
        dataBinder.setIgnoreNestedProperties(false);//false
        dataBinder.setIgnoreInvalidFields(false);//false
        dataBinder.setIgnoreUnknownFields(true);//true
        if (dataSourcePropertyValues == null) {
            Map<String, Object> rpr = new RelaxedPropertyResolver(env, "spring.datasource").getSubProperties(".");
            Map<String, Object> values = new HashMap<>(rpr);
            // 排除已经设置的属性
            values.remove("type");
            values.remove("driver-class-name");
            values.remove("url");
            values.remove("username");
            values.remove("password");
            dataSourcePropertyValues = new MutablePropertyValues(values);
        }
        dataBinder.bind(dataSourcePropertyValues);
    }

    /**
     * 初始化更多数据源
     */
    private void initCustomDataSources(Environment env) {
        // 读取配置文件获取更多数据源，也可以通过defaultDataSource读取数据库获取更多数据源
        if (loadDBSets!=null) {
//            logger.info("load file");
            // 修改之后：
            for (String key : loadDBSets.keySet()) {
                JSONObject dbset = (JSONObject) loadDBSets.get(key);
                DBModel model = new DBModel(dbset);
                Map<String, Object> dsNewMap = new HashMap<>();
                dsNewMap.put("type", model.getType());
                dsNewMap.put("driver-class-name", model.getDriverclassname());
                dsNewMap.put("url", model.getUrl());
                dsNewMap.put("username", model.getUsername());
                dsNewMap.put("password", model.getPassword());
                DataSource ds = buildDataSource(dsNewMap);
                customDataSources.put(key.trim(), ds);
                dataBinder(ds, env);
            }
        }else {
            // 修改之前的
//            logger.info("load properties");
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "interface.datasource.");
        String dsPrefixs = propertyResolver.getProperty("names");
        for (String dsPrefix : dsPrefixs.split(",")) {// 多个数据源
            Map<String, Object> dsMap = propertyResolver.getSubProperties(dsPrefix + ".");
            DataSource ds = buildDataSource(dsMap);
            customDataSources.put(dsPrefix, ds);
            dataBinder(ds, env);
        }
        }
    }

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {
        Map<Object, Object> targetDataSources = new HashMap<Object, Object>();
        // 将主数据源添加到更多数据源中
        targetDataSources.put("dataSource", defaultDataSource);
        DynamicDataSourceContextHolder.dataSourceIds.add("dataSource");
        // 添加更多数据源
        targetDataSources.putAll(customDataSources);
        for (String key : customDataSources.keySet()) {
            DynamicDataSourceContextHolder.dataSourceIds.add(key);
        }

        // 创建DynamicDataSource
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultTargetDataSource", defaultDataSource);
        mpv.addPropertyValue("targetDataSources", targetDataSources);
        registry.registerBeanDefinition("dataSource", beanDefinition);

        logger.info("Dynamic DataSource Registry");

    }

    /**
     * 创建DataSource
     *
     * @return
     * @author SHANHY
     * @create 2016年1月24日
     */
    @SuppressWarnings("unchecked")
    public DataSource buildDataSource(Map<String, Object> dsMap) {
        try {
            Object type = dsMap.get("type");
            if (type == null)
                type = DATASOURCE_TYPE_DEFAULT;// 默认DataSource

            Class<? extends DataSource> dataSourceType;
            dataSourceType = (Class<? extends DataSource>) Class.forName((String) type);

            String driverClassName = dsMap.get("driver-class-name").toString();
            String url = dsMap.get("url").toString();
            String username = dsMap.get("username").toString();
            String password = dsMap.get("password").toString();

            DataSourceBuilder factory = DataSourceBuilder.create().driverClassName(driverClassName).url(url)
                    .username(username).password(password).type(dataSourceType);
            return factory.build();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}
