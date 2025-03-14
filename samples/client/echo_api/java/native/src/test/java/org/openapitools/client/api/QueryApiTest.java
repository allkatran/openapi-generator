/*
 * Echo Server API
 * Echo Server API
 *
 * The version of the OpenAPI document: 0.1.0
 * Contact: team@openapitools.org
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.openapitools.client.api;

import org.openapitools.client.ApiException;
import org.openapitools.client.model.DataQuery;
import java.time.LocalDate;
import java.time.Instant;
import org.openapitools.client.model.Pet;
import org.openapitools.client.model.StringEnumRef;
import org.openapitools.client.model.TestQueryStyleDeepObjectExplodeTrueObjectAllOfQueryObjectParameter;
import org.openapitools.client.model.TestQueryStyleFormExplodeTrueArrayStringQueryObjectParameter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * API tests for QueryApi
 */
@Disabled
public class QueryApiTest {

    private final QueryApi api = new QueryApi();

    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testEnumRefStringTest() throws ApiException {
        String enumNonrefStringQuery = null;
        StringEnumRef enumRefStringQuery = null;
        String response = 
        api.testEnumRefString(enumNonrefStringQuery, enumRefStringQuery);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryDatetimeDateStringTest() throws ApiException {
        Instant datetimeQuery = null;
        LocalDate dateQuery = null;
        String stringQuery = null;
        String response = 
        api.testQueryDatetimeDateString(datetimeQuery, dateQuery, stringQuery);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryIntegerBooleanStringTest() throws ApiException {
        Integer integerQuery = null;
        Boolean booleanQuery = null;
        String stringQuery = null;
        String response = 
        api.testQueryIntegerBooleanString(integerQuery, booleanQuery, stringQuery);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleDeepObjectExplodeTrueObjectTest() throws ApiException {
        Pet queryObject = null;
        String response = 
        api.testQueryStyleDeepObjectExplodeTrueObject(queryObject);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleDeepObjectExplodeTrueObjectAllOfTest() throws ApiException {
        TestQueryStyleDeepObjectExplodeTrueObjectAllOfQueryObjectParameter queryObject = null;
        String response = 
        api.testQueryStyleDeepObjectExplodeTrueObjectAllOf(queryObject);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleFormExplodeFalseArrayIntegerTest() throws ApiException {
        List<Integer> queryObject = null;
        String response = 
        api.testQueryStyleFormExplodeFalseArrayInteger(queryObject);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleFormExplodeFalseArrayStringTest() throws ApiException {
        List<String> queryObject = null;
        String response = 
        api.testQueryStyleFormExplodeFalseArrayString(queryObject);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleFormExplodeTrueArrayStringTest() throws ApiException {
        TestQueryStyleFormExplodeTrueArrayStringQueryObjectParameter queryObject = null;
        String response = 
        api.testQueryStyleFormExplodeTrueArrayString(queryObject);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleFormExplodeTrueObjectTest() throws ApiException {
        Pet queryObject = null;
        String response = 
        api.testQueryStyleFormExplodeTrueObject(queryObject);
        
        // TODO: test validations
    }
    
    /**
     * Test query parameter(s)
     *
     * Test query parameter(s)
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void testQueryStyleFormExplodeTrueObjectAllOfTest() throws ApiException {
        DataQuery queryObject = null;
        String response = 
        api.testQueryStyleFormExplodeTrueObjectAllOf(queryObject);
        
        // TODO: test validations
    }
    
}
