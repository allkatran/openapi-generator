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

import com.samskivert.mustache.Mustache;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.samskivert.mustache.Mustache;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.VendorExtension;
import org.openapitools.codegen.meta.features.ClientModificationFeature;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.GlobalFeature;
import org.openapitools.codegen.meta.features.ParameterFeature;
import org.openapitools.codegen.meta.features.SchemaSupportFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.meta.features.WireFormatFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.templating.mustache.ReplaceAllLambda;
import org.openapitools.codegen.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.sort;

public class KotlinClientCodegen extends AbstractKotlinCodegen {

    private final Logger LOGGER = LoggerFactory.getLogger(KotlinClientCodegen.class);

    protected static final String JVM = "jvm";
    protected static final String JVM_KTOR = "jvm-ktor";
    protected static final String JVM_OKHTTP = "jvm-okhttp";
    protected static final String JVM_OKHTTP4 = "jvm-okhttp4";
    protected static final String JVM_RETROFIT2 = "jvm-retrofit2";
    protected static final String MULTIPLATFORM = "multiplatform";
    protected static final String JVM_VOLLEY = "jvm-volley";
    protected static final String JVM_VERTX = "jvm-vertx";
    protected static final String JVM_SPRING = "jvm-spring";
    protected static final String JVM_SPRING_WEBCLIENT = "jvm-spring-webclient";
    protected static final String JVM_SPRING_RESTCLIENT = "jvm-spring-restclient";

    public static final String USE_RX_JAVA3 = "useRxJava3";
    public static final String USE_COROUTINES = "useCoroutines";
    public static final String USE_NON_ASCII_HEADERS = "useNonAsciiHeaders";
    public static final String DO_NOT_USE_RX_AND_COROUTINES = "doNotUseRxAndCoroutines";
    public static final String GENERATE_ROOM_MODELS = "generateRoomModels";
    public static final String ROOM_MODEL_PACKAGE = "roomModelPackage";
    public static final String OMIT_GRADLE_PLUGIN_VERSIONS = "omitGradlePluginVersions";
    public static final String OMIT_GRADLE_WRAPPER = "omitGradleWrapper";
    public static final String USE_SETTINGS_GRADLE = "useSettingsGradle";
    public static final String IDEA = "idea";
    public static final String USE_SPRING_BOOT3 = "useSpringBoot3";
    public static final String USE_RESPONSE_AS_RETURN_TYPE = "useResponseAsReturnType";

    public static final String DATE_LIBRARY = "dateLibrary";
    public static final String REQUEST_DATE_CONVERTER = "requestDateConverter";
    public static final String COLLECTION_TYPE = "collectionType";
    public static final String FAIL_ON_UNKNOWN_PROPERTIES = "failOnUnknownProperties";

    public static final String MOSHI_CODE_GEN = "moshiCodeGen";

    public static final String EXPLICIT_API = "explicitApi";
    public static final String NULLABLE_RETURN_TYPE = "nullableReturnType";

    public static final String SUPPORT_ANDROID_API_LEVEL_25_AND_BELOW = "supportAndroidApiLevel25AndBelow";

    public static final String MAP_FILE_BINARY_TO_BYTE_ARRAY = "mapFileBinaryToByteArray";

    public static final String GENERATE_ONEOF_ANYOF_WRAPPERS = "generateOneOfAnyOfWrappers";

    protected static final String VENDOR_EXTENSION_BASE_NAME_LITERAL = "x-base-name-literal";


    @Setter protected String dateLibrary = DateLibrary.JAVA8.value;
    @Setter protected String requestDateConverter = RequestDateConverter.TO_JSON.value;
    @Setter protected String collectionType = CollectionType.LIST.value;
    protected boolean useRxJava3 = false;
    protected boolean useCoroutines = false;
    // backwards compatibility for openapi configs that specify neither rx1 nor rx2
    // (mustache does not allow for boolean operators so we need this extra field)
    protected boolean doNotUseRxAndCoroutines = true;
    protected boolean generateRoomModels = false;
    @Setter protected String roomModelPackage = "";
    @Setter protected boolean omitGradleWrapper = false;
    @Setter protected boolean mapFileBinaryToByteArray = false;
    @Setter protected boolean generateOneOfAnyOfWrappers = true;
    @Getter @Setter protected boolean failOnUnknownProperties = false;

    protected String authFolder;

    @Getter protected SERIALIZATION_LIBRARY_TYPE serializationLibrary = SERIALIZATION_LIBRARY_TYPE.moshi;
    public static final String SERIALIZATION_LIBRARY_DESC = "What serialization library to use: 'moshi' (default), or 'gson' or 'jackson' or 'kotlinx_serialization'";

    public enum SERIALIZATION_LIBRARY_TYPE {moshi, gson, jackson, kotlinx_serialization}

    public enum DateLibrary {
        STRING("string"),
        THREETENBP("threetenbp"),
        THREETENBP_LOCALDATETIME("threetenbp-localdatetime"),
        JAVA8("java8"),
        JAVA8_LOCALDATETIME("java8-localdatetime"),
        KOTLINX_DATETIME("kotlinx-datetime");

        public final String value;

        DateLibrary(String value) {
            this.value = value;
        }
    }

    public enum RequestDateConverter {
        TO_STRING("toString"),
        TO_JSON("toJson");

        public final String value;

        RequestDateConverter(String value) {
            this.value = value;
        }
    }

    public enum CollectionType {
        ARRAY("array"),
        LIST("list");

        public final String value;

        CollectionType(String value) {
            this.value = value;
        }
    }

    /**
     * Constructs an instance of `KotlinClientCodegen`.
     */
    public KotlinClientCodegen() {
        super();

        /*
         * OAuth flows supported _only_ by client explicitly setting bearer token. The "flows" are not supported.
         */
        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .excludeWireFormatFeatures(WireFormatFeature.XML, WireFormatFeature.PROTOBUF)
                .securityFeatures(EnumSet.of(
                        SecurityFeature.BasicAuth,
                        SecurityFeature.ApiKey,
                        SecurityFeature.BearerToken,
                        SecurityFeature.OAuth2_AuthorizationCode,//retrofit only
                        SecurityFeature.OAuth2_Implicit))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .excludeParameterFeatures(
                        ParameterFeature.Cookie
                )
                .includeClientModificationFeatures(ClientModificationFeature.BasePath)
        );

        artifactId = "kotlin-client";
        packageName = "org.openapitools.client";

        // cliOptions default redefinition need to be updated
        updateOption(CodegenConstants.ARTIFACT_ID, this.artifactId);
        updateOption(CodegenConstants.PACKAGE_NAME, this.packageName);

