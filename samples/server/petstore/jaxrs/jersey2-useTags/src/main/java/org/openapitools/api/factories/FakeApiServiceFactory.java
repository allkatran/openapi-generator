package org.openapitools.api.factories;

import org.openapitools.api.FakeApiService;
import org.openapitools.api.impl.FakeApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class FakeApiServiceFactory {
    private static final FakeApiService service = new FakeApiServiceImpl();

    public static FakeApiService getFakeApi() {
        return service;
    }
}
