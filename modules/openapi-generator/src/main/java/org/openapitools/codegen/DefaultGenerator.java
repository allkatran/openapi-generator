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

package org.openapitools.codegen;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.api.*;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.ignore.CodegenIgnoreProcessor;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.*;
import org.openapitools.codegen.serializer.SerializerUtils;
import org.openapitools.codegen.templating.CommonTemplateContentLocator;
import org.openapitools.codegen.templating.GeneratorTemplateContentLocator;
import org.openapitools.codegen.templating.MustacheEngineAdapter;
import org.openapitools.codegen.templating.TemplateManagerOptions;
import org.openapitools.codegen.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.openapitools.codegen.utils.OnceLogger.once;

@SuppressWarnings("rawtypes")
public class DefaultGenerator implements Generator {
    private static final String METADATA_DIR = ".openapi-generator";
    protected final Logger LOGGER = LoggerFactory.getLogger(DefaultGenerator.class);
    private final boolean dryRun;
    protected CodegenConfig config;
    protected ClientOptInput opts;
    protected OpenAPI openAPI;
    protected CodegenIgnoreProcessor ignoreProcessor;
    private Boolean generateApis = null;
    private Boolean generateModels = null;
    private Boolean generateRecursiveDependentModels = null;
    private Boolean generateSupportingFiles = null;
    private Boolean generateWebhooks = null;
    private Boolean generateApiTests = null;
    private Boolean generateApiDocumentation = null;
    private Boolean generateModelTests = null;
    private Boolean generateModelDocumentation = null;
    private Boolean generateMetadata = true;
    private String basePath;
    private String basePathWithoutHost;
    private String contextPath;
    private Map<String, String> generatorPropertyDefaults = new HashMap<>();
    /**
     * Retrieves an instance to the configured template processor, available after user-defined options are
     * applied via
     */
    @Getter protected TemplateProcessor templateProcessor = null;

    private List<TemplateDefinition> userDefinedTemplates = new ArrayList<>();
    private String generatorCheck = "spring";
    private String templateCheck = "apiController.mustache";


    public DefaultGenerator() {
        this(false);
    }