        outputFolder = "generated-code" + File.separator + "kotlin-client";
        modelTemplateFiles.put("model.mustache", ".kt");
        if (generateRoomModels) {
            modelTemplateFiles.put("model_room.mustache", ".kt");
        }
        apiTemplateFiles.put("api.mustache", ".kt");
        apiTestTemplateFiles.put("api_test.mustache", ".kt");
        modelTestTemplateFiles.put("model_test.mustache", ".kt");
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");
        embeddedTemplateDir = templateDir = "kotlin-client";
        apiPackage = packageName + ".apis";
        modelPackage = packageName + ".models";

        CliOption dateLibrary = new CliOption(DATE_LIBRARY, "Option. Date library to use");
        Map<String, String> dateOptions = new HashMap<>();
        dateOptions.put(DateLibrary.THREETENBP.value, "Threetenbp - Backport of JSR310 (jvm only, preferred for jdk < 1.8)");
        dateOptions.put(DateLibrary.THREETENBP_LOCALDATETIME.value, "Threetenbp - Backport of JSR310 (jvm only, for legacy app only)");
        dateOptions.put(DateLibrary.STRING.value, "String");
        dateOptions.put(DateLibrary.JAVA8.value, "Java 8 native JSR310 (jvm only, preferred for jdk 1.8+)");
        dateOptions.put(DateLibrary.JAVA8_LOCALDATETIME.value, "Java 8 native JSR310 (jvm only, for legacy app only)");
        dateOptions.put(DateLibrary.KOTLINX_DATETIME.value, "kotlinx-datetime (preferred for multiplatform)");
        dateLibrary.setEnum(dateOptions);
        dateLibrary.setDefault(this.dateLibrary);
        cliOptions.add(dateLibrary);

        CliOption collectionType = new CliOption(COLLECTION_TYPE, "Option. Collection type to use");
        Map<String, String> collectionOptions = new HashMap<>();
        collectionOptions.put(CollectionType.ARRAY.value, "kotlin.Array");
        collectionOptions.put(CollectionType.LIST.value, "kotlin.collections.List");
        collectionType.setEnum(collectionOptions);
        collectionType.setDefault(this.collectionType);
        cliOptions.add(collectionType);

        supportedLibraries.put(JVM_KTOR, "Platform: Java Virtual Machine. HTTP client: Ktor 1.6.7. JSON processing: Gson, Jackson (default).");
        supportedLibraries.put(JVM_OKHTTP4, "[DEFAULT] Platform: Java Virtual Machine. HTTP client: OkHttp 4.2.0 (Android 5.0+ and Java 8+). JSON processing: Moshi 1.8.0.");
        supportedLibraries.put(JVM_SPRING_WEBCLIENT, "Platform: Java Virtual Machine. HTTP: Spring 5 (or 6 with useSpringBoot3 enabled) WebClient. JSON processing: Jackson.");
        supportedLibraries.put(JVM_SPRING_RESTCLIENT, "Platform: Java Virtual Machine. HTTP: Spring 6 RestClient. JSON processing: Jackson.");
        supportedLibraries.put(JVM_RETROFIT2, "Platform: Java Virtual Machine. HTTP client: Retrofit 2.6.2.");
        supportedLibraries.put(MULTIPLATFORM, "Platform: Kotlin multiplatform. HTTP client: Ktor 1.6.7. JSON processing: Kotlinx Serialization: 1.2.1.");
        supportedLibraries.put(JVM_VOLLEY, "Platform: JVM for Android. HTTP client: Volley 1.2.1. JSON processing: gson 2.8.9");
        supportedLibraries.put(JVM_VERTX, "Platform: Java Virtual Machine. HTTP client: Vert.x Web Client. JSON processing: Moshi, Gson or Jackson.");

        CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY, "Library template (sub-template) to use");
        libraryOption.setEnum(supportedLibraries);
        libraryOption.setDefault(JVM_OKHTTP4);
        cliOptions.add(libraryOption);
        setLibrary(JVM_OKHTTP4);

        CliOption requestDateConverter = new CliOption(REQUEST_DATE_CONVERTER, "JVM-Option. Defines in how to handle date-time objects that are used for a request (as query or parameter)");
        Map<String, String> requestDateConverterOptions = new HashMap<>();
        requestDateConverterOptions.put(RequestDateConverter.TO_JSON.value, "[DEFAULT] Date formatter option using a json converter.");
        requestDateConverterOptions.put(RequestDateConverter.TO_STRING.value, "Use the 'toString'-method of the date-time object to retrieve the related string representation.");
        requestDateConverter.setEnum(requestDateConverterOptions);
        requestDateConverter.setDefault(this.requestDateConverter);
        cliOptions.add(requestDateConverter);

        cliOptions.add(CliOption.newBoolean(USE_RX_JAVA3, "Whether to use the RxJava3 adapter with the retrofit2 library."));
        cliOptions.add(CliOption.newBoolean(USE_COROUTINES, "Whether to use the Coroutines adapter with the retrofit2 library."));
        cliOptions.add(CliOption.newBoolean(USE_SPRING_BOOT3, "Whether to use the Spring Boot 3 with the jvm-spring-webclient library."));
        cliOptions.add(CliOption.newBoolean(OMIT_GRADLE_PLUGIN_VERSIONS, "Whether to declare Gradle plugin versions in build files."));
        cliOptions.add(CliOption.newBoolean(OMIT_GRADLE_WRAPPER, "Whether to omit Gradle wrapper for creating a sub project."));
        cliOptions.add(CliOption.newBoolean(USE_SETTINGS_GRADLE, "Whether the project uses settings.gradle."));
        cliOptions.add(CliOption.newBoolean(IDEA, "Add IntelliJ Idea plugin and mark Kotlin main and test folders as source folders."));

        cliOptions.add(CliOption.newBoolean(MOSHI_CODE_GEN, "Whether to enable codegen with the Moshi library. Refer to the [official Moshi doc](https://github.com/square/moshi#codegen) for more info."));
        cliOptions.add(CliOption.newBoolean(FAIL_ON_UNKNOWN_PROPERTIES, "Fail Jackson de-serialization on unknown properties", false));

        cliOptions.add(CliOption.newBoolean(EXPLICIT_API, "Generates code with explicit access modifiers to comply with Kotlin Explicit API Mode."));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.NON_PUBLIC_API, CodegenConstants.NON_PUBLIC_API_DESC));
        cliOptions.add(CliOption.newBoolean(NULLABLE_RETURN_TYPE, "Nullable return type"));

        cliOptions.add(CliOption.newBoolean(GENERATE_ROOM_MODELS, "Generate Android Room database models in addition to API models (JVM Volley library only)", false));

        cliOptions.add(CliOption.newBoolean(SUPPORT_ANDROID_API_LEVEL_25_AND_BELOW, "[WARNING] This flag will generate code that has a known security vulnerability. It uses `kotlin.io.createTempFile` instead of `java.nio.file.Files.createTempFile` in order to support Android API level 25 and below. For more info, please check the following links https://github.com/OpenAPITools/openapi-generator/security/advisories/GHSA-23x4-m842-fmwf, https://github.com/OpenAPITools/openapi-generator/pull/9284"));

        cliOptions.add(new CliOption(MAP_FILE_BINARY_TO_BYTE_ARRAY, "Map File and Binary to ByteArray (default: false)").defaultValue(Boolean.FALSE.toString()));

        cliOptions.add(CliOption.newBoolean(GENERATE_ONEOF_ANYOF_WRAPPERS, "Generate oneOf, anyOf schemas as wrappers. Only `jvm-retrofit2`(library), `gson`(serializationLibrary) support this option."));

        CliOption serializationLibraryOpt = new CliOption(CodegenConstants.SERIALIZATION_LIBRARY, SERIALIZATION_LIBRARY_DESC);
        cliOptions.add(serializationLibraryOpt.defaultValue(serializationLibrary.name()));

        cliOptions.add(CliOption.newBoolean(USE_NON_ASCII_HEADERS, "Allow to use non-ascii headers with the okhttp library"));
        cliOptions.add(CliOption.newBoolean(USE_RESPONSE_AS_RETURN_TYPE, "When using retrofit2 and coroutines, use `Response`<`T`> as return type instead of `T`.", true));
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "kotlin";
    }

    @Override
    public String getHelp() {
        return "Generates a Kotlin client.";
    }

    public boolean getGenerateRoomModels() {
        return generateRoomModels;
    }

    public boolean getOmitGradleWrapper() {
        return omitGradleWrapper;
    }

    public boolean getGenerateOneOfAnyOfWrappers() {
        return generateOneOfAnyOfWrappers;
    }

    public void setGenerateRoomModels(Boolean generateRoomModels) {
        this.generateRoomModels = generateRoomModels;
    }

    public void setUseRxJava3(boolean useRxJava3) {
        if (useRxJava3) {
            this.doNotUseRxAndCoroutines = false;
            this.useCoroutines = false;
        }
        this.useRxJava3 = useRxJava3;
    }

    public void setDoNotUseRxAndCoroutines(boolean doNotUseRxAndCoroutines) {
        if (doNotUseRxAndCoroutines) {
            this.useRxJava3 = false;
            this.useCoroutines = false;
        }
        this.doNotUseRxAndCoroutines = doNotUseRxAndCoroutines;
    }

    public void setUseCoroutines(boolean useCoroutines) {
        if (useCoroutines) {
            this.useRxJava3 = false;
            this.doNotUseRxAndCoroutines = false;
        }
        this.useCoroutines = useCoroutines;
    }

    /**
     * Sets the serialization engine for Kotlin
     *
     * @param enumSerializationLibrary The string representation of the serialization library as defined by
     *                                 {@link org.openapitools.codegen.languages.KotlinClientCodegen.SERIALIZATION_LIBRARY_TYPE}
     */
    public void setSerializationLibrary(final String enumSerializationLibrary) {
        try {
            this.serializationLibrary = SERIALIZATION_LIBRARY_TYPE.valueOf(enumSerializationLibrary);
        } catch (IllegalArgumentException ex) {
            StringBuilder sb = new StringBuilder(enumSerializationLibrary + " is an invalid enum property naming option. Please choose from:");
            for (SERIALIZATION_LIBRARY_TYPE t : SERIALIZATION_LIBRARY_TYPE.values()) {
                sb.append("\n  ").append(t.name());
            }
            throw new RuntimeException(sb.toString());
        }
    }

    @Override
    public String modelFilename(String templateName, String modelName) {
        String suffix = modelTemplateFiles().get(templateName);
        // If this was a proper template method, i wouldn't have to make myself throw up by doing this....
        if (getGenerateRoomModels() && suffix.startsWith("RoomModel")) {
            return roomModelFileFolder() + File.separator + toModelFilename(modelName) + suffix;
        } else {
            return modelFileFolder() + File.separator + toModelFilename(modelName) + suffix;
        }
    }

    public String roomModelFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + roomModelPackage.replace('.', File.separatorChar);
    }

    @Override
    public void processOpts() {
        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        } else {
            // Set the value to defaults if we haven't overridden
            if (MULTIPLATFORM.equals(getLibrary())) {
                setSourceFolder("src/commonMain/kotlin");
            } else if (JVM_VOLLEY.equals(getLibrary())) {
                // Android plugin wants it's source in java
                setSourceFolder("src/main/java");
            } else {
                setSourceFolder(super.sourceFolder);
            }
            additionalProperties.put(CodegenConstants.SOURCE_FOLDER, this.sourceFolder);
        }

        super.processOpts();

        boolean hasRx3 = additionalProperties.containsKey(USE_RX_JAVA3);
        boolean hasCoroutines = additionalProperties.containsKey(USE_COROUTINES);
        int optionCount = 0;
        if (hasRx3) {
            optionCount++;
        }
        if (hasCoroutines) {
            optionCount++;
        }
        boolean hasConflict = optionCount > 1;

        // RxJava & Coroutines
        if (hasConflict) {
            LOGGER.warn("You specified RxJava versions 1 and 2 and 3 or Coroutines together, please choose one of them.");
        } else if (hasRx3) {
            this.setUseRxJava3(Boolean.parseBoolean(additionalProperties.get(USE_RX_JAVA3).toString()));
        } else if (hasCoroutines) {
            this.setUseCoroutines(Boolean.parseBoolean(additionalProperties.get(USE_COROUTINES).toString()));
        }

        if (!hasRx3 && !hasCoroutines) {
            setDoNotUseRxAndCoroutines(true);
            additionalProperties.put(DO_NOT_USE_RX_AND_COROUTINES, true);
        }

        // infrastructure destination folder
        final String infrastructureFolder = (sourceFolder + File.separator + packageName + File.separator + "infrastructure").replace(".", "/");
        authFolder = (sourceFolder + File.separator + packageName + File.separator + "auth").replace(".", "/");

        // request destination folder
        final String requestFolder = (sourceFolder + File.separator + packageName + File.separator + "request").replace(".", "/");

        // auth destination folder
        final String authFolder = (sourceFolder + File.separator + packageName + File.separator + "auth").replace(".", "/");

        // additional properties
        if (additionalProperties.containsKey(DATE_LIBRARY)) {
            setDateLibrary(additionalProperties.get(DATE_LIBRARY).toString());
        }

        if (additionalProperties.containsKey(REQUEST_DATE_CONVERTER)) {
            setRequestDateConverter(additionalProperties.get(REQUEST_DATE_CONVERTER).toString());
        }

        if (additionalProperties.containsKey(OMIT_GRADLE_WRAPPER)) {
            setOmitGradleWrapper(Boolean.parseBoolean(additionalProperties.get(OMIT_GRADLE_WRAPPER).toString()));
        }

        if (additionalProperties.containsKey(CodegenConstants.SERIALIZATION_LIBRARY)) {
            setSerializationLibrary((String) additionalProperties.get(CodegenConstants.SERIALIZATION_LIBRARY));
            additionalProperties.put(this.serializationLibrary.name(), true);
        } else {
            additionalProperties.put(this.serializationLibrary.name(), true);
        }

        if (additionalProperties.containsKey(MAP_FILE_BINARY_TO_BYTE_ARRAY)) {
            setMapFileBinaryToByteArray(convertPropertyToBooleanAndWriteBack(MAP_FILE_BINARY_TO_BYTE_ARRAY));
        }
        additionalProperties.put(MAP_FILE_BINARY_TO_BYTE_ARRAY, mapFileBinaryToByteArray);
        if (mapFileBinaryToByteArray) {
            typeMapping.put("file", "kotlin.ByteArray");
            typeMapping.put("binary", "kotlin.ByteArray");
        }

        if (additionalProperties.containsKey(GENERATE_ONEOF_ANYOF_WRAPPERS)) {
            setGenerateOneOfAnyOfWrappers(Boolean.parseBoolean(additionalProperties.get(GENERATE_ONEOF_ANYOF_WRAPPERS).toString()));
        }

        if (additionalProperties.containsKey(FAIL_ON_UNKNOWN_PROPERTIES)) {
            setFailOnUnknownProperties(Boolean.parseBoolean(additionalProperties.get(FAIL_ON_UNKNOWN_PROPERTIES).toString()));
        } else {
            additionalProperties.put(FAIL_ON_UNKNOWN_PROPERTIES, false);
            setFailOnUnknownProperties(false);
        }

        commonSupportingFiles();

        switch (getLibrary()) {
            case JVM_KTOR:
                processJVMKtorLibrary(infrastructureFolder);
                break;
            case JVM_OKHTTP4:
                processJVMOkHttpLibrary(infrastructureFolder);
                break;
            case JVM_VOLLEY:
                processJVMVolleyLibrary(infrastructureFolder, requestFolder, authFolder);
                break;
            case JVM_RETROFIT2:
                processJVMRetrofit2Library(infrastructureFolder);
                break;
            case JVM_SPRING_WEBCLIENT:
                processJvmSpringWebClientLibrary(infrastructureFolder);
                break;
            case JVM_SPRING_RESTCLIENT:
                processJvmSpringRestClientLibrary(infrastructureFolder);
                break;
            case MULTIPLATFORM:
                processMultiplatformLibrary(infrastructureFolder);
                break;
            case JVM_VERTX:
                processJVMVertXLibrary(infrastructureFolder);
                break;
            default:
                break;
        }

        processDateLibrary();
        processRequestDateConverter();

        if (additionalProperties.containsKey(COLLECTION_TYPE)) {
            setCollectionType(additionalProperties.get(COLLECTION_TYPE).toString());
        }

        if (CollectionType.LIST.value.equals(collectionType)) {
            if (isModelMutable()) {
                typeMapping.put("array", "kotlin.collections.MutableList");
                typeMapping.put("list", "kotlin.collections.MutableList");
            } else {
                typeMapping.put("array", "kotlin.collections.List");
                typeMapping.put("list", "kotlin.collections.List");
            }
            additionalProperties.put("isList", true);
        }

        if (usesRetrofit2Library()) {
            boolean hasOAuthMethods = ProcessUtils.hasOAuthMethods(openAPI);

            if (hasOAuthMethods) {
                supportingFiles.add(new SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"));
                supportingFiles.add(new SupportingFile("auth/OAuthFlow.kt.mustache", authFolder, "OAuthFlow.kt"));
                supportingFiles.add(new SupportingFile("auth/OAuthOkHttpClient.kt.mustache", authFolder, "OAuthOkHttpClient.kt"));
            }

            if (hasOAuthMethods || ProcessUtils.hasApiKeyMethods(openAPI)) {
                supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.kt.mustache", authFolder, "ApiKeyAuth.kt"));
            }

            if (ProcessUtils.hasHttpBearerMethods(openAPI)) {
                supportingFiles.add(new SupportingFile("auth/HttpBearerAuth.kt.mustache", authFolder, "HttpBearerAuth.kt"));
            }

            if (ProcessUtils.hasHttpBasicMethods(openAPI)) {
                supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.kt.mustache", authFolder, "HttpBasicAuth.kt"));
            }
        }

        additionalProperties.put("sanitizeGeneric", (Mustache.Lambda) (fragment, writer) -> {
            String content = fragment.execute();
            for (final String s : List.of("<", ">", ",", " ", ".")) {
                content = content.replace(s, "");
            }
            writer.write(content);
        });
        // When a path is added to a Javadoc, if it ends with a `/*` is will cause a compiler error
        // as the parser interrupts that as a start of a multiline comment.
        // We replace paths like `/v1/foo/*` with `/v1/foo/<*>` to avoid this
        additionalProperties.put("sanitizePathComment", new ReplaceAllLambda("\\/\\*", "/<*>"));
    }

    private void processDateLibrary() {
        if (DateLibrary.THREETENBP.value.equals(dateLibrary) || DateLibrary.THREETENBP_LOCALDATETIME.value.equals(dateLibrary)) {
            processThreeTenBpDate(dateLibrary);
        } else if (DateLibrary.STRING.value.equals(dateLibrary)) {
            processStringDate();
        } else if (DateLibrary.JAVA8.value.equals(dateLibrary) || DateLibrary.JAVA8_LOCALDATETIME.value.equals(dateLibrary)) {
            processJava8Date(dateLibrary);
        } else if (DateLibrary.KOTLINX_DATETIME.value.equals(dateLibrary)) {
            processKotlinxDate();
        }
    }

    private void processRequestDateConverter() {
        if (RequestDateConverter.TO_JSON.value.equals(requestDateConverter)) {
            additionalProperties.put(RequestDateConverter.TO_JSON.value, true);
        } else if (RequestDateConverter.TO_STRING.value.equals(requestDateConverter)) {
            additionalProperties.put(RequestDateConverter.TO_STRING.value, true);
        }
    }

    private void processThreeTenBpDate(String dateLibrary) {
        additionalProperties.put(DateLibrary.THREETENBP.value, true);
        typeMapping.put("date", "LocalDate");
        importMapping.put("LocalDate", "org.threeten.bp.LocalDate");
        defaultIncludes.add("org.threeten.bp.LocalDate");

        if (dateLibrary.equals(DateLibrary.THREETENBP.value)) {
            typeMapping.put("date-time", "org.threeten.bp.OffsetDateTime");
            typeMapping.put("DateTime", "OffsetDateTime");
            importMapping.put("OffsetDateTime", "org.threeten.bp.OffsetDateTime");
            defaultIncludes.add("org.threeten.bp.OffsetDateTime");
        } else if (dateLibrary.equals(DateLibrary.THREETENBP_LOCALDATETIME.value)) {
            typeMapping.put("date-time", "org.threeten.bp.LocalDateTime");
            typeMapping.put("DateTime", "LocalDateTime");
            importMapping.put("LocalDateTime", "org.threeten.bp.LocalDateTime");
            defaultIncludes.add("org.threeten.bp.LocalDateTime");
        }
    }

    private void processStringDate() {
        typeMapping.put("date-time", "kotlin.String");
        typeMapping.put("date", "kotlin.String");
        typeMapping.put("Date", "kotlin.String");
        typeMapping.put("DateTime", "kotlin.String");
    }

    private void processJava8Date(String dateLibrary) {
        additionalProperties.put(DateLibrary.JAVA8.value, true);

        if (dateLibrary.equals(DateLibrary.JAVA8.value)) {
            typeMapping.put("date-time", "java.time.OffsetDateTime");
            typeMapping.put("DateTime", "OffsetDateTime");
            importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        } else if (dateLibrary.equals(DateLibrary.JAVA8_LOCALDATETIME.value)) {
            typeMapping.put("date-time", "java.time.LocalDateTime");
            typeMapping.put("DateTime", "LocalDateTime");
            importMapping.put("LocalDateTime", "java.time.LocalDateTime");
        }
    }

    private void processKotlinxDate() {
        additionalProperties.put(DateLibrary.KOTLINX_DATETIME.value, true);

        typeMapping.put("date-time", "Instant");
        typeMapping.put("date", "LocalDate");
        typeMapping.put("time", "LocalTime");

        typeMapping.put("DateTime", "Instant");
        typeMapping.put("Date", "LocalDate");
        typeMapping.put("Time", "LocalTime");

        importMapping.put("Instant", "kotlinx.datetime.Instant");
        importMapping.put("LocalDate", "kotlinx.datetime.LocalDate");
        importMapping.put("LocalTime", "kotlinx.datetime.LocalTime");
    }

    private void processJVMRetrofit2Library(String infrastructureFolder) {
        additionalProperties.put(JVM, true);
        additionalProperties.put(JVM_RETROFIT2, true);
        setUseResponseAsReturnType();
        supportingFiles.add(new SupportingFile("infrastructure/ApiClient.kt.mustache", infrastructureFolder, "ApiClient.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ResponseExt.kt.mustache", infrastructureFolder, "ResponseExt.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/CollectionFormats.kt.mustache", infrastructureFolder, "CollectionFormats.kt"));
        addSupportingSerializerAdapters(infrastructureFolder);
    }

    private void setUseResponseAsReturnType() {
        if (additionalProperties.containsKey(USE_RESPONSE_AS_RETURN_TYPE)) {
            convertPropertyToBooleanAndWriteBack(USE_RESPONSE_AS_RETURN_TYPE);
        } else {
            // default is true for backward compatibility
            additionalProperties.put(USE_RESPONSE_AS_RETURN_TYPE, true);
        }
    }

    private void processJVMVolleyLibrary(String infrastructureFolder, String requestFolder, String authFolder) {

        additionalProperties.put(JVM, true);
        additionalProperties.put(JVM_VOLLEY, true);

        if (additionalProperties.containsKey(GENERATE_ROOM_MODELS)) {
            this.setGenerateRoomModels(convertPropertyToBooleanAndWriteBack(GENERATE_ROOM_MODELS));
            // Hide this option behind a property getter and setter in case we need to check it elsewhere
            if (getGenerateRoomModels()) {
                modelTemplateFiles.put("model_room.mustache", "RoomModel.kt");
                supportingFiles.add(new SupportingFile("infrastructure/ITransformForStorage.mustache", infrastructureFolder, "ITransformForStorage.kt"));

            }
        } else {
            additionalProperties.put(GENERATE_ROOM_MODELS, generateRoomModels);
        }

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            if (!additionalProperties.containsKey(ROOM_MODEL_PACKAGE))
                this.setRoomModelPackage(packageName + ".models.room");
            else
                this.setRoomModelPackage(additionalProperties.get(ROOM_MODEL_PACKAGE).toString());
        }
        additionalProperties.put(ROOM_MODEL_PACKAGE, roomModelPackage);

        supportingFiles.add(new SupportingFile("infrastructure/CollectionFormats.kt.mustache", infrastructureFolder, "CollectionFormats.kt"));

        // We have auth related partial files, so they can be overridden, but don't generate them explicitly
        supportingFiles.add(new SupportingFile("request/GsonRequest.mustache", requestFolder, "GsonRequest.kt"));
        supportingFiles.add(new SupportingFile("request/IRequestFactory.mustache", requestFolder, "IRequestFactory.kt"));
        supportingFiles.add(new SupportingFile("request/RequestFactory.mustache", requestFolder, "RequestFactory.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/CollectionFormats.kt.mustache", infrastructureFolder, "CollectionFormats.kt"));

        if (getSerializationLibrary() != SERIALIZATION_LIBRARY_TYPE.gson) {
            throw new RuntimeException("This library currently only supports gson serialization. Try adding '--additional-properties serializationLibrary=gson' to your command.");
        }
        addSupportingSerializerAdapters(infrastructureFolder);
        supportingFiles.remove(new SupportingFile("jvm-common/infrastructure/Serializer.kt.mustache", infrastructureFolder, "Serializer.kt"));

    }

    private void addSupportingSerializerAdapters(final String infrastructureFolder) {
        supportingFiles.add(new SupportingFile("jvm-common/infrastructure/Serializer.kt.mustache", infrastructureFolder, "Serializer.kt"));

        switch (getSerializationLibrary()) {
            case moshi:
                if (enumUnknownDefaultCase) {
                    supportingFiles.add(new SupportingFile("jvm-common/infrastructure/SerializerHelper.kt.mustache", infrastructureFolder, "SerializerHelper.kt"));
                }
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/ByteArrayAdapter.kt.mustache", infrastructureFolder, "ByteArrayAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/UUIDAdapter.kt.mustache", infrastructureFolder, "UUIDAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalDateAdapter.kt.mustache", infrastructureFolder, "LocalDateAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalDateTimeAdapter.kt.mustache", infrastructureFolder, "LocalDateTimeAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/OffsetDateTimeAdapter.kt.mustache", infrastructureFolder, "OffsetDateTimeAdapter.kt"));
                addKotlinxDateTimeAdapters(infrastructureFolder);
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/BigDecimalAdapter.kt.mustache", infrastructureFolder, "BigDecimalAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/BigIntegerAdapter.kt.mustache", infrastructureFolder, "BigIntegerAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/URIAdapter.kt.mustache", infrastructureFolder, "URIAdapter.kt"));
                break;

            case gson:
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/ByteArrayAdapter.kt.mustache", infrastructureFolder, "ByteArrayAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalDateAdapter.kt.mustache", infrastructureFolder, "LocalDateAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalDateTimeAdapter.kt.mustache", infrastructureFolder, "LocalDateTimeAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/OffsetDateTimeAdapter.kt.mustache", infrastructureFolder, "OffsetDateTimeAdapter.kt"));
                addKotlinxDateTimeAdapters(infrastructureFolder);
                break;

            case jackson:
                break;

            case kotlinx_serialization:
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/AtomicBooleanAdapter.kt.mustache", infrastructureFolder, "AtomicBooleanAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/AtomicIntegerAdapter.kt.mustache", infrastructureFolder, "AtomicIntegerAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/AtomicLongAdapter.kt.mustache", infrastructureFolder, "AtomicLongAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/URIAdapter.kt.mustache", infrastructureFolder, "URIAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/URLAdapter.kt.mustache", infrastructureFolder, "URLAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/BigIntegerAdapter.kt.mustache", infrastructureFolder, "BigIntegerAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/BigDecimalAdapter.kt.mustache", infrastructureFolder, "BigDecimalAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalDateAdapter.kt.mustache", infrastructureFolder, "LocalDateAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalDateTimeAdapter.kt.mustache", infrastructureFolder, "LocalDateTimeAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/OffsetDateTimeAdapter.kt.mustache", infrastructureFolder, "OffsetDateTimeAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/UUIDAdapter.kt.mustache", infrastructureFolder, "UUIDAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/StringBuilderAdapter.kt.mustache", infrastructureFolder, "StringBuilderAdapter.kt"));
                supportingFiles.add(new SupportingFile("jvm-common/infrastructure/proguard-rules.pro.mustache", "", "proguard-rules.pro"));
                break;
        }
    }

    private void addKotlinxDateTimeAdapters(final String infrastructureFolder) {
        if (DateLibrary.KOTLINX_DATETIME.value.equals(dateLibrary)) {
            supportingFiles.add(new SupportingFile("jvm-common/infrastructure/InstantAdapter.kt.mustache", infrastructureFolder, "InstantAdapter.kt"));
            supportingFiles.add(new SupportingFile("jvm-common/infrastructure/LocalTimeAdapter.kt.mustache", infrastructureFolder, "LocalTimeAdapter.kt"));
        }
    }

    private void processJVMKtorLibrary(final String infrastructureFolder) {
        // in future kotlinx.serialization may be added
        if (this.serializationLibrary != SERIALIZATION_LIBRARY_TYPE.gson && this.serializationLibrary != SERIALIZATION_LIBRARY_TYPE.jackson) {
            this.serializationLibrary = SERIALIZATION_LIBRARY_TYPE.jackson;
        }

        additionalProperties.put(JVM, true);
        additionalProperties.put(JVM_KTOR, true);

        defaultIncludes.add("io.ktor.client.request.forms.InputProvider");

        importMapping.put("InputProvider", "io.ktor.client.request.forms.InputProvider");

        supportingFiles.add(new SupportingFile("infrastructure/ApiAbstractions.kt.mustache", infrastructureFolder, "ApiAbstractions.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ApiClient.kt.mustache", infrastructureFolder, "ApiClient.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/HttpResponse.kt.mustache", infrastructureFolder, "HttpResponse.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/RequestConfig.kt.mustache", infrastructureFolder, "RequestConfig.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/RequestMethod.kt.mustache", infrastructureFolder, "RequestMethod.kt"));

        supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.kt.mustache", authFolder, "ApiKeyAuth.kt"));
        supportingFiles.add(new SupportingFile("auth/Authentication.kt.mustache", authFolder, "Authentication.kt"));
        supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.kt.mustache", authFolder, "HttpBasicAuth.kt"));
        supportingFiles.add(new SupportingFile("auth/HttpBearerAuth.kt.mustache", authFolder, "HttpBearerAuth.kt"));
        supportingFiles.add(new SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"));
    }

    /**
     * Process Vert.x client options
     *
     * @param infrastructureFolder infrastructure destination folder
     */
    private void processJVMVertXLibrary(final String infrastructureFolder) {
        supportingFiles.add(new SupportingFile("infrastructure/ApiAbstractions.kt.mustache", infrastructureFolder, "ApiAbstractions.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ApiClient.kt.mustache", infrastructureFolder, "ApiClient.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/Errors.kt.mustache", infrastructureFolder, "Errors.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ApiResponse.kt.mustache", infrastructureFolder, "ApiResponse.kt"));
        addSupportingSerializerAdapters(infrastructureFolder);

        additionalProperties.put(JVM, true);
        additionalProperties.put(JVM_VERTX, true);
    }

    private void processJVMOkHttpLibrary(final String infrastructureFolder) {
        commonJvmMultiplatformSupportingFiles(infrastructureFolder);
        addSupportingSerializerAdapters(infrastructureFolder);

        additionalProperties.put(JVM, true);
        additionalProperties.put(JVM_OKHTTP, true);

        if (JVM_OKHTTP4.equals(getLibrary())) {
            additionalProperties.put(JVM_OKHTTP4, true);
        }

        supportedLibraries.put(JVM_OKHTTP, "A workaround to use the same template folder for both 'jvm-okhttp3' and 'jvm-okhttp4'.");
        setLibrary(JVM_OKHTTP);

        // jvm specific supporting files
        supportingFiles.add(new SupportingFile("infrastructure/Errors.kt.mustache", infrastructureFolder, "Errors.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ResponseExtensions.kt.mustache", infrastructureFolder, "ResponseExtensions.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ApiResponse.kt.mustache", infrastructureFolder, "ApiResponse.kt"));
    }

    private void processJvmSpring(final String infrastructureFolder) {
        if (getSerializationLibrary() != SERIALIZATION_LIBRARY_TYPE.jackson) {
            throw new RuntimeException("This library currently only supports jackson serialization. Try adding '--additional-properties serializationLibrary=jackson' to your command.");
        }

        commonJvmMultiplatformSupportingFiles(infrastructureFolder);
        addSupportingSerializerAdapters(infrastructureFolder);

        additionalProperties.put(JVM_SPRING, true);
        additionalProperties.put(JVM, true);
    }

    private void processJvmSpringWebClientLibrary(final String infrastructureFolder) {
        processJvmSpring(infrastructureFolder);
        additionalProperties.put(JVM_SPRING_WEBCLIENT, true);
    }

    private void processJvmSpringRestClientLibrary(final String infrastructureFolder) {
        if (additionalProperties.getOrDefault(USE_SPRING_BOOT3, false).equals(false)) {
            throw new RuntimeException("This library must use Spring Boot 3. Try adding '--additional-properties useSpringBoot3=true' to your command.");
        }

        processJvmSpring(infrastructureFolder);
        additionalProperties.put(JVM_SPRING_RESTCLIENT, true);
    }

    private void processMultiplatformLibrary(final String infrastructureFolder) {
        commonJvmMultiplatformSupportingFiles(infrastructureFolder);
        additionalProperties.put(MULTIPLATFORM, true);

        if (!DateLibrary.STRING.value.equals(dateLibrary) && !DateLibrary.KOTLINX_DATETIME.value.equals(dateLibrary)) {
            throw new RuntimeException("Multiplatform only supports string and kotlinx-datetime. Try adding '--additional-properties dateLibrary=kotlinx-datetime' to your command.");
        }

        setRequestDateConverter(RequestDateConverter.TO_STRING.value);

        // multiplatform default includes
        defaultIncludes.add("io.ktor.client.request.forms.InputProvider");
        defaultIncludes.add(packageName + ".infrastructure.Base64ByteArray");
        defaultIncludes.add(packageName + ".infrastructure.OctetByteArray");

        // multiplatform type mapping
        typeMapping.put("number", "kotlin.Double");
        typeMapping.put("file", "OctetByteArray");
        typeMapping.put("binary", "OctetByteArray");
        typeMapping.put("ByteArray", "Base64ByteArray");
        typeMapping.put("object", "kotlin.String");  // kotlin.Any not serializable

        // multiplatform import mapping
        importMapping.put("BigDecimal", "kotlin.Double");
        importMapping.put("UUID", "kotlin.String");
        importMapping.put("URI", "kotlin.String");
        importMapping.put("InputProvider", "io.ktor.client.request.forms.InputProvider");
        importMapping.put("File", packageName + ".infrastructure.OctetByteArray");
        importMapping.put("Timestamp", "kotlin.String");
        importMapping.put("LocalDateTime", "kotlin.String");
        importMapping.put("LocalDate", "kotlin.String");
        importMapping.put("LocalTime", "kotlin.String");
        importMapping.put("Base64ByteArray", packageName + ".infrastructure.Base64ByteArray");
        importMapping.put("OctetByteArray", packageName + ".infrastructure.OctetByteArray");

        // multiplatform specific supporting files
        supportingFiles.add(new SupportingFile("infrastructure/Base64ByteArray.kt.mustache", infrastructureFolder, "Base64ByteArray.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/Bytes.kt.mustache", infrastructureFolder, "Bytes.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/HttpResponse.kt.mustache", infrastructureFolder, "HttpResponse.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/OctetByteArray.kt.mustache", infrastructureFolder, "OctetByteArray.kt"));

        // multiplatform specific auth
        supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.kt.mustache", authFolder, "ApiKeyAuth.kt"));
        supportingFiles.add(new SupportingFile("auth/Authentication.kt.mustache", authFolder, "Authentication.kt"));
        supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.kt.mustache", authFolder, "HttpBasicAuth.kt"));
        supportingFiles.add(new SupportingFile("auth/HttpBearerAuth.kt.mustache", authFolder, "HttpBearerAuth.kt"));
        supportingFiles.add(new SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"));
    }


    private void commonJvmMultiplatformSupportingFiles(String infrastructureFolder) {
        supportingFiles.add(new SupportingFile("infrastructure/ApiClient.kt.mustache", infrastructureFolder, "ApiClient.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/ApiAbstractions.kt.mustache", infrastructureFolder, "ApiAbstractions.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/PartConfig.kt.mustache", infrastructureFolder, "PartConfig.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/RequestConfig.kt.mustache", infrastructureFolder, "RequestConfig.kt"));
        supportingFiles.add(new SupportingFile("infrastructure/RequestMethod.kt.mustache", infrastructureFolder, "RequestMethod.kt"));
    }

    private void commonSupportingFiles() {
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        if (getLibrary().equals(MULTIPLATFORM)) {
            supportingFiles.add(new SupportingFile("build.gradle.kts.mustache", "", "build.gradle.kts"));
            supportingFiles.add(new SupportingFile("settings.gradle.kts.mustache", "", "settings.gradle.kts"));
        } else if (getLibrary().equals(JVM_VOLLEY)) {
            supportingFiles.add(new SupportingFile("build.mustache", "", "build.gradle"));
            supportingFiles.add(new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
            supportingFiles.add(new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
            supportingFiles.add(new SupportingFile("manifest.mustache", "", "src/main/AndroidManifest.xml"));
        } else {
            supportingFiles.add(new SupportingFile("build.gradle.mustache", "", "build.gradle"));
            supportingFiles.add(new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
        }

        // gradle wrapper supporting files
        if (!getOmitGradleWrapper()) {
            supportingFiles.add(new SupportingFile("gradlew.mustache", "", "gradlew"));
            supportingFiles.add(new SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"));
            supportingFiles.add(new SupportingFile("gradle-wrapper.properties.mustache", "gradle.wrapper".replace(".", File.separator), "gradle-wrapper.properties"));
            supportingFiles.add(new SupportingFile("gradle-wrapper.jar", "gradle.wrapper".replace(".", File.separator), "gradle-wrapper.jar"));
        }
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        ModelsMap objects = super.postProcessModels(objs);

        for (ModelMap mo : objects.getModels()) {
            CodegenModel cm = mo.getModel();
            if (getGenerateRoomModels() || getGenerateOneOfAnyOfWrappers()) {
                cm.vendorExtensions.put("x-has-data-class-body", true);
            }

            // escape the variable base name for use as a string literal
            List<CodegenProperty> vars = Stream.of(
                            cm.vars,
                            cm.allVars,
                            cm.optionalVars,
                            cm.requiredVars,
                            cm.readOnlyVars,
                            cm.readWriteVars,
                            cm.parentVars
                    )
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            for (CodegenProperty var : vars) {
                var.vendorExtensions.put(VENDOR_EXTENSION_BASE_NAME_LITERAL, var.baseName.replace("$", "\\$"));
            }
        }

        return objects;
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        objs = super.postProcessAllModels(objs);
        if (getSerializationLibrary() == SERIALIZATION_LIBRARY_TYPE.kotlinx_serialization) {
            // The loop removes unneeded variables so commas are handled correctly in the related templates
            for (Map.Entry<String, ModelsMap> modelsMap : objs.entrySet()) {
                for (ModelMap mo : modelsMap.getValue().getModels()) {
                    CodegenModel cm = mo.getModel();
                    CodegenDiscriminator discriminator = cm.getDiscriminator();
                    if (discriminator == null) {
                        continue;
                    }
                    // Remove discriminator property from the base class, it is not needed in the generated code
                    getAllVarProperties(cm).forEach(list -> list.removeIf(var -> var.name == discriminator.getPropertyName()));

                    for (CodegenDiscriminator.MappedModel mappedModel : discriminator.getMappedModels()) {
                        // Add the mapping name to additionalProperties.disciminatorValue
                        // The mapping name is used to define SerializedName, which in result makes derived classes
                        // found by kotlinx-serialization during deserialization
                        CodegenProperty additionalProperties = mappedModel.getModel().getAdditionalProperties();
                        if (additionalProperties == null) {
                            additionalProperties = new CodegenProperty();
                            mappedModel.getModel().setAdditionalProperties(additionalProperties);
                        }
                        additionalProperties.discriminatorValue = mappedModel.getMappingName();
                        // Remove the discriminator property from the derived class, it is not needed in the generated code
                        getAllVarProperties(mappedModel.getModel()).forEach(list -> list.removeIf(prop -> prop.name == discriminator.getPropertyName()));
                    }

                }
            }
        }
        return objs;
    }
    private Stream<List<CodegenProperty>> getAllVarProperties(CodegenModel model) {
        return Stream.of(model.vars, model.allVars, model.optionalVars, model.requiredVars, model.readOnlyVars, model.readWriteVars);
    }

    private boolean usesRetrofit2Library() {
        return getLibrary() != null && getLibrary().contains(JVM_RETROFIT2);
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        super.postProcessOperationsWithModels(objs, allModels);
        OperationMap operations = objs.getOperations();
        boolean isResponseFile = false;
        if (operations != null) {
            List<CodegenOperation> ops = operations.getOperation();
            for (CodegenOperation operation : ops) {
                isResponseFile = isResponseFile || operation.isResponseFile;

                if (JVM_RETROFIT2.equals(getLibrary()) && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith("/")) {
                    operation.path = operation.path.substring(1);
                }

                if (JVM_OKHTTP.equals(getLibrary()) || JVM_OKHTTP4.equals(getLibrary())) {
                    // Ideally we would do content negotiation to choose the best mediatype, but that would be a next step.
                    // For now we take the first mediatype we can parse and send that.
                    Predicate<Map<String, String>> isSerializable = typeMapping -> {
                        String mediaTypeValue = typeMapping.get("mediaType");
                        if (mediaTypeValue == null)
                            return false;
                        // match on first part in mediaTypes like 'application/json; charset=utf-8'
                        int endIndex = mediaTypeValue.indexOf(';');
                        String mediaType = (endIndex == -1
                                ? mediaTypeValue
                                : mediaTypeValue.substring(0, endIndex)
                        ).trim();
                        return "multipart/form-data".equals(mediaType)
                                || "application/x-www-form-urlencoded".equals(mediaType)
                                || (mediaType.startsWith("application/") && mediaType.endsWith("json"))
                                || "application/octet-stream".equals(mediaType)
                                || "text/plain".equals(mediaType);
                    };
                    operation.consumes = operation.consumes == null ? null : operation.consumes.stream()
                            .filter(isSerializable)
                            .limit(1)
                            .collect(Collectors.toList());
                    operation.hasConsumes = operation.consumes != null && !operation.consumes.isEmpty();

                    operation.produces = operation.produces == null ? null : operation.produces.stream()
                            .filter(isSerializable)
                            .collect(Collectors.toList());
                    operation.hasProduces = operation.produces != null && !operation.produces.isEmpty();
                }

                // set multipart against all relevant operations
                if (operation.hasConsumes == Boolean.TRUE) {
                    if (isMultipartType(operation.consumes)) {
                        operation.isMultipart = Boolean.TRUE;
                    }
                }

                // import okhttp3.MultipartBody if any parameter is a file
                for (CodegenParameter param : operation.allParams) {
                    if (Boolean.TRUE.equals(param.isFile)) {
                        operations.put("x-kotlin-multipart-import", true);
                    }

                    if (param.isQueryParam && "form".equals(param.style) && param.isExplode && param.isModel) {
                        // query parameter (style: form, explode) referencing models need to import
                        // models defined in the properties of the models
                        operations.put("x-kotlin-import-models", true);
                    }
                }

                if (usesRetrofit2Library() && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith("/")) {
                    operation.path = operation.path.substring(1);
                }

                // sorting operation parameters to make sure path params are parsed before query params
                if (operation.allParams != null) {
                    sort(operation.allParams, (one, another) -> {
                        if (one.isPathParam && another.isQueryParam) {
                            return -1;
                        }
                        if (one.isQueryParam && another.isPathParam) {
                            return 1;
                        }

                        return 0;
                    });
                }

                // modify the data type of binary form parameters to a more friendly type for ktor builds
                if ((JVM_KTOR.equals(getLibrary()) || MULTIPLATFORM.equals(getLibrary())) && operation.allParams != null) {
                    for (CodegenParameter param : operation.allParams) {
                        if (param.dataFormat != null && param.dataFormat.equals("binary")) {
                            if (param.isContainer) {
                                param.baseType = param.dataType = typeMapping.get(collectionType) + "<io.ktor.client.request.forms.FormPart<io.ktor.client.request.forms.InputProvider>>";
                            } else {
                                param.baseType = param.dataType = "io.ktor.client.request.forms.FormPart<io.ktor.client.request.forms.InputProvider>";
                            }
                        }
                    }
                }
            }
        }
        objs.put("isResponseFile", isResponseFile);
        return objs;
    }

    private static boolean isMultipartType(List<Map<String, String>> consumes) {
        Map<String, String> firstType = consumes.get(0);
        if (firstType != null) {
            return "multipart/form-data".equals(firstType.get("mediaType"));
        }
        return false;
    }

    @Override
    public void postProcess() {
        System.out.println("################################################################################");
        System.out.println("# Thanks for using OpenAPI Generator.                                          #");
        System.out.println("# Please consider donation to help us maintain this project \uD83D\uDE4F                 #");
        System.out.println("# https://opencollective.com/openapi_generator/donate                          #");
        System.out.println("#                                                                              #");
        System.out.println("# This generator's contributed by Jim Schubert (https://github.com/jimschubert)#");
        System.out.println("# Please support his work directly via https://patreon.com/jimschubert \uD83D\uDE4F      #");
        System.out.println("################################################################################");
    }

    @Override
    public List<VendorExtension> getSupportedVendorExtensions() {
        var extensions = super.getSupportedVendorExtensions();
        extensions.add(VendorExtension.X_CLASS_EXTRA_ANNOTATION);
        extensions.add(VendorExtension.X_FIELD_EXTRA_ANNOTATION);
        return extensions;
    }
}
