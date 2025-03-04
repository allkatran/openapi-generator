/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.URLPathUtils;

import java.io.File;
import java.net.URL;
import java.util.*;

import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.StringUtils.camelize;

/**
 * Created by prokarma on 04/09/17.
 */
@Setter
public class JavaPKMSTServerCodegen extends AbstractJavaCodegen {

    public static final String CONFIG_PACKAGE = "configPackage";
    public static final String BASE_PACKAGE = "basePackage";
    public static final String TITLE = "title";
    public static final String EUREKA_URI = "eurekaUri";
    public static final String ZIPKIN_URI = "zipkinUri";
    public static final String SPRINGADMIN_URI = "springBootAdminUri";
    @Getter protected String basePackage = "com.prokarma.pkmst";
    @Getter protected String serviceName = "Pkmst";
    @Getter protected String configPackage = "com.prokarma.pkmst.config";
    @Getter protected String title;
    protected String eurekaUri;
    protected String zipkinUri;
    protected String springBootAdminUri;

    public JavaPKMSTServerCodegen() {
        super();

        modifyFeatureSet(features -> features.includeDocumentationFeatures(DocumentationFeature.Readme));

        groupId = "com.prokarma";
        artifactId = "pkmst-microservice";
        embeddedTemplateDir = templateDir = "java-pkmst";
        apiPackage = "com.prokarma.pkmst.controller";
        modelPackage = "com.prokarma.pkmst.model";
        invokerPackage = "com.prokarma.pkmst.controller";

        // clioOptions default redefinition need to be updated
        updateOption(CodegenConstants.GROUP_ID, this.getGroupId());
        updateOption(CodegenConstants.INVOKER_PACKAGE, this.getInvokerPackage());
        updateOption(CodegenConstants.ARTIFACT_ID, this.getArtifactId());
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);

        this.jackson = true;

        this.cliOptions.add(new CliOption("basePackage", "base package for java source code"));
        this.cliOptions.add(new CliOption("serviceName", "Service Name"));
        this.cliOptions.add(new CliOption(TITLE, "server title name or client service name"));
        this.cliOptions.add(new CliOption("eurekaUri", "Eureka URI"));
        this.cliOptions.add(new CliOption("zipkinUri", "Zipkin URI"));
        this.cliOptions.add(new CliOption("springBootAdminUri", "Spring-Boot URI"));
        // Middleware config
        this.cliOptions.add(new CliOption("pkmstInterceptor", "PKMST Interceptor"));

        this.apiTestTemplateFiles.put("api_test.mustache", ".java");