    public DefaultGenerator(Boolean dryRun) {
        this.dryRun = Boolean.TRUE.equals(dryRun);
        LOGGER.info("Generating with dryRun={}", this.dryRun);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Generator opts(ClientOptInput opts) {
        this.opts = opts;
        this.openAPI = opts.getOpenAPI();
        this.config = opts.getConfig();

        List<TemplateDefinition> userFiles = opts.getUserDefinedTemplates();
        if (userFiles != null) {
            this.userDefinedTemplates = Collections.unmodifiableList(userFiles);
        }

        TemplateManagerOptions templateManagerOptions = new TemplateManagerOptions(this.config.isEnableMinimalUpdate(), this.config.isSkipOverwrite());

        if (this.dryRun) {
            this.templateProcessor = new DryRunTemplateManager(templateManagerOptions);
        } else {
            TemplatingEngineAdapter templatingEngine = this.config.getTemplatingEngine();

            if (templatingEngine instanceof MustacheEngineAdapter) {
                MustacheEngineAdapter mustacheEngineAdapter = (MustacheEngineAdapter) templatingEngine;
                mustacheEngineAdapter.setCompiler(this.config.processCompiler(mustacheEngineAdapter.getCompiler()));
            }

            TemplatePathLocator commonTemplateLocator = new CommonTemplateContentLocator();
            TemplatePathLocator generatorTemplateLocator = new GeneratorTemplateContentLocator(this.config);
            this.templateProcessor = new TemplateManager(
                    templateManagerOptions,
                    templatingEngine,
                    new TemplatePathLocator[]{generatorTemplateLocator, commonTemplateLocator}
            );
        }

        String ignoreFileLocation = this.config.getIgnoreFilePathOverride();
        if (ignoreFileLocation != null) {
            final File ignoreFile = new File(ignoreFileLocation);
            if (ignoreFile.exists() && ignoreFile.canRead()) {
                this.ignoreProcessor = new CodegenIgnoreProcessor(ignoreFile);
            } else {
                LOGGER.warn("Ignore file specified at {} is not valid. This will fall back to an existing ignore file if present in the output directory.", ignoreFileLocation);
            }
        }

        if (this.ignoreProcessor == null) {
            this.ignoreProcessor = new CodegenIgnoreProcessor(this.config.getOutputDir());
        }

        return this;
    }

    /**
     * Programmatically disable the output of .openapi-generator/VERSION, .openapi-generator-ignore,
     * or other metadata files used by OpenAPI Generator.
     *
     * @param generateMetadata true: enable outputs, false: disable outputs
     */
    @SuppressWarnings("WeakerAccess")
    public void setGenerateMetadata(Boolean generateMetadata) {
        this.generateMetadata = generateMetadata;
    }

    /**
     * Set generator properties otherwise pulled from system properties.
     * Useful for running tests in parallel without relying on System.properties.
     *
     * @param key   The system property key
     * @param value The system property value
     */
    @SuppressWarnings("WeakerAccess")
    public void setGeneratorPropertyDefault(final String key, final String value) {
        this.generatorPropertyDefaults.put(key, value);
    }

    private Boolean getGeneratorPropertyDefaultSwitch(final String key, final Boolean defaultValue) {
        String result = null;
        if (this.generatorPropertyDefaults.containsKey(key)) {
            result = this.generatorPropertyDefaults.get(key);
        }
        if (result != null) {
            return Boolean.valueOf(result);
        }
        return defaultValue;
    }

    void configureGeneratorProperties() {
        // allows generating only models by specifying a CSV of models to generate, or empty for all
        // NOTE: Boolean.TRUE is required below rather than `true` because of JVM boxing constraints and type inference.
        generateApis = GlobalSettings.getProperty(CodegenConstants.APIS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.APIS, null);
        generateModels = GlobalSettings.getProperty(CodegenConstants.MODELS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODELS, null);
        generateSupportingFiles = GlobalSettings.getProperty(CodegenConstants.SUPPORTING_FILES) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.SUPPORTING_FILES, null);
        generateWebhooks = GlobalSettings.getProperty(CodegenConstants.WEBHOOKS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.WEBHOOKS, null);

        if (generateApis == null && generateModels == null && generateSupportingFiles == null && generateWebhooks == null) {
            // no specifics are set, generate everything
            generateApis = generateModels = generateSupportingFiles = generateWebhooks = true;
        } else {
            if (generateApis == null) {
                generateApis = false;
            }
            if (generateModels == null) {
                generateModels = false;
            }
            if (generateSupportingFiles == null) {
                generateSupportingFiles = false;
            }
            if (generateWebhooks == null) {
                generateWebhooks = false;
            }
        }
        // model/api tests and documentation options rely on parent generate options (api or model) and no other options.
        // They default to true in all scenarios and can only be marked false explicitly
        generateModelTests = GlobalSettings.getProperty(CodegenConstants.MODEL_TESTS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.MODEL_TESTS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODEL_TESTS, true);
        generateModelDocumentation = GlobalSettings.getProperty(CodegenConstants.MODEL_DOCS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.MODEL_DOCS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODEL_DOCS, true);
        generateApiTests = GlobalSettings.getProperty(CodegenConstants.API_TESTS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.API_TESTS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.API_TESTS, true);
        generateApiDocumentation = GlobalSettings.getProperty(CodegenConstants.API_DOCS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.API_DOCS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.API_DOCS, true);
        generateRecursiveDependentModels = GlobalSettings.getProperty(CodegenConstants.GENERATE_RECURSIVE_DEPENDENT_MODELS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.GENERATE_RECURSIVE_DEPENDENT_MODELS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.GENERATE_RECURSIVE_DEPENDENT_MODELS, false);

        // Additional properties added for tests to exclude references in project related files
        config.additionalProperties().put(CodegenConstants.GENERATE_API_TESTS, generateApiTests);
        config.additionalProperties().put(CodegenConstants.GENERATE_MODEL_TESTS, generateModelTests);

        config.additionalProperties().put(CodegenConstants.GENERATE_API_DOCS, generateApiDocumentation);
        config.additionalProperties().put(CodegenConstants.GENERATE_MODEL_DOCS, generateModelDocumentation);

        config.additionalProperties().put(CodegenConstants.GENERATE_APIS, generateApis);
        config.additionalProperties().put(CodegenConstants.GENERATE_MODELS, generateModels);
        config.additionalProperties().put(CodegenConstants.GENERATE_WEBHOOKS, generateWebhooks);
        config.additionalProperties().put(CodegenConstants.GENERATE_RECURSIVE_DEPENDENT_MODELS, generateRecursiveDependentModels);

        if (!generateApiTests && !generateModelTests) {
            config.additionalProperties().put(CodegenConstants.EXCLUDE_TESTS, true);
        }

        if (GlobalSettings.getProperty("debugOpenAPI") != null) {
            System.out.println(SerializerUtils.toJsonString(openAPI));
        } else if (GlobalSettings.getProperty("debugSwagger") != null) {
            // This exists for backward compatibility
            // We fall to this block only if debugOpenAPI is null. No need to dump this twice.
            LOGGER.info("Please use system property 'debugOpenAPI' instead of 'debugSwagger'.");
            System.out.println(SerializerUtils.toJsonString(openAPI));
        }

        config.processOpts();
        if (opts != null && opts.getGeneratorSettings() != null) {
            config.typeMapping().putAll(opts.getGeneratorSettings().getTypeMappings());
            config.importMapping().putAll(opts.getGeneratorSettings().getImportMappings());
        }

        // normalize the spec
        try {
            if (config.getUseOpenapiNormalizer()) {
                SemVer version = new SemVer(openAPI.getOpenapi());
                if (version.atLeast("3.1.0")) {
                    config.openapiNormalizer().put("NORMALIZE_31SPEC", "true");
                }
                OpenAPINormalizer openapiNormalizer = OpenAPINormalizer.createNormalizer(openAPI, config.openapiNormalizer());
                openapiNormalizer.normalize();
            }
        } catch (Exception e) {
            LOGGER.error("An exception occurred in OpenAPI Normalizer. Please report the issue via https://github.com/openapitools/openapi-generator/issues/new/: ");
            e.printStackTrace();
        }

        // resolve inline models
        if (config.getUseInlineModelResolver()) {
            InlineModelResolver inlineModelResolver = new InlineModelResolver();
            inlineModelResolver.setInlineSchemaNameMapping(config.inlineSchemaNameMapping());
            inlineModelResolver.setInlineSchemaOptions(config.inlineSchemaOption());

            inlineModelResolver.flatten(openAPI);
        }

        config.preprocessOpenAPI(openAPI);

        // set OpenAPI to make these available to all methods
        config.setOpenAPI(openAPI);

        if (!config.additionalProperties().containsKey("generatorVersion")) {
            config.additionalProperties().put("generatorVersion", ImplementationVersion.read());
        }
        config.additionalProperties().put("generatedDate", ZonedDateTime.now().toString());
        config.additionalProperties().put("generatedYear", String.valueOf(ZonedDateTime.now().getYear()));
        config.additionalProperties().put("generatorClass", config.getClass().getName());
        config.additionalProperties().put("inputSpec", config.getInputSpec());

        if (openAPI.getExtensions() != null) {
            config.vendorExtensions().putAll(openAPI.getExtensions());
        }

        // TODO: Allow user to define _which_ servers object in the array to target.
        // Configures contextPath/basePath according to api document's servers
        URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());
        contextPath = removeTrailingSlash(config.escapeText(url.getPath())); // for backward compatibility
        basePathWithoutHost = contextPath;
        if (URLPathUtils.isRelativeUrl(openAPI.getServers())) {
            basePath = removeTrailingSlash(basePathWithoutHost);
        } else {
            basePath = removeTrailingSlash(config.escapeText(URLPathUtils.getHost(openAPI, config.serverVariableOverrides())));
        }
    }

    private void configureOpenAPIInfo() {
        Info info = this.openAPI.getInfo();
        if (info == null) {
            return;
        }
        if (info.getTitle() != null) {
            config.additionalProperties().put("appName", config.escapeText(info.getTitle()));
        }
        if (info.getVersion() != null) {
            config.additionalProperties().put("appVersion", config.escapeText(info.getVersion()));
        } else {
            LOGGER.error("Missing required field info version. Default appVersion set to 1.0.0");
            config.additionalProperties().put("appVersion", "1.0.0");
        }

        if (StringUtils.isEmpty(info.getDescription())) {
            // set a default description if none if provided
            config.additionalProperties().put("appDescription",
                    "No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)");
            config.additionalProperties().put("appDescriptionWithNewLines", config.additionalProperties().get("appDescription"));
            config.additionalProperties().put("unescapedAppDescription", "No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)");
        } else {
            config.additionalProperties().put("appDescription", config.escapeText(info.getDescription()));
            config.additionalProperties().put("appDescriptionWithNewLines", config.escapeTextWhileAllowingNewLines(info.getDescription()));
            config.additionalProperties().put("unescapedAppDescription", info.getDescription());
        }

        if (this.openAPI.getSpecVersion().equals(SpecVersion.V31) && !StringUtils.isEmpty(info.getSummary())) {
            config.additionalProperties().put("appSummary", config.escapeText(info.getSummary()));
            config.additionalProperties().put("appSummaryWithNewLines", config.escapeTextWhileAllowingNewLines(info.getSummary()));
            config.additionalProperties().put("unescapedAppSummary", info.getSummary());
        }

        if (info.getContact() != null) {
            Contact contact = info.getContact();
            if (contact.getEmail() != null) {
                config.additionalProperties().put("infoEmail", config.escapeText(contact.getEmail()));
            }
            if (contact.getName() != null) {
                config.additionalProperties().put("infoName", config.escapeText(contact.getName()));
            }
            if (contact.getUrl() != null) {
                config.additionalProperties().put("infoUrl", config.escapeText(contact.getUrl()));
            }
        }

        if (info.getLicense() != null) {
            License license = info.getLicense();
            if (license.getName() != null) {
                config.additionalProperties().put("licenseInfo", config.escapeText(license.getName()));
            }
            if (license.getUrl() != null) {
                config.additionalProperties().put("licenseUrl", config.escapeText(license.getUrl()));
            }
        }

        if (info.getVersion() != null) {
            config.additionalProperties().put("version", config.escapeText(info.getVersion()));
        } else {
            LOGGER.error("Missing required field info version. Default version set to 1.0.0");
            config.additionalProperties().put("version", "1.0.0");
        }

        if (info.getTermsOfService() != null) {
            config.additionalProperties().put("termsOfService", config.escapeText(info.getTermsOfService()));
        }
    }

    private void generateModelTests(List<File> files, Map<String, Object> models, String modelName) throws IOException {
        // to generate model test files
        for (Map.Entry<String, String> configModelTestTemplateFilesEntry : config.modelTestTemplateFiles().entrySet()) {
            String templateName = configModelTestTemplateFilesEntry.getKey();
            String suffix = configModelTestTemplateFilesEntry.getValue();
            String filename = config.modelTestFileFolder() + File.separator + config.toModelTestFilename(modelName) + suffix;

            if (generateModelTests) {
                // do not overwrite test file that already exists (regardless of config's skipOverwrite setting)
                File modelTestFile = new File(filename);
                if (modelTestFile.exists()) {
                    this.templateProcessor.skip(modelTestFile.toPath(), "Test files never overwrite an existing file of the same name.");
                } else {
                    File written = processTemplateToFile(models, templateName, filename, generateModelTests, CodegenConstants.MODEL_TESTS, config.modelTestFileFolder());
                    if (written != null) {
                        files.add(written);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(written, "model-test");
                        }
                    }
                }
            } else if (dryRun) {
                Path skippedPath = java.nio.file.Paths.get(filename);
                this.templateProcessor.skip(skippedPath, "Skipped by modelTests option supplied by user.");
            }
        }
    }

    private void generateModelDocumentation(List<File> files, Map<String, Object> models, String modelName) throws IOException {
        for (String templateName : config.modelDocTemplateFiles().keySet()) {
            String docExtension = config.getDocExtension();
            String suffix = docExtension != null ? docExtension : config.modelDocTemplateFiles().get(templateName);
            String filename = config.modelDocFileFolder() + File.separator + config.toModelDocFilename(modelName) + suffix;

            File written = processTemplateToFile(models, templateName, filename, generateModelDocumentation, CodegenConstants.MODEL_DOCS);
            if (written != null) {
                files.add(written);
                if (config.isEnablePostProcessFile() && !dryRun) {
                    config.postProcessFile(written, "model-doc");
                }
            }
        }
    }

    private void generateModel(List<File> files, Map<String, Object> models, String modelName) throws IOException {
        for (String templateName : config.modelTemplateFiles().keySet()) {
            File written;
            if (config.templateOutputDirs().containsKey(templateName)) {
                String outputDir = config.getOutputDir() + File.separator + config.templateOutputDirs().get(templateName);
                String filename = config.modelFilename(templateName, modelName, outputDir);
                written = processTemplateToFile(models, templateName, filename, generateModels, CodegenConstants.MODELS, outputDir);
            } else {
                String filename = config.modelFilename(templateName, modelName);
                written = processTemplateToFile(models, templateName, filename, generateModels, CodegenConstants.MODELS);
            }
            if (written != null) {
                files.add(written);
                if (config.isEnablePostProcessFile() && !dryRun) {
                    config.postProcessFile(written, "model");
                }
            }
        }
    }

    void generateModels(List<File> files, List<ModelMap> allModels, List<String> unusedModels, List<ModelMap> aliasModels) {
        generateModels(files, allModels, unusedModels, aliasModels, new ArrayList<>(), DefaultGenerator.this::modelKeys);
    }

    void generateModels(List<File> files, List<ModelMap> allModels, List<String> unusedModels, List<ModelMap> aliasModels, List<String> processedModels, Supplier<Set<String>> modelKeysSupplier) {
        if (!generateModels) {
            // TODO: Process these anyway and add to dryRun info
            LOGGER.info("Skipping generation of models.");
            return;
        }

        Set<String> modelKeys = modelKeysSupplier.get();
        if (modelKeys.isEmpty()) {
            return;
        }

        // store all processed models
        Map<String, ModelsMap> allProcessedModels = new TreeMap<>((o1, o2) -> ObjectUtils.compare(config.toModelName(o1), config.toModelName(o2)));

        Boolean skipFormModel = GlobalSettings.getProperty(CodegenConstants.SKIP_FORM_MODEL) != null ?
                Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.SKIP_FORM_MODEL)) :
                getGeneratorPropertyDefaultSwitch(CodegenConstants.SKIP_FORM_MODEL, true);

        // process models only
        for (String name : modelKeys) {
            processedModels.add(name);
            try {
                //don't generate models that have an import mapping
                if (config.schemaMapping().containsKey(name)) {
                    LOGGER.info("Model {} not generated due to schema mapping", name);
                    continue;
                }

                // don't generate models that are not used as object (e.g. form parameters)
                if (unusedModels.contains(name)) {
                    if (Boolean.FALSE.equals(skipFormModel)) {
                        // if skipFormModel sets to true, still generate the model and log the result
                        LOGGER.info("Model {} (marked as unused due to form parameters) is generated due to the global property `skipFormModel` set to false", name);
                    } else {
                        LOGGER.info("Model {} not generated since it's marked as unused (due to form parameters) and `skipFormModel` (global property) set to true (default)", name);
                        // TODO: Should this be added to dryRun? If not, this seems like a weird place to return early from processing.
                        continue;
                    }
                }

                Schema schema = ModelUtils.getSchemas(this.openAPI).get(name);

                if (schema.getExtensions() != null && Boolean.TRUE.equals(schema.getExtensions().get("x-internal"))) {
                    LOGGER.info("Model {} not generated since x-internal is set to true", name);
                    continue;
                } else if (ModelUtils.isFreeFormObject(schema, openAPI)) { // check to see if it's a free-form object
                    if (!ModelUtils.shouldGenerateFreeFormObjectModel(name, config)) {
                        LOGGER.info("Model {} not generated since it's a free-form object", name);
                        continue;
                    }
                } else if (ModelUtils.isMapSchema(schema)) { // check to see if it's a "map" model
                    if (!ModelUtils.shouldGenerateMapModel(schema)) {
                        // schema without property, i.e. alias to map
                        LOGGER.info("Model {} not generated since it's an alias to map (without property) and `generateAliasAsModel` is set to false (default)", name);
                        continue;
                    }
                } else if (ModelUtils.isArraySchema(schema)) { // check to see if it's an "array" model
                    if (!ModelUtils.shouldGenerateArrayModel(schema)) {
                        // schema without property, i.e. alias to array
                        LOGGER.info("Model {} not generated since it's an alias to array (without property) and `generateAliasAsModel` is set to false (default)", name);
                        continue;
                    }
                }

                Map<String, Schema> schemaMap = new HashMap<>();
                schemaMap.put(name, schema);
                ModelsMap models = processModels(config, schemaMap);
                models.put("classname", config.toModelName(name));
                models.putAll(config.additionalProperties());
                allProcessedModels.put(name, models);
            } catch (Exception e) {
                throw new RuntimeException("Could not process model '" + name + "'" + ".Please make sure that your schema is correct!", e);
            }
        }

        // loop through all models to update children models, isSelfReference, isCircularReference, etc
        allProcessedModels = config.updateAllModels(allProcessedModels);

        // post process all processed models
        allProcessedModels = config.postProcessAllModels(allProcessedModels);

        if (generateRecursiveDependentModels) {
            for (ModelsMap modelsMap : allProcessedModels.values()) {
                for (ModelMap mm : modelsMap.getModels()) {
                    CodegenModel cm = mm.getModel();
                    if (cm != null) {
                        for (CodegenProperty variable : cm.getVars()) {
                            generateModelsForVariable(files, allModels, unusedModels, aliasModels, processedModels, variable);
                        }
                        //TODO:  handle interfaces
                        String parentSchema = cm.getParentSchema();
                        if (parentSchema != null && !processedModels.contains(parentSchema) && ModelUtils.getSchemas(this.openAPI).containsKey(parentSchema)) {
                            generateModels(files, allModels, unusedModels, aliasModels, processedModels, () -> Set.of(parentSchema));
                        }
                    }
                }
            }
        }

        // generate files based on processed models
        for (String modelName : allProcessedModels.keySet()) {
            ModelsMap models = allProcessedModels.get(modelName);
            models.put("modelPackage", config.modelPackage());
            try {
                //don't generate models that have a schema mapping
                if (config.schemaMapping().containsKey(modelName)) {
                    continue;
                }

                // TODO revise below as we've already performed unaliasing so that the isAlias check may be removed
                List<ModelMap> modelList = models.getModels();
                if (modelList != null && !modelList.isEmpty()) {
                    ModelMap modelTemplate = modelList.get(0);
                    if (modelTemplate != null && modelTemplate.getModel() != null) {
                        CodegenModel m = modelTemplate.getModel();
                        if (m.isAlias) {
                            // alias to number, string, enum, etc, which should not be generated as model
                            // but aliases are still used to dereference models in some languages (such as in html2).
                            aliasModels.add(modelTemplate);  // Store aliases in the separate list.
                            continue;  // Don't create user-defined classes for aliases
                        }
                    }
                    allModels.add(modelTemplate);
                }

                // to generate model files
                generateModel(files, models, modelName);

                // to generate model test files
                generateModelTests(files, models, modelName);

                // to generate model documentation files
                generateModelDocumentation(files, models, modelName);

            } catch (Exception e) {
                throw new RuntimeException("Could not generate model '" + modelName + "'", e);
            }
        }
        if (GlobalSettings.getProperty("debugModels") != null) {
            LOGGER.info("############ Model info ############");
            Json.prettyPrint(allModels);
        }
    }

    /**
     * this method guesses the schema type of in parent model used variable and if the schema type is available it let the generate the model for the type of this variable
     */
    private void generateModelsForVariable(List<File> files, List<ModelMap> allModels, List<String> unusedModels, List<ModelMap> aliasModels, List<String> processedModels, CodegenProperty variable) {
        if (variable == null) {
            return;
        }

        final String schemaKey = calculateModelKey(variable.getOpenApiType(), variable.getRef());
        Map<String, Schema> allSchemas = ModelUtils.getSchemas(this.openAPI);
        if (!processedModels.contains(schemaKey) && allSchemas.containsKey(schemaKey)) {
            generateModels(files, allModels, unusedModels, aliasModels, processedModels, () -> Set.of(schemaKey));
        } else if (variable.getComplexType() != null && variable.getComposedSchemas() == null) {
            String ref = variable.getHasItems() ? variable.getItems().getRef() : variable.getRef();
            final String key = calculateModelKey(variable.getComplexType(), ref);
            if (!processedModels.contains(key) && allSchemas.containsKey(key)) {
                generateModels(files, allModels, unusedModels, aliasModels, processedModels, () -> Set.of(key));
            } else {
                LOGGER.info("Type " + variable.getComplexType() + " of variable " + variable.getName() + " could not be resolve because it is not declared as a model.");
            }
        } else {
            LOGGER.info("Type " + variable.getOpenApiType() + " of variable " + variable.getName() + " could not be resolve because it is not declared as a model.");
        }
    }

    private String calculateModelKey(String type, String ref) {
        Map<String, Schema> schemaMap = ModelUtils.getSchemas(this.openAPI);
        Set<String> keys = schemaMap.keySet();
        String simpleRef;
        if (keys.contains(type)) {
            return type;
        } else if (keys.contains(simpleRef = ModelUtils.getSimpleRef(ref))) {
            return simpleRef;
        } else {
            return type;
        }
    }

    /**
     * this method splits the specified property by commas, trims any results for spaces and
     * newlines, and returns them as a Set of Strings. the method will return an empty
     * set if the specified property has not been set or is an empty string.
     */
    private Set<String> getPropertyAsSet(String propertyName) {
        String propertyRaw = GlobalSettings.getProperty(propertyName);
        if (propertyRaw == null || propertyRaw.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(propertyRaw.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    private Set<String> modelKeys() {
        final Map<String, Schema> schemas = ModelUtils.getSchemas(this.openAPI);
        if (schemas == null) {
            LOGGER.warn("Skipping generation of models because specification document has no schemas.");
            return Collections.emptySet();
        }

        Set<String> modelsToGenerate = getPropertyAsSet(CodegenConstants.MODELS);
        Set<String> modelKeys = schemas.keySet();
        if (modelsToGenerate != null && !modelsToGenerate.isEmpty()) {
            Set<String> updatedKeys = new HashSet<>();
            for (String m : modelKeys) {
                if (modelsToGenerate.contains(m)) {
                    updatedKeys.add(m);
                }
            }

            modelKeys = updatedKeys;
        }
        return modelKeys;
    }

    @SuppressWarnings("unchecked")
    void generateApis(List<File> files, List<OperationsMap> allOperations, List<ModelMap> allModels) {
        if (!generateApis) {
            // TODO: Process these anyway and present info via dryRun?
            LOGGER.info("Skipping generation of APIs.");
            return;
        }
        Map<String, List<CodegenOperation>> paths = processPaths(this.openAPI.getPaths());
        Set<String> apisToGenerate = getPropertyAsSet(CodegenConstants.APIS);
        if (apisToGenerate != null && !apisToGenerate.isEmpty()) {
            Map<String, List<CodegenOperation>> updatedPaths = new TreeMap<>();
            for (String m : paths.keySet()) {
                if (apisToGenerate.contains(m)) {
                    updatedPaths.put(m, paths.get(m));
                }
            }
            paths = updatedPaths;
        }
        for (String tag : paths.keySet()) {
            try {
                List<CodegenOperation> ops = paths.get(tag);
                if (!this.config.isSkipSortingOperations()) {
                    // sort operations by operationId
                    ops.sort((one, another) -> ObjectUtils.compare(one.operationId, another.operationId));
                }
                OperationsMap operation = processOperations(config, tag, ops, allModels);
                URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());
                operation.put("basePath", basePath);
                operation.put("basePathWithoutHost", removeTrailingSlash(config.encodePath(url.getPath())));
                operation.put("contextPath", contextPath);
                operation.put("baseName", tag);
                Optional.ofNullable(openAPI.getTags()).orElseGet(Collections::emptyList).stream()
                        .map(Tag::getName)
                        .filter(Objects::nonNull)
                        .filter(tag::equalsIgnoreCase)
                        .findFirst()
                        .ifPresent(tagName -> operation.put("operationTagName", config.escapeText(tagName)));
                operation.put("operationTagDescription", "");
                Optional.ofNullable(openAPI.getTags()).orElseGet(Collections::emptyList).stream()
                        .filter(t -> tag.equalsIgnoreCase(t.getName()))
                        .map(Tag::getDescription)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(description -> operation.put("operationTagDescription", config.escapeText(description)));
                Optional.ofNullable(config.additionalProperties().get("appVersion")).ifPresent(version -> operation.put("version", version));
                operation.put("apiPackage", config.apiPackage());
                operation.put("modelPackage", config.modelPackage());
                operation.putAll(config.additionalProperties());
                operation.put("classname", config.toApiName(tag));
                operation.put("classVarName", config.toApiVarName(tag));
                operation.put("importPath", config.toApiImport(tag));
                operation.put("classFilename", config.toApiFilename(tag));
                operation.put("strictSpecBehavior", config.isStrictSpecBehavior());
                Optional.ofNullable(openAPI.getInfo()).map(Info::getLicense).ifPresent(license -> operation.put("license", license));
                Optional.ofNullable(openAPI.getInfo()).map(Info::getContact).ifPresent(contact -> operation.put("contact", contact));

                if (allModels == null || allModels.isEmpty()) {
                    operation.put("hasModel", false);
                } else {
                    operation.put("hasModel", true);
                }

                if (!config.vendorExtensions().isEmpty()) {
                    operation.put("vendorExtensions", config.vendorExtensions());
                }

                // process top-level x-group-parameters
                if (config.vendorExtensions().containsKey("x-group-parameters")) {
                    boolean isGroupParameters = Boolean.parseBoolean(config.vendorExtensions().get("x-group-parameters").toString());

                    OperationMap objectMap = operation.getOperations();
                    List<CodegenOperation> operations = objectMap.getOperation();
                    for (CodegenOperation op : operations) {
                        if (isGroupParameters && !op.vendorExtensions.containsKey("x-group-parameters")) {
                            op.vendorExtensions.put("x-group-parameters", Boolean.TRUE);
                        }
                    }
                }

                // Pass sortParamsByRequiredFlag through to the Mustache template...
                boolean sortParamsByRequiredFlag = true;
                if (this.config.additionalProperties().containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)) {
                    sortParamsByRequiredFlag = Boolean.parseBoolean(this.config.additionalProperties().get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG).toString());
                }
                operation.put("sortParamsByRequiredFlag", sortParamsByRequiredFlag);

                /* consumes, produces are no longer defined in OAS3.0
                processMimeTypes(swagger.getConsumes(), operation, "consumes");
                processMimeTypes(swagger.getProduces(), operation, "produces");
                */

                allOperations.add(operation);

                addAuthenticationSwitches(operation);

                for (String templateName : config.apiTemplateFiles().keySet()) {
                    File written = null;
                    if (config.templateOutputDirs().containsKey(templateName)) {
                        String outputDir = config.getOutputDir() + File.separator + config.templateOutputDirs().get(templateName);
                        String filename = config.apiFilename(templateName, tag, outputDir);
                        // do not overwrite apiController file for spring server
                        if (apiFilePreCheck(filename, generatorCheck, templateName, templateCheck)) {
                            written = processTemplateToFile(operation, templateName, filename, generateApis, CodegenConstants.APIS, outputDir);
                        } else {
                            LOGGER.info("Implementation file {} is not overwritten", filename);
                        }
                    } else {
                        String filename = config.apiFilename(templateName, tag);
                        if (apiFilePreCheck(filename, generatorCheck, templateName, templateCheck)) {
                            written = processTemplateToFile(operation, templateName, filename, generateApis, CodegenConstants.APIS);
                        } else {
                            LOGGER.info("Implementation file {} is not overwritten", filename);
                        }
                    }
                    if (written != null) {
                        files.add(written);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(written, "api");
                        }
                    }
                }

                // to generate api test files
                for (String templateName : config.apiTestTemplateFiles().keySet()) {
                    String filename = config.apiTestFilename(templateName, tag);
                    File apiTestFile = new File(filename);
                    // do not overwrite test file that already exists
                    if (apiTestFile.exists()) {
                        this.templateProcessor.skip(apiTestFile.toPath(), "Test files never overwrite an existing file of the same name.");
                    } else {
                        File written = processTemplateToFile(operation, templateName, filename, generateApiTests, CodegenConstants.API_TESTS, config.apiTestFileFolder());
                        if (written != null) {
                            files.add(written);
                            if (config.isEnablePostProcessFile() && !dryRun) {
                                config.postProcessFile(written, "api-test");
                            }
                        }
                    }
                }

                // to generate api documentation files
                for (String templateName : config.apiDocTemplateFiles().keySet()) {
                    String filename = config.apiDocFilename(templateName, tag);
                    File written = processTemplateToFile(operation, templateName, filename, generateApiDocumentation, CodegenConstants.API_DOCS);
                    if (written != null) {
                        files.add(written);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(written, "api-doc");
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Could not generate api file for '" + tag + "'", e);
            }
        }
        if (GlobalSettings.getProperty("debugOperations") != null) {
            LOGGER.info("############ Operation info ############");
            Json.prettyPrint(allOperations);
        }

    }

    void generateWebhooks(List<File> files, List<WebhooksMap> allWebhooks, List<ModelMap> allModels) {
        if (!generateWebhooks) {
            // TODO: Process these anyway and present info via dryRun?
            LOGGER.info("Skipping generation of Webhooks.");
            return;
        }
        Map<String, List<CodegenOperation>> webhooks = processWebhooks(this.openAPI.getWebhooks());
        Set<String> webhooksToGenerate = getPropertyAsSet(CodegenConstants.WEBHOOKS);
        if (webhooksToGenerate != null && !webhooksToGenerate.isEmpty()) {
            Map<String, List<CodegenOperation>> Webhooks = new TreeMap<>();
            for (String m : webhooks.keySet()) {
                if (webhooksToGenerate.contains(m)) {
                    Webhooks.put(m, webhooks.get(m));
                }
            }
            webhooks = Webhooks;
        }
        for (String tag : webhooks.keySet()) {
            try {
                List<CodegenOperation> wks = webhooks.get(tag);
                wks.sort((one, another) -> ObjectUtils.compare(one.operationId, another.operationId));
                WebhooksMap operation = processWebhooks(config, tag, wks, allModels);
                URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());
                operation.put("basePath", basePath);
                operation.put("basePathWithoutHost", removeTrailingSlash(config.encodePath(url.getPath())));
                operation.put("contextPath", contextPath);
                operation.put("baseName", tag);
                Optional.ofNullable(openAPI.getTags()).orElseGet(Collections::emptyList).stream()
                        .map(Tag::getName)
                        .filter(Objects::nonNull)
                        .filter(tag::equalsIgnoreCase)
                        .findFirst()
                        .ifPresent(tagName -> operation.put("operationTagName", config.escapeText(tagName)));
                operation.put("operationTagDescription", "");
                Optional.ofNullable(openAPI.getTags()).orElseGet(Collections::emptyList).stream()
                        .filter(t -> tag.equalsIgnoreCase(t.getName()))
                        .map(Tag::getDescription)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(description -> operation.put("operationTagDescription", config.escapeText(description)));
                Optional.ofNullable(config.additionalProperties().get("appVersion")).ifPresent(version -> operation.put("version", version));
                operation.put("apiPackage", config.apiPackage());
                operation.put("modelPackage", config.modelPackage());
                operation.putAll(config.additionalProperties());
                operation.put("classname", config.toApiName(tag));
                operation.put("classVarName", config.toApiVarName(tag));
                operation.put("importPath", config.toApiImport(tag));
                operation.put("classFilename", config.toApiFilename(tag));
                operation.put("strictSpecBehavior", config.isStrictSpecBehavior());
                Optional.ofNullable(openAPI.getInfo()).map(Info::getLicense).ifPresent(license -> operation.put("license", license));
                Optional.ofNullable(openAPI.getInfo()).map(Info::getContact).ifPresent(contact -> operation.put("contact", contact));

                if (allModels == null || allModels.isEmpty()) {
                    operation.put("hasModel", false);
                } else {
                    operation.put("hasModel", true);
                }

                if (!config.vendorExtensions().isEmpty()) {
                    operation.put("vendorExtensions", config.vendorExtensions());
                }

                // process top-level x-group-parameters
                if (config.vendorExtensions().containsKey("x-group-parameters")) {
                    boolean isGroupParameters = Boolean.parseBoolean(config.vendorExtensions().get("x-group-parameters").toString());

                    OperationMap objectMap = operation.getWebhooks();
                    List<CodegenOperation> operations = objectMap.getOperation();
                    for (CodegenOperation op : operations) {
                        if (isGroupParameters && !op.vendorExtensions.containsKey("x-group-parameters")) {
                            op.vendorExtensions.put("x-group-parameters", Boolean.TRUE);
                        }
                    }
                }

                // Pass sortParamsByRequiredFlag through to the Mustache template...
                boolean sortParamsByRequiredFlag = true;
                if (this.config.additionalProperties().containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)) {
                    sortParamsByRequiredFlag = Boolean.parseBoolean(this.config.additionalProperties().get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG).toString());
                }
                operation.put("sortParamsByRequiredFlag", sortParamsByRequiredFlag);

                /* consumes, produces are no longer defined in OAS3.0
                processMimeTypes(swagger.getConsumes(), operation, "consumes");
                processMimeTypes(swagger.getProduces(), operation, "produces");
                */

                allWebhooks.add(operation);

                addAuthenticationSwitches(operation);

                for (String templateName : config.apiTemplateFiles().keySet()) {
                    File written = null;
                    if (config.templateOutputDirs().containsKey(templateName)) {
                        String outputDir = config.getOutputDir() + File.separator + config.templateOutputDirs().get(templateName);
                        String filename = config.apiFilename(templateName, tag, outputDir);
                        // do not overwrite apiController file for spring server
                        if (apiFilePreCheck(filename, generatorCheck, templateName, templateCheck)) {
                            written = processTemplateToFile(operation, templateName, filename, generateWebhooks, CodegenConstants.WEBHOOKS, outputDir);
                        } else {
                            LOGGER.info("Implementation file {} is not overwritten", filename);
                        }
                    } else {
                        String filename = config.apiFilename(templateName, tag);
                        if (apiFilePreCheck(filename, generatorCheck, templateName, templateCheck)) {
                            written = processTemplateToFile(operation, templateName, filename, generateWebhooks, CodegenConstants.WEBHOOKS);
                        } else {
                            LOGGER.info("Implementation file {} is not overwritten", filename);
                        }
                    }
                    if (written != null) {
                        files.add(written);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(written, "api");
                        }
                    }
                }

                // to generate api test files
                for (String templateName : config.apiTestTemplateFiles().keySet()) {
                    String filename = config.apiTestFilename(templateName, tag);
                    File apiTestFile = new File(filename);
                    // do not overwrite test file that already exists
                    if (apiTestFile.exists()) {
                        this.templateProcessor.skip(apiTestFile.toPath(), "Test files never overwrite an existing file of the same name.");
                    } else {
                        File written = processTemplateToFile(operation, templateName, filename, generateApiTests, CodegenConstants.API_TESTS, config.apiTestFileFolder());
                        if (written != null) {
                            files.add(written);
                            if (config.isEnablePostProcessFile() && !dryRun) {
                                config.postProcessFile(written, "api-test");
                            }
                        }
                    }
                }

                // to generate api documentation files
                for (String templateName : config.apiDocTemplateFiles().keySet()) {
                    String filename = config.apiDocFilename(templateName, tag);
                    File written = processTemplateToFile(operation, templateName, filename, generateApiDocumentation, CodegenConstants.API_DOCS);
                    if (written != null) {
                        files.add(written);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(written, "api-doc");
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Could not generate api file for '" + tag + "'", e);
            }
        }
        if (GlobalSettings.getProperty("debugOperations") != null) {
            LOGGER.info("############ Operation info ############");
            Json.prettyPrint(allWebhooks);
        }

    }

    // checking if apiController file is already existed for spring generator
    private boolean apiFilePreCheck(String filename, String generator, String templateName, String apiControllerTemplate) {
        File apiFile = new File(filename);
        return !(apiFile.exists() && config.getName().equals(generator) && templateName.equals(apiControllerTemplate));
    }

    /*
     * Generate .openapi-generator-ignore if the option openapiGeneratorIgnoreFile is enabled.
     */
    private void generateOpenapiGeneratorIgnoreFile() {
        if (config.getOpenapiGeneratorIgnoreList() == null || config.getOpenapiGeneratorIgnoreList().isEmpty()) {
            return;
        }

        final String openapiGeneratorIgnore = ".openapi-generator-ignore";
        String ignoreFileNameTarget = config.outputFolder() + File.separator + openapiGeneratorIgnore;
        File ignoreFile = new File(ignoreFileNameTarget);
        // use the entries provided by the users to pre-populate .openapi-generator-ignore
        try {
            LOGGER.info("Writing file " + ignoreFileNameTarget + " (which is always overwritten when the option `openapiGeneratorIgnoreFile` is enabled.)");
            new File(config.outputFolder()).mkdirs();
            if (!ignoreFile.createNewFile()) {
                // file may already exist, do nothing
            }

            String header = String.join("\n",
                    "# IMPORTANT: this file is generated with the option `openapiGeneratorIgnoreList` enabled",
                    "# (--openapi-generator-ignore-list in CLI for example) so the entries below are pre-populated based",
                    "# on the input provided by the users and this file will be overwritten every time when the option is",
                    "# enabled (which is the exact opposite of the default behaviour to not overwrite",
                    "# .openapi-generator-ignore if the file exists).",
                    "",
                    "# OpenAPI Generator Ignore",
                    "# Generated by openapi-generator https://github.com/openapitools/openapi-generator",
                    "",
                    "# Use this file to prevent files from being overwritten by the generator.",
                    "# The patterns follow closely to .gitignore or .dockerignore.",
                    "",
                    "# As an example, the C# client generator defines ApiClient.cs.",
                    "# You can make changes and tell OpenAPI Generator to ignore just this file by uncommenting the following line:",
                    "#ApiClient.cs",
                    "",
                    "# You can match any string of characters against a directory, file or extension with a single asterisk (*):",
                    "#foo/*/qux",
                    "# The above matches foo/bar/qux and foo/baz/qux, but not foo/bar/baz/qux",
                    "",
                    "# You can recursively match patterns against a directory, file or extension with a double asterisk (**):",
                    "#foo/**/qux",
                    "# This matches foo/bar/qux, foo/baz/qux, and foo/bar/baz/qux",
                    "",
                    "# You can also negate patterns with an exclamation (!).",
                    "# For example, you can ignore all files in a docs folder with the file extension .md:",
                    "#docs/*.md",
                    "# Then explicitly reverse the ignore rule for a single file:",
                    "#!docs/README.md",
                    "",
                    "# The following entries are pre-populated based on the input obtained via",
                    "# the option `openapiGeneratorIgnoreList` (--openapi-generator-ignore-list in CLI for example).",
                    "");
            Writer fileWriter = Files.newBufferedWriter(ignoreFile.toPath(), StandardCharsets.UTF_8);
            fileWriter.write(header);
            // add entries provided by the users
            for (String entry : config.getOpenapiGeneratorIgnoreList()) {
                fileWriter.write(entry);
                fileWriter.write("\n");
            }
            fileWriter.close();
            // re-create ignore processor based on the newly-created .openapi-generator-ignore
            this.ignoreProcessor = new CodegenIgnoreProcessor(ignoreFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate .openapi-generator-ignore when the option `openapiGeneratorIgnoreList` is enabled: ", e);
        }
    }

    private void generateSupportingFiles(List<File> files, Map<String, Object> bundle) {
        if (!generateSupportingFiles) {
            // TODO: process these anyway and report via dryRun?
            LOGGER.info("Skipping generation of supporting files.");
            return;
        }

        Set<String> supportingFilesToGenerate = getPropertyAsSet(CodegenConstants.SUPPORTING_FILES);
        for (SupportingFile support : config.supportingFiles()) {
            try {
                String outputFolder = config.outputFolder();
                if (StringUtils.isNotEmpty(support.getFolder())) {
                    outputFolder += File.separator + support.getFolder();
                }
                File of = new File(outputFolder);
                String outputFilename = new File(support.getDestinationFilename()).isAbsolute() // split
                        ? support.getDestinationFilename()
                        : outputFolder + File.separator + support.getDestinationFilename().replace('/', File.separatorChar);

                if (!of.isDirectory()) {
                    // check that its not a dryrun and the files in the directory aren't ignored before we make the directory
                    if (!dryRun && ignoreProcessor.allowsFile(new File(outputFilename)) && !of.mkdirs()) {
                        once(LOGGER).debug("Output directory {} not created. It {}.", outputFolder, of.exists() ? "already exists." : "may not have appropriate permissions.");
                    }
                }


                boolean shouldGenerate = true;
                if (supportingFilesToGenerate != null && !supportingFilesToGenerate.isEmpty()) {
                    shouldGenerate = supportingFilesToGenerate.contains(support.getDestinationFilename());
                }

                File written = processTemplateToFile(bundle, support.getTemplateFile(), outputFilename, shouldGenerate, CodegenConstants.SUPPORTING_FILES);
                if (written != null) {
                    files.add(written);
                    if (config.isEnablePostProcessFile() && !dryRun) {
                        config.postProcessFile(written, "supporting-file");
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not generate supporting file '" + support + "'", e);
            }
        }

        // Consider .openapi-generator-ignore a supporting file
        // Output .openapi-generator-ignore if it doesn't exist and wasn't explicitly created by a generator
        // and the option openapiGeneratorIgnoreList is not set
        if (config.openapiGeneratorIgnoreList() == null || config.openapiGeneratorIgnoreList().isEmpty()) {
            final String openapiGeneratorIgnore = ".openapi-generator-ignore";
            String ignoreFileNameTarget = config.outputFolder() + File.separator + openapiGeneratorIgnore;
            File ignoreFile = new File(ignoreFileNameTarget);
            if (generateMetadata) {
                try {
                    boolean shouldGenerate = !ignoreFile.exists();
                    if (shouldGenerate && supportingFilesToGenerate != null && !supportingFilesToGenerate.isEmpty()) {
                        shouldGenerate = supportingFilesToGenerate.contains(openapiGeneratorIgnore);
                    }
                    File written = processTemplateToFile(bundle, openapiGeneratorIgnore, ignoreFileNameTarget, shouldGenerate, CodegenConstants.SUPPORTING_FILES);
                    if (written != null) {
                        files.add(written);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(written, "openapi-generator-ignore");
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Could not generate supporting file '" + ignoreFileNameTarget + "'", e);
                }
            } else {
                this.templateProcessor.skip(ignoreFile.toPath(), "Skipped by generateMetadata option supplied by user.");
            }
        }

        generateVersionMetadata(files);
    }

    Map<String, Object> buildSupportFileBundle(List<OperationsMap> allOperations, List<ModelMap> allModels, List<ModelMap> aliasModels) {
        return this.buildSupportFileBundle(allOperations, allModels, aliasModels, null);
    }

    Map<String, Object> buildSupportFileBundle(List<OperationsMap> allOperations, List<ModelMap> allModels, List<ModelMap> aliasModels, List<WebhooksMap> allWebhooks) {

        Map<String, Object> bundle = new HashMap<>(config.additionalProperties());
        bundle.put("apiPackage", config.apiPackage());

        ApiInfoMap apis = new ApiInfoMap();
        apis.setApis(allOperations);

        URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());

        bundle.put("openAPI", openAPI);
        bundle.put("basePath", basePath);
        bundle.put("basePathWithoutHost", basePathWithoutHost);
        bundle.put("scheme", URLPathUtils.getScheme(url, config));
        bundle.put("host", url.getHost());
        if (url.getPort() != 80 && url.getPort() != 443 && url.getPort() != -1) {
            bundle.put("port", url.getPort());
        }
        bundle.put("contextPath", contextPath);
        bundle.put("apiInfo", apis);
        bundle.put("webhooks", allWebhooks);
        bundle.put("models", allModels);
        bundle.put("aliasModels", aliasModels);
        bundle.put("apiFolder", config.apiPackage().replace('.', File.separatorChar));
        bundle.put("modelPackage", config.modelPackage());
        bundle.put("library", config.getLibrary());
        bundle.put("generatorLanguageVersion", config.generatorLanguageVersion());
        // todo verify support and operation bundles have access to the common variables

        addAuthenticationSwitches(bundle);

        List<CodegenServer> servers = config.fromServers(openAPI.getServers());
        if (servers != null && !servers.isEmpty()) {
            servers.forEach(server -> server.url = removeTrailingSlash(server.url));
            bundle.put("servers", servers);
            bundle.put("hasServers", true);
        }

        boolean hasOperationServers = allOperations != null && allOperations.stream()
                .flatMap(om -> om.getOperations().getOperation().stream())
                .anyMatch(o -> o.servers != null && !o.servers.isEmpty());
        bundle.put("hasOperationServers", hasOperationServers);

        if (openAPI.getExternalDocs() != null) {
            bundle.put("externalDocs", openAPI.getExternalDocs());
        }

        for (int i = 0; i < allModels.size() - 1; i++) {
            CodegenModel m = allModels.get(i).getModel();
            m.hasMoreModels = true;
        }

        config.postProcessSupportingFileData(bundle);

        if (GlobalSettings.getProperty("debugSupportingFiles") != null) {
            LOGGER.info("############ Supporting file info ############");
            Json.prettyPrint(bundle);
        }
        return bundle;
    }

    /**
     * Add authentication methods to the given map
     * This adds a boolean and a collection for each authentication type to the map.
     * <p>
     * Examples:
     * <p>
     * boolean hasOAuthMethods
     * <p>
     * List&lt;CodegenSecurity&gt; oauthMethods
     *
     * @param bundle the map which the booleans and collections will be added
     */
    void addAuthenticationSwitches(Map<String, Object> bundle) {
        Map<String, SecurityScheme> securitySchemeMap = openAPI.getComponents() != null ? openAPI.getComponents().getSecuritySchemes() : null;
        List<CodegenSecurity> authMethods = config.fromSecurity(securitySchemeMap);
        if (authMethods != null && !authMethods.isEmpty()) {
            bundle.put("authMethods", authMethods);
            bundle.put("hasAuthMethods", true);

            if (ProcessUtils.hasOAuthMethods(authMethods)) {
                bundle.put("hasOAuthMethods", true);
                bundle.put("oauthMethods", ProcessUtils.getOAuthMethods(authMethods));
            }
            if (ProcessUtils.hasOpenIdConnectMethods(authMethods)) {
                bundle.put("hasOpenIdConnectMethods", true);
                bundle.put("openIdConnectMethods", ProcessUtils.getOpenIdConnectMethods(authMethods));
            }
            if (ProcessUtils.hasHttpBearerMethods(authMethods)) {
                bundle.put("hasHttpBearerMethods", true);
                bundle.put("httpBearerMethods", ProcessUtils.getHttpBearerMethods(authMethods));
            }
            if (ProcessUtils.hasHttpSignatureMethods(authMethods)) {
                bundle.put("hasHttpSignatureMethods", true);
                bundle.put("httpSignatureMethods", ProcessUtils.getHttpSignatureMethods(authMethods));
            }
            if (ProcessUtils.hasHttpBasicMethods(authMethods)) {
                bundle.put("hasHttpBasicMethods", true);
                bundle.put("httpBasicMethods", ProcessUtils.getHttpBasicMethods(authMethods));
            }
            if (ProcessUtils.hasApiKeyMethods(authMethods)) {
                bundle.put("hasApiKeyMethods", true);
                bundle.put("apiKeyMethods", ProcessUtils.getApiKeyMethods(authMethods));
            }
        }
    }

    @Override
    public List<File> generate() {
        if (openAPI == null) {
            throw new RuntimeException("Issues with the OpenAPI input. Possible causes: invalid/missing spec, malformed JSON/YAML files, etc.");
        }

        if (config == null) {
            throw new RuntimeException("missing config!");
        }

        if (config.getGeneratorMetadata() == null) {
            LOGGER.warn("Generator '{}' is missing generator metadata!", config.getName());
        } else {
            GeneratorMetadata generatorMetadata = config.getGeneratorMetadata();
            if (StringUtils.isNotEmpty(generatorMetadata.getGenerationMessage())) {
                LOGGER.info(generatorMetadata.getGenerationMessage());
            }

            Stability stability = generatorMetadata.getStability();
            String stabilityMessage = String.format(Locale.ROOT, "Generator '%s' is considered %s.", config.getName(), stability.value());
            if (stability == Stability.DEPRECATED) {
                LOGGER.warn(stabilityMessage);
            } else {
                LOGGER.info(stabilityMessage);
            }
        }

        configureGeneratorProperties();
        configureOpenAPIInfo();

        config.processOpenAPI(openAPI);

        processUserDefinedTemplates();

        // generate .openapi-generator-ignore if the option openapiGeneratorIgnoreFile is enabled
        generateOpenapiGeneratorIgnoreFile();

        List<File> files = new ArrayList<>();
        // models
        List<String> filteredSchemas = ModelUtils.getSchemasUsedOnlyInFormParam(openAPI);
        List<ModelMap> allModels = new ArrayList<>();
        List<ModelMap> aliasModels = new ArrayList<>();
        generateModels(files, allModels, filteredSchemas, aliasModels);
        // apis
        List<OperationsMap> allOperations = new ArrayList<>();
        generateApis(files, allOperations, allModels);
        // webhooks
        List<WebhooksMap> allWebhooks = new ArrayList<>();
        generateWebhooks(files, allWebhooks, allModels);
        // supporting files
        Map<String, Object> bundle = buildSupportFileBundle(allOperations, allModels, aliasModels, allWebhooks);
        generateSupportingFiles(files, bundle);

        if (dryRun) {
            boolean verbose = Boolean.parseBoolean(GlobalSettings.getProperty("verbose"));
            StringBuilder sb = new StringBuilder();

            sb.append(System.lineSeparator()).append(System.lineSeparator());
            sb.append("Dry Run Results:");
            sb.append(System.lineSeparator()).append(System.lineSeparator());

            Map<String, DryRunStatus> dryRunStatusMap = ((DryRunTemplateManager) this.templateProcessor).getDryRunStatusMap();

            dryRunStatusMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                DryRunStatus status = entry.getValue();
                try {
                    status.appendTo(sb);
                    sb.append(System.lineSeparator());
                    if (verbose) {
                        sb.append("  ")
                                .append(StringUtils.rightPad(status.getState().getDescription(), 20, "."))
                                .append(" ").append(status.getReason())
                                .append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    LOGGER.debug("Unable to document dry run status for {}.", entry.getKey());
                }
            });

            sb.append(System.lineSeparator()).append(System.lineSeparator());
            sb.append("States:");
            sb.append(System.lineSeparator()).append(System.lineSeparator());

            for (DryRunStatus.State state : DryRunStatus.State.values()) {
                sb.append("  - ").append(state.getShortDisplay()).append(" ").append(state.getDescription()).append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            LOGGER.error(sb.toString());
        } else {
            // This exists here rather than in the method which generates supporting files to avoid accidentally adding files after this metadata.
            if (generateSupportingFiles) {
                generateFilesMetadata(files);
            }
        }

        // post-process
        config.postProcess();

        // reset GlobalSettings, so that the running thread can be reused for another generator-run
        GlobalSettings.reset();

        return files;
    }

    private void processUserDefinedTemplates() {
        // TODO: initial behavior is "merge" user defined with built-in templates. consider offering user a "replace" option.
        if (userDefinedTemplates != null && !userDefinedTemplates.isEmpty()) {
            Map<String, SupportingFile> supportingFilesMap = config.supportingFiles().stream()
                    .collect(Collectors.toMap(TemplateDefinition::getTemplateFile, Function.identity(), (oldValue, newValue) -> oldValue));

            // TemplateFileType.SupportingFiles
            userDefinedTemplates.stream()
                    .filter(i -> i.getTemplateType().equals(TemplateFileType.SupportingFiles))
                    .forEach(userDefinedTemplate -> {
                        SupportingFile newFile = new SupportingFile(
                                userDefinedTemplate.getTemplateFile(),
                                userDefinedTemplate.getFolder(),
                                userDefinedTemplate.getDestinationFilename()
                        );
                        if (supportingFilesMap.containsKey(userDefinedTemplate.getTemplateFile())) {
                            SupportingFile f = supportingFilesMap.get(userDefinedTemplate.getTemplateFile());
                            config.supportingFiles().remove(f);

                            if (!f.isCanOverwrite()) {
                                newFile.doNotOverwrite();
                            }
                        }
                        config.supportingFiles().add(newFile);
                    });

            // Others, excluding TemplateFileType.SupportingFiles
            userDefinedTemplates.stream()
                    .filter(i -> !i.getTemplateType().equals(TemplateFileType.SupportingFiles))
                    .forEach(userDefinedTemplate -> {
                        // determine file extension…
                        // if template is in format api.ts.mustache, we'll extract .ts
                        // if user has provided an example destination filename, we'll use that extension
                        String templateFile = userDefinedTemplate.getTemplateFile();
                        int lastSeparator = templateFile.lastIndexOf('.');
                        String templateExt = FilenameUtils.getExtension(templateFile.substring(0, lastSeparator));
                        if (StringUtils.isBlank(templateExt)) {
                            // hack: destination filename in this scenario might be a suffix like Impl.java
                            templateExt = userDefinedTemplate.getDestinationFilename();
                        } else {
                            templateExt = StringUtils.prependIfMissing(templateExt, ".");
                        }
                        String templateOutputFolder = userDefinedTemplate.getFolder();
                        if (!templateOutputFolder.isEmpty()) {
                            config.templateOutputDirs().put(templateFile, templateOutputFolder);
                        }
                        switch (userDefinedTemplate.getTemplateType()) {
                            case API:
                                config.apiTemplateFiles().put(templateFile, templateExt);
                                break;
                            case Model:
                                config.modelTemplateFiles().put(templateFile, templateExt);
                                break;
                            case APIDocs:
                                config.apiDocTemplateFiles().put(templateFile, templateExt);
                                break;
                            case ModelDocs:
                                config.modelDocTemplateFiles().put(templateFile, templateExt);
                                break;
                            case APITests:
                                config.apiTestTemplateFiles().put(templateFile, templateExt);
                                break;
                            case ModelTests:
                                config.modelTestTemplateFiles().put(templateFile, templateExt);
                                break;
                            case SupportingFiles:
                                // excluded by filter
                                break;
                        }
                    });
        }
    }

    protected File processTemplateToFile(Map<String, Object> templateData, String templateName, String outputFilename, boolean shouldGenerate, String skippedByOption) throws IOException {
        return processTemplateToFile(templateData, templateName, outputFilename, shouldGenerate, skippedByOption, this.config.getOutputDir());
    }

    private final Set<String> seenFiles = new HashSet<>();

    private File processTemplateToFile(Map<String, Object> templateData, String templateName, String outputFilename, boolean shouldGenerate, String skippedByOption, String intendedOutputDir) throws IOException {
        String adjustedOutputFilename = outputFilename.replaceAll("//", "/").replace('/', File.separatorChar);
        File target = new File(adjustedOutputFilename);
        if (ignoreProcessor.allowsFile(target)) {
            if (shouldGenerate) {
                Path outDir = java.nio.file.Paths.get(intendedOutputDir).toAbsolutePath();
                Path absoluteTarget = target.toPath().toAbsolutePath();
                if (!absoluteTarget.startsWith(outDir)) {
                    throw new RuntimeException(String.format(Locale.ROOT, "Target files must be generated within the output directory; absoluteTarget=%s outDir=%s", absoluteTarget, outDir));
                }

                if (seenFiles.stream().filter(f -> f.toLowerCase(Locale.ROOT).equals(absoluteTarget.toString().toLowerCase(Locale.ROOT))).findAny().isPresent()) {
                    LOGGER.warn("Duplicate file path detected. Not all operating systems can handle case sensitive file paths. path={}", absoluteTarget.toString());
                }
                seenFiles.add(absoluteTarget.toString());
                return this.templateProcessor.write(templateData, templateName, target);
            } else {
                this.templateProcessor.skip(target.toPath(), String.format(Locale.ROOT, "Skipped by %s options supplied by user.", skippedByOption));
                return null;
            }
        } else {
            this.templateProcessor.ignore(target.toPath(), "Ignored by rule in ignore file.");
            return null;
        }
    }

    public Map<String, List<CodegenOperation>> processPaths(Paths paths) {
        Map<String, List<CodegenOperation>> ops = new TreeMap<>();
        // when input file is not valid and doesn't contain any paths
        if (paths == null) {
            return ops;
        }
        for (Map.Entry<String, PathItem> pathsEntry : paths.entrySet()) {
            String resourcePath = pathsEntry.getKey();
            PathItem path = pathsEntry.getValue();
            processOperation(resourcePath, "get", path.getGet(), ops, path);
            processOperation(resourcePath, "head", path.getHead(), ops, path);
            processOperation(resourcePath, "put", path.getPut(), ops, path);
            processOperation(resourcePath, "post", path.getPost(), ops, path);
            processOperation(resourcePath, "delete", path.getDelete(), ops, path);
            processOperation(resourcePath, "patch", path.getPatch(), ops, path);
            processOperation(resourcePath, "options", path.getOptions(), ops, path);
            processOperation(resourcePath, "trace", path.getTrace(), ops, path);
        }
        return ops;
    }

    public Map<String, List<CodegenOperation>> processWebhooks(Map<String, PathItem> webhooks) {
        Map<String, List<CodegenOperation>> ops = new TreeMap<>();
        // when input file is not valid and doesn't contain any paths
        if (webhooks == null) {
            return ops;
        }
        for (Map.Entry<String, PathItem> webhooksEntry : webhooks.entrySet()) {
            String resourceKey = webhooksEntry.getKey();
            PathItem path = webhooksEntry.getValue();
            processOperation(resourceKey, "get", path.getGet(), ops, path);
            processOperation(resourceKey, "head", path.getHead(), ops, path);
            processOperation(resourceKey, "put", path.getPut(), ops, path);
            processOperation(resourceKey, "post", path.getPost(), ops, path);
            processOperation(resourceKey, "delete", path.getDelete(), ops, path);
            processOperation(resourceKey, "patch", path.getPatch(), ops, path);
            processOperation(resourceKey, "options", path.getOptions(), ops, path);
            processOperation(resourceKey, "trace", path.getTrace(), ops, path);
        }
        return ops;
    }

    private void processOperation(String resourcePath, String httpMethod, Operation operation, Map<String, List<CodegenOperation>> operations, PathItem path) {
        if (operation == null) {
            return;
        }

        if (GlobalSettings.getProperty("debugOperations") != null) {
            LOGGER.info("processOperation: resourcePath=  {}\t;{} {}\n", resourcePath, httpMethod, operation);
        }

        List<Tag> tags = new ArrayList<>();
        List<String> tagNames = operation.getTags();
        List<Tag> swaggerTags = openAPI.getTags();
        if (tagNames != null) {
            if (swaggerTags == null) {
                for (String tagName : tagNames) {
                    tags.add(new Tag().name(tagName));
                }
            } else {
                for (String tagName : tagNames) {
                    boolean foundTag = false;
                    for (Tag tag : swaggerTags) {
                        if (tag.getName().equals(tagName)) {
                            tags.add(tag);
                            foundTag = true;
                            break;
                        }
                    }

                    if (!foundTag) {
                        tags.add(new Tag().name(tagName));
                    }
                }
            }
        }

        if (tags.isEmpty()) {
            tags.add(new Tag().name("default"));
        }

        /*
         build up a set of parameter "ids" defined at the operation level
         per the swagger 2.0 spec "A unique parameter is defined by a combination of a name and location"
          i'm assuming "location" == "in"
        */
        Set<String> operationParameters = new HashSet<>();
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                operationParameters.add(generateParameterId(parameter));
            }
        }

        //need to propagate path level down to the operation
        if (path.getParameters() != null) {
            for (Parameter parameter : path.getParameters()) {
                //skip propagation if a parameter with the same name is already defined at the operation level
                if (!operationParameters.contains(generateParameterId(parameter))) {
                    operation.addParametersItem(parameter);
                }
            }
        }

        final Map<String, SecurityScheme> securitySchemes = openAPI.getComponents() != null ? openAPI.getComponents().getSecuritySchemes() : null;
        final List<SecurityRequirement> globalSecurities = openAPI.getSecurity();
        for (Tag tag : tags) {
            try {
                if (operation.getExtensions() != null && Boolean.TRUE.equals(operation.getExtensions().get("x-internal"))) {
                    // skip operation if x-internal sets to true
                    LOGGER.info("Operation ({} {} - {}) not generated since x-internal is set to true",
                            httpMethod, resourcePath, operation.getOperationId());
                } else {
                    CodegenOperation codegenOperation = config.fromOperation(resourcePath, httpMethod, operation, path.getServers());
                    codegenOperation.tags = new ArrayList<>(tags);
                    config.addOperationToGroup(config.sanitizeTag(tag.getName()), resourcePath, operation, codegenOperation, operations);

                    List<SecurityRequirement> securities = operation.getSecurity();
                    if (securities != null && securities.isEmpty()) {
                        continue;
                    }

                    Map<String, SecurityScheme> authMethods = getAuthMethods(securities, securitySchemes);

                    if (authMethods != null && !authMethods.isEmpty()) {
                        List<CodegenSecurity> fullAuthMethods = config.fromSecurity(authMethods);
                        codegenOperation.authMethods = filterAuthMethods(fullAuthMethods, securities);
                        codegenOperation.hasAuthMethods = true;
                    } else {
                        authMethods = getAuthMethods(globalSecurities, securitySchemes);

                        if (authMethods != null && !authMethods.isEmpty()) {
                            List<CodegenSecurity> fullAuthMethods = config.fromSecurity(authMethods);
                            codegenOperation.authMethods = filterAuthMethods(fullAuthMethods, globalSecurities);
                            codegenOperation.hasAuthMethods = true;
                        }
                    }
                }
            } catch (Exception ex) {
                String msg = "Could not process operation:\n" //
                        + "  Tag: " + tag + "\n"//
                        + "  Operation: " + operation.getOperationId() + "\n" //
                        + "  Resource: " + httpMethod + " " + resourcePath + "\n"//
                        + "  Schemas: " + openAPI.getComponents().getSchemas() + "\n"  //
                        + "  Exception: " + ex.getMessage();
                throw new RuntimeException(msg, ex);
            }
        }
    }

    private static String generateParameterId(Parameter parameter) {
        return null == parameter.get$ref() ? parameter.getName() + ":" + parameter.getIn() : parameter.get$ref();
    }

    private OperationsMap processOperations(CodegenConfig config, String tag, List<CodegenOperation> ops, List<ModelMap> allModels) {
        OperationsMap operations = new OperationsMap();
        OperationMap objs = new OperationMap();
        objs.setClassname(config.toApiName(tag));
        objs.setPathPrefix(config.toApiVarName(tag));

        // check for nickname uniqueness
        if (config.getAddSuffixToDuplicateOperationNicknames()) {
            Set<String> opIds = new HashSet<>();
            int counter = 0;
            for (CodegenOperation op : ops) {
                String opId = op.nickname;
                if (opIds.contains(opId)) {
                    counter++;
                    op.nickname += "_" + counter;
                }
                opIds.add(opId);
            }
        }
        objs.setOperation(ops);

        operations.setOperation(objs);
        operations.put("package", config.apiPackage());

        Set<String> allImports = new ConcurrentSkipListSet<>();
        for (CodegenOperation op : ops) {
            allImports.addAll(op.imports);
        }

        Map<String, String> mappings = getAllImportsMappings(allImports);
        Set<Map<String, String>> imports = toImportsObjects(mappings);

        //Some codegen implementations rely on a list interface for the imports
        operations.setImports(new ArrayList<>(imports));

        // add a flag to indicate whether there's any {{import}}
        if (!imports.isEmpty()) {
            operations.put("hasImport", true);
        }

        config.postProcessOperationsWithModels(operations, allModels);
        return operations;
    }

    private WebhooksMap processWebhooks(CodegenConfig config, String tag, List<CodegenOperation> wks, List<ModelMap> allModels) {
        WebhooksMap operations = new WebhooksMap();
        OperationMap objs = new OperationMap();
        objs.setClassname(config.toApiName(tag));
        objs.setPathPrefix(config.toApiVarName(tag));

        // check for nickname uniqueness
        if (config.getAddSuffixToDuplicateOperationNicknames()) {
            Set<String> opIds = new HashSet<>();
            int counter = 0;
            for (CodegenOperation op : wks) {
                String opId = op.nickname;
                if (opIds.contains(opId)) {
                    counter++;
                    op.nickname += "_" + counter;
                }
                opIds.add(opId);
            }
        }
        objs.setOperation(wks);

        operations.setWebhooks(objs);
        operations.put("package", config.apiPackage());

        Set<String> allImports = new ConcurrentSkipListSet<>();
        for (CodegenOperation op : wks) {
            allImports.addAll(op.imports);
        }

        Map<String, String> mappings = getAllImportsMappings(allImports);
        Set<Map<String, String>> imports = toImportsObjects(mappings);

        //Some codegen implementations rely on a list interface for the imports
        operations.setImports(new ArrayList<>(imports));

        // add a flag to indicate whether there's any {{import}}
        if (!imports.isEmpty()) {
            operations.put("hasImport", true);
        }

        config.postProcessWebhooksWithModels(operations, allModels);
        return operations;
    }

    /**
     * Transforms a set of imports to a map with key config.toModelImport(import) and value the import string.
     *
     * @param allImports - Set of imports
     * @return Map of fully qualified import path and initial import.
     */
    private Map<String, String> getAllImportsMappings(Set<String> allImports) {
        Map<String, String> result = new HashMap<>();
        allImports.forEach(nextImport -> {
            String mapping = config.importMapping().get(nextImport);
            if (mapping != null) {
                result.put(mapping, nextImport);
            } else {
                result.putAll(config.toModelImportMap(nextImport));
            }
        });
        return result;
    }

    /**
     * Using an import map created via {@link #getAllImportsMappings(Set)} to build a list import objects.
     * The import objects have two keys: import and classname which hold the key and value of the initial map entry.
     *
     * @param mappedImports Map of fully qualified import and import
     * @return The set of unique imports
     */
    private Set<Map<String, String>> toImportsObjects(Map<String, String> mappedImports) {
        Set<Map<String, String>> result = new TreeSet<>(
                Comparator.comparing(o -> o.get("classname"))
        );

        mappedImports.forEach((key, value) -> {
            Map<String, String> im = new LinkedHashMap<>();
            im.put("import", key);
            im.put("classname", value);
            result.add(im);
        });
        return result;
    }

    private ModelsMap processModels(CodegenConfig config, Map<String, Schema> definitions) {
        ModelsMap objs = new ModelsMap();
        objs.put("package", config.modelPackage());
        List<ModelMap> modelMaps = new ArrayList<>();
        Set<String> allImports = new LinkedHashSet<>();
        for (Map.Entry<String, Schema> definitionsEntry : definitions.entrySet()) {
            String key = definitionsEntry.getKey();
            Schema schema = definitionsEntry.getValue();
            if (schema == null) {
                LOGGER.warn("Schema {} cannot be null in processModels", key);
                continue;
            }
            CodegenModel cm = config.fromModel(key, schema);
            ModelMap mo = new ModelMap();
            mo.setModel(cm);
            mo.put("importPath", config.toModelImport(cm.classname));
            modelMaps.add(mo);

            cm.removeSelfReferenceImport();

            allImports.addAll(cm.imports);
        }
        objs.setModels(modelMaps);
        Set<String> importSet = new ConcurrentSkipListSet<>();
        for (String nextImport : allImports) {
            String mapping = config.importMapping().get(nextImport);
            if (mapping == null) {
                mapping = config.toModelImport(nextImport);
            }
            if (mapping != null && !config.defaultIncludes().contains(mapping)) {
                importSet.add(mapping);
            }
            // add instantiation types
            mapping = config.instantiationTypes().get(nextImport);
            if (mapping != null && !config.defaultIncludes().contains(mapping)) {
                importSet.add(mapping);
            }
        }
        List<Map<String, String>> imports = new ArrayList<>();
        for (String s : importSet) {
            Map<String, String> item = new HashMap<>();
            item.put("import", s);
            imports.add(item);
        }
        objs.setImports(imports);
        config.postProcessModels(objs);
        return objs;
    }

    private Map<String, SecurityScheme> getAuthMethods(List<SecurityRequirement> securities, Map<String, SecurityScheme> securitySchemes) {
        if (securities == null || (securitySchemes == null || securitySchemes.isEmpty())) {
            return null;
        }
        final Map<String, SecurityScheme> authMethods = new HashMap<>();
        for (SecurityRequirement requirement : securities) {
            for (Map.Entry<String, List<String>> entry : requirement.entrySet()) {
                final String key = entry.getKey();
                SecurityScheme securityScheme = securitySchemes.get(key);
                if (securityScheme != null) {

                    if (securityScheme.getType().equals(SecurityScheme.Type.OAUTH2)) {
                        OAuthFlows oauthUpdatedFlows = new OAuthFlows();
                        oauthUpdatedFlows.extensions(securityScheme.getFlows().getExtensions());

                        SecurityScheme oauthUpdatedScheme = new SecurityScheme()
                                .type(securityScheme.getType())
                                .description(securityScheme.getDescription())
                                .name(securityScheme.getName())
                                .$ref(securityScheme.get$ref())
                                .in(securityScheme.getIn())
                                .scheme(securityScheme.getScheme())
                                .bearerFormat(securityScheme.getBearerFormat())
                                .openIdConnectUrl(securityScheme.getOpenIdConnectUrl())
                                .extensions(securityScheme.getExtensions())
                                .flows(oauthUpdatedFlows);

                        // Ensure inserted AuthMethod only contains scopes of actual operation, and not all of them defined in the Security Component
                        // have to iterate through and create new SecurityScheme objects with the scopes 'fixed/updated'
                        final OAuthFlows securitySchemeFlows = securityScheme.getFlows();


                        if (securitySchemeFlows.getAuthorizationCode() != null) {
                            OAuthFlow updatedFlow = cloneOAuthFlow(securitySchemeFlows.getAuthorizationCode(), entry.getValue());

                            oauthUpdatedFlows.setAuthorizationCode(updatedFlow);
                        }
                        if (securitySchemeFlows.getImplicit() != null) {
                            OAuthFlow updatedFlow = cloneOAuthFlow(securitySchemeFlows.getImplicit(), entry.getValue());

                            oauthUpdatedFlows.setImplicit(updatedFlow);
                        }
                        if (securitySchemeFlows.getPassword() != null) {
                            OAuthFlow updatedFlow = cloneOAuthFlow(securitySchemeFlows.getPassword(), entry.getValue());

                            oauthUpdatedFlows.setPassword(updatedFlow);
                        }
                        if (securitySchemeFlows.getClientCredentials() != null) {
                            OAuthFlow updatedFlow = cloneOAuthFlow(securitySchemeFlows.getClientCredentials(), entry.getValue());

                            oauthUpdatedFlows.setClientCredentials(updatedFlow);
                        }

                        authMethods.put(key, oauthUpdatedScheme);
                    } else if (securityScheme.getType().equals(SecurityScheme.Type.OPENIDCONNECT)) {
                        // Security scheme only allows to add scope in Flows, so randomly using authorization code flow
                        OAuthFlows openIdConnectUpdatedFlows = new OAuthFlows();
                        OAuthFlow flow = new OAuthFlow();
                        Scopes flowScopes = new Scopes();
                        securities.stream()
                                .map(secReq -> secReq.get(key))
                                .filter(Objects::nonNull)
                                .flatMap(List::stream)
                                .forEach(value -> flowScopes.put(value, value));
                        flow.scopes(flowScopes);
                        openIdConnectUpdatedFlows.authorizationCode(flow);

                        SecurityScheme openIdConnectUpdatedScheme = new SecurityScheme()
                                .type(securityScheme.getType())
                                .description(securityScheme.getDescription())
                                .name(securityScheme.getName())
                                .$ref(securityScheme.get$ref())
                                .in(securityScheme.getIn())
                                .scheme(securityScheme.getScheme())
                                .bearerFormat(securityScheme.getBearerFormat())
                                .openIdConnectUrl(securityScheme.getOpenIdConnectUrl())
                                .extensions(securityScheme.getExtensions())
                                .flows(openIdConnectUpdatedFlows);

                        authMethods.put(key, openIdConnectUpdatedScheme);
                    } else {
                        authMethods.put(key, securityScheme);
                    }
                }
            }
        }
        return authMethods;
    }

    private static OAuthFlow cloneOAuthFlow(OAuthFlow originFlow, List<String> operationScopes) {
        Scopes newScopes = new Scopes();
        for (String operationScope : operationScopes) {
            if (originFlow.getScopes().containsKey(operationScope)) {
                newScopes.put(operationScope, originFlow.getScopes().get(operationScope));
            }
        }

        return new OAuthFlow()
                .authorizationUrl(originFlow.getAuthorizationUrl())
                .tokenUrl(originFlow.getTokenUrl())
                .refreshUrl(originFlow.getRefreshUrl())
                .extensions(originFlow.getExtensions())
                .scopes(newScopes);
    }

    private List<CodegenSecurity> filterAuthMethods(List<CodegenSecurity> authMethods, List<SecurityRequirement> securities) {
        if (securities == null || securities.isEmpty() || authMethods == null) {
            return authMethods;
        }

        List<CodegenSecurity> result = new ArrayList<>();

        for (CodegenSecurity security : authMethods) {
            boolean filtered = false;
            if (security != null) {
                for (SecurityRequirement requirement : securities) {
                    List<String> opScopes = requirement.get(security.name);
                    if (opScopes != null) {
                        // We have operation-level scopes for this method, so filter the auth method to
                        // describe the operation auth method with only the scopes that it requires.
                        // We have to create a new auth method instance because the original object must
                        // not be modified.
                        CodegenSecurity opSecurity = security.filterByScopeNames(opScopes);
                        result.add(opSecurity);
                        filtered = true;
                        break;
                    }
                }
            }

            // If we didn't get a filtered version, then we can keep the original auth method.
            if (!filtered) {
                result.add(security);
            }
        }

        return result;
    }

    /**
     * Generates a file at .openapi-generator/VERSION to track the version of user's latest run.
     *
     * @param files The list tracking generated files
     */
    private void generateVersionMetadata(List<File> files) {
        String versionMetadata = config.outputFolder() + File.separator + METADATA_DIR + File.separator + config.getVersionMetadataFilename();
        if (generateMetadata) {
            File versionMetadataFile = new File(versionMetadata);
            try {
                File written = this.templateProcessor.writeToFile(versionMetadata, (ImplementationVersion.read() + "\n").getBytes(StandardCharsets.UTF_8));
                if (written != null) {
                    files.add(versionMetadataFile);
                    if (config.isEnablePostProcessFile() && !dryRun) {
                        config.postProcessFile(written, "openapi-generator-version");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not generate supporting file '" + versionMetadata + "'", e);
            }
        } else {
            Path metadata = java.nio.file.Paths.get(versionMetadata);
            this.templateProcessor.skip(metadata, "Skipped by generateMetadata option supplied by user.");
        }
    }

    private Path absPath(File input) {
        // intentionally creates a new absolute path instance, disconnected from underlying FileSystem provider of File
        return java.nio.file.Paths.get(input.getAbsolutePath());
    }

    /**
     * Generates a file at .openapi-generator/FILES to track the files created by the user's latest run.
     * This is ideal for CI and regeneration of code without stale/unused files from older generations.
     *
     * @param files The list tracking generated files
     */
    private void generateFilesMetadata(List<File> files) {
        if (generateMetadata) {
            try {
                StringBuilder sb = new StringBuilder();
                Path outDir = absPath(new File(this.config.getOutputDir()));

                List<File> filesToSort = new ArrayList<>();

                // Avoid side-effecting sort in this path when generateMetadata=true
                files.forEach(f -> {
                    // We have seen NPE on CI for getPath() returning null, so guard against this (to be fixed in 5.0 template management refactor)
                    //noinspection ConstantConditions
                    if (f != null && f.getPath() != null) {
                        filesToSort.add(outDir.relativize(absPath(f)).normalize().toFile());
                    }
                });

                // NOTE: Don't use File.separator here as we write linux-style paths to FILES, and File.separator will
                // result in incorrect match on Windows machines.
                String relativeMeta = METADATA_DIR + "/VERSION";

                final List<String> relativePaths = new ArrayList<>(filesToSort.size());
                filesToSort.forEach(f -> {
                    // some Java implementations don't honor .relativize documentation fully.
                    // When outDir is /a/b and the input is /a/b/c/d, the result should be c/d.
                    // Some implementations make the output ./c/d which seems to mix the logic
                    // as documented for symlinks. So we need to trim any / or ./ from the start,
                    // as nobody should be generating into system root and our expectation is no ./
                    String relativePath = removeStart(removeStart(f.toString(), "." + File.separator), File.separator);
                    if (File.separator.equals("\\")) {
                        // ensure that windows outputs same FILES format
                        relativePath = relativePath.replace(File.separator, "/");
                    }
                    if (!relativePath.equals(relativeMeta)) {
                        relativePaths.add(relativePath);
                    }
                });

                Collections.sort(relativePaths, (a, b) -> IOCase.SENSITIVE.checkCompareTo(a, b));
                relativePaths.forEach(relativePath -> {
                    sb.append(relativePath).append(System.lineSeparator());
                });

                String targetFile = config.outputFolder() + File.separator + METADATA_DIR + File.separator + config.getFilesMetadataFilename();

                File filesFile = this.templateProcessor.writeToFile(targetFile, sb.toString().getBytes(StandardCharsets.UTF_8));
                if (filesFile != null) {
                    files.add(filesFile);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to write FILES metadata to track generated files.");
            }
        }
    }

    private String removeTrailingSlash(String value) {
        return StringUtils.removeEnd(value, "/");
    }

}
