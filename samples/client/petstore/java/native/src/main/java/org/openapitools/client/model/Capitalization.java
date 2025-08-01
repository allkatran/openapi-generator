/*
 * OpenAPI Petstore
 * This spec is mainly for testing Petstore server and contains fake endpoints, models. Please do not use this for any other purpose. Special characters: \" \\
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.openapitools.client.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import org.openapitools.client.ApiClient;
/**
 * Capitalization
 */
@JsonPropertyOrder({
  Capitalization.JSON_PROPERTY_SMALL_CAMEL,
  Capitalization.JSON_PROPERTY_CAPITAL_CAMEL,
  Capitalization.JSON_PROPERTY_SMALL_SNAKE,
  Capitalization.JSON_PROPERTY_CAPITAL_SNAKE,
  Capitalization.JSON_PROPERTY_SC_A_E_T_H_FLOW_POINTS,
  Capitalization.JSON_PROPERTY_A_T_T_N_A_M_E
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class Capitalization {
  public static final String JSON_PROPERTY_SMALL_CAMEL = "smallCamel";
  @javax.annotation.Nullable
  private String smallCamel;

  public static final String JSON_PROPERTY_CAPITAL_CAMEL = "CapitalCamel";
  @javax.annotation.Nullable
  private String capitalCamel;

  public static final String JSON_PROPERTY_SMALL_SNAKE = "small_Snake";
  @javax.annotation.Nullable
  private String smallSnake;

  public static final String JSON_PROPERTY_CAPITAL_SNAKE = "Capital_Snake";
  @javax.annotation.Nullable
  private String capitalSnake;

  public static final String JSON_PROPERTY_SC_A_E_T_H_FLOW_POINTS = "SCA_ETH_Flow_Points";
  @javax.annotation.Nullable
  private String scAETHFlowPoints;

  public static final String JSON_PROPERTY_A_T_T_N_A_M_E = "ATT_NAME";
  @javax.annotation.Nullable
  private String ATT_NAME;

  public Capitalization() { 
  }

  public Capitalization smallCamel(@javax.annotation.Nullable String smallCamel) {
    this.smallCamel = smallCamel;
    return this;
  }

  /**
   * Get smallCamel
   * @return smallCamel
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SMALL_CAMEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getSmallCamel() {
    return smallCamel;
  }


  @JsonProperty(JSON_PROPERTY_SMALL_CAMEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSmallCamel(@javax.annotation.Nullable String smallCamel) {
    this.smallCamel = smallCamel;
  }


  public Capitalization capitalCamel(@javax.annotation.Nullable String capitalCamel) {
    this.capitalCamel = capitalCamel;
    return this;
  }

  /**
   * Get capitalCamel
   * @return capitalCamel
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CAPITAL_CAMEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getCapitalCamel() {
    return capitalCamel;
  }


  @JsonProperty(JSON_PROPERTY_CAPITAL_CAMEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCapitalCamel(@javax.annotation.Nullable String capitalCamel) {
    this.capitalCamel = capitalCamel;
  }


  public Capitalization smallSnake(@javax.annotation.Nullable String smallSnake) {
    this.smallSnake = smallSnake;
    return this;
  }

  /**
   * Get smallSnake
   * @return smallSnake
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SMALL_SNAKE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getSmallSnake() {
    return smallSnake;
  }


  @JsonProperty(JSON_PROPERTY_SMALL_SNAKE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSmallSnake(@javax.annotation.Nullable String smallSnake) {
    this.smallSnake = smallSnake;
  }


  public Capitalization capitalSnake(@javax.annotation.Nullable String capitalSnake) {
    this.capitalSnake = capitalSnake;
    return this;
  }

  /**
   * Get capitalSnake
   * @return capitalSnake
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CAPITAL_SNAKE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getCapitalSnake() {
    return capitalSnake;
  }


  @JsonProperty(JSON_PROPERTY_CAPITAL_SNAKE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCapitalSnake(@javax.annotation.Nullable String capitalSnake) {
    this.capitalSnake = capitalSnake;
  }


  public Capitalization scAETHFlowPoints(@javax.annotation.Nullable String scAETHFlowPoints) {
    this.scAETHFlowPoints = scAETHFlowPoints;
    return this;
  }

  /**
   * Get scAETHFlowPoints
   * @return scAETHFlowPoints
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SC_A_E_T_H_FLOW_POINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getScAETHFlowPoints() {
    return scAETHFlowPoints;
  }


  @JsonProperty(JSON_PROPERTY_SC_A_E_T_H_FLOW_POINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setScAETHFlowPoints(@javax.annotation.Nullable String scAETHFlowPoints) {
    this.scAETHFlowPoints = scAETHFlowPoints;
  }


  public Capitalization ATT_NAME(@javax.annotation.Nullable String ATT_NAME) {
    this.ATT_NAME = ATT_NAME;
    return this;
  }

  /**
   * Name of the pet 
   * @return ATT_NAME
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_A_T_T_N_A_M_E)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getATTNAME() {
    return ATT_NAME;
  }


  @JsonProperty(JSON_PROPERTY_A_T_T_N_A_M_E)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setATTNAME(@javax.annotation.Nullable String ATT_NAME) {
    this.ATT_NAME = ATT_NAME;
  }


  /**
   * Return true if this Capitalization object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o, false, null, true);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Capitalization {\n");
    sb.append("    smallCamel: ").append(toIndentedString(smallCamel)).append("\n");
    sb.append("    capitalCamel: ").append(toIndentedString(capitalCamel)).append("\n");
    sb.append("    smallSnake: ").append(toIndentedString(smallSnake)).append("\n");
    sb.append("    capitalSnake: ").append(toIndentedString(capitalSnake)).append("\n");
    sb.append("    scAETHFlowPoints: ").append(toIndentedString(scAETHFlowPoints)).append("\n");
    sb.append("    ATT_NAME: ").append(toIndentedString(ATT_NAME)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `smallCamel` to the URL query string
    if (getSmallCamel() != null) {
      joiner.add(String.format("%ssmallCamel%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getSmallCamel()))));
    }

    // add `CapitalCamel` to the URL query string
    if (getCapitalCamel() != null) {
      joiner.add(String.format("%sCapitalCamel%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getCapitalCamel()))));
    }

    // add `small_Snake` to the URL query string
    if (getSmallSnake() != null) {
      joiner.add(String.format("%ssmall_Snake%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getSmallSnake()))));
    }

    // add `Capital_Snake` to the URL query string
    if (getCapitalSnake() != null) {
      joiner.add(String.format("%sCapital_Snake%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getCapitalSnake()))));
    }

    // add `SCA_ETH_Flow_Points` to the URL query string
    if (getScAETHFlowPoints() != null) {
      joiner.add(String.format("%sSCA_ETH_Flow_Points%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getScAETHFlowPoints()))));
    }

    // add `ATT_NAME` to the URL query string
    if (getATTNAME() != null) {
      joiner.add(String.format("%sATT_NAME%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getATTNAME()))));
    }

    return joiner.toString();
  }

    public static class Builder {

    private Capitalization instance;

    public Builder() {
      this(new Capitalization());
    }

    protected Builder(Capitalization instance) {
      this.instance = instance;
    }

    public Capitalization.Builder smallCamel(String smallCamel) {
      this.instance.smallCamel = smallCamel;
      return this;
    }
    public Capitalization.Builder capitalCamel(String capitalCamel) {
      this.instance.capitalCamel = capitalCamel;
      return this;
    }
    public Capitalization.Builder smallSnake(String smallSnake) {
      this.instance.smallSnake = smallSnake;
      return this;
    }
    public Capitalization.Builder capitalSnake(String capitalSnake) {
      this.instance.capitalSnake = capitalSnake;
      return this;
    }
    public Capitalization.Builder scAETHFlowPoints(String scAETHFlowPoints) {
      this.instance.scAETHFlowPoints = scAETHFlowPoints;
      return this;
    }
    public Capitalization.Builder ATT_NAME(String ATT_NAME) {
      this.instance.ATT_NAME = ATT_NAME;
      return this;
    }


    /**
    * returns a built Capitalization instance.
    *
    * The builder is not reusable.
    */
    public Capitalization build() {
      try {
        return this.instance;
      } finally {
        // ensure that this.instance is not reused
        this.instance = null;
      }
    }

    @Override
    public String toString() {
      return getClass() + "=(" + instance + ")";
    }
  }

  /**
  * Create a builder with no initialized field.
  */
  public static Capitalization.Builder builder() {
    return new Capitalization.Builder();
  }

  /**
  * Create a builder with a shallow copy of this instance.
  */
  public Capitalization.Builder toBuilder() {
    return new Capitalization.Builder()
      .smallCamel(getSmallCamel())
      .capitalCamel(getCapitalCamel())
      .smallSnake(getSmallSnake())
      .capitalSnake(getCapitalSnake())
      .scAETHFlowPoints(getScAETHFlowPoints())
      .ATT_NAME(getATTNAME());
  }

}