        if (".md".equals(this.modelDocTemplateFiles.get("model_doc.mustache"))) {
            this.modelDocTemplateFiles.remove("model_doc.mustache");
        }
        if (".md".equals(this.apiDocTemplateFiles.get("api_doc.mustache"))) {
            this.apiDocTemplateFiles.remove("api_doc.mustache");
        }
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "java-pkmst";
    }

    @Override
    public String getHelp() {
        return "Generates a PKMST SpringBoot Server application using the SpringFox integration."
                + " Also enables EurekaServerClient / Zipkin / Spring-Boot admin";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        if (this.additionalProperties.containsKey("basePackage")) {
            this.setBasePackage((String) this.additionalProperties.get("basePackage"));
            this.setInvokerPackage(this.getBasePackage());
            this.apiPackage = this.getBasePackage() + ".controller";
            this.modelPackage = this.getBasePackage() + ".model";
            this.setConfigPackage(this.getBasePackage() + ".config");
        } else {
            this.additionalProperties.put(BASE_PACKAGE, basePackage);
            this.additionalProperties.put(CONFIG_PACKAGE, this.getConfigPackage());
            this.additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
            this.additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }
        convertPropertyToStringAndWriteBack("groupId", this::setGroupId);

        convertPropertyToStringAndWriteBack("artifactId", this::setArtifactId);
        convertPropertyToStringAndWriteBack("artifactVersion", this::setArtifactVersion);
        convertPropertyToStringAndWriteBack("serviceName", this::setServiceName);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING, this::setSerializeBigDecimalAsString);
        convertPropertyToStringAndWriteBack(CodegenConstants.SERIALIZABLE_MODEL, this::setTitle);
        this.additionalProperties.put(CodegenConstants.SERIALIZABLE_MODEL, serializableModel);

        convertPropertyToStringAndWriteBack(EUREKA_URI, this::setEurekaUri);
        convertPropertyToStringAndWriteBack(ZIPKIN_URI, this::setZipkinUri);
        convertPropertyToStringAndWriteBack(SPRINGADMIN_URI, this::setSpringBootAdminUri);
        this.additionalProperties.put("java8", true);

        if (this.additionalProperties.containsKey(WITH_XML)) {
            this.setWithXml(Boolean.parseBoolean(additionalProperties.get(WITH_XML).toString()));
        }
        this.additionalProperties.put(WITH_XML, withXml);

        this.apiTemplateFiles.put("api.mustache", ".java");
        this.apiTemplateFiles.put("apiController.mustache", "Controller.java");

        this.modelTemplateFiles.put("model.mustache", ".java");

        this.supportingFiles.add(new SupportingFile("SpringBootApplication.mustache",
                (this.getSourceFolder() + File.separator + this.getBasePackage()).replace(".", File.separator),
                this.getServiceName() + "Application" + ".java"));

        this.supportingFiles
                .add(new SupportingFile("config" + File.separator + "openapiDocumentationConfig.mustache",
                        (this.sourceFolder + File.separator + this.getConfigPackage()).replace(".",
                                java.io.File.separator) + File.separator + "swagger",
                        "OpenAPIDocumentationConfig.java"));

        this.supportingFiles.add(new SupportingFile("config" + File.separator + "pkmstproperties.mustache",
                (this.sourceFolder + File.separator + this.getConfigPackage()).replace(".", java.io.File.separator)
                        + File.separator + "swagger",
                "PkmstProperties.java"));
        this.supportingFiles.add(new SupportingFile("config" + File.separator + "appconfig.mustache",
                (this.sourceFolder + File.separator + this.getConfigPackage()).replace(".", java.io.File.separator)
                        + File.separator,
                "AppConfig.java"));

        // Security
        this.supportingFiles
                .add(new SupportingFile("security" + File.separator + "authorizationServerConfiguration.mustache",
                        (this.sourceFolder + File.separator + this.basePackage).replace(".", File.separator)
                                + File.separator + "security",
                        "AuthorizationServerConfiguration.java"));
        this.supportingFiles
                .add(new SupportingFile("security" + File.separator + "oAuth2SecurityConfiguration.mustache",
                        (this.sourceFolder + File.separator + this.basePackage).replace(".", File.separator)
                                + File.separator + "security",
                        "OAuth2SecurityConfiguration.java"));
        this.supportingFiles
                .add(new SupportingFile("security" + File.separator + "resourceServerConfiguration.mustache",
                        (this.sourceFolder + File.separator + this.basePackage).replace(".", File.separator)
                                + File.separator + "security",
                        "ResourceServerConfiguration.java"));

        // logging

        this.supportingFiles.add(new SupportingFile("logging" + File.separator + "httpLoggingFilter.mustache",
                (this.sourceFolder + File.separator + this.basePackage).replace(".", File.separator) + File.separator
                        + "logging",
                "HttpLoggingFilter.java"));

        // Resources
        this.supportingFiles.add(new SupportingFile("resources" + File.separator + "application-local.mustache",
                ("src.main.resources").replace(".", java.io.File.separator), "application-local.yml"));
        this.supportingFiles.add(new SupportingFile("resources" + File.separator + "application-dev.mustache",
                ("src.main.resources").replace(".", java.io.File.separator), "application-dev.yml"));
        this.supportingFiles.add(new SupportingFile("resources" + File.separator + "application-dev-config.mustache",
                ("src.main.resources").replace(".", java.io.File.separator), "application-dev-config.yml"));
        this.supportingFiles.add(new SupportingFile("resources" + File.separator + "bootstrap.mustache",
                ("src.main.resources").replace(".", java.io.File.separator), "bootstrap.yml"));

        // POM
        this.supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));

        // Readme
        this.supportingFiles.add(new SupportingFile("readme.mustache", "", "Readme.md"));

        // manifest

        this.supportingFiles.add(new SupportingFile("manifest.mustache", "", "manifest.yml"));

        // docker
        this.supportingFiles.add(new SupportingFile("docker.mustache", "", "Dockerfile"));

        // logstash

        this.supportingFiles.add(new SupportingFile("logstash.mustache", "", "logstash.conf"));

        // Cucumber
        this.supportingFiles.add(new SupportingFile("cucumber" + File.separator + "executeReport.mustache",
                this.testFolder + File.separator + this.basePackage.replace(".", File.separator) + File.separator
                        + "cucumber" + File.separator + "report",
                "ExecuteReport.java"));

        this.supportingFiles.add(new SupportingFile(
                "cucumber" + File.separator + "cucumberTest.mustache", this.testFolder + File.separator
                + this.basePackage.replace(".", File.separator) + File.separator + "cucumber",
                serviceName + "Test.java"));

        this.supportingFiles.add(new SupportingFile(
                "cucumber" + File.separator + "cucumberSteps.mustache", this.testFolder + File.separator
                + this.basePackage.replace(".", File.separator) + File.separator + "cucumber",
                serviceName + "Steps.java"));

        this.supportingFiles.add(new SupportingFile(
                "cucumber" + File.separator + "package.mustache", this.testFolder + File.separator
                + this.basePackage.replace(".", File.separator) + File.separator + "cucumber",
                serviceName + "package-info.java"));

        // test resources
        this.supportingFiles.add(new SupportingFile("cucumber" + File.separator + "cucumber.mustache",
                (("src.test.resources") + File.separator + this.basePackage).replace(".", File.separator)
                        + File.separator + "cucumber",
                serviceName + ".feature"));

        this.supportingFiles.add(new SupportingFile("testresources" + File.separator + "bootstrap.mustache",
                ("src.test.resources").replace(".", java.io.File.separator), "bootstrap.yml"));
        this.supportingFiles.add(new SupportingFile("testresources" + File.separator + "application.mustache",
                ("src.test.resources").replace(".", java.io.File.separator), "application.properties"));
        this.supportingFiles.add(new SupportingFile("testresources" + File.separator + "application-test.mustache",
                ("src.test.resources").replace(".", java.io.File.separator), "application-test.properties"));

        // Gatling
        this.supportingFiles.add(new SupportingFile("gatling" + File.separator + "gatling.mustache",
                ("src.test.resources").replace(".", java.io.File.separator), "gatling.conf"));

        this.supportingFiles.add(new SupportingFile("gatling" + File.separator + "application.mustache",
                ("src.test.resources").replace(".", java.io.File.separator), "application.conf"));

        this.supportingFiles.add(new SupportingFile(
                "gatling" + File.separator + "testapi.mustache", ("src") + File.separator + ("test") + File.separator
                + ("scala") + File.separator + ("scalaFiles").replace(".", java.io.File.separator),
                "testapi.scala"));

        // adding class for integration test
        this.supportingFiles.add(new SupportingFile(
                "integration" + File.separator + "integrationtest.mustache", this.testFolder + File.separator
                + this.basePackage.replace(".", File.separator) + File.separator + "controller",
                serviceName + "IT.java"));
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationMap operations = objs.getOperations();
        if (operations != null) {
            List<CodegenOperation> ops = operations.getOperation();
            for (final CodegenOperation operation : ops) {
                List<CodegenResponse> responses = operation.responses;
                if (responses != null) {
                    for (final CodegenResponse resp : responses) {
                        if ("0".equals(resp.code)) {
                            resp.code = "200";
                        }
                        doDataTypeAssignment(resp.dataType, new DataTypeAssigner() {

                            @Override
                            public void setReturnType(final String returnType) {
                                resp.dataType = returnType;
                            }

                            @Override
                            public void setReturnContainer(final String returnContainer) {
                                resp.containerType = returnContainer;
                            }
                        });
                    }
                }

                doDataTypeAssignment(operation.returnType, new DataTypeAssigner() {

                    @Override
                    public void setReturnType(final String returnType) {
                        operation.returnType = returnType;
                    }

                    @Override
                    public void setReturnContainer(final String returnContainer) {
                        operation.returnContainer = returnContainer;
                    }
                });

                handleImplicitHeaders(operation);
            }
        }

        return objs;
    }

    /**
     * @param returnType       The return type that needs to be converted
     * @param dataTypeAssigner An object that will assign the data to the respective fields
     *                         in the model.
     */
    private void doDataTypeAssignment(String returnType, DataTypeAssigner dataTypeAssigner) {
        final String rt = returnType;
        if (rt == null) {
            dataTypeAssigner.setReturnType("Void");
        } else if (rt.startsWith("List")) {
            int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("List<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("List");
            }
        } else if (rt.startsWith("Map")) {
            int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("Map<".length(), end).split(",")[1].trim());
                dataTypeAssigner.setReturnContainer("Map");
            }
        } else if (rt.startsWith("Set")) {
            int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("Set<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("Set");
            }
        }
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        if ("null".equals(property.example)) {
            property.example = null;
        }

        // Add imports for Jackson
        if (!Boolean.TRUE.equals(model.isEnum)) {
            model.imports.add("JsonProperty");

            if (Boolean.TRUE.equals(model.hasEnums)) {
                model.imports.add("JsonValue");
            }
        } else { // enum class
            // Needed imports for Jackson's JsonCreator
            if (isJackson()) {
                model.imports.add("JsonCreator");
            }
        }

    }

    @Override
    public ModelsMap postProcessModelsEnum(ModelsMap objs) {
        objs = super.postProcessModelsEnum(objs);

        // Add imports for Jackson
        List<Map<String, String>> imports = objs.getImports();
        for (ModelMap mo : objs.getModels()) {
            CodegenModel cm = mo.getModel();
            // for enum model
            if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                cm.imports.add(this.importMapping.get("JsonValue"));
                Map<String, String> item = new HashMap<>();
                item.put("import", this.importMapping.get("JsonValue"));
                imports.add(item);
            }
        }

        return objs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        if (openAPI == null || openAPI.getPaths() == null) {
            return;
        }
        if (openAPI.getTags() != null) {
            List<ResourcePath> resourcePaths = new ArrayList<>();
            for (Tag tag : openAPI.getTags()) {
                ResourcePath resourcePath = new ResourcePath();
                resourcePath.setPath(tag.getName());
                resourcePaths.add(resourcePath);
            }
            this.additionalProperties.put("resourcePaths", resourcePaths);
        }
        // get vendor extensions

        Map<String, Object> vendorExt = openAPI.getInfo().getExtensions();
        if (vendorExt != null && !vendorExt.toString().isEmpty()) {
            if (vendorExt.containsKey("x-codegen")) {

                Map<String, String> uris = (Map<String, String>) vendorExt.get("x-codegen");
                if (uris.containsKey("eurekaUri")) {
                    String eurekaUri = uris.get("eurekaUri");
                    additionalProperties.put(EUREKA_URI, eurekaUri);
                }
                if (uris.containsKey("zipkinUri")) {
                    String zipkinUri = uris.get("zipkinUri");
                    additionalProperties.put(ZIPKIN_URI, zipkinUri);
                }
                if (uris.containsKey("springBootAdminUri")) {
                    String springBootAdminUri = uris.get("springBootAdminUri");
                    additionalProperties.put(SPRINGADMIN_URI, springBootAdminUri);
                }
                if (uris.containsKey("pkmstInterceptor")) {
                    String pkmstInterceptor = uris.get("pkmstInterceptor");
                    additionalProperties.put("pkmstInterceptor", pkmstInterceptor);
                }
            }
        }

        /* comment out below as it's already done in AbstractJavaCodegen
        for (String pathname : openAPI.getPaths().keySet()) {
            PathItem path = openAPI.getPaths().get(pathname);
            if (path.readOperations() == null) {
                continue;
            }
            for (Operation operation : path.readOperations()) {
                boolean hasFormParameters = hasFormParameter(operation);

                // only add content-Type if its no a GET-Method
                if (path.getGet() != null || !operation.equals(path.getGet())) {
                    String defaultContentType = hasFormParameters ? "application/x-www-form-urlencoded"
                            : "application/json";
                    List<String> consumes = new ArrayList<String>(getConsumesInfo(operation));
                    String contentType = consumes == null || consumes.isEmpty() ? defaultContentType : consumes.get(0);
                    operation.addExtension("x-contentType", contentType);
                }
                String accepts = getAccept(operation);
                operation.addExtension("x-accepts", accepts);
            }
        }*/

        /* TODO the following logic shouldn't need any more
        if ("/".equals(openAPI.getBasePath())) {
            openAPI.setBasePath("");
        }
        */

        if (!additionalProperties.containsKey(TITLE)) {
            // From the title, compute a reasonable name for the package and the
            // API
            String title = openAPI.getInfo().getTitle();

            // Drop any API suffix
            if (title != null) {
                title = title.trim().replace(" ", "-");
                if (title.toUpperCase(Locale.ROOT).endsWith("API")) {
                    title = title.substring(0, title.length() - 3);
                }

                this.title = camelize(sanitizeName(title), LOWERCASE_FIRST_LETTER);
            }
            additionalProperties.put(TITLE, this.title);
        }

        URL url = URLPathUtils.getServerURL(openAPI, serverVariableOverrides());
        this.additionalProperties.put("serverPort", URLPathUtils.getPort(url, 8080));

        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> openAPIGetPathsEntry : openAPI.getPaths().entrySet()) {
                String pathname = openAPIGetPathsEntry.getKey();
                PathItem path = openAPIGetPathsEntry.getValue();
                if (path.readOperations() != null) {
                    for (Operation operation : path.readOperations()) {
                        if (operation.getTags() != null) {
                            List<Map<String, String>> tags = new ArrayList<Map<String, String>>();
                            for (String tag : operation.getTags()) {
                                Map<String, String> value = new HashMap<String, String>();
                                value.put("tag", tag);
                                tags.add(value);
                            }
                            if (operation.getTags().size() > 0) {
                                String tag = operation.getTags().get(0);
                                operation.setTags(Arrays.asList(tag));
                            }
                            operation.addExtension("x-tags", tags);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
                                    Map<String, List<CodegenOperation>> operations) {
        super.addOperationToGroup(tag, resourcePath, operation, co, operations);
        co.subresourceOperation = !co.path.isEmpty();
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        name = sanitizeName(name);
        return camelize(name) + "Api";
    }

    @Override
    public String apiFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + apiPackage().replace(".", "/");
    }

    @Override
    public String apiTestFileFolder() {
        return this.outputFolder + "/" + this.testFolder + "/" + apiPackage().replace(".", "/");
    }

    @Override
    public String modelFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + modelPackage().replace(".", "/");
    }

    @Override
    public String apiDocFileFolder() {
        return (this.outputFolder + "/" + this.apiDocPath).replace("/", File.separator);
    }

    @Override
    public String modelDocFileFolder() {
        return (this.outputFolder + "/" + this.modelDocPath).replace("/", File.separator);
    }

    private interface DataTypeAssigner {

        void setReturnType(String returnType);

        void setReturnContainer(String returnContainer);
    }

    @Getter
    @Setter
    private static class ResourcePath {

        private String path;

        @Override
        public String toString() {
            return this.path;
        }
    }
}
